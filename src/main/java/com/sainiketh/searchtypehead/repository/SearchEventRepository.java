package com.sainiketh.searchtypehead.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sainiketh.searchtypehead.model.SearchEvent;

import java.time.LocalDateTime;
import java.util.List;

public interface SearchEventRepository
        extends JpaRepository<SearchEvent, Long> {

    @Query("SELECT e.query, COUNT(e) FROM SearchEvent e WHERE e.searchedAt > :cutoff GROUP BY e.query")
    List<Object[]> countRecentSearches(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT e.query FROM SearchEvent e WHERE e.searchedAt > :cutoff GROUP BY e.query ORDER BY COUNT(e) DESC")
    List<String> findTrendingSearches(@Param("cutoff") LocalDateTime cutoff, Pageable pageable);
}