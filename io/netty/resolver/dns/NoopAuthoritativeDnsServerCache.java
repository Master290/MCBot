package io.netty.resolver.dns;

import io.netty.channel.EventLoop;
import java.net.InetSocketAddress;


















public final class NoopAuthoritativeDnsServerCache
  implements AuthoritativeDnsServerCache
{
  public static final NoopAuthoritativeDnsServerCache INSTANCE = new NoopAuthoritativeDnsServerCache();
  
  private NoopAuthoritativeDnsServerCache() {}
  
  public DnsServerAddressStream get(String hostname)
  {
    return null;
  }
  


  public void cache(String hostname, InetSocketAddress address, long originalTtl, EventLoop loop) {}
  


  public void clear() {}
  


  public boolean clear(String hostname)
  {
    return false;
  }
}
