package io.netty.resolver.dns;

import io.netty.channel.EventLoop;















public final class NoopDnsCnameCache
  implements DnsCnameCache
{
  public static final NoopDnsCnameCache INSTANCE = new NoopDnsCnameCache();
  
  private NoopDnsCnameCache() {}
  
  public String get(String hostname)
  {
    return null;
  }
  


  public void cache(String hostname, String cname, long originalTtl, EventLoop loop) {}
  


  public void clear() {}
  


  public boolean clear(String hostname)
  {
    return false;
  }
}
