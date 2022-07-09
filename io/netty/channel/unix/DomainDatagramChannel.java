package io.netty.channel.unix;

import io.netty.channel.Channel;

public abstract interface DomainDatagramChannel
  extends UnixChannel, Channel
{
  public abstract DomainDatagramChannelConfig config();
  
  public abstract boolean isConnected();
  
  public abstract DomainSocketAddress localAddress();
  
  public abstract DomainSocketAddress remoteAddress();
}
