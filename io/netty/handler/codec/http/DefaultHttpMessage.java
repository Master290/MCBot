package io.netty.handler.codec.http;

import io.netty.util.internal.ObjectUtil;




















public abstract class DefaultHttpMessage
  extends DefaultHttpObject
  implements HttpMessage
{
  private static final int HASH_CODE_PRIME = 31;
  private HttpVersion version;
  private final HttpHeaders headers;
  
  protected DefaultHttpMessage(HttpVersion version)
  {
    this(version, true, false);
  }
  


  protected DefaultHttpMessage(HttpVersion version, boolean validateHeaders, boolean singleFieldHeaders)
  {
    this(version, singleFieldHeaders ? new CombinedHttpHeaders(validateHeaders) : new DefaultHttpHeaders(validateHeaders));
  }
  




  protected DefaultHttpMessage(HttpVersion version, HttpHeaders headers)
  {
    this.version = ((HttpVersion)ObjectUtil.checkNotNull(version, "version"));
    this.headers = ((HttpHeaders)ObjectUtil.checkNotNull(headers, "headers"));
  }
  
  public HttpHeaders headers()
  {
    return headers;
  }
  
  @Deprecated
  public HttpVersion getProtocolVersion()
  {
    return protocolVersion();
  }
  
  public HttpVersion protocolVersion()
  {
    return version;
  }
  
  public int hashCode()
  {
    int result = 1;
    result = 31 * result + headers.hashCode();
    result = 31 * result + version.hashCode();
    result = 31 * result + super.hashCode();
    return result;
  }
  
  public boolean equals(Object o)
  {
    if (!(o instanceof DefaultHttpMessage)) {
      return false;
    }
    
    DefaultHttpMessage other = (DefaultHttpMessage)o;
    
    return (headers().equals(other.headers())) && 
      (protocolVersion().equals(other.protocolVersion())) && 
      (super.equals(o));
  }
  
  public HttpMessage setProtocolVersion(HttpVersion version)
  {
    this.version = ((HttpVersion)ObjectUtil.checkNotNull(version, "version"));
    return this;
  }
}
