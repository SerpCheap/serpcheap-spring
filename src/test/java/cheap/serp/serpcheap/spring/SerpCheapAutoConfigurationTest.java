package cheap.serp.serpcheap.spring;

import static org.assertj.core.api.Assertions.assertThat;

import cheap.serp.serpcheap.RankParams;
import cheap.serp.serpcheap.ScrapeParams;
import cheap.serp.serpcheap.SearchParams;
import cheap.serp.serpcheap.SerpCheap;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class SerpCheapAutoConfigurationTest {

  private static final String SEARCH_JSON =
      "{\"search\":\"shoes\",\"page\":1,\"organic\":[{\"position\":1,\"title\":\"Nike\",\"link\":\"https://nike.test\"}]}";

  private final ApplicationContextRunner runner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(SerpCheapAutoConfiguration.class));

  @Test
  void createsClientWhenApiKeySet() {
    runner.withPropertyValues("serpcheap.api-key=k")
        .run(ctx -> assertThat(ctx).hasSingleBean(SerpCheap.class));
  }

  @Test
  void contextFailsWithoutApiKey() {
    runner.run(ctx -> assertThat(ctx).hasFailed());
  }

  @Test
  void backsOffWhenUserDefinesClient() {
    SerpCheap custom = new SerpCheap("user-key");
    runner.withBean("serpCheap", SerpCheap.class, () -> custom)
        .withPropertyValues("serpcheap.api-key=k")
        .run(ctx -> assertThat(ctx.getBean(SerpCheap.class)).isSameAs(custom));
  }

  @Test
  void bindsProperties() throws IOException {
    try (Mock mock = Mock.start(SEARCH_JSON)) {
      runner.withPropertyValues(
              "serpcheap.api-key=k",
              "serpcheap.base-url=" + mock.baseUrl(),
              "serpcheap.timeout=5s",
              "serpcheap.max-retries=0")
          .run(ctx -> {
            SerpCheap client = ctx.getBean(SerpCheap.class);
            assertThat(client.search(SearchParams.of("shoes")).organic).hasSize(1);
            assertThat(mock.hits()).isEqualTo(1);
          });
    }
  }

  @Test
  void noCachedClientWithoutCacheManager() {
    runner.withPropertyValues("serpcheap.api-key=k")
        .run(ctx -> assertThat(ctx).doesNotHaveBean(SerpCheapClient.class));
  }

  @Test
  void cachedClientCreatedWithCacheManager() {
    runner.withUserConfiguration(CachingConfig.class)
        .withPropertyValues("serpcheap.api-key=k")
        .run(ctx -> assertThat(ctx).hasSingleBean(SerpCheapClient.class));
  }

  @Test
  void cachedClientCanBeDisabled() {
    runner.withUserConfiguration(CachingConfig.class)
        .withPropertyValues("serpcheap.api-key=k", "serpcheap.cache.enabled=false")
        .run(ctx -> assertThat(ctx).doesNotHaveBean(SerpCheapClient.class));
  }

  @Test
  void cachedClientCachesResultsAndExposesRaw() throws IOException {
    try (Mock mock = Mock.start(SEARCH_JSON)) {
      runner.withUserConfiguration(CachingConfig.class)
          .withPropertyValues(
              "serpcheap.api-key=k",
              "serpcheap.base-url=" + mock.baseUrl(),
              "serpcheap.max-retries=0")
          .run(ctx -> {
            SerpCheapClient client = ctx.getBean(SerpCheapClient.class);
            client.search(SearchParams.of("shoes"));
            client.search(SearchParams.of("shoes"));
            assertThat(mock.hits()).isEqualTo(1);

            client.raw().search(SearchParams.of("shoes"));
            assertThat(mock.hits()).isEqualTo(2);
          });
    }
  }

  @Test
  void distinctOptionsDoNotCollide() throws IOException {
    try (Mock mock = Mock.start(SEARCH_JSON)) {
      runner.withUserConfiguration(CachingConfig.class)
          .withPropertyValues("serpcheap.api-key=k", "serpcheap.base-url=" + mock.baseUrl(), "serpcheap.max-retries=0")
          .run(ctx -> {
            SerpCheapClient client = ctx.getBean(SerpCheapClient.class);
            client.search(SearchParams.builder().q("shoes").hl("pt").build());
            client.search(SearchParams.builder().q("shoes").hl("de").build());
            assertThat(mock.hits()).isEqualTo(2);
          });
    }
  }

  @Test
  void cachesScrapeAndRank() throws IOException {
    var scrape = "{\"url\":\"https://example.test\",\"title\":\"Example\"}";
    try (Mock mock = Mock.start(scrape)) {
      runner.withUserConfiguration(CachingConfig.class)
          .withPropertyValues("serpcheap.api-key=k", "serpcheap.base-url=" + mock.baseUrl(), "serpcheap.max-retries=0")
          .run(ctx -> {
            SerpCheapClient client = ctx.getBean(SerpCheapClient.class);
            client.scrape(ScrapeParams.of("https://example.test"));
            client.scrape(ScrapeParams.of("https://example.test"));
            assertThat(mock.hits()).isEqualTo(1);
          });
    }

    var rank = "{\"url\":\"nike.test\",\"search\":\"shoes\",\"gl\":\"us\",\"match_type\":\"domain\","
        + "\"pages_scanned\":1,\"found\":true,\"rank\":1,\"matches\":[],\"organic\":[]}";
    try (Mock mock = Mock.start(rank)) {
      runner.withUserConfiguration(CachingConfig.class)
          .withPropertyValues("serpcheap.api-key=k", "serpcheap.base-url=" + mock.baseUrl(), "serpcheap.max-retries=0")
          .run(ctx -> {
            SerpCheapClient client = ctx.getBean(SerpCheapClient.class);
            client.rank(RankParams.builder().url("nike.test").q("shoes").build());
            client.rank(RankParams.builder().url("nike.test").q("shoes").build());
            assertThat(mock.hits()).isEqualTo(1);
          });
    }
  }

  @Configuration
  @EnableCaching
  static class CachingConfig {
    @Bean
    CacheManager cacheManager() {
      return new ConcurrentMapCacheManager("serpcheap");
    }
  }

  /** Minimal in-process API: fixed 200 body, counts hits. */
  static final class Mock implements AutoCloseable {
    private final HttpServer server;
    private final AtomicInteger hits = new AtomicInteger();

    private Mock(HttpServer server) {
      this.server = server;
    }

    static Mock start(String body) throws IOException {
      HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      Mock mock = new Mock(s);
      byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
      s.createContext("/", exchange -> {
        mock.hits.incrementAndGet();
        exchange.getRequestBody().readAllBytes();
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
          out.write(bytes);
        }
      });
      s.setExecutor(null);
      s.start();
      return mock;
    }

    String baseUrl() {
      return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    int hits() {
      return hits.get();
    }

    @Override
    public void close() {
      server.stop(0);
    }
  }
}
