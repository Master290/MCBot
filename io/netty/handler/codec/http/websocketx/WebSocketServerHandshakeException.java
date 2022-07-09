package io.netty.handler.codec.http.websocketx;

import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpRequest;























public final class WebSocketServerHandshakeException
  extends WebSocketHandshakeException
{
  private static final long serialVersionUID = 1L;
  private final HttpRequest request;
  
  public WebSocketServerHandshakeException(String message)
  {
    this(message, null);
  }
  
  public WebSocketServerHandshakeException(String message, HttpRequest httpRequest) {
    super(message);
    if (httpRequest != null)
    {
      request = new DefaultHttpRequest(httpRequest.protocolVersion(), httpRequest.method(), httpRequest.uri(), httpRequest.headers());
    } else {
      request = null;
    }
  }
  


  public HttpRequest request()
  {
    return request;
  }
}
