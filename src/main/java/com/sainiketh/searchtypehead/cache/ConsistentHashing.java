package com.sainiketh.searchtypehead.cache;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

@Component
public class ConsistentHashing {

    private final SortedMap<Integer, CacheNode> ring =
            new TreeMap<>();

    public ConsistentHashing() {

        addNode(new CacheNode("Node-1"));
        addNode(new CacheNode("Node-2"));
        addNode(new CacheNode("Node-3"));
    }

    private void addNode(CacheNode node) {
        ring.put(node.getNodeName().hashCode(), node);
    }

    public CacheNode getNode(String key) {

        if (ring.isEmpty()) {
            return null;
        }

        int hash = key.hashCode();

        if (!ring.containsKey(hash)) {

            SortedMap<Integer, CacheNode> tailMap =
                    ring.tailMap(hash);

            hash = tailMap.isEmpty()
                    ? ring.firstKey()
                    : tailMap.firstKey();
        }

        return ring.get(hash);
    }

    public Collection<CacheNode> getAllNodes() {
        return ring.values();
    }
}