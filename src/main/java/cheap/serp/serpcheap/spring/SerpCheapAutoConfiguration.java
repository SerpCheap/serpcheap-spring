package cheap.serp.serpcheap.spring;

import cheap.serp.serpcheap.ClientOptions;
import cheap.serp.serpcheap.SerpCheap;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;

/** Auto-configures a {@link SerpCheap} client (and a cached wrapper) from {@code serpcheap.*}. */
@AutoConfiguration
@ConditionalOnClass(SerpCheap.class)
@EnableConfigurationProperties(SerpCheapProperties.class)
public class SerpCheapAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public SerpCheap serpCheap(SerpCheapProperties props) {
    if (props.getApiKey() == null || props.getApiKey().isBlank()) {
      throw new IllegalStateException(
          "serpcheap.api-key is not set. Get a key at https://app.serp.cheap.");
    }
    ClientOptions opts = ClientOptions.builder()
        .baseUrl(props.getBaseUrl())
        .timeout(props.getTimeout())
        .maxRetries(props.getMaxRetries())
        .build();
    return new SerpCheap(props.getApiKey(), opts);
  }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnBean(CacheManager.class)
  @ConditionalOnProperty(prefix = "serpcheap.cache", name = "enabled", matchIfMissing = true)
  public SerpCheapClient serpCheapClient(SerpCheap serpCheap) {
    return new SerpCheapClient(serpCheap);
  }

  @Bean
  @ConditionalOnMissingBean
  public SerpCheapCacheKeyGenerator serpCheapCacheKeyGenerator() {
    return new SerpCheapCacheKeyGenerator();
  }
}
