package io.netty.handler.codec.socksx.v4;

public abstract interface Socks4CommandRequest
  extends Socks4Message
{
  public abstract Socks4CommandType type();
  
  public abstract String userId();
  
  public abstract String dstAddr();
  
  public abstract int dstPort();
}
