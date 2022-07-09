package io.netty.resolver.dns;

import io.netty.channel.EventLoop;

public abstract interface DnsCnameCache
{
  public abstract String get(String paramString);
  
  public abstract void cache(String paramString1, String paramString2, long paramLong, EventLoop paramEventLoop);
  
  public abstract void clear();
  
  public abstract boolean clear(String paramString);
}
