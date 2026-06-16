# serpcheap-spring-boot-starter

Spring Boot starter for the [serp.cheap](https://serp.cheap) **Google Search API** — auto-configures a `SerpCheap` client from `application.properties`, with real-time Google SERP data (organic results, ads, knowledge graph, page scraping, rank tracking) and optional `@Cacheable` result caching.

It's the **cheapest Google Search API** we know of — $0.0003 per cached search, $0.0006 fresh, no monthly minimum (~10× cheaper than SerpApi).

## Install

```xml
<dependency>
  <groupId>cheap.serp</groupId>
  <artifactId>serpcheap-spring-boot-starter</artifactId>
  <version>0.1.0</version>
</dependency>
```

Set your API key (get one at [app.serp.cheap](https://app.serp.cheap)):

```properties
serpcheap.api-key=${SERPCHEAP_API_KEY}
```

## Usage

The starter auto-configures a `SerpCheap` bean — inject it anywhere:

```java
import cheap.serp.serpcheap.SerpCheap;
import cheap.serp.serpcheap.SearchParams;

@Service
public class SearchService {
  private final SerpCheap serpcheap;

  public SearchService(SerpCheap serpcheap) {
    this.serpcheap = serpcheap;
  }

  public SearchResponse run(String query) {
    return serpcheap.search(SearchParams.of(query));
  }
}
```

### Cached client

If your app has a `CacheManager` (i.e. `@EnableCaching` + a cache provider), the starter also exposes a `SerpCheapClient` that caches `search`/`scrape`/`rank` results in your cache (cache name `serpcheap`) so repeat calls don't spend credits. Inject `SerpCheapClient` for cached calls, or call `client.raw()` for the uncached SDK client:

```java
public SearchService(SerpCheapClient client) {
  this.client = client;
}

client.search(SearchParams.of("best running shoes")); // cached
client.raw().search(SearchParams.of("best running shoes")); // uncached
```

Disable it with `serpcheap.cache.enabled=false`.

## Configuration

| Property | Default | Description |
| --- | --- | --- |
| `serpcheap.api-key` | — | API key (required) |
| `serpcheap.base-url` | `https://api.serp.cheap` | API base URL |
| `serpcheap.timeout` | `15s` | Per-request timeout |
| `serpcheap.max-retries` | `2` | Retry count on transient errors |
| `serpcheap.cache.enabled` | `true` | Toggle the cached `SerpCheapClient` bean |

Requires Spring Boot 3 / Java 17+.

## License

MIT
