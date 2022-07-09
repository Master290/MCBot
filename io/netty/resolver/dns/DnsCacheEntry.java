package io.netty.resolver.dns;

import java.net.InetAddress;

public abstract interface DnsCacheEntry
{
  public abstract InetAddress address();
  
  public abstract Throwable cause();
}
