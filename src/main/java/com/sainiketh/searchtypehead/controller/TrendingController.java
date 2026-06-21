package com.sainiketh.searchtypehead.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sainiketh.searchtypehead.repository.SearchEventRepository;

import java.time.LocalDateTime;
import java.util.List;

@RestController
public class TrendingController {

    private final SearchEventRepository repository;

    public TrendingController(SearchEventRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/trending")
    public List<String> trending() {
        // Query top 10 queries within the last 1 hour
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
        return repository.findTrendingSearches(cutoff, PageRequest.of(0, 10));
    }
}