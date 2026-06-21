package com.sainiketh.searchtypehead;

import com.sainiketh.searchtypehead.batch.BatchWriter;
import com.sainiketh.searchtypehead.batch.SearchBuffer;
import com.sainiketh.searchtypehead.cache.CacheNode;
import com.sainiketh.searchtypehead.cache.ConsistentHashing;
import com.sainiketh.searchtypehead.controller.SearchController;
import com.sainiketh.searchtypehead.controller.SuggestionController;
import com.sainiketh.searchtypehead.controller.TrendingController;
import com.sainiketh.searchtypehead.model.SearchEvent;
import com.sainiketh.searchtypehead.model.SearchQuery;
import com.sainiketh.searchtypehead.repository.SearchEventRepository;
import com.sainiketh.searchtypehead.repository.SearchQueryRepository;
import com.sainiketh.searchtypehead.service.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class SearchTypeaheadRequirementsTest {

    @Autowired
    private SuggestionController suggestionController;

    @Autowired
    private SearchController searchController;

    @Autowired
    private TrendingController trendingController;

    @Autowired
    private SearchQueryRepository searchQueryRepository;

    @Autowired
    private SearchEventRepository searchEventRepository;

    @Autowired
    private SearchBuffer searchBuffer;

    @Autowired
    private BatchWriter batchWriter;

    @Autowired
    private ConsistentHashing consistentHashing;

    @Autowired
    private CacheService cacheService;

    @BeforeEach
    void setUp() {
        searchBuffer.clear();
        searchQueryRepository.deleteAll();
        searchEventRepository.deleteAll();
        for (CacheNode node : consistentHashing.getAllNodes()) {
            node.clear();
        }
    }

    @Test
    void testTypeaheadSuggestions_Functional() {
        // Pre-populate some query counts
        searchQueryRepository.save(new SearchQuery("apple iphone", 100L));
        searchQueryRepository.save(new SearchQuery("apple macbook", 150L));
        searchQueryRepository.save(new SearchQuery("apple watch", 80L));

        // 1. Basic sorting by count descending
        List<SearchQuery> suggestions = suggestionController.suggest("apple", "basic");
        assertEquals(3, suggestions.size());
        assertEquals("apple macbook", suggestions.get(0).getQuery());
        assertEquals("apple iphone", suggestions.get(1).getQuery());
        assertEquals("apple watch", suggestions.get(2).getQuery());

        // 2. Limit to 10
        // Insert 15 matching items starting with "cherry"
        for (int i = 0; i < 15; i++) {
            searchQueryRepository.save(new SearchQuery("cherry airpods " + i, 10L + i));
        }

        // We expect exactly 10 suggestions starting with "cherry"
        List<SearchQuery> limitSuggestions = suggestionController.suggest("cherry", "basic");
        assertEquals(10, limitSuggestions.size(), "Suggestions should return at most 10 suggestions");

        // 3. Edge Cases: case-insensitivity
        List<SearchQuery> mixedCaseSuggestions = suggestionController.suggest("ApPlE", "basic");
        assertFalse(mixedCaseSuggestions.isEmpty());
        assertTrue(mixedCaseSuggestions.stream().allMatch(s -> s.getQuery().toLowerCase().startsWith("apple")));

        // 4. Edge Cases: no matches
        List<SearchQuery> noMatchSuggestions = suggestionController.suggest("xyz", "basic");
        assertTrue(noMatchSuggestions.isEmpty());
    }

    @Test
    void testSearchSubmission_Functional() {
        // Submit search
        Map<String, String> response = searchController.search("macbook pro");
        assertEquals("Searched", response.get("message"));

        // Verify search event was recorded immediately
        List<SearchEvent> events = searchEventRepository.findAll();
        assertEquals(1, events.size());
        assertEquals("macbook pro", events.get(0).getQuery());

        // Verify query was added to the buffer (not yet written to database)
        assertTrue(searchBuffer.getBuffer().containsKey("macbook pro"));
        SearchQuery queryInDb = searchQueryRepository.findByQueryIgnoreCase("macbook pro");
        assertNull(queryInDb, "Should not be written to DB synchronously");

        // Flush batch and check if it is written to the DB
        batchWriter.flush();
        queryInDb = searchQueryRepository.findByQueryIgnoreCase("macbook pro");
        assertNotNull(queryInDb);
        assertEquals(1L, queryInDb.getTotalCount());
    }

    @Test
    void testConsistentHashing() {
        String key1 = "apple";
        String key2 = "banana";

        CacheNode node1a = consistentHashing.getNode(key1);
        CacheNode node1b = consistentHashing.getNode(key1);
        CacheNode node2 = consistentHashing.getNode(key2);

        assertNotNull(node1a);
        assertNotNull(node2);
        assertEquals(node1a.getNodeName(), node1b.getNodeName(), "Same key must consistently hash to the same node");
    }

    @Test
    void testBatchWrites() {
        // Record several searches for the same query
        searchBuffer.addSearch("macbook");
        searchBuffer.addSearch("macbook");
        searchBuffer.addSearch("iphone");
        searchBuffer.addSearch("macbook");

        assertEquals(3L, searchBuffer.getBuffer().get("macbook"));
        assertEquals(1L, searchBuffer.getBuffer().get("iphone"));

        // Flush batch
        batchWriter.flush();

        // Check database
        SearchQuery macbookQuery = searchQueryRepository.findByQueryIgnoreCase("macbook");
        SearchQuery iphoneQuery = searchQueryRepository.findByQueryIgnoreCase("iphone");

        assertNotNull(macbookQuery);
        assertEquals(3L, macbookQuery.getTotalCount());

        assertNotNull(iphoneQuery);
        assertEquals(1L, iphoneQuery.getTotalCount());

        // Verify buffer is cleared
        assertTrue(searchBuffer.getBuffer().isEmpty());
    }

    @Test
    void testCacheDebugEndpointExists() {
        Map<String, Object> debugInfo = suggestionController.cacheDebug("apple");
        assertEquals("apple", debugInfo.get("prefix"));
        assertNotNull(debugInfo.get("responsibleNode"));
        assertNotNull(debugInfo.get("isHit"));
    }

    @Test
    void testRecencyAwareRanking() {
        // Pre-populate some queries with low popularity
        searchQueryRepository.save(new SearchQuery("apple macbook", 10L));
        searchQueryRepository.save(new SearchQuery("apple iphone", 100L)); // iPhone has high historical popularity

        // In Basic mode, "apple iphone" must be first
        List<SearchQuery> basicSuggestions = suggestionController.suggest("apple", "basic");
        assertEquals("apple iphone", basicSuggestions.get(0).getQuery());

        // Now, record search events for "apple macbook" (simulates recent activity)
        searchEventRepository.save(new SearchEvent("apple macbook", LocalDateTime.now()));
        searchEventRepository.save(new SearchEvent("apple macbook", LocalDateTime.now()));

        // In Enhanced mode, "apple macbook" must jump to the top due to recent search events
        List<SearchQuery> enhancedSuggestions = suggestionController.suggest("apple", "enhanced");
        assertEquals("apple macbook", enhancedSuggestions.get(0).getQuery(), 
                "Enhanced mode should prioritize recently searched queries");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCacheExpiryTTL() throws Exception {
        searchQueryRepository.save(new SearchQuery("apple macbook", 10L));

        // 1. Initial query to populate cache
        suggestionController.suggest("apple", "basic");

        String cacheKey = "basic:apple";
        CacheNode node = consistentHashing.getNode(cacheKey);
        assertTrue(node.contains(cacheKey), "Cache should contain the entry after query");

        // 2. Manipulate timestamp via reflection to simulate age > 60 seconds
        Field storageField = CacheNode.class.getDeclaredField("storage");
        storageField.setAccessible(true);
        Map<String, ?> storage = (Map<String, ?>) storageField.get(node);
        Object cachedValueObj = storage.get(cacheKey);
        assertNotNull(cachedValueObj);

        Field createdAtField = cachedValueObj.getClass().getDeclaredField("createdAt");
        createdAtField.setAccessible(true);
        // Set creation time to 70 seconds ago
        createdAtField.set(cachedValueObj, System.currentTimeMillis() - 70000);

        // 3. Query suggest again. Expiry check should trigger eviction and return false in contains
        assertFalse(node.contains(cacheKey), "Cache entry should be evicted after TTL expires");
        assertNull(node.get(cacheKey), "Evicted cache entry must return null");
    }

    @Test
    void testPerformanceSimulationAndMetrics() {
        System.out.println("Starting 100,000 query dataset performance simulation...");

        // 1. Generate 100k queries and insert in batches
        long startTime = System.currentTimeMillis();
        List<SearchQuery> queries = new ArrayList<>();
        for (int i = 0; i < 100000; i++) {
            queries.add(new SearchQuery("product_" + i, (long) (Math.random() * 1000)));
            if (queries.size() >= 10000) {
                searchQueryRepository.saveAll(queries);
                queries.clear();
            }
        }
        if (!queries.isEmpty()) {
            searchQueryRepository.saveAll(queries);
        }
        long loadTime = System.currentTimeMillis() - startTime;
        System.out.println("Loaded 100,000 queries in " + loadTime + " ms.");

        // Add a few common prefix items
        searchQueryRepository.save(new SearchQuery("test_iphone 15", 5000L));
        searchQueryRepository.save(new SearchQuery("test_iphone 14", 4000L));
        searchQueryRepository.save(new SearchQuery("test_iphone 13", 3000L));
        searchQueryRepository.save(new SearchQuery("test_iphone 12", 2000L));
        searchQueryRepository.save(new SearchQuery("test_iphone 11", 1000L));

        // 2. Perform mock request workload to measure latency and cache hits
        int requestsCount = 1000;
        List<Long> latencies = new ArrayList<>();
        int cacheHits = 0;
        int cacheMisses = 0;

        // Warm up and pre-cache some queries
        suggestionController.suggest("test_iphone", "enhanced");

        for (int i = 0; i < requestsCount; i++) {
            String q;
            if (i % 5 == 0) {
                q = "test_iphone"; // Should hit cache after first request
            } else {
                q = "product_" + (i % 500); // Varied prefixes to query DB / cache
            }

            // Determine if hit or miss based on whether the node currently contains the key
            String cacheKey = "enhanced:" + q.toLowerCase();
            boolean isHit = consistentHashing.getNode(cacheKey).contains(cacheKey);
            if (isHit) {
                cacheHits++;
            } else {
                cacheMisses++;
            }

            long reqStart = System.nanoTime();
            suggestionController.suggest(q, "enhanced");
            long reqEnd = System.nanoTime();
            
            long latencyNs = reqEnd - reqStart;
            latencies.add(TimeUnit.NANOSECONDS.toMillis(latencyNs));
        }

        // Calculate statistics
        double avgLatency = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
        
        // Sort to find percentiles
        latencies.sort(Long::compare);
        long p50Latency = latencies.get((int) (requestsCount * 0.50));
        long p90Latency = latencies.get((int) (requestsCount * 0.90));
        long p95Latency = latencies.get((int) (requestsCount * 0.95));
        long p99Latency = latencies.get((int) (requestsCount * 0.99));

        double hitRate = (double) cacheHits / requestsCount * 100.0;

        System.out.println("====================================================");
        System.out.println("PERFORMANCE METRICS REPORT (100,000 Query Dataset)");
        System.out.println("====================================================");
        System.out.println("Total Requests Run: " + requestsCount);
        System.out.println("Average Latency:    " + String.format("%.2f", avgLatency) + " ms");
        System.out.println("p50 Latency (Median):" + p50Latency + " ms");
        System.out.println("p90 Latency:        " + p90Latency + " ms");
        System.out.println("p95 Latency:        " + p95Latency + " ms");
        System.out.println("p99 Latency:        " + p99Latency + " ms");
        System.out.println("Cache Hit Rate:     " + String.format("%.2f", hitRate) + "%");
        System.out.println("====================================================");
    }
}
