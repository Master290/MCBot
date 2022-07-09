package io.netty.resolver.dns;

import io.netty.channel.EventLoop;
import io.netty.handler.codec.dns.DnsRecord;
import io.netty.util.internal.ObjectUtil;
import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
























public class DefaultDnsCache
  implements DnsCache
{
  private final Cache<DefaultDnsCacheEntry> resolveCache = new Cache()
  {
    protected boolean shouldReplaceAll(DefaultDnsCache.DefaultDnsCacheEntry entry)
    {
      return entry.cause() != null;
    }
    
    protected boolean equals(DefaultDnsCache.DefaultDnsCacheEntry entry, DefaultDnsCache.DefaultDnsCacheEntry otherEntry)
    {
      if (entry.address() != null) {
        return entry.address().equals(otherEntry.address());
      }
      if (otherEntry.address() != null) {
        return false;
      }
      return entry.cause().equals(otherEntry.cause());
    }
  };
  
  private final int minTtl;
  
  private final int maxTtl;
  
  private final int negativeTtl;
  

  public DefaultDnsCache()
  {
    this(0, Cache.MAX_SUPPORTED_TTL_SECS, 0);
  }
  





  public DefaultDnsCache(int minTtl, int maxTtl, int negativeTtl)
  {
    this.minTtl = Math.min(Cache.MAX_SUPPORTED_TTL_SECS, ObjectUtil.checkPositiveOrZero(minTtl, "minTtl"));
    this.maxTtl = Math.min(Cache.MAX_SUPPORTED_TTL_SECS, ObjectUtil.checkPositiveOrZero(maxTtl, "maxTtl"));
    if (minTtl > maxTtl) {
      throw new IllegalArgumentException("minTtl: " + minTtl + ", maxTtl: " + maxTtl + " (expected: 0 <= minTtl <= maxTtl)");
    }
    
    this.negativeTtl = ObjectUtil.checkPositiveOrZero(negativeTtl, "negativeTtl");
  }
  




  public int minTtl()
  {
    return minTtl;
  }
  




  public int maxTtl()
  {
    return maxTtl;
  }
  



  public int negativeTtl()
  {
    return negativeTtl;
  }
  
  public void clear()
  {
    resolveCache.clear();
  }
  
  public boolean clear(String hostname)
  {
    ObjectUtil.checkNotNull(hostname, "hostname");
    return resolveCache.clear(appendDot(hostname));
  }
  
  private static boolean emptyAdditionals(DnsRecord[] additionals) {
    return (additionals == null) || (additionals.length == 0);
  }
  
  public List<? extends DnsCacheEntry> get(String hostname, DnsRecord[] additionals)
  {
    ObjectUtil.checkNotNull(hostname, "hostname");
    if (!emptyAdditionals(additionals)) {
      return Collections.emptyList();
    }
    
    return resolveCache.get(appendDot(hostname));
  }
  

  public DnsCacheEntry cache(String hostname, DnsRecord[] additionals, InetAddress address, long originalTtl, EventLoop loop)
  {
    ObjectUtil.checkNotNull(hostname, "hostname");
    ObjectUtil.checkNotNull(address, "address");
    ObjectUtil.checkNotNull(loop, "loop");
    DefaultDnsCacheEntry e = new DefaultDnsCacheEntry(hostname, address);
    if ((maxTtl == 0) || (!emptyAdditionals(additionals))) {
      return e;
    }
    resolveCache.cache(appendDot(hostname), e, Math.max(minTtl, (int)Math.min(maxTtl, originalTtl)), loop);
    return e;
  }
  
  public DnsCacheEntry cache(String hostname, DnsRecord[] additionals, Throwable cause, EventLoop loop)
  {
    ObjectUtil.checkNotNull(hostname, "hostname");
    ObjectUtil.checkNotNull(cause, "cause");
    ObjectUtil.checkNotNull(loop, "loop");
    
    DefaultDnsCacheEntry e = new DefaultDnsCacheEntry(hostname, cause);
    if ((negativeTtl == 0) || (!emptyAdditionals(additionals))) {
      return e;
    }
    
    resolveCache.cache(appendDot(hostname), e, negativeTtl, loop);
    return e;
  }
  
  public String toString()
  {
    return 
    



      "DefaultDnsCache(minTtl=" + minTtl + ", maxTtl=" + maxTtl + ", negativeTtl=" + negativeTtl + ", cached resolved hostname=" + resolveCache.size() + ')';
  }
  
  private static final class DefaultDnsCacheEntry implements DnsCacheEntry
  {
    private final String hostname;
    private final InetAddress address;
    private final Throwable cause;
    
    DefaultDnsCacheEntry(String hostname, InetAddress address) {
      this.hostname = hostname;
      this.address = address;
      cause = null;
    }
    
    DefaultDnsCacheEntry(String hostname, Throwable cause) {
      this.hostname = hostname;
      this.cause = cause;
      address = null;
    }
    
    public InetAddress address()
    {
      return address;
    }
    
    public Throwable cause()
    {
      return cause;
    }
    
    String hostname() {
      return hostname;
    }
    
    public String toString()
    {
      if (cause != null) {
        return hostname + '/' + cause;
      }
      return address.toString();
    }
  }
  
  private static String appendDot(String hostname)
  {
    return hostname + '.';
  }
}
