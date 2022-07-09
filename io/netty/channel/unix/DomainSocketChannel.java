package io.netty.channel.unix;

import io.netty.channel.socket.DuplexChannel;

public abstract interface DomainSocketChannel
  extends UnixChannel, DuplexChannel
{
  public abstract DomainSocketAddress remoteAddress();
  
  public abstract DomainSocketAddress localAddress();
  
  public abstract DomainSocketChannelConfig config();
}
