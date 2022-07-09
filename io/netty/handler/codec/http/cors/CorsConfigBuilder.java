package io.netty.handler.codec.http.cors;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.util.internal.ObjectUtil;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;





















public final class CorsConfigBuilder
{
  final Set<String> origins;
  final boolean anyOrigin;
  boolean allowNullOrigin;
  
  public static CorsConfigBuilder forAnyOrigin()
  {
    return new CorsConfigBuilder();
  }
  




  public static CorsConfigBuilder forOrigin(String origin)
  {
    if ("*".equals(origin)) {
      return new CorsConfigBuilder();
    }
    return new CorsConfigBuilder(new String[] { origin });
  }
  




  public static CorsConfigBuilder forOrigins(String... origins)
  {
    return new CorsConfigBuilder(origins);
  }
  



  boolean enabled = true;
  boolean allowCredentials;
  final Set<String> exposeHeaders = new HashSet();
  long maxAge;
  final Set<HttpMethod> requestMethods = new HashSet();
  final Set<String> requestHeaders = new HashSet();
  final Map<CharSequence, Callable<?>> preflightHeaders = new HashMap();
  

  private boolean noPreflightHeaders;
  
  boolean shortCircuit;
  

  CorsConfigBuilder(String... origins)
  {
    this.origins = new LinkedHashSet(Arrays.asList(origins));
    anyOrigin = false;
  }
  




  CorsConfigBuilder()
  {
    anyOrigin = true;
    origins = Collections.emptySet();
  }
  






  public CorsConfigBuilder allowNullOrigin()
  {
    allowNullOrigin = true;
    return this;
  }
  




  public CorsConfigBuilder disable()
  {
    enabled = false;
    return this;
  }
  
























  public CorsConfigBuilder exposeHeaders(String... headers)
  {
    exposeHeaders.addAll(Arrays.asList(headers));
    return this;
  }
  
























  public CorsConfigBuilder exposeHeaders(CharSequence... headers)
  {
    for (CharSequence header : headers) {
      exposeHeaders.add(header.toString());
    }
    return this;
  }
  














  public CorsConfigBuilder allowCredentials()
  {
    allowCredentials = true;
    return this;
  }
  








  public CorsConfigBuilder maxAge(long max)
  {
    maxAge = max;
    return this;
  }
  






  public CorsConfigBuilder allowedRequestMethods(HttpMethod... methods)
  {
    requestMethods.addAll(Arrays.asList(methods));
    return this;
  }
  















  public CorsConfigBuilder allowedRequestHeaders(String... headers)
  {
    requestHeaders.addAll(Arrays.asList(headers));
    return this;
  }
  















  public CorsConfigBuilder allowedRequestHeaders(CharSequence... headers)
  {
    for (CharSequence header : headers) {
      requestHeaders.add(header.toString());
    }
    return this;
  }
  









  public CorsConfigBuilder preflightResponseHeader(CharSequence name, Object... values)
  {
    if (values.length == 1) {
      preflightHeaders.put(name, new ConstantValueGenerator(values[0], null));
    } else {
      preflightResponseHeader(name, Arrays.asList(values));
    }
    return this;
  }
  










  public <T> CorsConfigBuilder preflightResponseHeader(CharSequence name, Iterable<T> value)
  {
    preflightHeaders.put(name, new ConstantValueGenerator(value, null));
    return this;
  }
  














  public <T> CorsConfigBuilder preflightResponseHeader(CharSequence name, Callable<T> valueGenerator)
  {
    preflightHeaders.put(name, valueGenerator);
    return this;
  }
  




  public CorsConfigBuilder noPreflightResponseHeaders()
  {
    noPreflightHeaders = true;
    return this;
  }
  









  public CorsConfigBuilder shortCircuit()
  {
    shortCircuit = true;
    return this;
  }
  




  public CorsConfig build()
  {
    if ((preflightHeaders.isEmpty()) && (!noPreflightHeaders)) {
      preflightHeaders.put(HttpHeaderNames.DATE, DateValueGenerator.INSTANCE);
      preflightHeaders.put(HttpHeaderNames.CONTENT_LENGTH, new ConstantValueGenerator("0", null));
    }
    return new CorsConfig(this);
  }
  




  private static final class ConstantValueGenerator
    implements Callable<Object>
  {
    private final Object value;
    




    private ConstantValueGenerator(Object value)
    {
      this.value = ObjectUtil.checkNotNullWithIAE(value, "value");
    }
    
    public Object call()
    {
      return value;
    }
  }
  




  private static final class DateValueGenerator
    implements Callable<Date>
  {
    static final DateValueGenerator INSTANCE = new DateValueGenerator();
    
    private DateValueGenerator() {}
    
    public Date call() throws Exception { return new Date(); }
  }
}
