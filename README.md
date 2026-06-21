# 🚀 Search Typeahead System

## 📖 Overview

The **Search Typeahead System** is a scalable backend application that provides real-time search suggestions, tracks user search behavior, identifies trending searches, and optimizes performance using **caching**, **consistent hashing**, and **batch database writes**.

The project is inspired by the architecture used in large-scale systems such as Google Search, Amazon Search, and YouTube Search Suggestions.

---

## ✨ Features

### 🔍 Typeahead Search Suggestions

* Real-time search suggestions
* Prefix-based matching
* Top 10 suggestions returned
* Results ranked by popularity

### 📈 Search Popularity Tracking

* Every search updates query popularity
* Frequently searched queries appear first

### 🔥 Trending Searches

* Tracks individual search events
* Returns the most popular searches

### ⚡ Distributed Cache Simulation

* Multiple cache nodes
* Consistent hashing for key distribution
* Cache hit/miss tracking

### 🗄️ PostgreSQL Persistence

* Stores search queries
* Stores search events
* Maintains search counts

### 🚀 Batch Writes

* Search requests buffered in memory
* Periodic flush to database
* Reduces database write load

---

# 🏗️ System Architecture

```text
                        ┌──────────────┐
                        │    Client    │
                        └──────┬───────┘
                               │
                               ▼
                    ┌────────────────────┐
                    │ Spring Boot APIs   │
                    └──────┬─────┬───────┘
                           │     │
                           │     │
                           ▼     ▼
               ┌─────────────────────────┐
               │ Distributed Cache Layer │
               │ (Consistent Hash Ring)  │
               └───────────┬─────────────┘
                           │
                           ▼
                   ┌──────────────┐
                   │ PostgreSQL   │
                   └──────────────┘

                           ▲
                           │
                    ┌────────────┐
                    │ Batch      │
                    │ Writer     │
                    └─────┬──────┘
                          │
                    ┌─────▼─────┐
                    │ Search    │
                    │ Buffer    │
                    └───────────┘
```

---

# 🧠 Design Decisions

## Consistent Hashing

The cache layer uses consistent hashing to distribute keys among cache nodes.

### Benefits

* Better scalability
* Uniform key distribution
* Minimal key movement when nodes are added or removed
* Reduced cache misses

---

## Batch Writes

Instead of writing every search directly to PostgreSQL:

### Traditional Approach

```text
Search → Database Write
Search → Database Write
Search → Database Write
```

### Implemented Approach

```text
Search → Buffer
Search → Buffer
Search → Buffer

Every 10 Seconds

Buffer → Batch Write → PostgreSQL
```

### Benefits

* Fewer database operations
* Improved throughput
* Better performance under load

---

# 🗃️ Database Schema

## search_queries

Stores aggregated search counts.

| Column      | Type    |
| ----------- | ------- |
| id          | BIGINT  |
| query       | VARCHAR |
| total_count | BIGINT  |

---

## search_events

Stores individual searches.

| Column      | Type      |
| ----------- | --------- |
| id          | BIGINT    |
| query       | VARCHAR   |
| searched_at | TIMESTAMP |

---

# 📡 API Documentation

## 1. Submit Search

### Request

```http
POST /search?query=iphone
```

### Response

```json
{
  "message": "Searched"
}
```

---

## 2. Get Suggestions

### Request

```http
GET /suggest?q=ip
```

### Response

```json
[
  {
    "id": 1,
    "query": "iphone",
    "totalCount": 100005
  },
  {
    "id": 2,
    "query": "iphone 15",
    "totalCount": 85000
  }
]
```

---

## 3. Get Trending Searches

### Request

```http
GET /trending
```

### Response

```json
[
  "iphone",
  "ipl",
  "chatgpt"
]
```

---

# 📊 Project Workflow

## Search Flow

```text
User Search
     │
     ▼
POST /search
     │
     ▼
Search Buffer
     │
     ▼
Batch Writer
     │
     ▼
PostgreSQL
```

---

## Suggestion Flow

```text
User Request
     │
     ▼
GET /suggest
     │
     ▼
Cache Lookup
     │
 ┌───┴────┐
 │        │
 ▼        ▼
Hit      Miss
 │         │
 ▼         ▼
Return   PostgreSQL
            │
            ▼
      Store In Cache
            │
            ▼
         Return
```

---

# 🧪 Sample Dataset

The application automatically loads sample data on startup.

```text
iphone
iphone 15
iphone charger
java tutorial
```

---

# 🛠️ Technology Stack

| Technology      | Purpose               |
| --------------- | --------------------- |
| Java 25         | Programming Language  |
| Spring Boot     | Backend Framework     |
| Spring Data JPA | ORM Layer             |
| PostgreSQL      | Database              |
| Maven           | Dependency Management |
| Git & GitHub    | Version Control       |

---

# ⚙️ Setup Instructions

## Clone Repository

```bash
git clone https://github.com/Sainiketh1907/search-typeahead.git
```

---

## Create Database

```sql
CREATE DATABASE search_typeahead;
```

---

## Configure Database

Update:

```properties
src/main/resources/application.properties
```

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/search_typeahead
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
```

---

## Run Application

```bash
./mvnw spring-boot:run
```

Application starts on:

```text
http://localhost:8080
```

---

# 📈 Performance Optimizations

✅ Cache Layer

✅ Consistent Hashing

✅ Search Buffering

✅ Batch Writes

✅ Popularity-Based Ranking

✅ Event Logging

---

# 🎯 Key Learnings

* System Design Fundamentals
* Consistent Hashing
* Caching Strategies
* Batch Processing
* Database Optimization
* Spring Boot Architecture
* API Design
* Scalability Concepts

---

# 👨‍💻 Author

**Sai Niketh**

Scaler School of Technology

GitHub: https://github.com/Sainiketh1907
