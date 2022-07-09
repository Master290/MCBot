package io.netty.handler.codec.http.websocketx;

import io.netty.util.AsciiString;






















public final class WebSocketScheme
{
  public static final WebSocketScheme WS = new WebSocketScheme(80, "ws");
  



  public static final WebSocketScheme WSS = new WebSocketScheme(443, "wss");
  private final int port;
  private final AsciiString name;
  
  private WebSocketScheme(int port, String name)
  {
    this.port = port;
    this.name = AsciiString.cached(name);
  }
  
  public AsciiString name() {
    return name;
  }
  
  public int port() {
    return port;
  }
  
  public boolean equals(Object o)
  {
    if (!(o instanceof WebSocketScheme)) {
      return false;
    }
    WebSocketScheme other = (WebSocketScheme)o;
    return (other.port() == port) && (other.name().equals(name));
  }
  
  public int hashCode()
  {
    return port * 31 + name.hashCode();
  }
  
  public String toString()
  {
    return name.toString();
  }
}
