package io.netty.channel.udt;

import io.netty.channel.Channel;
import java.net.InetSocketAddress;

@Deprecated
public abstract interface UdtChannel
  extends Channel
{
  public abstract UdtChannelConfig config();
  
  public abstract InetSocketAddress localAddress();
  
  public abstract InetSocketAddress remoteAddress();
}
