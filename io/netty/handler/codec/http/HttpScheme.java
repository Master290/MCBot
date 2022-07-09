package io.netty.handler.codec.http;

import io.netty.util.AsciiString;























public final class HttpScheme
{
  public static final HttpScheme HTTP = new HttpScheme(80, "http");
  



  public static final HttpScheme HTTPS = new HttpScheme(443, "https");
  private final int port;
  private final AsciiString name;
  
  private HttpScheme(int port, String name)
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
    if (!(o instanceof HttpScheme)) {
      return false;
    }
    HttpScheme other = (HttpScheme)o;
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
