package com.sainiketh.searchtypehead.cache;

import java.util.HashMap;
import java.util.Map;

public class CacheNode {

    private static final long TTL_MS = 60000; // 60 seconds TTL

    private final String nodeName;

    private final Map<String, CachedValue> storage =
            new HashMap<>();

    public CacheNode(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void put(String key, Object value) {
        storage.put(key, new CachedValue(value));
    }

    public Object get(String key) {
        CachedValue cachedValue = storage.get(key);
        if (cachedValue == null) {
            return null;
        }
        if (cachedValue.isExpired(TTL_MS)) {
            storage.remove(key);
            return null;
        }
        return cachedValue.getValue();
    }

    public boolean contains(String key) {
        CachedValue cachedValue = storage.get(key);
        if (cachedValue == null) {
            return false;
        }
        if (cachedValue.isExpired(TTL_MS)) {
            storage.remove(key);
            return false;
        }
        return true;
    }

    public void clear() {
        storage.clear();
    }

    private static class CachedValue {
        private final Object value;
        private final long createdAt;

        public CachedValue(Object value) {
            this.value = value;
            this.createdAt = System.currentTimeMillis();
        }

        public Object getValue() {
            return value;
        }

        public boolean isExpired(long ttlMs) {
            return (System.currentTimeMillis() - createdAt) > ttlMs;
        }
    }
}