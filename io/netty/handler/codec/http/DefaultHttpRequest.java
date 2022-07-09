package io.netty.handler.codec.http;

import io.netty.util.internal.ObjectUtil;
























public class DefaultHttpRequest
  extends DefaultHttpMessage
  implements HttpRequest
{
  private static final int HASH_CODE_PRIME = 31;
  private HttpMethod method;
  private String uri;
  
  public DefaultHttpRequest(HttpVersion httpVersion, HttpMethod method, String uri)
  {
    this(httpVersion, method, uri, true);
  }
  







  public DefaultHttpRequest(HttpVersion httpVersion, HttpMethod method, String uri, boolean validateHeaders)
  {
    super(httpVersion, validateHeaders, false);
    this.method = ((HttpMethod)ObjectUtil.checkNotNull(method, "method"));
    this.uri = ((String)ObjectUtil.checkNotNull(uri, "uri"));
  }
  







  public DefaultHttpRequest(HttpVersion httpVersion, HttpMethod method, String uri, HttpHeaders headers)
  {
    super(httpVersion, headers);
    this.method = ((HttpMethod)ObjectUtil.checkNotNull(method, "method"));
    this.uri = ((String)ObjectUtil.checkNotNull(uri, "uri"));
  }
  
  @Deprecated
  public HttpMethod getMethod()
  {
    return method();
  }
  
  public HttpMethod method()
  {
    return method;
  }
  
  @Deprecated
  public String getUri()
  {
    return uri();
  }
  
  public String uri()
  {
    return uri;
  }
  
  public HttpRequest setMethod(HttpMethod method)
  {
    this.method = ((HttpMethod)ObjectUtil.checkNotNull(method, "method"));
    return this;
  }
  
  public HttpRequest setUri(String uri)
  {
    this.uri = ((String)ObjectUtil.checkNotNull(uri, "uri"));
    return this;
  }
  
  public HttpRequest setProtocolVersion(HttpVersion version)
  {
    super.setProtocolVersion(version);
    return this;
  }
  
  public int hashCode()
  {
    int result = 1;
    result = 31 * result + method.hashCode();
    result = 31 * result + uri.hashCode();
    result = 31 * result + super.hashCode();
    return result;
  }
  
  public boolean equals(Object o)
  {
    if (!(o instanceof DefaultHttpRequest)) {
      return false;
    }
    
    DefaultHttpRequest other = (DefaultHttpRequest)o;
    
    return (method().equals(other.method())) && 
      (uri().equalsIgnoreCase(other.uri())) && 
      (super.equals(o));
  }
  
  public String toString()
  {
    return HttpMessageUtil.appendRequest(new StringBuilder(256), this).toString();
  }
}
