package com.sainiketh.searchtypehead.batch;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sainiketh.searchtypehead.model.SearchQuery;
import com.sainiketh.searchtypehead.repository.SearchQueryRepository;

import java.util.Map;

@Component
public class BatchWriter {

    private final SearchBuffer buffer;
    private final SearchQueryRepository repository;

    public BatchWriter(
            SearchBuffer buffer,
            SearchQueryRepository repository
    ) {
        this.buffer = buffer;
        this.repository = repository;
    }

    @Scheduled(fixedRate = 10000)
    public void flush() {

        if (buffer.getBuffer().isEmpty()) {
            return;
        }

        System.out.println("Flushing batch to database...");

        for (Map.Entry<String, Long> entry :
                buffer.getBuffer().entrySet()) {

            String query = entry.getKey();
            Long count = entry.getValue();

            SearchQuery searchQuery =
                    repository.findByQueryIgnoreCase(query);

            if (searchQuery != null) {

                searchQuery.setTotalCount(
                        searchQuery.getTotalCount() + count
                );

                repository.save(searchQuery);

            } else {

                repository.save(
                        new SearchQuery(query, count)
                );
            }
        }

        buffer.clear();
    }
}