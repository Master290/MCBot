package io.netty.handler.codec.socksx.v5;

import java.util.List;

public abstract interface Socks5InitialRequest
  extends Socks5Message
{
  public abstract List<Socks5AuthMethod> authMethods();
}
