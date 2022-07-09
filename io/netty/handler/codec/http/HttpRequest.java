package io.netty.handler.codec.http;

public abstract interface HttpRequest
  extends HttpMessage
{
  @Deprecated
  public abstract HttpMethod getMethod();
  
  public abstract HttpMethod method();
  
  public abstract HttpRequest setMethod(HttpMethod paramHttpMethod);
  
  @Deprecated
  public abstract String getUri();
  
  public abstract String uri();
  
  public abstract HttpRequest setUri(String paramString);
  
  public abstract HttpRequest setProtocolVersion(HttpVersion paramHttpVersion);
}
