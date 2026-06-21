package com.sainiketh.searchtypehead.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sainiketh.searchtypehead.model.SearchQuery;

import java.util.List;

public interface SearchQueryRepository extends JpaRepository<SearchQuery, Long> {

    List<SearchQuery>
    findByQueryStartingWithIgnoreCaseOrderByTotalCountDesc(
            String prefix,
            Pageable pageable
    );

    SearchQuery findByQueryIgnoreCase(String query);
}