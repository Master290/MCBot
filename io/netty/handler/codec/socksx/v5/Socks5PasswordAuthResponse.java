package io.netty.handler.codec.socksx.v5;

public abstract interface Socks5PasswordAuthResponse
  extends Socks5Message
{
  public abstract Socks5PasswordAuthStatus status();
}
