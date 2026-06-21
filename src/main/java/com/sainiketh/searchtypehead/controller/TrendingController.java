package com.sainiketh.searchtypehead.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sainiketh.searchtypehead.model.SearchEvent;
import com.sainiketh.searchtypehead.repository.SearchEventRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class TrendingController {

    private final SearchEventRepository repository;

    public TrendingController(SearchEventRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/trending")
    public List<String> trending() {

        List<SearchEvent> events = repository.findAll();

        Map<String, Long> counts =
                events.stream()
                        .collect(Collectors.groupingBy(
                                SearchEvent::getQuery,
                                Collectors.counting()
                        ));

        return counts.entrySet()
                .stream()
                .sorted((a, b) ->
                        Long.compare(
                                b.getValue(),
                                a.getValue()
                        ))
                .limit(10)
                .map(Map.Entry::getKey)
                .toList();
    }
}