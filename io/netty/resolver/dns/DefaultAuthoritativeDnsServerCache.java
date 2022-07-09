package io.netty.resolver.dns;

import io.netty.channel.EventLoop;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;





















public class DefaultAuthoritativeDnsServerCache
  implements AuthoritativeDnsServerCache
{
  private final int minTtl;
  private final int maxTtl;
  private final Comparator<InetSocketAddress> comparator;
  private final Cache<InetSocketAddress> resolveCache = new Cache()
  {
    protected boolean shouldReplaceAll(InetSocketAddress entry) {
      return false;
    }
    
    protected boolean equals(InetSocketAddress entry, InetSocketAddress otherEntry)
    {
      if (PlatformDependent.javaVersion() >= 7) {
        return entry.getHostString().equalsIgnoreCase(otherEntry.getHostString());
      }
      return entry.getHostName().equalsIgnoreCase(otherEntry.getHostName());
    }
    
    protected void sortEntries(String hostname, List<InetSocketAddress> entries)
    {
      if (comparator != null) {
        Collections.sort(entries, comparator);
      }
    }
  };
  


  public DefaultAuthoritativeDnsServerCache()
  {
    this(0, Cache.MAX_SUPPORTED_TTL_SECS, null);
  }
  







  public DefaultAuthoritativeDnsServerCache(int minTtl, int maxTtl, Comparator<InetSocketAddress> comparator)
  {
    this.minTtl = Math.min(Cache.MAX_SUPPORTED_TTL_SECS, ObjectUtil.checkPositiveOrZero(minTtl, "minTtl"));
    this.maxTtl = Math.min(Cache.MAX_SUPPORTED_TTL_SECS, ObjectUtil.checkPositive(maxTtl, "maxTtl"));
    if (minTtl > maxTtl) {
      throw new IllegalArgumentException("minTtl: " + minTtl + ", maxTtl: " + maxTtl + " (expected: 0 <= minTtl <= maxTtl)");
    }
    
    this.comparator = comparator;
  }
  

  public DnsServerAddressStream get(String hostname)
  {
    ObjectUtil.checkNotNull(hostname, "hostname");
    
    List<? extends InetSocketAddress> addresses = resolveCache.get(hostname);
    if ((addresses == null) || (addresses.isEmpty())) {
      return null;
    }
    return new SequentialDnsServerAddressStream(addresses, 0);
  }
  
  public void cache(String hostname, InetSocketAddress address, long originalTtl, EventLoop loop)
  {
    ObjectUtil.checkNotNull(hostname, "hostname");
    ObjectUtil.checkNotNull(address, "address");
    ObjectUtil.checkNotNull(loop, "loop");
    
    if ((PlatformDependent.javaVersion() >= 7) && (address.getHostString() == null))
    {

      return;
    }
    
    resolveCache.cache(hostname, address, Math.max(minTtl, (int)Math.min(maxTtl, originalTtl)), loop);
  }
  
  public void clear()
  {
    resolveCache.clear();
  }
  
  public boolean clear(String hostname)
  {
    return resolveCache.clear((String)ObjectUtil.checkNotNull(hostname, "hostname"));
  }
  
  public String toString()
  {
    return 
      "DefaultAuthoritativeDnsServerCache(minTtl=" + minTtl + ", maxTtl=" + maxTtl + ", cached nameservers=" + resolveCache.size() + ')';
  }
}
