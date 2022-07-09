package io.netty.resolver.dns;

import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoop;
import io.netty.channel.ReflectiveChannelFactory;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.InternetProtocolFamily;
import io.netty.channel.socket.SocketChannel;
import io.netty.resolver.HostsFileEntriesResolver;
import io.netty.resolver.ResolvedAddressTypes;
import io.netty.util.internal.ObjectUtil;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;






















public final class DnsNameResolverBuilder
{
  volatile EventLoop eventLoop;
  private ChannelFactory<? extends DatagramChannel> channelFactory;
  private ChannelFactory<? extends SocketChannel> socketChannelFactory;
  private DnsCache resolveCache;
  private DnsCnameCache cnameCache;
  private AuthoritativeDnsServerCache authoritativeDnsServerCache;
  private SocketAddress localAddress;
  private Integer minTtl;
  private Integer maxTtl;
  private Integer negativeTtl;
  private long queryTimeoutMillis = -1L;
  private ResolvedAddressTypes resolvedAddressTypes = DnsNameResolver.DEFAULT_RESOLVE_ADDRESS_TYPES;
  private boolean completeOncePreferredResolved;
  private boolean recursionDesired = true;
  private int maxQueriesPerResolve = -1;
  private boolean traceEnabled;
  private int maxPayloadSize = 4096;
  private boolean optResourceEnabled = true;
  private HostsFileEntriesResolver hostsFileEntriesResolver = HostsFileEntriesResolver.DEFAULT;
  
  private DnsServerAddressStreamProvider dnsServerAddressStreamProvider = DnsServerAddressStreamProviders.platformDefault();
  private DnsQueryLifecycleObserverFactory dnsQueryLifecycleObserverFactory = NoopDnsQueryLifecycleObserverFactory.INSTANCE;
  
  private String[] searchDomains;
  private int ndots = -1;
  private boolean decodeIdn = true;
  





  public DnsNameResolverBuilder() {}
  




  public DnsNameResolverBuilder(EventLoop eventLoop)
  {
    eventLoop(eventLoop);
  }
  





  public DnsNameResolverBuilder eventLoop(EventLoop eventLoop)
  {
    this.eventLoop = eventLoop;
    return this;
  }
  
  protected ChannelFactory<? extends DatagramChannel> channelFactory() {
    return channelFactory;
  }
  





  public DnsNameResolverBuilder channelFactory(ChannelFactory<? extends DatagramChannel> channelFactory)
  {
    this.channelFactory = channelFactory;
    return this;
  }
  






  public DnsNameResolverBuilder channelType(Class<? extends DatagramChannel> channelType)
  {
    return channelFactory(new ReflectiveChannelFactory(channelType));
  }
  







  public DnsNameResolverBuilder socketChannelFactory(ChannelFactory<? extends SocketChannel> channelFactory)
  {
    socketChannelFactory = channelFactory;
    return this;
  }
  








  public DnsNameResolverBuilder socketChannelType(Class<? extends SocketChannel> channelType)
  {
    if (channelType == null) {
      return socketChannelFactory(null);
    }
    return socketChannelFactory(new ReflectiveChannelFactory(channelType));
  }
  





  public DnsNameResolverBuilder resolveCache(DnsCache resolveCache)
  {
    this.resolveCache = resolveCache;
    return this;
  }
  





  public DnsNameResolverBuilder cnameCache(DnsCnameCache cnameCache)
  {
    this.cnameCache = cnameCache;
    return this;
  }
  





  public DnsNameResolverBuilder dnsQueryLifecycleObserverFactory(DnsQueryLifecycleObserverFactory lifecycleObserverFactory)
  {
    dnsQueryLifecycleObserverFactory = ((DnsQueryLifecycleObserverFactory)ObjectUtil.checkNotNull(lifecycleObserverFactory, "lifecycleObserverFactory"));
    return this;
  }
  






  @Deprecated
  public DnsNameResolverBuilder authoritativeDnsServerCache(DnsCache authoritativeDnsServerCache)
  {
    this.authoritativeDnsServerCache = new AuthoritativeDnsServerCacheAdapter(authoritativeDnsServerCache);
    return this;
  }
  





  public DnsNameResolverBuilder authoritativeDnsServerCache(AuthoritativeDnsServerCache authoritativeDnsServerCache)
  {
    this.authoritativeDnsServerCache = authoritativeDnsServerCache;
    return this;
  }
  




  public DnsNameResolverBuilder localAddress(SocketAddress localAddress)
  {
    this.localAddress = localAddress;
    return this;
  }
  











  public DnsNameResolverBuilder ttl(int minTtl, int maxTtl)
  {
    this.maxTtl = Integer.valueOf(maxTtl);
    this.minTtl = Integer.valueOf(minTtl);
    return this;
  }
  





  public DnsNameResolverBuilder negativeTtl(int negativeTtl)
  {
    this.negativeTtl = Integer.valueOf(negativeTtl);
    return this;
  }
  





  public DnsNameResolverBuilder queryTimeoutMillis(long queryTimeoutMillis)
  {
    this.queryTimeoutMillis = queryTimeoutMillis;
    return this;
  }
  






  public static ResolvedAddressTypes computeResolvedAddressTypes(InternetProtocolFamily... internetProtocolFamilies)
  {
    if ((internetProtocolFamilies == null) || (internetProtocolFamilies.length == 0)) {
      return DnsNameResolver.DEFAULT_RESOLVE_ADDRESS_TYPES;
    }
    if (internetProtocolFamilies.length > 2) {
      throw new IllegalArgumentException("No more than 2 InternetProtocolFamilies");
    }
    
    switch (1.$SwitchMap$io$netty$channel$socket$InternetProtocolFamily[internetProtocolFamilies[0].ordinal()]) {
    case 1: 
      return (internetProtocolFamilies.length >= 2) && (internetProtocolFamilies[1] == InternetProtocolFamily.IPv6) ? ResolvedAddressTypes.IPV4_PREFERRED : ResolvedAddressTypes.IPV4_ONLY;
    

    case 2: 
      return (internetProtocolFamilies.length >= 2) && (internetProtocolFamilies[1] == InternetProtocolFamily.IPv4) ? ResolvedAddressTypes.IPV6_PREFERRED : ResolvedAddressTypes.IPV6_ONLY;
    }
    
    
    throw new IllegalArgumentException("Couldn't resolve ResolvedAddressTypes from InternetProtocolFamily array");
  }
  









  public DnsNameResolverBuilder resolvedAddressTypes(ResolvedAddressTypes resolvedAddressTypes)
  {
    this.resolvedAddressTypes = resolvedAddressTypes;
    return this;
  }
  






  public DnsNameResolverBuilder completeOncePreferredResolved(boolean completeOncePreferredResolved)
  {
    this.completeOncePreferredResolved = completeOncePreferredResolved;
    return this;
  }
  





  public DnsNameResolverBuilder recursionDesired(boolean recursionDesired)
  {
    this.recursionDesired = recursionDesired;
    return this;
  }
  





  public DnsNameResolverBuilder maxQueriesPerResolve(int maxQueriesPerResolve)
  {
    this.maxQueriesPerResolve = maxQueriesPerResolve;
    return this;
  }
  








  @Deprecated
  public DnsNameResolverBuilder traceEnabled(boolean traceEnabled)
  {
    this.traceEnabled = traceEnabled;
    return this;
  }
  





  public DnsNameResolverBuilder maxPayloadSize(int maxPayloadSize)
  {
    this.maxPayloadSize = maxPayloadSize;
    return this;
  }
  







  public DnsNameResolverBuilder optResourceEnabled(boolean optResourceEnabled)
  {
    this.optResourceEnabled = optResourceEnabled;
    return this;
  }
  




  public DnsNameResolverBuilder hostsFileEntriesResolver(HostsFileEntriesResolver hostsFileEntriesResolver)
  {
    this.hostsFileEntriesResolver = hostsFileEntriesResolver;
    return this;
  }
  
  protected DnsServerAddressStreamProvider nameServerProvider() {
    return dnsServerAddressStreamProvider;
  }
  





  public DnsNameResolverBuilder nameServerProvider(DnsServerAddressStreamProvider dnsServerAddressStreamProvider)
  {
    this.dnsServerAddressStreamProvider = ((DnsServerAddressStreamProvider)ObjectUtil.checkNotNull(dnsServerAddressStreamProvider, "dnsServerAddressStreamProvider"));
    return this;
  }
  





  public DnsNameResolverBuilder searchDomains(Iterable<String> searchDomains)
  {
    ObjectUtil.checkNotNull(searchDomains, "searchDomains");
    
    List<String> list = new ArrayList(4);
    
    for (String f : searchDomains) {
      if (f == null) {
        break;
      }
      

      if (!list.contains(f))
      {


        list.add(f);
      }
    }
    this.searchDomains = ((String[])list.toArray(new String[0]));
    return this;
  }
  






  public DnsNameResolverBuilder ndots(int ndots)
  {
    this.ndots = ndots;
    return this;
  }
  
  private DnsCache newCache() {
    return new DefaultDnsCache(ObjectUtil.intValue(minTtl, 0), ObjectUtil.intValue(maxTtl, Integer.MAX_VALUE), ObjectUtil.intValue(negativeTtl, 0));
  }
  
  private AuthoritativeDnsServerCache newAuthoritativeDnsServerCache() {
    return new DefaultAuthoritativeDnsServerCache(
      ObjectUtil.intValue(minTtl, 0), ObjectUtil.intValue(maxTtl, Integer.MAX_VALUE), new NameServerComparator(
      

      DnsNameResolver.preferredAddressType(resolvedAddressTypes).addressType()));
  }
  
  private DnsCnameCache newCnameCache() {
    return new DefaultDnsCnameCache(
      ObjectUtil.intValue(minTtl, 0), ObjectUtil.intValue(maxTtl, Integer.MAX_VALUE));
  }
  






  public DnsNameResolverBuilder decodeIdn(boolean decodeIdn)
  {
    this.decodeIdn = decodeIdn;
    return this;
  }
  




  public DnsNameResolver build()
  {
    if (eventLoop == null) {
      throw new IllegalStateException("eventLoop should be specified to build a DnsNameResolver.");
    }
    
    if ((this.resolveCache != null) && ((minTtl != null) || (maxTtl != null) || (negativeTtl != null))) {
      throw new IllegalStateException("resolveCache and TTLs are mutually exclusive");
    }
    
    if ((this.authoritativeDnsServerCache != null) && ((minTtl != null) || (maxTtl != null) || (negativeTtl != null))) {
      throw new IllegalStateException("authoritativeDnsServerCache and TTLs are mutually exclusive");
    }
    
    DnsCache resolveCache = this.resolveCache != null ? this.resolveCache : newCache();
    DnsCnameCache cnameCache = this.cnameCache != null ? this.cnameCache : newCnameCache();
    
    AuthoritativeDnsServerCache authoritativeDnsServerCache = this.authoritativeDnsServerCache != null ? this.authoritativeDnsServerCache : newAuthoritativeDnsServerCache();
    return new DnsNameResolver(eventLoop, channelFactory, socketChannelFactory, resolveCache, cnameCache, authoritativeDnsServerCache, localAddress, dnsQueryLifecycleObserverFactory, queryTimeoutMillis, resolvedAddressTypes, recursionDesired, maxQueriesPerResolve, traceEnabled, maxPayloadSize, optResourceEnabled, hostsFileEntriesResolver, dnsServerAddressStreamProvider, searchDomains, ndots, decodeIdn, completeOncePreferredResolved);
  }
  

























  public DnsNameResolverBuilder copy()
  {
    DnsNameResolverBuilder copiedBuilder = new DnsNameResolverBuilder();
    
    if (eventLoop != null) {
      copiedBuilder.eventLoop(eventLoop);
    }
    
    if (channelFactory != null) {
      copiedBuilder.channelFactory(channelFactory);
    }
    
    if (socketChannelFactory != null) {
      copiedBuilder.socketChannelFactory(socketChannelFactory);
    }
    
    if (resolveCache != null) {
      copiedBuilder.resolveCache(resolveCache);
    }
    
    if (cnameCache != null) {
      copiedBuilder.cnameCache(cnameCache);
    }
    if ((maxTtl != null) && (minTtl != null)) {
      copiedBuilder.ttl(minTtl.intValue(), maxTtl.intValue());
    }
    
    if (negativeTtl != null) {
      copiedBuilder.negativeTtl(negativeTtl.intValue());
    }
    
    if (authoritativeDnsServerCache != null) {
      copiedBuilder.authoritativeDnsServerCache(authoritativeDnsServerCache);
    }
    
    if (dnsQueryLifecycleObserverFactory != null) {
      copiedBuilder.dnsQueryLifecycleObserverFactory(dnsQueryLifecycleObserverFactory);
    }
    
    copiedBuilder.queryTimeoutMillis(queryTimeoutMillis);
    copiedBuilder.resolvedAddressTypes(resolvedAddressTypes);
    copiedBuilder.recursionDesired(recursionDesired);
    copiedBuilder.maxQueriesPerResolve(maxQueriesPerResolve);
    copiedBuilder.traceEnabled(traceEnabled);
    copiedBuilder.maxPayloadSize(maxPayloadSize);
    copiedBuilder.optResourceEnabled(optResourceEnabled);
    copiedBuilder.hostsFileEntriesResolver(hostsFileEntriesResolver);
    
    if (dnsServerAddressStreamProvider != null) {
      copiedBuilder.nameServerProvider(dnsServerAddressStreamProvider);
    }
    
    if (searchDomains != null) {
      copiedBuilder.searchDomains(Arrays.asList(searchDomains));
    }
    
    copiedBuilder.ndots(ndots);
    copiedBuilder.decodeIdn(decodeIdn);
    copiedBuilder.completeOncePreferredResolved(completeOncePreferredResolved);
    
    return copiedBuilder;
  }
}
