package com.sainiketh.searchtypehead.model;

import jakarta.persistence.*;

@Entity
@Table(name = "search_queries")
public class SearchQuery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String query;

    private Long totalCount;

    public SearchQuery() {
    }

    public SearchQuery(String query, Long totalCount) {
        this.query = query;
        this.totalCount = totalCount;
    }

    public Long getId() {
        return id;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }
}