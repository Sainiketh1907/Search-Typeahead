package com.sainiketh.searchtypehead.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sainiketh.searchtypehead.model.SearchQuery;
import com.sainiketh.searchtypehead.repository.SearchQueryRepository;
import com.sainiketh.searchtypehead.service.CacheService;

import java.util.Comparator;
import java.util.List;

@RestController
public class SuggestionController {

    private final SearchQueryRepository repository;
    private final CacheService cacheService;

    public SuggestionController(
            SearchQueryRepository repository,
            CacheService cacheService
    ) {
        this.repository = repository;
        this.cacheService = cacheService;
    }

    @SuppressWarnings("unchecked")
@GetMapping("/suggest")
    public List<SearchQuery> suggest(
            @RequestParam String q
    ) {

        Object cachedResult = cacheService.get(q);

        if(cachedResult != null) {
            return (List<SearchQuery>) cachedResult;
        }

        List<SearchQuery> suggestions =
                repository.findAll()
                        .stream()
                        .filter(item ->
                                item.getQuery()
                                        .toLowerCase()
                                        .startsWith(q.toLowerCase()))
                        .sorted(
                                Comparator.comparing(
                                        SearchQuery::getTotalCount
                                ).reversed()
                        )
                        .limit(10)
                        .toList();

        cacheService.put(q, suggestions);

        return suggestions;
    }
}