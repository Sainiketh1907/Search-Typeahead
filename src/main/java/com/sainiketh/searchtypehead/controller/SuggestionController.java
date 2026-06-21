package com.sainiketh.searchtypehead.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sainiketh.searchtypehead.model.SearchQuery;
import com.sainiketh.searchtypehead.repository.SearchQueryRepository;
import com.sainiketh.searchtypehead.repository.SearchEventRepository;
import com.sainiketh.searchtypehead.service.CacheService;
import com.sainiketh.searchtypehead.cache.ConsistentHashing;
import com.sainiketh.searchtypehead.cache.CacheNode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class SuggestionController {

    private final SearchQueryRepository repository;
    private final SearchEventRepository searchEventRepository;
    private final CacheService cacheService;
    private final ConsistentHashing consistentHashing;

    public SuggestionController(
            SearchQueryRepository repository,
            SearchEventRepository searchEventRepository,
            CacheService cacheService,
            ConsistentHashing consistentHashing
    ) {
        this.repository = repository;
        this.searchEventRepository = searchEventRepository;
        this.cacheService = cacheService;
        this.consistentHashing = consistentHashing;
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/suggest")
    public List<SearchQuery> suggest(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false, defaultValue = "enhanced") String mode
    ) {
        if (q == null || q.trim().isEmpty()) {
            return List.of();
        }

        String cacheKey = mode.toLowerCase() + ":" + q.trim().toLowerCase();
        Object cachedResult = cacheService.get(cacheKey);

        if (cachedResult != null) {
            return (List<SearchQuery>) cachedResult;
        }

        List<SearchQuery> suggestions;

        if ("basic".equalsIgnoreCase(mode)) {
            // Basic Mode: Query database and sort by totalCount descending, limit 10
            suggestions = repository.findByQueryStartingWithIgnoreCaseOrderByTotalCountDesc(
                    q.trim(),
                    PageRequest.of(0, 10)
            );
        } else {
            // Enhanced Mode: Query top 50 prefix-matched items
            List<SearchQuery> candidates = repository.findByQueryStartingWithIgnoreCaseOrderByTotalCountDesc(
                    q.trim(),
                    PageRequest.of(0, 50)
            );

            // Fetch recent event counts (last 2 minutes) to compute recency score
            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(2);
            List<Object[]> recentCountsList = searchEventRepository.countRecentSearches(cutoff);
            Map<String, Long> recentCountsMap = recentCountsList.stream()
                    .collect(Collectors.toMap(
                            row -> ((String) row[0]).toLowerCase(),
                            row -> (Long) row[1],
                            Long::sum
                    ));

            // Rank candidates by combining popularity and recency frequency
            suggestions = candidates.stream()
                    .sorted((a, b) -> {
                        long countA = a.getTotalCount();
                        long recentA = recentCountsMap.getOrDefault(a.getQuery().toLowerCase(), 0L);
                        long scoreA = countA + (recentA * 5000);

                        long countB = b.getTotalCount();
                        long recentB = recentCountsMap.getOrDefault(b.getQuery().toLowerCase(), 0L);
                        long scoreB = countB + (recentB * 5000);

                        return Long.compare(scoreB, scoreA); // descending score
                    })
                    .limit(10)
                    .toList();
        }

        cacheService.put(cacheKey, suggestions);
        return suggestions;
    }

    @GetMapping("/cache/debug")
    public Map<String, Object> cacheDebug(
            @RequestParam String prefix
    ) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return Map.of("error", "Prefix cannot be empty");
        }

        String searchKey = "enhanced:" + prefix.trim().toLowerCase();
        CacheNode node = consistentHashing.getNode(searchKey);

        boolean isHit = false;
        if (node != null) {
            // Check both mode keys on the responsible node
            isHit = node.contains("enhanced:" + prefix.trim().toLowerCase()) 
                    || node.contains("basic:" + prefix.trim().toLowerCase());
        }

        return Map.of(
                "prefix", prefix,
                "responsibleNode", node != null ? node.getNodeName() : "None",
                "isHit", isHit
        );
    }
}