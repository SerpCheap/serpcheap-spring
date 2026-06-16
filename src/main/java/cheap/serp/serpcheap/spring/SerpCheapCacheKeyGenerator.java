package cheap.serp.serpcheap.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.springframework.cache.interceptor.KeyGenerator;

/** Keys a cached call on its method plus a SHA-256 of the JSON of every parameter field. */
public class SerpCheapCacheKeyGenerator implements KeyGenerator {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Override
  public Object generate(Object target, Method method, Object... params) {
    try {
      byte[] json = MAPPER.writeValueAsBytes(params.length > 0 ? params[0] : "");
      byte[] hash = MessageDigest.getInstance("SHA-256").digest(json);
      return method.getName() + ":" + HexFormat.of().formatHex(hash);
    } catch (Exception e) {
      throw new IllegalStateException("Could not build a serp.cheap cache key for " + method.getName(), e);
    }
  }
}
