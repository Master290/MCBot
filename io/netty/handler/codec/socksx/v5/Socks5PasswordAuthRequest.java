package io.netty.handler.codec.socksx.v5;

public abstract interface Socks5PasswordAuthRequest
  extends Socks5Message
{
  public abstract String username();
  
  public abstract String password();
}
