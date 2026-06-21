package com.sainiketh.searchtypehead.cache;

import java.util.HashMap;
import java.util.Map;

public class CacheNode {

    private final String nodeName;

    private final Map<String, Object> storage =
            new HashMap<>();

    public CacheNode(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void put(String key, Object value) {
        storage.put(key, value);
    }

    public Object get(String key) {
        return storage.get(key);
    }

    public boolean contains(String key) {
        return storage.containsKey(key);
    }
}