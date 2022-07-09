package io.netty.resolver.dns;

import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.channel.socket.InternetProtocolFamily;
import io.netty.handler.codec.dns.DnsRecord;
import io.netty.handler.codec.dns.DnsRecordType;
import io.netty.util.concurrent.Promise;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;



















final class DnsAddressResolveContext
  extends DnsResolveContext<InetAddress>
{
  private final DnsCache resolveCache;
  private final AuthoritativeDnsServerCache authoritativeDnsServerCache;
  private final boolean completeEarlyIfPossible;
  
  DnsAddressResolveContext(DnsNameResolver parent, Promise<?> originalPromise, String hostname, DnsRecord[] additionals, DnsServerAddressStream nameServerAddrs, int allowedQueries, DnsCache resolveCache, AuthoritativeDnsServerCache authoritativeDnsServerCache, boolean completeEarlyIfPossible)
  {
    super(parent, originalPromise, hostname, 1, parent
      .resolveRecordTypes(), additionals, nameServerAddrs, allowedQueries);
    this.resolveCache = resolveCache;
    this.authoritativeDnsServerCache = authoritativeDnsServerCache;
    this.completeEarlyIfPossible = completeEarlyIfPossible;
  }
  




  DnsResolveContext<InetAddress> newResolverContext(DnsNameResolver parent, Promise<?> originalPromise, String hostname, int dnsClass, DnsRecordType[] expectedTypes, DnsRecord[] additionals, DnsServerAddressStream nameServerAddrs, int allowedQueries)
  {
    return new DnsAddressResolveContext(parent, originalPromise, hostname, additionals, nameServerAddrs, allowedQueries, resolveCache, authoritativeDnsServerCache, completeEarlyIfPossible);
  }
  

  InetAddress convertRecord(DnsRecord record, String hostname, DnsRecord[] additionals, EventLoop eventLoop)
  {
    return DnsAddressDecoder.decodeAddress(record, hostname, parent.isDecodeIdn());
  }
  
  List<InetAddress> filterResults(List<InetAddress> unfiltered)
  {
    Collections.sort(unfiltered, PreferredAddressTypeComparator.comparator(parent.preferredAddressType()));
    return unfiltered;
  }
  
  boolean isCompleteEarly(InetAddress resolved)
  {
    return (completeEarlyIfPossible) && (parent.preferredAddressType().addressType() == resolved.getClass());
  }
  

  boolean isDuplicateAllowed()
  {
    return false;
  }
  

  void cache(String hostname, DnsRecord[] additionals, DnsRecord result, InetAddress convertedResult)
  {
    resolveCache.cache(hostname, additionals, convertedResult, result.timeToLive(), parent.ch.eventLoop());
  }
  
  void cache(String hostname, DnsRecord[] additionals, UnknownHostException cause)
  {
    resolveCache.cache(hostname, additionals, cause, parent.ch.eventLoop());
  }
  

  void doSearchDomainQuery(String hostname, Promise<List<InetAddress>> nextPromise)
  {
    if (!DnsNameResolver.doResolveAllCached(hostname, additionals, nextPromise, resolveCache, parent
      .resolvedInternetProtocolFamiliesUnsafe())) {
      super.doSearchDomainQuery(hostname, nextPromise);
    }
  }
  
  DnsCache resolveCache()
  {
    return resolveCache;
  }
  
  AuthoritativeDnsServerCache authoritativeDnsServerCache()
  {
    return authoritativeDnsServerCache;
  }
}
