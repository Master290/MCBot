package io.netty.handler.codec.socksx.v5;

public abstract interface Socks5CommandResponse
  extends Socks5Message
{
  public abstract Socks5CommandStatus status();
  
  public abstract Socks5AddressType bndAddrType();
  
  public abstract String bndAddr();
  
  public abstract int bndPort();
}
