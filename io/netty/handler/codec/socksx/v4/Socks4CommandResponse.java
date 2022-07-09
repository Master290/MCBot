package io.netty.handler.codec.socksx.v4;

public abstract interface Socks4CommandResponse
  extends Socks4Message
{
  public abstract Socks4CommandStatus status();
  
  public abstract String dstAddr();
  
  public abstract int dstPort();
}
