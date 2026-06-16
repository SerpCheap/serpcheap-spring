package cheap.serp.serpcheap.spring;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds {@code serpcheap.*} application properties for the serp.cheap client. */
@ConfigurationProperties(prefix = "serpcheap")
public class SerpCheapProperties {

  /** API key. Get one at https://app.serp.cheap. */
  private String apiKey;

  private String baseUrl = "https://api.serp.cheap";

  private Duration timeout = Duration.ofSeconds(15);

  private int maxRetries = 2;

  private final Cache cache = new Cache();

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public Duration getTimeout() {
    return timeout;
  }

  public void setTimeout(Duration timeout) {
    this.timeout = timeout;
  }

  public int getMaxRetries() {
    return maxRetries;
  }

  public void setMaxRetries(int maxRetries) {
    this.maxRetries = maxRetries;
  }

  public Cache getCache() {
    return cache;
  }

  /** Result caching — active when a Spring {@code CacheManager} is present. */
  public static class Cache {

    /** Toggle the cached {@code SerpCheapClient} bean. */
    private boolean enabled = true;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }
}
