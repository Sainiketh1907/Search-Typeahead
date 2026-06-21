package com.sainiketh.searchtypehead.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import com.sainiketh.searchtypehead.model.SearchQuery;
import com.sainiketh.searchtypehead.repository.SearchQueryRepository;
import com.sainiketh.searchtypehead.repository.SearchEventRepository;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class DataLoader implements CommandLineRunner {

    private final SearchQueryRepository repository;
    private final SearchEventRepository searchEventRepository;

    @Value("${app.force-seed:false}")
    private boolean forceSeed;

    public DataLoader(SearchQueryRepository repository, SearchEventRepository searchEventRepository) {
        this.repository = repository;
        this.searchEventRepository = searchEventRepository;
    }

    @Override
    public void run(String... args) {

        if (forceSeed) {
            System.out.println("FORCE_SEED is enabled. Truncating database tables...");
            searchEventRepository.deleteAllInBatch();
            repository.deleteAllInBatch();
        }

        if (forceSeed || repository.count() == 0) {
            long startTime = System.currentTimeMillis();
            org.springframework.core.io.ClassPathResource resource = 
                    new org.springframework.core.io.ClassPathResource("unigram_freq.csv");

            if (resource.exists()) {
                System.out.println("Found Google Web Corpus dataset in classpath resources!");
                System.out.println("Starting import of unigram_freq.csv dataset...");
                
                try (BufferedReader br = new BufferedReader(new java.io.InputStreamReader(resource.getInputStream()))) {
                    String line = br.readLine(); // skip header
                    List<SearchQuery> batch = new ArrayList<>();
                    Set<String> seen = new HashSet<>();
                    int loadedCount = 0;

                    while ((line = br.readLine()) != null) {
                        String[] parts = line.split(",");
                        if (parts.length >= 2) {
                            String word = parts[0].trim();
                            String lowerWord = word.toLowerCase();
                            try {
                                long countVal = Long.parseLong(parts[1].trim());
                                if (!word.isEmpty() && !seen.contains(lowerWord)) {
                                    seen.add(lowerWord);
                                    batch.add(new SearchQuery(word, countVal));

                                    if (batch.size() >= 10000) {
                                        repository.saveAll(batch);
                                        batch.clear();
                                    }
                                    loadedCount++;
                                }
                            } catch (NumberFormatException e) {
                                // skip malformed numbers
                            }
                        }
                    }
                    if (!batch.isEmpty()) {
                        repository.saveAll(batch);
                    }
                    long endTime = System.currentTimeMillis();
                    System.out.println("Successfully seeded " + loadedCount + " queries from Google Web Corpus in " + (endTime - startTime) + " ms!");
                    return;
                } catch (IOException e) {
                    System.err.println("Error reading unigram_freq.csv, falling back to generated dataset: " + e.getMessage());
                }
            }

            // Fallback to generated technical terms dataset
            System.out.println("Starting seeding of 100,000 generated queries...");
            List<SearchQuery> batch = new ArrayList<>();
            batch.add(new SearchQuery("iphone", 100000L));
            batch.add(new SearchQuery("iphone 15", 85000L));
            batch.add(new SearchQuery("iphone charger", 60000L));
            batch.add(new SearchQuery("java tutorial", 40000L));

            String[] tech = {"java", "python", "spring", "docker", "kubernetes", "javascript", "react", "html", "css", "sql", "postgres", "git", "maven", "aws", "cloud", "rust", "go", "linux", "c++", "typescript", "angular", "vue", "redis", "mongodb", "kafka", "spark", "graphql", "rest api"};
            String[] topic = {"tutorial", "course", "basics", "advanced", "interview questions", "best practices", "examples", "performance", "deployment", "guide", "framework", "library", "design patterns", "architecture", "testing", "security", "optimization"};
            String[] suffix = {"for beginners", "in 2026", "step by step", "with examples", "complete guide", "made easy", "in 10 minutes", "crash course", "cheatsheet", "from scratch"};

            int count = 0;
            outer:
            for (String t : tech) {
                for (String tp : topic) {
                    for (String s : suffix) {
                        for (int i = 1; i <= 21; i++) {
                            String queryText = t + " " + tp + " " + s + " " + i;
                            long countVal = (long) (Math.random() * 5000) + 1;
                            batch.add(new SearchQuery(queryText, countVal));

                            if (batch.size() >= 5000) {
                                repository.saveAll(batch);
                                batch.clear();
                            }
                            count++;
                            if (count >= 100000) {
                                break outer;
                            }
                        }
                    }
                }
            }

            if (!batch.isEmpty()) {
                repository.saveAll(batch);
            }

            long endTime = System.currentTimeMillis();
            System.out.println("Successfully seeded " + repository.count() + " queries in " + (endTime - startTime) + " ms!");
        }
    }
}