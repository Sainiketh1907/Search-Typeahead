package com.sainiketh.searchtypehead.batch;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class SearchBuffer {

    private final ConcurrentHashMap<String, Long> buffer =
            new ConcurrentHashMap<>();

    public void addSearch(String query) {

        buffer.merge(query, 1L, Long::sum);
    }

    public ConcurrentHashMap<String, Long> getBuffer() {
        return buffer;
    }

    public void clear() {
        buffer.clear();
    }
}