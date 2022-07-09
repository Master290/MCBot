package io.netty.handler.codec.http;

import io.netty.util.internal.ObjectUtil;
























public class DefaultHttpResponse
  extends DefaultHttpMessage
  implements HttpResponse
{
  private HttpResponseStatus status;
  
  public DefaultHttpResponse(HttpVersion version, HttpResponseStatus status)
  {
    this(version, status, true, false);
  }
  






  public DefaultHttpResponse(HttpVersion version, HttpResponseStatus status, boolean validateHeaders)
  {
    this(version, status, validateHeaders, false);
  }
  












  public DefaultHttpResponse(HttpVersion version, HttpResponseStatus status, boolean validateHeaders, boolean singleFieldHeaders)
  {
    super(version, validateHeaders, singleFieldHeaders);
    this.status = ((HttpResponseStatus)ObjectUtil.checkNotNull(status, "status"));
  }
  






  public DefaultHttpResponse(HttpVersion version, HttpResponseStatus status, HttpHeaders headers)
  {
    super(version, headers);
    this.status = ((HttpResponseStatus)ObjectUtil.checkNotNull(status, "status"));
  }
  
  @Deprecated
  public HttpResponseStatus getStatus()
  {
    return status();
  }
  
  public HttpResponseStatus status()
  {
    return status;
  }
  
  public HttpResponse setStatus(HttpResponseStatus status)
  {
    this.status = ((HttpResponseStatus)ObjectUtil.checkNotNull(status, "status"));
    return this;
  }
  
  public HttpResponse setProtocolVersion(HttpVersion version)
  {
    super.setProtocolVersion(version);
    return this;
  }
  
  public String toString()
  {
    return HttpMessageUtil.appendResponse(new StringBuilder(256), this).toString();
  }
  
  public int hashCode()
  {
    int result = 1;
    result = 31 * result + status.hashCode();
    result = 31 * result + super.hashCode();
    return result;
  }
  
  public boolean equals(Object o)
  {
    if (!(o instanceof DefaultHttpResponse)) {
      return false;
    }
    
    DefaultHttpResponse other = (DefaultHttpResponse)o;
    
    return (status.equals(other.status())) && (super.equals(o));
  }
}
