# Search Typeahead System

A Spring Boot application that demonstrates a search typeahead workflow with live suggestions, trending queries, search analytics, caching, consistent hashing, and batch database writes.

The app is intentionally small, but it models the kind of backend behavior used by large search and e-commerce platforms:

- Suggest results while the user types
- Rank suggestions by popularity
- Persist search queries and events
- Cache reads behind a consistent-hash ring
- Buffer writes and flush them in batches

## What it does

- Serves a browser UI at `/`
- Returns up to 10 prefix-matched suggestions from `/suggest?q=...`
- Accepts search submissions through `/search?query=...`
- Returns trending search terms from `/trending`
- Loads a small sample dataset on startup so the UI works immediately

## Features

### Typeahead suggestions

- Live prefix search while typing
- Results sorted by `totalCount` in descending order
- Top 10 results returned
- Search terms highlighted in the UI

### Search analytics

- Every submitted search is recorded as an event
- Query counts are maintained in the `search_queries` table
- Trending searches are derived from stored search events

### Cache layer

- A simple distributed cache simulation is implemented in memory
- Keys are mapped to cache nodes using consistent hashing
- Cache hit/miss behavior is visible in the application logs

### Batch writes

- Search counts are accumulated in memory first
- A scheduled writer flushes buffered counts to PostgreSQL every 10 seconds
- This reduces the number of writes for repeated searches

## Architecture

```text
Browser UI
               |
               v
Spring Boot Controllers
               |
               +--> Suggestion lookup -> Cache -> PostgreSQL
               |
               +--> Search submission -> Search event repository
               |
               +--> Trending lookup -> Search event repository
               |
               v
Batch writer + in-memory buffer
               |
               v
PostgreSQL
```

### Request flow

#### Suggestion flow

```text
User types query
               -> GET /suggest?q=...
               -> cache lookup
               -> cache hit: return cached suggestions
               -> cache miss: query database, sort, limit to 10, cache result
```

#### Search flow

```text
User submits search
               -> POST /search?query=...
               -> store search event immediately
               -> add query to buffer
               -> batch writer flushes counts to PostgreSQL later
```

## Tech Stack

| Layer | Technology |
| --- | --- |
| Language | Java 25 |
| Backend | Spring Boot |
| Persistence | Spring Data JPA |
| Database | PostgreSQL |
| Build tool | Maven |
| Frontend | HTML, CSS, JavaScript |
| Containerization | Docker, Docker Compose |

## Project Structure

```text
src/main/java/com/sainiketh/searchtypehead/
     batch/         Batch writer and in-memory buffer
     cache/         Cache node and consistent hashing ring
     config/        Sample data loader
     controller/    REST endpoints
     model/         JPA entities
     repository/    Spring Data repositories
     service/       Cache and search services

src/main/resources/
     application.properties
     static/        Browser UI
```

## Data Model

### `search_queries`

Aggregated search counts.

| Column | Type | Description |
| --- | --- | --- |
| id | BIGINT | Primary key |
| query | VARCHAR | Search phrase |
| total_count | BIGINT | Popularity count |

### `search_events`

Individual search submissions.

| Column | Type | Description |
| --- | --- | --- |
| id | BIGINT | Primary key |
| query | VARCHAR | Search phrase |
| searched_at | TIMESTAMP | Time of submission |

## API Reference

### `POST /search?query=iphone`

Records a submitted search.

Response:

```json
{
     "message": "Searched"
}
```

### `GET /suggest?q=ip`

Returns prefix-matched suggestions sorted by popularity.

Example response:

```json
[
     {
          "id": 1,
          "query": "iphone",
          "totalCount": 100000
     },
     {
          "id": 2,
          "query": "iphone 15",
          "totalCount": 85000
     },
     {
          "id": 3,
          "query": "iphone charger",
          "totalCount": 60000
     }
]
```

### `GET /trending`

Returns the most frequently searched terms.

Example response:

```json
[
     "iphone",
     "java tutorial",
     "iphone 15"
]
```

## Sample Dataset

The application automatically loads sample data on startup when the database is empty.

- iphone
- iphone 15
- iphone charger
- java tutorial

## Running the App

### Option 1: Run with Docker

This is the easiest way to run the project.

```bash
docker compose up --build
```

Then open:

```text
http://localhost:8080
```

Docker Compose starts:

- the Spring Boot app
- PostgreSQL
- Redis container included in the compose file

Note: the current application uses PostgreSQL and an in-memory consistent-hashing cache simulation. The Redis container is present in the compose stack but is not required by the app runtime.

### Option 2: Run locally without Docker

1. Start PostgreSQL locally.
2. Create the database:

```sql
CREATE DATABASE search_typeahead;
```

3. Set environment variables or update `src/main/resources/application.properties`.
4. Start the app:

```bash
./mvnw spring-boot:run
```

## Configuration

The main datasource settings are read from environment variables first, then default to PostgreSQL values.

```properties
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/search_typeahead}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:postgres}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:postgres}
spring.datasource.driver-class-name=${SPRING_DATASOURCE_DRIVER_CLASS_NAME:org.postgresql.Driver}
```

## Verification

Once the app is running, you can test it with these requests:

```bash
curl "http://localhost:8080/suggest?q=iph"
curl -X POST "http://localhost:8080/search?query=MacBook"
curl "http://localhost:8080/trending"
```

Expected behavior:

- Suggestions return matching queries
- Search submission returns `{"message":"Searched"}`
- Trending updates after searches are submitted

## Troubleshooting

- If the app cannot connect to PostgreSQL, confirm the database is running and the credentials match your environment.
- If Docker fails on startup, rebuild with `docker compose up --build` after stopping stale containers.
- If you see no suggestions, make sure the sample data has been loaded and try a prefix like `ip` or `java`.
- If you change the code and the Docker image seems stale, rebuild the containers so the JAR is regenerated.

## Author

Sai Niketh

GitHub: https://github.com/Sainiketh1907
# Search Typeahead System

A Spring Boot application that demonstrates a search typeahead workflow with live suggestions, trending queries, search analytics, caching, consistent hashing, and batch database writes.

The app is intentionally small, but it models the kind of backend behavior used by large search and e-commerce platforms:

- Suggest results while the user types
- Rank suggestions by popularity
- Persist search queries and events
- Cache reads behind a consistent-hash ring
- Buffer writes and flush them in batches

## What it does

- Serves a browser UI at `/`
- Returns up to 10 prefix-matched suggestions from `/suggest?q=...`
- Accepts search submissions through `/search?query=...`
- Returns trending search terms from `/trending`
- Loads a small sample dataset on startup so the UI works immediately

## Features

### Typeahead suggestions

- Live prefix search while typing
- Results sorted by `totalCount` in descending order
- Top 10 results returned
- Search terms highlighted in the UI

### Search analytics

- Every submitted search is recorded as an event
- Query counts are maintained in the `search_queries` table
- Trending searches are derived from stored search events

### Cache layer

- A simple distributed cache simulation is implemented in memory
- Keys are mapped to cache nodes using consistent hashing
- Cache hit/miss behavior is visible in the application logs

### Batch writes

- Search counts are accumulated in memory first
- A scheduled writer flushes buffered counts to PostgreSQL every 10 seconds
- This reduces the number of writes for repeated searches

## Architecture

```text
Browser UI
      |
      v
Spring Boot Controllers
      |
      +--> Suggestion lookup -> Cache -> PostgreSQL
      |
      +--> Search submission -> Search event repository
      |
      +--> Trending lookup -> Search event repository
      |
      v
Batch writer + in-memory buffer
      |
      v
PostgreSQL
```

### Request flow

#### Suggestion flow

```text
User types query
      -> GET /suggest?q=...
      -> cache lookup
      -> cache hit: return cached suggestions
      -> cache miss: query database, sort, limit to 10, cache result
```

#### Search flow

```text
User submits search
      -> POST /search?query=...
      -> store search event immediately
      -> add query to buffer
      -> batch writer flushes counts to PostgreSQL later
```

## Tech Stack

| Layer | Technology |
| --- | --- |
| Language | Java 25 |
| Backend | Spring Boot |
| Persistence | Spring Data JPA |
| Database | PostgreSQL |
| Build tool | Maven |
| Frontend | HTML, CSS, JavaScript |
| Containerization | Docker, Docker Compose |

## Project Structure

```text
src/main/java/com/sainiketh/searchtypehead/
     batch/         Batch writer and in-memory buffer
     cache/         Cache node and consistent hashing ring
     config/        Sample data loader
     controller/    REST endpoints
     model/         JPA entities
     repository/    Spring Data repositories
     service/       Cache and search services

src/main/resources/
     application.properties
     static/        Browser UI
```

## Data Model

### `search_queries`

Aggregated search counts.

| Column | Type | Description |
| --- | --- | --- |
| id | BIGINT | Primary key |
| query | VARCHAR | Search phrase |
| total_count | BIGINT | Popularity count |

### `search_events`

Individual search submissions.

| Column | Type | Description |
| --- | --- | --- |
| id | BIGINT | Primary key |
| query | VARCHAR | Search phrase |
| searched_at | TIMESTAMP | Time of submission |

## API Reference

### `POST /search?query=iphone`

Records a submitted search.

Response:

```json
{
     "message": "Searched"
}
```

### `GET /suggest?q=ip`

Returns prefix-matched suggestions sorted by popularity.

Example response:

```json
[
     {
          "id": 1,
          "query": "iphone",
          "totalCount": 100000
     },
     {
          "id": 2,
          "query": "iphone 15",
          "totalCount": 85000
     },
     {
          "id": 3,
          "query": "iphone charger",
          "totalCount": 60000
     }
]
```

### `GET /trending`

Returns the most frequently searched terms.

Example response:

```json
[
     "iphone",
     "java tutorial",
     "iphone 15"
]
```

## Sample Dataset

The application automatically loads sample data on startup when the database is empty.

- iphone
- iphone 15
- iphone charger
- java tutorial

## Running the App

### Option 1: Run with Docker

This is the easiest way to run the project.

```bash
docker compose up --build
```

Then open:

```text
http://localhost:8080
```

Docker Compose starts:

- the Spring Boot app
- PostgreSQL
- Redis container included in the compose file

Note: the current application uses PostgreSQL and an in-memory consistent-hashing cache simulation. The Redis container is present in the compose stack but is not required by the app runtime.

### Option 2: Run locally without Docker

1. Start PostgreSQL locally.
2. Create the database:

```sql
CREATE DATABASE search_typeahead;
```

3. Set environment variables or update `src/main/resources/application.properties`.
4. Start the app:

```bash
./mvnw spring-boot:run
```

## Configuration

The main datasource settings are read from environment variables first, then default to PostgreSQL values.

```properties
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/search_typeahead}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:postgres}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:postgres}
spring.datasource.driver-class-name=${SPRING_DATASOURCE_DRIVER_CLASS_NAME:org.postgresql.Driver}
```

## Verification

Once the app is running, you can test it with these requests:

```bash
curl "http://localhost:8080/suggest?q=iph"
curl -X POST "http://localhost:8080/search?query=MacBook"
curl "http://localhost:8080/trending"
```

Expected behavior:

- Suggestions return matching queries
- Search submission returns `{"message":"Searched"}`
- Trending updates after searches are submitted

## Troubleshooting

- If the app cannot connect to PostgreSQL, confirm the database is running and the credentials match your environment.
- If Docker fails on startup, rebuild with `docker compose up --build` after stopping stale containers.
- If you see no suggestions, make sure the sample data has been loaded and try a prefix like `ip` or `java`.
- If you change the code and the Docker image seems stale, rebuild the containers so the JAR is regenerated.

