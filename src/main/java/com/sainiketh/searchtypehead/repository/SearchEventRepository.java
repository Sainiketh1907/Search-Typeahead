package com.sainiketh.searchtypehead.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sainiketh.searchtypehead.model.SearchEvent;

public interface SearchEventRepository
        extends JpaRepository<SearchEvent, Long> {
}