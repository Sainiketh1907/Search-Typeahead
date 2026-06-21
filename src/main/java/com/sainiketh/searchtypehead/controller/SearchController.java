package com.sainiketh.searchtypehead.controller;

import org.springframework.web.bind.annotation.*;

import com.sainiketh.searchtypehead.service.SearchService;

import java.util.Map;

@RestController
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping("/search")
    public Map<String, String> search(
            @RequestParam String query
    ) {

        searchService.recordSearch(query);

        return Map.of(
                "message",
                "Searched"
        );
    }
}