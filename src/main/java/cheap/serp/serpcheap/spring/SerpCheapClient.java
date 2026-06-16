package cheap.serp.serpcheap.spring;

import cheap.serp.serpcheap.RankParams;
import cheap.serp.serpcheap.RankResponse;
import cheap.serp.serpcheap.ScrapeParams;
import cheap.serp.serpcheap.ScrapeResponse;
import cheap.serp.serpcheap.SearchParams;
import cheap.serp.serpcheap.SearchResponse;
import cheap.serp.serpcheap.SerpCheap;
import org.springframework.cache.annotation.Cacheable;

/**
 * Caches search/scrape/rank results in the application's Spring {@code CacheManager}
 * so repeat calls don't spend credits. Created only when a {@code CacheManager} bean
 * exists. Use {@link #raw()} for the uncached SDK client.
 */
public class SerpCheapClient {

  private final SerpCheap delegate;

  public SerpCheapClient(SerpCheap delegate) {
    this.delegate = delegate;
  }

  /** The raw, uncached SDK client. */
  public SerpCheap raw() {
    return delegate;
  }

  @Cacheable(cacheNames = "serpcheap", key = "'search:' + #params.q + '|' + #params.gl + '|' + #params.page")
  public SearchResponse search(SearchParams params) {
    return delegate.search(params);
  }

  @Cacheable(cacheNames = "serpcheap", key = "'scrape:' + #params.url")
  public ScrapeResponse scrape(ScrapeParams params) {
    return delegate.scrape(params);
  }

  @Cacheable(cacheNames = "serpcheap", key = "'rank:' + #params.url + '|' + #params.q")
  public RankResponse rank(RankParams params) {
    return delegate.rank(params);
  }
}
