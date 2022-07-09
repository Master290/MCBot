package io.netty.handler.codec.socksx.v5;

public abstract interface Socks5CommandRequest
  extends Socks5Message
{
  public abstract Socks5CommandType type();
  
  public abstract Socks5AddressType dstAddrType();
  
  public abstract String dstAddr();
  
  public abstract int dstPort();
}
