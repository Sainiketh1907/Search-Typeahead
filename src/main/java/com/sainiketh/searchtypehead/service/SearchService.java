package com.sainiketh.searchtypehead.service;

import org.springframework.stereotype.Service;

import com.sainiketh.searchtypehead.batch.SearchBuffer;
import com.sainiketh.searchtypehead.model.SearchEvent;
import com.sainiketh.searchtypehead.repository.SearchEventRepository;

import java.time.LocalDateTime;

@Service
public class SearchService {

    private final SearchBuffer searchBuffer;
    private final SearchEventRepository eventRepository;

    public SearchService(
            SearchBuffer searchBuffer,
            SearchEventRepository eventRepository
    ) {
        this.searchBuffer = searchBuffer;
        this.eventRepository = eventRepository;
    }

    public void recordSearch(String query) {

        // Add to batch buffer
        searchBuffer.addSearch(query);

        // Save search event immediately
        SearchEvent event =
                new SearchEvent(
                        query,
                        LocalDateTime.now()
                );

        eventRepository.save(event);
    }
}