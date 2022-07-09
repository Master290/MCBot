package io.netty.handler.codec.socksx.v5;

public abstract interface Socks5InitialResponse
  extends Socks5Message
{
  public abstract Socks5AuthMethod authMethod();
}
