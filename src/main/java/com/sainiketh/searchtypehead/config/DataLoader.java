package com.sainiketh.searchtypehead.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.sainiketh.searchtypehead.model.SearchQuery;
import com.sainiketh.searchtypehead.repository.SearchQueryRepository;

@Component
public class DataLoader implements CommandLineRunner {

    private final SearchQueryRepository repository;

    public DataLoader(SearchQueryRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {

        if(repository.count() == 0) {

            repository.save(new SearchQuery("iphone", 100000L));
            repository.save(new SearchQuery("iphone 15", 85000L));
            repository.save(new SearchQuery("iphone charger", 60000L));
            repository.save(new SearchQuery("java tutorial", 40000L));

            System.out.println("Sample data loaded!");
        }
    }
}