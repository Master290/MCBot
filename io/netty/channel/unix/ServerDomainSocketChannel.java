package io.netty.channel.unix;

import io.netty.channel.ServerChannel;

public abstract interface ServerDomainSocketChannel
  extends ServerChannel, UnixChannel
{
  public abstract DomainSocketAddress remoteAddress();
  
  public abstract DomainSocketAddress localAddress();
}
