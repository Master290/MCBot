package io.netty.channel.socket;

import java.net.InetSocketAddress;

public abstract interface SocketChannel
  extends DuplexChannel
{
  public abstract ServerSocketChannel parent();
  
  public abstract SocketChannelConfig config();
  
  public abstract InetSocketAddress localAddress();
  
  public abstract InetSocketAddress remoteAddress();
}
