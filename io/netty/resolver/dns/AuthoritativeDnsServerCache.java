package io.netty.resolver.dns;

import io.netty.channel.EventLoop;
import java.net.InetSocketAddress;

public abstract interface AuthoritativeDnsServerCache
{
  public abstract DnsServerAddressStream get(String paramString);
  
  public abstract void cache(String paramString, InetSocketAddress paramInetSocketAddress, long paramLong, EventLoop paramEventLoop);
  
  public abstract void clear();
  
  public abstract boolean clear(String paramString);
}
