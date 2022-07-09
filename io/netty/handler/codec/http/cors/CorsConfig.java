package io.netty.handler.codec.http.cors;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.util.internal.StringUtil;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;


















public final class CorsConfig
{
  private final Set<String> origins;
  private final boolean anyOrigin;
  private final boolean enabled;
  private final Set<String> exposeHeaders;
  private final boolean allowCredentials;
  private final long maxAge;
  private final Set<HttpMethod> allowedRequestMethods;
  private final Set<String> allowedRequestHeaders;
  private final boolean allowNullOrigin;
  private final Map<CharSequence, Callable<?>> preflightHeaders;
  private final boolean shortCircuit;
  
  CorsConfig(CorsConfigBuilder builder)
  {
    origins = new LinkedHashSet(origins);
    anyOrigin = anyOrigin;
    enabled = enabled;
    exposeHeaders = exposeHeaders;
    allowCredentials = allowCredentials;
    maxAge = maxAge;
    allowedRequestMethods = requestMethods;
    allowedRequestHeaders = requestHeaders;
    allowNullOrigin = allowNullOrigin;
    preflightHeaders = preflightHeaders;
    shortCircuit = shortCircuit;
  }
  




  public boolean isCorsSupportEnabled()
  {
    return enabled;
  }
  




  public boolean isAnyOriginSupported()
  {
    return anyOrigin;
  }
  




  public String origin()
  {
    return origins.isEmpty() ? "*" : (String)origins.iterator().next();
  }
  




  public Set<String> origins()
  {
    return origins;
  }
  








  public boolean isNullOriginAllowed()
  {
    return allowNullOrigin;
  }
  





















  public Set<String> exposedHeaders()
  {
    return Collections.unmodifiableSet(exposeHeaders);
  }
  
















  public boolean isCredentialsAllowed()
  {
    return allowCredentials;
  }
  









  public long maxAge()
  {
    return maxAge;
  }
  





  public Set<HttpMethod> allowedRequestMethods()
  {
    return Collections.unmodifiableSet(allowedRequestMethods);
  }
  







  public Set<String> allowedRequestHeaders()
  {
    return Collections.unmodifiableSet(allowedRequestHeaders);
  }
  




  public HttpHeaders preflightResponseHeaders()
  {
    if (this.preflightHeaders.isEmpty()) {
      return EmptyHttpHeaders.INSTANCE;
    }
    HttpHeaders preflightHeaders = new DefaultHttpHeaders();
    for (Map.Entry<CharSequence, Callable<?>> entry : this.preflightHeaders.entrySet()) {
      Object value = getValue((Callable)entry.getValue());
      if ((value instanceof Iterable)) {
        preflightHeaders.add((CharSequence)entry.getKey(), (Iterable)value);
      } else {
        preflightHeaders.add((CharSequence)entry.getKey(), value);
      }
    }
    return preflightHeaders;
  }
  









  public boolean isShortCircuit()
  {
    return shortCircuit;
  }
  


  @Deprecated
  public boolean isShortCurcuit()
  {
    return isShortCircuit();
  }
  
  private static <T> T getValue(Callable<T> callable) {
    try {
      return callable.call();
    } catch (Exception e) {
      throw new IllegalStateException("Could not generate value for callable [" + callable + ']', e);
    }
  }
  
  public String toString()
  {
    return StringUtil.simpleClassName(this) + "[enabled=" + enabled + ", origins=" + origins + ", anyOrigin=" + anyOrigin + ", exposedHeaders=" + exposeHeaders + ", isCredentialsAllowed=" + allowCredentials + ", maxAge=" + maxAge + ", allowedRequestMethods=" + allowedRequestMethods + ", allowedRequestHeaders=" + allowedRequestHeaders + ", preflightHeaders=" + preflightHeaders + ']';
  }
  










  @Deprecated
  public static Builder withAnyOrigin()
  {
    return new Builder();
  }
  


  @Deprecated
  public static Builder withOrigin(String origin)
  {
    if ("*".equals(origin)) {
      return new Builder();
    }
    return new Builder(new String[] { origin });
  }
  


  @Deprecated
  public static Builder withOrigins(String... origins)
  {
    return new Builder(origins);
  }
  



  @Deprecated
  public static class Builder
  {
    private final CorsConfigBuilder builder;
    


    @Deprecated
    public Builder(String... origins)
    {
      builder = new CorsConfigBuilder(origins);
    }
    


    @Deprecated
    public Builder()
    {
      builder = new CorsConfigBuilder();
    }
    


    @Deprecated
    public Builder allowNullOrigin()
    {
      builder.allowNullOrigin();
      return this;
    }
    


    @Deprecated
    public Builder disable()
    {
      builder.disable();
      return this;
    }
    


    @Deprecated
    public Builder exposeHeaders(String... headers)
    {
      builder.exposeHeaders(headers);
      return this;
    }
    


    @Deprecated
    public Builder allowCredentials()
    {
      builder.allowCredentials();
      return this;
    }
    


    @Deprecated
    public Builder maxAge(long max)
    {
      builder.maxAge(max);
      return this;
    }
    


    @Deprecated
    public Builder allowedRequestMethods(HttpMethod... methods)
    {
      builder.allowedRequestMethods(methods);
      return this;
    }
    


    @Deprecated
    public Builder allowedRequestHeaders(String... headers)
    {
      builder.allowedRequestHeaders(headers);
      return this;
    }
    


    @Deprecated
    public Builder preflightResponseHeader(CharSequence name, Object... values)
    {
      builder.preflightResponseHeader(name, values);
      return this;
    }
    


    @Deprecated
    public <T> Builder preflightResponseHeader(CharSequence name, Iterable<T> value)
    {
      builder.preflightResponseHeader(name, value);
      return this;
    }
    


    @Deprecated
    public <T> Builder preflightResponseHeader(String name, Callable<T> valueGenerator)
    {
      builder.preflightResponseHeader(name, valueGenerator);
      return this;
    }
    


    @Deprecated
    public Builder noPreflightResponseHeaders()
    {
      builder.noPreflightResponseHeaders();
      return this;
    }
    


    @Deprecated
    public CorsConfig build()
    {
      return builder.build();
    }
    


    @Deprecated
    public Builder shortCurcuit()
    {
      builder.shortCircuit();
      return this;
    }
  }
  
  @Deprecated
  public static final class DateValueGenerator
    implements Callable<Date>
  {
    public DateValueGenerator() {}
    
    public Date call() throws Exception
    {
      return new Date();
    }
  }
}
