package io.netty.resolver.dns;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.AddressedEnvelope;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFactory;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.InternetProtocolFamily;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.dns.DatagramDnsQueryEncoder;
import io.netty.handler.codec.dns.DatagramDnsResponse;
import io.netty.handler.codec.dns.DatagramDnsResponseDecoder;
import io.netty.handler.codec.dns.DefaultDnsRawRecord;
import io.netty.handler.codec.dns.DnsQuestion;
import io.netty.handler.codec.dns.DnsRawRecord;
import io.netty.handler.codec.dns.DnsRecord;
import io.netty.handler.codec.dns.DnsRecordType;
import io.netty.handler.codec.dns.DnsResponse;
import io.netty.handler.codec.dns.TcpDnsQueryEncoder;
import io.netty.handler.codec.dns.TcpDnsResponseDecoder;
import io.netty.resolver.DefaultHostsFileEntriesResolver;
import io.netty.resolver.HostsFileEntriesResolver;
import io.netty.resolver.InetNameResolver;
import io.netty.resolver.ResolvedAddressTypes;
import io.netty.util.NetUtil;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.EmptyArrays;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.lang.reflect.Method;
import java.net.IDN;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DnsNameResolver
  extends InetNameResolver
{
  private static final InternalLogger logger;
  private static final String LOCALHOST = "localhost";
  private static final String WINDOWS_HOST_NAME;
  private static final InetAddress LOCALHOST_ADDRESS;
  private static final DnsRecord[] EMPTY_ADDITIONALS;
  private static final DnsRecordType[] IPV4_ONLY_RESOLVED_RECORD_TYPES;
  private static final InternetProtocolFamily[] IPV4_ONLY_RESOLVED_PROTOCOL_FAMILIES;
  private static final DnsRecordType[] IPV4_PREFERRED_RESOLVED_RECORD_TYPES;
  private static final InternetProtocolFamily[] IPV4_PREFERRED_RESOLVED_PROTOCOL_FAMILIES;
  private static final DnsRecordType[] IPV6_ONLY_RESOLVED_RECORD_TYPES;
  private static final InternetProtocolFamily[] IPV6_ONLY_RESOLVED_PROTOCOL_FAMILIES;
  private static final DnsRecordType[] IPV6_PREFERRED_RESOLVED_RECORD_TYPES;
  private static final InternetProtocolFamily[] IPV6_PREFERRED_RESOLVED_PROTOCOL_FAMILIES;
  static final ResolvedAddressTypes DEFAULT_RESOLVE_ADDRESS_TYPES;
  static final String[] DEFAULT_SEARCH_DOMAINS;
  private static final UnixResolverOptions DEFAULT_OPTIONS;
  
  static
  {
    logger = InternalLoggerFactory.getInstance(DnsNameResolver.class);
    


    EMPTY_ADDITIONALS = new DnsRecord[0];
    IPV4_ONLY_RESOLVED_RECORD_TYPES = new DnsRecordType[] { DnsRecordType.A };
    
    IPV4_ONLY_RESOLVED_PROTOCOL_FAMILIES = new InternetProtocolFamily[] { InternetProtocolFamily.IPv4 };
    
    IPV4_PREFERRED_RESOLVED_RECORD_TYPES = new DnsRecordType[] { DnsRecordType.A, DnsRecordType.AAAA };
    
    IPV4_PREFERRED_RESOLVED_PROTOCOL_FAMILIES = new InternetProtocolFamily[] { InternetProtocolFamily.IPv4, InternetProtocolFamily.IPv6 };
    
    IPV6_ONLY_RESOLVED_RECORD_TYPES = new DnsRecordType[] { DnsRecordType.AAAA };
    
    IPV6_ONLY_RESOLVED_PROTOCOL_FAMILIES = new InternetProtocolFamily[] { InternetProtocolFamily.IPv6 };
    
    IPV6_PREFERRED_RESOLVED_RECORD_TYPES = new DnsRecordType[] { DnsRecordType.AAAA, DnsRecordType.A };
    
    IPV6_PREFERRED_RESOLVED_PROTOCOL_FAMILIES = new InternetProtocolFamily[] { InternetProtocolFamily.IPv6, InternetProtocolFamily.IPv4 };
    






    if ((NetUtil.isIpV4StackPreferred()) || (!anyInterfaceSupportsIpV6())) {
      DEFAULT_RESOLVE_ADDRESS_TYPES = ResolvedAddressTypes.IPV4_ONLY;
      LOCALHOST_ADDRESS = NetUtil.LOCALHOST4;
    }
    else if (NetUtil.isIpV6AddressesPreferred()) {
      DEFAULT_RESOLVE_ADDRESS_TYPES = ResolvedAddressTypes.IPV6_PREFERRED;
      LOCALHOST_ADDRESS = NetUtil.LOCALHOST6;
    } else {
      DEFAULT_RESOLVE_ADDRESS_TYPES = ResolvedAddressTypes.IPV4_PREFERRED;
      LOCALHOST_ADDRESS = NetUtil.LOCALHOST4;
    }
    
    String hostName;
    try
    {
      hostName = PlatformDependent.isWindows() ? InetAddress.getLocalHost().getHostName() : null;
    } catch (Exception ignore) { String hostName;
      hostName = null;
    }
    WINDOWS_HOST_NAME = hostName;
    

    String[] searchDomains;
    

    try
    {
      List<String> list = PlatformDependent.isWindows() ? getSearchDomainsHack() : UnixResolverDnsServerAddressStreamProvider.parseEtcResolverSearchDomains();
      searchDomains = (String[])list.toArray(new String[0]);
    } catch (Exception ignore) {
      String[] searchDomains;
      searchDomains = EmptyArrays.EMPTY_STRINGS;
    }
    DEFAULT_SEARCH_DOMAINS = searchDomains;
    UnixResolverOptions options;
    try
    {
      options = UnixResolverDnsServerAddressStreamProvider.parseEtcResolverOptions();
    } catch (Exception ignore) { UnixResolverOptions options;
      options = UnixResolverOptions.newBuilder().build();
    }
    DEFAULT_OPTIONS = options;
  }
  

  private static boolean anyInterfaceSupportsIpV6()
  {
    try
    {
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      while (interfaces.hasMoreElements()) {
        NetworkInterface iface = (NetworkInterface)interfaces.nextElement();
        Enumeration<InetAddress> addresses = iface.getInetAddresses();
        while (addresses.hasMoreElements()) {
          InetAddress inetAddress = (InetAddress)addresses.nextElement();
          if (((inetAddress instanceof Inet6Address)) && (!inetAddress.isAnyLocalAddress()) && 
            (!inetAddress.isLoopbackAddress()) && (!inetAddress.isLinkLocalAddress())) {
            return true;
          }
        }
      }
    } catch (SocketException e) {
      logger.debug("Unable to detect if any interface supports IPv6, assuming IPv4-only", e);
    }
    
    return false;
  }
  

  private static List<String> getSearchDomainsHack()
    throws Exception
  {
    if (PlatformDependent.javaVersion() < 9)
    {

      Class<?> configClass = Class.forName("sun.net.dns.ResolverConfiguration");
      Method open = configClass.getMethod("open", new Class[0]);
      Method nameservers = configClass.getMethod("searchlist", new Class[0]);
      Object instance = open.invoke(null, new Object[0]);
      
      return (List)nameservers.invoke(instance, new Object[0]);
    }
    return Collections.emptyList();
  }
  
  private static final DatagramDnsResponseDecoder DATAGRAM_DECODER = new DatagramDnsResponseDecoder()
  {
    protected DnsResponse decodeResponse(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
      DnsResponse response = super.decodeResponse(ctx, packet);
      if (((ByteBuf)packet.content()).isReadable())
      {


        response.setTruncated(true);
        
        if (DnsNameResolver.logger.isDebugEnabled()) {
          DnsNameResolver.logger.debug("{} RECEIVED: UDP truncated packet received, consider adjusting maxPayloadSize for the {}.", ctx
          
            .channel(), StringUtil.simpleClassName(DnsNameResolver.class));
        }
      }
      return response;
    }
  };
  private static final DatagramDnsQueryEncoder DATAGRAM_ENCODER = new DatagramDnsQueryEncoder();
  private static final TcpDnsQueryEncoder TCP_ENCODER = new TcpDnsQueryEncoder();
  

  final Future<Channel> channelFuture;
  

  final Channel ch;
  
  private final Comparator<InetSocketAddress> nameServerComparator;
  
  final DnsQueryContextManager queryContextManager = new DnsQueryContextManager();
  

  private final DnsCache resolveCache;
  
  private final AuthoritativeDnsServerCache authoritativeDnsServerCache;
  
  private final DnsCnameCache cnameCache;
  
  private final FastThreadLocal<DnsServerAddressStream> nameServerAddrStream = new FastThreadLocal()
  {
    protected DnsServerAddressStream initialValue()
    {
      return dnsServerAddressStreamProvider.nameServerAddressStream("");
    }
  };
  


  private final long queryTimeoutMillis;
  


  private final int maxQueriesPerResolve;
  


  private final ResolvedAddressTypes resolvedAddressTypes;
  

  private final InternetProtocolFamily[] resolvedInternetProtocolFamilies;
  

  private final boolean recursionDesired;
  

  private final int maxPayloadSize;
  

  private final boolean optResourceEnabled;
  

  private final HostsFileEntriesResolver hostsFileEntriesResolver;
  

  private final DnsServerAddressStreamProvider dnsServerAddressStreamProvider;
  

  private final String[] searchDomains;
  

  private final int ndots;
  

  private final boolean supportsAAAARecords;
  

  private final boolean supportsARecords;
  

  private final InternetProtocolFamily preferredAddressType;
  

  private final DnsRecordType[] resolveRecordTypes;
  

  private final boolean decodeIdn;
  

  private final DnsQueryLifecycleObserverFactory dnsQueryLifecycleObserverFactory;
  

  private final boolean completeOncePreferredResolved;
  

  private final ChannelFactory<? extends SocketChannel> socketChannelFactory;
  


  @Deprecated
  public DnsNameResolver(EventLoop eventLoop, ChannelFactory<? extends DatagramChannel> channelFactory, DnsCache resolveCache, DnsCache authoritativeDnsServerCache, DnsQueryLifecycleObserverFactory dnsQueryLifecycleObserverFactory, long queryTimeoutMillis, ResolvedAddressTypes resolvedAddressTypes, boolean recursionDesired, int maxQueriesPerResolve, boolean traceEnabled, int maxPayloadSize, boolean optResourceEnabled, HostsFileEntriesResolver hostsFileEntriesResolver, DnsServerAddressStreamProvider dnsServerAddressStreamProvider, String[] searchDomains, int ndots, boolean decodeIdn)
  {
    this(eventLoop, channelFactory, resolveCache, new AuthoritativeDnsServerCacheAdapter(authoritativeDnsServerCache), dnsQueryLifecycleObserverFactory, queryTimeoutMillis, resolvedAddressTypes, recursionDesired, maxQueriesPerResolve, traceEnabled, maxPayloadSize, optResourceEnabled, hostsFileEntriesResolver, dnsServerAddressStreamProvider, searchDomains, ndots, decodeIdn);
  }
  














































  @Deprecated
  public DnsNameResolver(EventLoop eventLoop, ChannelFactory<? extends DatagramChannel> channelFactory, DnsCache resolveCache, AuthoritativeDnsServerCache authoritativeDnsServerCache, DnsQueryLifecycleObserverFactory dnsQueryLifecycleObserverFactory, long queryTimeoutMillis, ResolvedAddressTypes resolvedAddressTypes, boolean recursionDesired, int maxQueriesPerResolve, boolean traceEnabled, int maxPayloadSize, boolean optResourceEnabled, HostsFileEntriesResolver hostsFileEntriesResolver, DnsServerAddressStreamProvider dnsServerAddressStreamProvider, String[] searchDomains, int ndots, boolean decodeIdn)
  {
    this(eventLoop, channelFactory, null, resolveCache, NoopDnsCnameCache.INSTANCE, authoritativeDnsServerCache, dnsQueryLifecycleObserverFactory, queryTimeoutMillis, resolvedAddressTypes, recursionDesired, maxQueriesPerResolve, traceEnabled, maxPayloadSize, optResourceEnabled, hostsFileEntriesResolver, dnsServerAddressStreamProvider, searchDomains, ndots, decodeIdn, false);
  }
  






















  DnsNameResolver(EventLoop eventLoop, ChannelFactory<? extends DatagramChannel> channelFactory, ChannelFactory<? extends SocketChannel> socketChannelFactory, DnsCache resolveCache, DnsCnameCache cnameCache, AuthoritativeDnsServerCache authoritativeDnsServerCache, DnsQueryLifecycleObserverFactory dnsQueryLifecycleObserverFactory, long queryTimeoutMillis, ResolvedAddressTypes resolvedAddressTypes, boolean recursionDesired, int maxQueriesPerResolve, boolean traceEnabled, int maxPayloadSize, boolean optResourceEnabled, HostsFileEntriesResolver hostsFileEntriesResolver, DnsServerAddressStreamProvider dnsServerAddressStreamProvider, String[] searchDomains, int ndots, boolean decodeIdn, boolean completeOncePreferredResolved)
  {
    this(eventLoop, channelFactory, socketChannelFactory, resolveCache, cnameCache, authoritativeDnsServerCache, null, dnsQueryLifecycleObserverFactory, queryTimeoutMillis, resolvedAddressTypes, recursionDesired, maxQueriesPerResolve, traceEnabled, maxPayloadSize, optResourceEnabled, hostsFileEntriesResolver, dnsServerAddressStreamProvider, searchDomains, ndots, decodeIdn, completeOncePreferredResolved);
  }
  
























  DnsNameResolver(EventLoop eventLoop, ChannelFactory<? extends DatagramChannel> channelFactory, ChannelFactory<? extends SocketChannel> socketChannelFactory, final DnsCache resolveCache, final DnsCnameCache cnameCache, final AuthoritativeDnsServerCache authoritativeDnsServerCache, SocketAddress localAddress, DnsQueryLifecycleObserverFactory dnsQueryLifecycleObserverFactory, long queryTimeoutMillis, ResolvedAddressTypes resolvedAddressTypes, boolean recursionDesired, int maxQueriesPerResolve, boolean traceEnabled, int maxPayloadSize, boolean optResourceEnabled, HostsFileEntriesResolver hostsFileEntriesResolver, DnsServerAddressStreamProvider dnsServerAddressStreamProvider, String[] searchDomains, int ndots, boolean decodeIdn, boolean completeOncePreferredResolved)
  {
    super(eventLoop);
    this.queryTimeoutMillis = (queryTimeoutMillis > 0L ? queryTimeoutMillis : TimeUnit.SECONDS
    
      .toMillis(DEFAULT_OPTIONS.timeout()));
    this.resolvedAddressTypes = (resolvedAddressTypes != null ? resolvedAddressTypes : DEFAULT_RESOLVE_ADDRESS_TYPES);
    this.recursionDesired = recursionDesired;
    this.maxQueriesPerResolve = (maxQueriesPerResolve > 0 ? maxQueriesPerResolve : DEFAULT_OPTIONS.attempts());
    this.maxPayloadSize = ObjectUtil.checkPositive(maxPayloadSize, "maxPayloadSize");
    this.optResourceEnabled = optResourceEnabled;
    this.hostsFileEntriesResolver = ((HostsFileEntriesResolver)ObjectUtil.checkNotNull(hostsFileEntriesResolver, "hostsFileEntriesResolver"));
    this.dnsServerAddressStreamProvider = 
      ((DnsServerAddressStreamProvider)ObjectUtil.checkNotNull(dnsServerAddressStreamProvider, "dnsServerAddressStreamProvider"));
    this.resolveCache = ((DnsCache)ObjectUtil.checkNotNull(resolveCache, "resolveCache"));
    this.cnameCache = ((DnsCnameCache)ObjectUtil.checkNotNull(cnameCache, "cnameCache"));
    this.dnsQueryLifecycleObserverFactory = (traceEnabled ? new BiDnsQueryLifecycleObserverFactory(new LoggingDnsQueryLifeCycleObserverFactory(), dnsQueryLifecycleObserverFactory) : (dnsQueryLifecycleObserverFactory instanceof NoopDnsQueryLifecycleObserverFactory) ? new LoggingDnsQueryLifeCycleObserverFactory() : 
    



      (DnsQueryLifecycleObserverFactory)ObjectUtil.checkNotNull(dnsQueryLifecycleObserverFactory, "dnsQueryLifecycleObserverFactory"));
    this.searchDomains = (searchDomains != null ? (String[])searchDomains.clone() : DEFAULT_SEARCH_DOMAINS);
    this.ndots = (ndots >= 0 ? ndots : DEFAULT_OPTIONS.ndots());
    this.decodeIdn = decodeIdn;
    this.completeOncePreferredResolved = completeOncePreferredResolved;
    this.socketChannelFactory = socketChannelFactory;
    switch (7.$SwitchMap$io$netty$resolver$ResolvedAddressTypes[this.resolvedAddressTypes.ordinal()]) {
    case 1: 
      supportsAAAARecords = false;
      supportsARecords = true;
      resolveRecordTypes = IPV4_ONLY_RESOLVED_RECORD_TYPES;
      resolvedInternetProtocolFamilies = IPV4_ONLY_RESOLVED_PROTOCOL_FAMILIES;
      break;
    case 2: 
      supportsAAAARecords = true;
      supportsARecords = true;
      resolveRecordTypes = IPV4_PREFERRED_RESOLVED_RECORD_TYPES;
      resolvedInternetProtocolFamilies = IPV4_PREFERRED_RESOLVED_PROTOCOL_FAMILIES;
      break;
    case 3: 
      supportsAAAARecords = true;
      supportsARecords = false;
      resolveRecordTypes = IPV6_ONLY_RESOLVED_RECORD_TYPES;
      resolvedInternetProtocolFamilies = IPV6_ONLY_RESOLVED_PROTOCOL_FAMILIES;
      break;
    case 4: 
      supportsAAAARecords = true;
      supportsARecords = true;
      resolveRecordTypes = IPV6_PREFERRED_RESOLVED_RECORD_TYPES;
      resolvedInternetProtocolFamilies = IPV6_PREFERRED_RESOLVED_PROTOCOL_FAMILIES;
      break;
    default: 
      throw new IllegalArgumentException("Unknown ResolvedAddressTypes " + resolvedAddressTypes);
    }
    preferredAddressType = preferredAddressType(this.resolvedAddressTypes);
    this.authoritativeDnsServerCache = ((AuthoritativeDnsServerCache)ObjectUtil.checkNotNull(authoritativeDnsServerCache, "authoritativeDnsServerCache"));
    nameServerComparator = new NameServerComparator(preferredAddressType.addressType());
    
    Bootstrap b = new Bootstrap();
    b.group(executor());
    b.channelFactory(channelFactory);
    b.option(ChannelOption.DATAGRAM_CHANNEL_ACTIVE_ON_REGISTRATION, Boolean.valueOf(true));
    final DnsResponseHandler responseHandler = new DnsResponseHandler(executor().newPromise());
    b.handler(new ChannelInitializer()
    {
      protected void initChannel(DatagramChannel ch) {
        ch.pipeline().addLast(new ChannelHandler[] { DnsNameResolver.DATAGRAM_ENCODER, DnsNameResolver.DATAGRAM_DECODER, responseHandler });
      }
      
    });
    channelFuture = channelActivePromise;
    ChannelFuture future;
    ChannelFuture future; if (localAddress == null) {
      future = b.register();
    } else {
      future = b.bind(localAddress);
    }
    Throwable cause = future.cause();
    if (cause != null) {
      if ((cause instanceof RuntimeException)) {
        throw ((RuntimeException)cause);
      }
      if ((cause instanceof Error)) {
        throw ((Error)cause);
      }
      throw new IllegalStateException("Unable to create / register Channel", cause);
    }
    ch = future.channel();
    ch.config().setRecvByteBufAllocator(new FixedRecvByteBufAllocator(maxPayloadSize));
    
    ch.closeFuture().addListener(new ChannelFutureListener()
    {
      public void operationComplete(ChannelFuture future) {
        resolveCache.clear();
        cnameCache.clear();
        authoritativeDnsServerCache.clear();
      }
    });
  }
  
  static InternetProtocolFamily preferredAddressType(ResolvedAddressTypes resolvedAddressTypes) {
    switch (7.$SwitchMap$io$netty$resolver$ResolvedAddressTypes[resolvedAddressTypes.ordinal()]) {
    case 1: 
    case 2: 
      return InternetProtocolFamily.IPv4;
    case 3: 
    case 4: 
      return InternetProtocolFamily.IPv6;
    }
    throw new IllegalArgumentException("Unknown ResolvedAddressTypes " + resolvedAddressTypes);
  }
  

  InetSocketAddress newRedirectServerAddress(InetAddress server)
  {
    return new InetSocketAddress(server, 53);
  }
  
  final DnsQueryLifecycleObserverFactory dnsQueryLifecycleObserverFactory() {
    return dnsQueryLifecycleObserverFactory;
  }
  











  protected DnsServerAddressStream newRedirectDnsServerStream(String hostname, List<InetSocketAddress> nameservers)
  {
    DnsServerAddressStream cached = authoritativeDnsServerCache().get(hostname);
    if ((cached == null) || (cached.size() == 0))
    {

      Collections.sort(nameservers, nameServerComparator);
      return new SequentialDnsServerAddressStream(nameservers, 0);
    }
    return cached;
  }
  


  public DnsCache resolveCache()
  {
    return resolveCache;
  }
  


  public DnsCnameCache cnameCache()
  {
    return cnameCache;
  }
  


  public AuthoritativeDnsServerCache authoritativeDnsServerCache()
  {
    return authoritativeDnsServerCache;
  }
  



  public long queryTimeoutMillis()
  {
    return queryTimeoutMillis;
  }
  



  public ResolvedAddressTypes resolvedAddressTypes()
  {
    return resolvedAddressTypes;
  }
  
  InternetProtocolFamily[] resolvedInternetProtocolFamiliesUnsafe() {
    return resolvedInternetProtocolFamilies;
  }
  
  final String[] searchDomains() {
    return searchDomains;
  }
  
  final int ndots() {
    return ndots;
  }
  
  final boolean supportsAAAARecords() {
    return supportsAAAARecords;
  }
  
  final boolean supportsARecords() {
    return supportsARecords;
  }
  
  final InternetProtocolFamily preferredAddressType() {
    return preferredAddressType;
  }
  
  final DnsRecordType[] resolveRecordTypes() {
    return resolveRecordTypes;
  }
  
  final boolean isDecodeIdn() {
    return decodeIdn;
  }
  



  public boolean isRecursionDesired()
  {
    return recursionDesired;
  }
  



  public int maxQueriesPerResolve()
  {
    return maxQueriesPerResolve;
  }
  


  public int maxPayloadSize()
  {
    return maxPayloadSize;
  }
  



  public boolean isOptResourceEnabled()
  {
    return optResourceEnabled;
  }
  



  public HostsFileEntriesResolver hostsFileEntriesResolver()
  {
    return hostsFileEntriesResolver;
  }
  





  public void close()
  {
    if (ch.isOpen()) {
      ch.close();
    }
  }
  
  protected EventLoop executor()
  {
    return (EventLoop)super.executor();
  }
  
  private InetAddress resolveHostsFileEntry(String hostname) {
    if (hostsFileEntriesResolver == null) {
      return null;
    }
    InetAddress address = hostsFileEntriesResolver.address(hostname, resolvedAddressTypes);
    return (address == null) && (isLocalWindowsHost(hostname)) ? LOCALHOST_ADDRESS : address;
  }
  
  private List<InetAddress> resolveHostsFileEntries(String hostname) {
    if (hostsFileEntriesResolver == null)
      return null;
    List<InetAddress> addresses;
    List<InetAddress> addresses;
    if ((hostsFileEntriesResolver instanceof DefaultHostsFileEntriesResolver))
    {
      addresses = ((DefaultHostsFileEntriesResolver)hostsFileEntriesResolver).addresses(hostname, resolvedAddressTypes);
    } else {
      InetAddress address = hostsFileEntriesResolver.address(hostname, resolvedAddressTypes);
      addresses = address != null ? Collections.singletonList(address) : null;
    }
    return (addresses == null) && (isLocalWindowsHost(hostname)) ? 
      Collections.singletonList(LOCALHOST_ADDRESS) : addresses;
  }
  






  private static boolean isLocalWindowsHost(String hostname)
  {
    return (PlatformDependent.isWindows()) && (
      ("localhost".equalsIgnoreCase(hostname)) || ((WINDOWS_HOST_NAME != null) && 
      (WINDOWS_HOST_NAME.equalsIgnoreCase(hostname))));
  }
  







  public final Future<InetAddress> resolve(String inetHost, Iterable<DnsRecord> additionals)
  {
    return resolve(inetHost, additionals, executor().newPromise());
  }
  









  public final Future<InetAddress> resolve(String inetHost, Iterable<DnsRecord> additionals, Promise<InetAddress> promise)
  {
    ObjectUtil.checkNotNull(promise, "promise");
    DnsRecord[] additionalsArray = toArray(additionals, true);
    try {
      doResolve(inetHost, additionalsArray, promise, resolveCache);
      return promise;
    } catch (Exception e) {
      return promise.setFailure(e);
    }
  }
  







  public final Future<List<InetAddress>> resolveAll(String inetHost, Iterable<DnsRecord> additionals)
  {
    return resolveAll(inetHost, additionals, executor().newPromise());
  }
  









  public final Future<List<InetAddress>> resolveAll(String inetHost, Iterable<DnsRecord> additionals, Promise<List<InetAddress>> promise)
  {
    ObjectUtil.checkNotNull(promise, "promise");
    DnsRecord[] additionalsArray = toArray(additionals, true);
    try {
      doResolveAll(inetHost, additionalsArray, promise, resolveCache);
      return promise;
    } catch (Exception e) {
      return promise.setFailure(e);
    }
  }
  
  protected void doResolve(String inetHost, Promise<InetAddress> promise) throws Exception
  {
    doResolve(inetHost, EMPTY_ADDITIONALS, promise, resolveCache);
  }
  










  public final Future<List<DnsRecord>> resolveAll(DnsQuestion question)
  {
    return resolveAll(question, EMPTY_ADDITIONALS, executor().newPromise());
  }
  











  public final Future<List<DnsRecord>> resolveAll(DnsQuestion question, Iterable<DnsRecord> additionals)
  {
    return resolveAll(question, additionals, executor().newPromise());
  }
  













  public final Future<List<DnsRecord>> resolveAll(DnsQuestion question, Iterable<DnsRecord> additionals, Promise<List<DnsRecord>> promise)
  {
    DnsRecord[] additionalsArray = toArray(additionals, true);
    return resolveAll(question, additionalsArray, promise);
  }
  
  private Future<List<DnsRecord>> resolveAll(DnsQuestion question, DnsRecord[] additionals, Promise<List<DnsRecord>> promise)
  {
    ObjectUtil.checkNotNull(question, "question");
    ObjectUtil.checkNotNull(promise, "promise");
    

    DnsRecordType type = question.type();
    String hostname = question.name();
    
    if ((type == DnsRecordType.A) || (type == DnsRecordType.AAAA)) {
      List<InetAddress> hostsFileEntries = resolveHostsFileEntries(hostname);
      if (hostsFileEntries != null) {
        List<DnsRecord> result = new ArrayList();
        for (InetAddress hostsFileEntry : hostsFileEntries) {
          ByteBuf content = null;
          if ((hostsFileEntry instanceof Inet4Address)) {
            if (type == DnsRecordType.A) {
              content = Unpooled.wrappedBuffer(hostsFileEntry.getAddress());
            }
          } else if (((hostsFileEntry instanceof Inet6Address)) && 
            (type == DnsRecordType.AAAA)) {
            content = Unpooled.wrappedBuffer(hostsFileEntry.getAddress());
          }
          
          if (content != null)
          {

            result.add(new DefaultDnsRawRecord(hostname, type, 86400L, content));
          }
        }
        
        if (!result.isEmpty()) {
          trySuccess(promise, result);
          return promise;
        }
      }
    }
    


    DnsServerAddressStream nameServerAddrs = dnsServerAddressStreamProvider.nameServerAddressStream(hostname);
    new DnsRecordResolveContext(this, promise, question, additionals, nameServerAddrs, maxQueriesPerResolve)
      .resolve(promise);
    return promise;
  }
  
  private static DnsRecord[] toArray(Iterable<DnsRecord> additionals, boolean validateType) {
    ObjectUtil.checkNotNull(additionals, "additionals");
    if ((additionals instanceof Collection)) {
      Collection<DnsRecord> records = (Collection)additionals;
      for (DnsRecord r : additionals) {
        validateAdditional(r, validateType);
      }
      return (DnsRecord[])records.toArray(new DnsRecord[records.size()]);
    }
    
    Iterator<DnsRecord> additionalsIt = additionals.iterator();
    if (!additionalsIt.hasNext()) {
      return EMPTY_ADDITIONALS;
    }
    Object records = new ArrayList();
    do {
      DnsRecord r = (DnsRecord)additionalsIt.next();
      validateAdditional(r, validateType);
      ((List)records).add(r);
    } while (additionalsIt.hasNext());
    
    return (DnsRecord[])((List)records).toArray(new DnsRecord[((List)records).size()]);
  }
  
  private static void validateAdditional(DnsRecord record, boolean validateType) {
    ObjectUtil.checkNotNull(record, "record");
    if ((validateType) && ((record instanceof DnsRawRecord))) {
      throw new IllegalArgumentException("DnsRawRecord implementations not allowed: " + record);
    }
  }
  
  private InetAddress loopbackAddress() {
    return preferredAddressType().localhost();
  }
  





  protected void doResolve(String inetHost, DnsRecord[] additionals, Promise<InetAddress> promise, DnsCache resolveCache)
    throws Exception
  {
    if ((inetHost == null) || (inetHost.isEmpty()))
    {
      promise.setSuccess(loopbackAddress());
      return;
    }
    byte[] bytes = NetUtil.createByteArrayFromIpAddressString(inetHost);
    if (bytes != null)
    {
      promise.setSuccess(InetAddress.getByAddress(bytes));
      return;
    }
    
    String hostname = hostname(inetHost);
    
    InetAddress hostsFileEntry = resolveHostsFileEntry(hostname);
    if (hostsFileEntry != null) {
      promise.setSuccess(hostsFileEntry);
      return;
    }
    
    if (!doResolveCached(hostname, additionals, promise, resolveCache)) {
      doResolveUncached(hostname, additionals, promise, resolveCache, true);
    }
  }
  


  private boolean doResolveCached(String hostname, DnsRecord[] additionals, Promise<InetAddress> promise, DnsCache resolveCache)
  {
    List<? extends DnsCacheEntry> cachedEntries = resolveCache.get(hostname, additionals);
    if ((cachedEntries == null) || (cachedEntries.isEmpty())) {
      return false;
    }
    
    Throwable cause = ((DnsCacheEntry)cachedEntries.get(0)).cause();
    if (cause == null) {
      int numEntries = cachedEntries.size();
      
      for (InternetProtocolFamily f : resolvedInternetProtocolFamilies) {
        for (int i = 0; i < numEntries; i++) {
          DnsCacheEntry e = (DnsCacheEntry)cachedEntries.get(i);
          if (f.addressType().isInstance(e.address())) {
            trySuccess(promise, e.address());
            return true;
          }
        }
      }
      return false;
    }
    tryFailure(promise, cause);
    return true;
  }
  
  static <T> boolean trySuccess(Promise<T> promise, T result)
  {
    boolean notifiedRecords = promise.trySuccess(result);
    if (!notifiedRecords)
    {


      logger.trace("Failed to notify success ({}) to a promise: {}", result, promise);
    }
    return notifiedRecords;
  }
  
  private static void tryFailure(Promise<?> promise, Throwable cause) {
    if (!promise.tryFailure(cause))
    {


      logger.trace("Failed to notify failure to a promise: {}", promise, cause);
    }
  }
  


  private void doResolveUncached(String hostname, DnsRecord[] additionals, final Promise<InetAddress> promise, DnsCache resolveCache, boolean completeEarlyIfPossible)
  {
    Promise<List<InetAddress>> allPromise = executor().newPromise();
    doResolveAllUncached(hostname, additionals, promise, allPromise, resolveCache, true);
    allPromise.addListener(new FutureListener()
    {
      public void operationComplete(Future<List<InetAddress>> future) {
        if (future.isSuccess()) {
          DnsNameResolver.trySuccess(promise, ((List)future.getNow()).get(0));
        } else {
          DnsNameResolver.tryFailure(promise, future.cause());
        }
      }
    });
  }
  
  protected void doResolveAll(String inetHost, Promise<List<InetAddress>> promise) throws Exception
  {
    doResolveAll(inetHost, EMPTY_ADDITIONALS, promise, resolveCache);
  }
  





  protected void doResolveAll(String inetHost, DnsRecord[] additionals, Promise<List<InetAddress>> promise, DnsCache resolveCache)
    throws Exception
  {
    if ((inetHost == null) || (inetHost.isEmpty()))
    {
      promise.setSuccess(Collections.singletonList(loopbackAddress()));
      return;
    }
    byte[] bytes = NetUtil.createByteArrayFromIpAddressString(inetHost);
    if (bytes != null)
    {
      promise.setSuccess(Collections.singletonList(InetAddress.getByAddress(bytes)));
      return;
    }
    
    String hostname = hostname(inetHost);
    
    List<InetAddress> hostsFileEntries = resolveHostsFileEntries(hostname);
    if (hostsFileEntries != null) {
      promise.setSuccess(hostsFileEntries);
      return;
    }
    
    if (!doResolveAllCached(hostname, additionals, promise, resolveCache, resolvedInternetProtocolFamilies)) {
      doResolveAllUncached(hostname, additionals, promise, promise, resolveCache, completeOncePreferredResolved);
    }
  }
  




  static boolean doResolveAllCached(String hostname, DnsRecord[] additionals, Promise<List<InetAddress>> promise, DnsCache resolveCache, InternetProtocolFamily[] resolvedInternetProtocolFamilies)
  {
    List<? extends DnsCacheEntry> cachedEntries = resolveCache.get(hostname, additionals);
    if ((cachedEntries == null) || (cachedEntries.isEmpty())) {
      return false;
    }
    
    Throwable cause = ((DnsCacheEntry)cachedEntries.get(0)).cause();
    if (cause == null) {
      List<InetAddress> result = null;
      int numEntries = cachedEntries.size();
      for (InternetProtocolFamily f : resolvedInternetProtocolFamilies) {
        for (int i = 0; i < numEntries; i++) {
          DnsCacheEntry e = (DnsCacheEntry)cachedEntries.get(i);
          if (f.addressType().isInstance(e.address())) {
            if (result == null) {
              result = new ArrayList(numEntries);
            }
            result.add(e.address());
          }
        }
      }
      if (result != null) {
        trySuccess(promise, result);
        return true;
      }
      return false;
    }
    tryFailure(promise, cause);
    return true;
  }
  







  private void doResolveAllUncached(final String hostname, final DnsRecord[] additionals, final Promise<?> originalPromise, final Promise<List<InetAddress>> promise, final DnsCache resolveCache, final boolean completeEarlyIfPossible)
  {
    EventExecutor executor = executor();
    if (executor.inEventLoop()) {
      doResolveAllUncached0(hostname, additionals, originalPromise, promise, resolveCache, completeEarlyIfPossible);
    }
    else {
      executor.execute(new Runnable()
      {
        public void run() {
          DnsNameResolver.this.doResolveAllUncached0(hostname, additionals, originalPromise, promise, resolveCache, completeEarlyIfPossible);
        }
      });
    }
  }
  






  private void doResolveAllUncached0(String hostname, DnsRecord[] additionals, Promise<?> originalPromise, Promise<List<InetAddress>> promise, DnsCache resolveCache, boolean completeEarlyIfPossible)
  {
    assert (executor().inEventLoop());
    

    DnsServerAddressStream nameServerAddrs = dnsServerAddressStreamProvider.nameServerAddressStream(hostname);
    new DnsAddressResolveContext(this, originalPromise, hostname, additionals, nameServerAddrs, maxQueriesPerResolve, resolveCache, authoritativeDnsServerCache, completeEarlyIfPossible)
    

      .resolve(promise);
  }
  
  private static String hostname(String inetHost) {
    String hostname = IDN.toASCII(inetHost);
    
    if ((StringUtil.endsWith(inetHost, '.')) && (!StringUtil.endsWith(hostname, '.'))) {
      hostname = hostname + ".";
    }
    return hostname;
  }
  


  public Future<AddressedEnvelope<DnsResponse, InetSocketAddress>> query(DnsQuestion question)
  {
    return query(nextNameServerAddress(), question);
  }
  



  public Future<AddressedEnvelope<DnsResponse, InetSocketAddress>> query(DnsQuestion question, Iterable<DnsRecord> additionals)
  {
    return query(nextNameServerAddress(), question, additionals);
  }
  



  public Future<AddressedEnvelope<DnsResponse, InetSocketAddress>> query(DnsQuestion question, Promise<AddressedEnvelope<? extends DnsResponse, InetSocketAddress>> promise)
  {
    return query(nextNameServerAddress(), question, Collections.emptyList(), promise);
  }
  
  private InetSocketAddress nextNameServerAddress() {
    return ((DnsServerAddressStream)nameServerAddrStream.get()).next();
  }
  




  public Future<AddressedEnvelope<DnsResponse, InetSocketAddress>> query(InetSocketAddress nameServerAddr, DnsQuestion question)
  {
    return query0(nameServerAddr, question, EMPTY_ADDITIONALS, true, ch.newPromise(), ch
      .eventLoop().newPromise());
  }
  




  public Future<AddressedEnvelope<DnsResponse, InetSocketAddress>> query(InetSocketAddress nameServerAddr, DnsQuestion question, Iterable<DnsRecord> additionals)
  {
    return query0(nameServerAddr, question, toArray(additionals, false), true, ch.newPromise(), ch
      .eventLoop().newPromise());
  }
  





  public Future<AddressedEnvelope<DnsResponse, InetSocketAddress>> query(InetSocketAddress nameServerAddr, DnsQuestion question, Promise<AddressedEnvelope<? extends DnsResponse, InetSocketAddress>> promise)
  {
    return query0(nameServerAddr, question, EMPTY_ADDITIONALS, true, ch.newPromise(), promise);
  }
  






  public Future<AddressedEnvelope<DnsResponse, InetSocketAddress>> query(InetSocketAddress nameServerAddr, DnsQuestion question, Iterable<DnsRecord> additionals, Promise<AddressedEnvelope<? extends DnsResponse, InetSocketAddress>> promise)
  {
    return query0(nameServerAddr, question, toArray(additionals, false), true, ch.newPromise(), promise);
  }
  




  public static boolean isTransportOrTimeoutError(Throwable cause)
  {
    return (cause != null) && ((cause.getCause() instanceof DnsNameResolverException));
  }
  




  public static boolean isTimeoutError(Throwable cause)
  {
    return (cause != null) && ((cause.getCause() instanceof DnsNameResolverTimeoutException));
  }
  
  final void flushQueries() {
    ch.flush();
  }
  




  final Future<AddressedEnvelope<DnsResponse, InetSocketAddress>> query0(InetSocketAddress nameServerAddr, DnsQuestion question, DnsRecord[] additionals, boolean flush, ChannelPromise writePromise, Promise<AddressedEnvelope<? extends DnsResponse, InetSocketAddress>> promise)
  {
    assert (!writePromise.isVoid());
    
    Promise<AddressedEnvelope<DnsResponse, InetSocketAddress>> castPromise = cast(
      (Promise)ObjectUtil.checkNotNull(promise, "promise"));
    try
    {
      new DatagramDnsQueryContext(this, nameServerAddr, question, additionals, castPromise).query(flush, writePromise);
      return castPromise;
    } catch (Exception e) {
      return castPromise.setFailure(e);
    }
  }
  
  private static Promise<AddressedEnvelope<DnsResponse, InetSocketAddress>> cast(Promise<?> promise)
  {
    return promise;
  }
  
  final DnsServerAddressStream newNameServerAddressStream(String hostname) {
    return dnsServerAddressStreamProvider.nameServerAddressStream(hostname);
  }
  
  private final class DnsResponseHandler extends ChannelInboundHandlerAdapter
  {
    private final Promise<Channel> channelActivePromise;
    
    DnsResponseHandler() {
      this.channelActivePromise = channelActivePromise;
    }
    
    public void channelRead(ChannelHandlerContext ctx, Object msg)
    {
      final DatagramDnsResponse res = (DatagramDnsResponse)msg;
      final int queryId = res.id();
      
      if (DnsNameResolver.logger.isDebugEnabled()) {
        DnsNameResolver.logger.debug("{} RECEIVED: UDP [{}: {}], {}", new Object[] { ch, Integer.valueOf(queryId), res.sender(), res });
      }
      
      final DnsQueryContext qCtx = queryContextManager.get(res.sender(), queryId);
      if (qCtx == null) {
        DnsNameResolver.logger.debug("Received a DNS response with an unknown ID: UDP [{}: {}]", ch, Integer.valueOf(queryId));
        res.release();
        return;
      }
      

      if ((!res.isTruncated()) || (socketChannelFactory == null)) {
        qCtx.finish(res);
        return;
      }
      
      Bootstrap bs = new Bootstrap();
      

      ((Bootstrap)((Bootstrap)((Bootstrap)bs.option(ChannelOption.SO_REUSEADDR, Boolean.valueOf(true))).group(executor())).channelFactory(socketChannelFactory))
        .handler(DnsNameResolver.TCP_ENCODER);
      bs.connect(res.sender()).addListener(new ChannelFutureListener()
      {
        public void operationComplete(ChannelFuture future) {
          if (!future.isSuccess()) {
            if (DnsNameResolver.logger.isDebugEnabled()) {
              DnsNameResolver.logger.debug("Unable to fallback to TCP [{}]", Integer.valueOf(queryId), future.cause());
            }
            

            qCtx.finish(res);
            return;
          }
          final Channel channel = future.channel();
          

          Promise<AddressedEnvelope<DnsResponse, InetSocketAddress>> promise = channel.eventLoop().newPromise();
          

          final TcpDnsQueryContext tcpCtx = new TcpDnsQueryContext(DnsNameResolver.this, channel, (InetSocketAddress)channel.remoteAddress(), qCtx.question(), DnsNameResolver.EMPTY_ADDITIONALS, promise);
          
          channel.pipeline().addLast(new ChannelHandler[] { new TcpDnsResponseDecoder() });
          channel.pipeline().addLast(new ChannelHandler[] { new ChannelInboundHandlerAdapter()
          {
            public void channelRead(ChannelHandlerContext ctx, Object msg) {
              Channel channel = ctx.channel();
              DnsResponse response = (DnsResponse)msg;
              int queryId = response.id();
              
              if (DnsNameResolver.logger.isDebugEnabled()) {
                DnsNameResolver.logger.debug("{} RECEIVED: TCP [{}: {}], {}", new Object[] { channel, Integer.valueOf(queryId), channel
                  .remoteAddress(), response });
              }
              
              DnsQueryContext foundCtx = queryContextManager.get(val$res.sender(), queryId);
              if (foundCtx == tcpCtx) {
                tcpCtx.finish(new DnsNameResolver.AddressedEnvelopeAdapter(
                  (InetSocketAddress)ctx.channel().remoteAddress(), 
                  (InetSocketAddress)ctx.channel().localAddress(), response));
              }
              else {
                response.release();
                tcpCtx.tryFailure("Received TCP DNS response with unexpected ID", null, false);
                DnsNameResolver.logger.debug("Received a DNS response with an unexpected ID: TCP [{}: {}]", channel, 
                  Integer.valueOf(queryId));
              }
            }
            
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            {
              if ((tcpCtx.tryFailure("TCP fallback error", cause, false)) && (DnsNameResolver.logger.isDebugEnabled())) {
                DnsNameResolver.logger.debug("{} Error during processing response: TCP [{}: {}]", new Object[] {ctx
                  .channel(), Integer.valueOf(val$queryId), ctx
                  .channel().remoteAddress(), cause });
              }
              
            }
          } });
          promise.addListener(new FutureListener()
          {

            public void operationComplete(Future<AddressedEnvelope<DnsResponse, InetSocketAddress>> future)
            {
              channel.close();
              
              if (future.isSuccess()) {
                val$qCtx.finish((AddressedEnvelope)future.getNow());
                val$res.release();
              }
              else {
                val$qCtx.finish(val$res);
              }
            }
          });
          tcpCtx.query(true, future.channel().newPromise());
        }
      });
    }
    
    public void channelActive(ChannelHandlerContext ctx) throws Exception
    {
      super.channelActive(ctx);
      channelActivePromise.setSuccess(ctx.channel());
    }
    
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    {
      if ((cause instanceof CorruptedFrameException)) {
        DnsNameResolver.logger.debug("Unable to decode DNS response: UDP [{}]", ctx.channel(), cause);
      } else {
        DnsNameResolver.logger.warn("Unexpected exception: UDP [{}]", ctx.channel(), cause);
      }
    }
  }
  
  private static final class AddressedEnvelopeAdapter implements AddressedEnvelope<DnsResponse, InetSocketAddress> {
    private final InetSocketAddress sender;
    private final InetSocketAddress recipient;
    private final DnsResponse response;
    
    AddressedEnvelopeAdapter(InetSocketAddress sender, InetSocketAddress recipient, DnsResponse response) {
      this.sender = sender;
      this.recipient = recipient;
      this.response = response;
    }
    
    public DnsResponse content()
    {
      return response;
    }
    
    public InetSocketAddress sender()
    {
      return sender;
    }
    
    public InetSocketAddress recipient()
    {
      return recipient;
    }
    
    public AddressedEnvelope<DnsResponse, InetSocketAddress> retain()
    {
      response.retain();
      return this;
    }
    
    public AddressedEnvelope<DnsResponse, InetSocketAddress> retain(int increment)
    {
      response.retain(increment);
      return this;
    }
    
    public AddressedEnvelope<DnsResponse, InetSocketAddress> touch()
    {
      response.touch();
      return this;
    }
    
    public AddressedEnvelope<DnsResponse, InetSocketAddress> touch(Object hint)
    {
      response.touch(hint);
      return this;
    }
    
    public int refCnt()
    {
      return response.refCnt();
    }
    
    public boolean release()
    {
      return response.release();
    }
    
    public boolean release(int decrement)
    {
      return response.release(decrement);
    }
    
    public boolean equals(Object obj)
    {
      if (this == obj) {
        return true;
      }
      
      if (!(obj instanceof AddressedEnvelope)) {
        return false;
      }
      

      AddressedEnvelope<?, SocketAddress> that = (AddressedEnvelope)obj;
      if (sender() == null) {
        if (that.sender() != null) {
          return false;
        }
      } else if (!sender().equals(that.sender())) {
        return false;
      }
      
      if (recipient() == null) {
        if (that.recipient() != null) {
          return false;
        }
      } else if (!recipient().equals(that.recipient())) {
        return false;
      }
      
      return response.equals(obj);
    }
    
    public int hashCode()
    {
      int hashCode = response.hashCode();
      if (sender() != null) {
        hashCode = hashCode * 31 + sender().hashCode();
      }
      if (recipient() != null) {
        hashCode = hashCode * 31 + recipient().hashCode();
      }
      return hashCode;
    }
  }
}
