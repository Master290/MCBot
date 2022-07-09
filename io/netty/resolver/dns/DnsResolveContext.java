package io.netty.resolver.dns;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.AddressedEnvelope;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.dns.DefaultDnsQuestion;
import io.netty.handler.codec.dns.DefaultDnsRecordDecoder;
import io.netty.handler.codec.dns.DnsQuestion;
import io.netty.handler.codec.dns.DnsRawRecord;
import io.netty.handler.codec.dns.DnsRecord;
import io.netty.handler.codec.dns.DnsRecordType;
import io.netty.handler.codec.dns.DnsResponse;
import io.netty.handler.codec.dns.DnsResponseCode;
import io.netty.handler.codec.dns.DnsSection;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.SuppressJava6Requirement;
import io.netty.util.internal.ThrowableUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;




















abstract class DnsResolveContext<T>
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(DnsResolveContext.class);
  

  private static final RuntimeException NXDOMAIN_QUERY_FAILED_EXCEPTION = DnsResolveContextException.newStatic("No answer found and NXDOMAIN response code returned", DnsResolveContext.class, "onResponse(..)");
  

  private static final RuntimeException CNAME_NOT_FOUND_QUERY_FAILED_EXCEPTION = DnsResolveContextException.newStatic("No matching CNAME record found", DnsResolveContext.class, "onResponseCNAME(..)");
  

  private static final RuntimeException NO_MATCHING_RECORD_QUERY_FAILED_EXCEPTION = DnsResolveContextException.newStatic("No matching record type found", DnsResolveContext.class, "onResponseAorAAAA(..)");
  

  private static final RuntimeException UNRECOGNIZED_TYPE_QUERY_FAILED_EXCEPTION = DnsResolveContextException.newStatic("Response type was unrecognized", DnsResolveContext.class, "onResponse(..)");
  

  private static final RuntimeException NAME_SERVERS_EXHAUSTED_EXCEPTION = DnsResolveContextException.newStatic("No name servers returned an answer", DnsResolveContext.class, "tryToFinishResolve(..)");
  
  final DnsNameResolver parent;
  
  private final Promise<?> originalPromise;
  
  private final DnsServerAddressStream nameServerAddrs;
  
  private final String hostname;
  private final int dnsClass;
  private final DnsRecordType[] expectedTypes;
  final DnsRecord[] additionals;
  private final Set<Future<AddressedEnvelope<DnsResponse, InetSocketAddress>>> queriesInProgress = Collections.newSetFromMap(new IdentityHashMap());
  
  private List<T> finalResult;
  
  private int allowedQueries;
  
  private boolean triedCNAME;
  private boolean completeEarly;
  
  DnsResolveContext(DnsNameResolver parent, Promise<?> originalPromise, String hostname, int dnsClass, DnsRecordType[] expectedTypes, DnsRecord[] additionals, DnsServerAddressStream nameServerAddrs, int allowedQueries)
  {
    assert (expectedTypes.length > 0);
    
    this.parent = parent;
    this.originalPromise = originalPromise;
    this.hostname = hostname;
    this.dnsClass = dnsClass;
    this.expectedTypes = expectedTypes;
    this.additionals = additionals;
    
    this.nameServerAddrs = ((DnsServerAddressStream)ObjectUtil.checkNotNull(nameServerAddrs, "nameServerAddrs"));
    this.allowedQueries = allowedQueries;
  }
  
  static final class DnsResolveContextException extends RuntimeException
  {
    private static final long serialVersionUID = 1209303419266433003L;
    
    private DnsResolveContextException(String message) {
      super();
    }
    
    @SuppressJava6Requirement(reason="uses Java 7+ Exception.<init>(String, Throwable, boolean, boolean) but is guarded by version checks")
    private DnsResolveContextException(String message, boolean shared)
    {
      super(null, false, true);
      assert (shared);
    }
    




    public Throwable fillInStackTrace() { return this; }
    
    static DnsResolveContextException newStatic(String message, Class<?> clazz, String method) {
      DnsResolveContextException exception;
      DnsResolveContextException exception;
      if (PlatformDependent.javaVersion() >= 7) {
        exception = new DnsResolveContextException(message, true);
      } else {
        exception = new DnsResolveContextException(message);
      }
      return (DnsResolveContextException)ThrowableUtil.unknownStackTrace(exception, clazz, method);
    }
  }
  


  DnsCache resolveCache()
  {
    return parent.resolveCache();
  }
  


  DnsCnameCache cnameCache()
  {
    return parent.cnameCache();
  }
  


  AuthoritativeDnsServerCache authoritativeDnsServerCache()
  {
    return parent.authoritativeDnsServerCache();
  }
  




  abstract DnsResolveContext<T> newResolverContext(DnsNameResolver paramDnsNameResolver, Promise<?> paramPromise, String paramString, int paramInt1, DnsRecordType[] paramArrayOfDnsRecordType, DnsRecord[] paramArrayOfDnsRecord, DnsServerAddressStream paramDnsServerAddressStream, int paramInt2);
  



  abstract T convertRecord(DnsRecord paramDnsRecord, String paramString, DnsRecord[] paramArrayOfDnsRecord, EventLoop paramEventLoop);
  



  abstract List<T> filterResults(List<T> paramList);
  



  abstract boolean isCompleteEarly(T paramT);
  



  abstract boolean isDuplicateAllowed();
  



  abstract void cache(String paramString, DnsRecord[] paramArrayOfDnsRecord, DnsRecord paramDnsRecord, T paramT);
  



  abstract void cache(String paramString, DnsRecord[] paramArrayOfDnsRecord, UnknownHostException paramUnknownHostException);
  



  void resolve(final Promise<List<T>> promise)
  {
    final String[] searchDomains = parent.searchDomains();
    if ((searchDomains.length == 0) || (parent.ndots() == 0) || (StringUtil.endsWith(hostname, '.'))) {
      internalResolve(hostname, promise);
    } else {
      final boolean startWithoutSearchDomain = hasNDots();
      String initialHostname = hostname + '.' + searchDomains[0];
      final int initialSearchDomainIdx = startWithoutSearchDomain ? 0 : 1;
      
      Promise<List<T>> searchDomainPromise = parent.executor().newPromise();
      searchDomainPromise.addListener(new FutureListener() {
        private int searchDomainIdx = initialSearchDomainIdx;
        
        public void operationComplete(Future<List<T>> future) {
          Throwable cause = future.cause();
          if (cause == null) {
            List<T> result = (List)future.getNow();
            if (!promise.trySuccess(result)) {
              for (T item : result) {
                ReferenceCountUtil.safeRelease(item);
              }
            }
          }
          else if (DnsNameResolver.isTransportOrTimeoutError(cause)) {
            promise.tryFailure(new DnsResolveContext.SearchDomainUnknownHostException(cause, hostname));
          } else if (searchDomainIdx < searchDomains.length) {
            Promise<List<T>> newPromise = parent.executor().newPromise();
            newPromise.addListener(this);
            doSearchDomainQuery(hostname + '.' + searchDomains[(searchDomainIdx++)], newPromise);
          } else if (!startWithoutSearchDomain) {
            DnsResolveContext.this.internalResolve(hostname, promise);
          } else {
            promise.tryFailure(new DnsResolveContext.SearchDomainUnknownHostException(cause, hostname));
          }
          
        }
      });
      doSearchDomainQuery(initialHostname, searchDomainPromise);
    }
  }
  
  private boolean hasNDots() {
    int idx = hostname.length() - 1; for (int dots = 0; idx >= 0; idx--) {
      if (hostname.charAt(idx) == '.') { dots++; if (dots >= parent.ndots())
          return true;
      }
    }
    return false;
  }
  
  private static final class SearchDomainUnknownHostException extends UnknownHostException {
    private static final long serialVersionUID = -8573510133644997085L;
    
    SearchDomainUnknownHostException(Throwable cause, String originalHostname) {
      super();
      setStackTrace(cause.getStackTrace());
      

      initCause(cause.getCause());
    }
    

    public Throwable fillInStackTrace()
    {
      return this;
    }
  }
  
  void doSearchDomainQuery(String hostname, Promise<List<T>> nextPromise) {
    DnsResolveContext<T> nextContext = newResolverContext(parent, originalPromise, hostname, dnsClass, expectedTypes, additionals, nameServerAddrs, parent
    
      .maxQueriesPerResolve());
    nextContext.internalResolve(hostname, nextPromise);
  }
  
  private static String hostnameWithDot(String name) {
    if (StringUtil.endsWith(name, '.')) {
      return name;
    }
    return name + '.';
  }
  


  static String cnameResolveFromCache(DnsCnameCache cnameCache, String name)
    throws UnknownHostException
  {
    String first = cnameCache.get(hostnameWithDot(name));
    if (first == null)
    {
      return name;
    }
    
    String second = cnameCache.get(hostnameWithDot(first));
    if (second == null)
    {
      return first;
    }
    
    checkCnameLoop(name, first, second);
    return cnameResolveFromCacheLoop(cnameCache, name, first, second);
  }
  

  private static String cnameResolveFromCacheLoop(DnsCnameCache cnameCache, String hostname, String first, String mapping)
    throws UnknownHostException
  {
    boolean advance = false;
    
    String name = mapping;
    
    while ((mapping = cnameCache.get(hostnameWithDot(name))) != null) {
      checkCnameLoop(hostname, first, mapping);
      name = mapping;
      if (advance) {
        first = cnameCache.get(first);
      }
      advance = !advance;
    }
    return name;
  }
  
  private static void checkCnameLoop(String hostname, String first, String second) throws UnknownHostException {
    if (first.equals(second))
    {
      throw new UnknownHostException("CNAME loop detected for '" + hostname + '\'');
    }
  }
  
  private void internalResolve(String name, Promise<List<T>> promise) {
    try {
      name = cnameResolveFromCache(cnameCache(), name);
    } catch (Throwable cause) {
      promise.tryFailure(cause);
      return;
    }
    try
    {
      DnsServerAddressStream nameServerAddressStream = getNameServers(name);
      
      int end = expectedTypes.length - 1;
      for (int i = 0; i < end; i++) {
        if (!query(name, expectedTypes[i], nameServerAddressStream.duplicate(), false, promise)) {
          return;
        }
      }
      query(name, expectedTypes[end], nameServerAddressStream, false, promise);
    }
    finally {
      parent.flushQueries();
    }
  }
  



  private DnsServerAddressStream getNameServersFromCache(String hostname)
  {
    int len = hostname.length();
    
    if (len == 0)
    {
      return null;
    }
    

    if (hostname.charAt(len - 1) != '.') {
      hostname = hostname + ".";
    }
    
    int idx = hostname.indexOf('.');
    if (idx == hostname.length() - 1)
    {
      return null;
    }
    

    for (;;)
    {
      hostname = hostname.substring(idx + 1);
      
      int idx2 = hostname.indexOf('.');
      if ((idx2 <= 0) || (idx2 == hostname.length() - 1))
      {
        return null;
      }
      idx = idx2;
      
      DnsServerAddressStream entries = authoritativeDnsServerCache().get(hostname);
      if (entries != null)
      {

        return entries;
      }
    }
  }
  





  private void query(final DnsServerAddressStream nameServerAddrStream, final int nameServerAddrStreamIndex, final DnsQuestion question, final DnsQueryLifecycleObserver queryLifecycleObserver, boolean flush, final Promise<List<T>> promise, Throwable cause)
  {
    if ((completeEarly) || (nameServerAddrStreamIndex >= nameServerAddrStream.size()) || (allowedQueries == 0) || 
      (originalPromise.isCancelled()) || (promise.isCancelled())) {
      tryToFinishResolve(nameServerAddrStream, nameServerAddrStreamIndex, question, queryLifecycleObserver, promise, cause);
      
      return;
    }
    
    allowedQueries -= 1;
    
    InetSocketAddress nameServerAddr = nameServerAddrStream.next();
    if (nameServerAddr.isUnresolved()) {
      queryUnresolvedNameServer(nameServerAddr, nameServerAddrStream, nameServerAddrStreamIndex, question, queryLifecycleObserver, promise, cause);
      
      return;
    }
    ChannelPromise writePromise = parent.ch.newPromise();
    
    Promise<AddressedEnvelope<? extends DnsResponse, InetSocketAddress>> queryPromise = parent.ch.eventLoop().newPromise();
    

    Future<AddressedEnvelope<DnsResponse, InetSocketAddress>> f = parent.query0(nameServerAddr, question, additionals, flush, writePromise, queryPromise);
    
    queriesInProgress.add(f);
    
    queryLifecycleObserver.queryWritten(nameServerAddr, writePromise);
    
    f.addListener(new FutureListener()
    {
      public void operationComplete(Future<AddressedEnvelope<DnsResponse, InetSocketAddress>> future) {
        queriesInProgress.remove(future);
        
        if ((promise.isDone()) || (future.isCancelled())) {
          queryLifecycleObserver.queryCancelled(allowedQueries);
          


          AddressedEnvelope<DnsResponse, InetSocketAddress> result = (AddressedEnvelope)future.getNow();
          if (result != null) {
            result.release();
          }
          return;
        }
        
        Throwable queryCause = future.cause();
        try {
          if (queryCause == null) {
            DnsResolveContext.this.onResponse(nameServerAddrStream, nameServerAddrStreamIndex, question, (AddressedEnvelope)future.getNow(), queryLifecycleObserver, promise);
          }
          else
          {
            queryLifecycleObserver.queryFailed(queryCause);
            DnsResolveContext.this.query(nameServerAddrStream, nameServerAddrStreamIndex + 1, question, 
              DnsResolveContext.access$500(DnsResolveContext.this, question), true, promise, queryCause);
          }
          
          DnsResolveContext.this.tryToFinishResolve(nameServerAddrStream, nameServerAddrStreamIndex, question, NoopDnsQueryLifecycleObserver.INSTANCE, promise, queryCause); } finally { DnsResolveContext.this.tryToFinishResolve(nameServerAddrStream, nameServerAddrStreamIndex, question, NoopDnsQueryLifecycleObserver.INSTANCE, promise, queryCause);
        }
      }
    });
  }
  










  private void queryUnresolvedNameServer(final InetSocketAddress nameServerAddr, final DnsServerAddressStream nameServerAddrStream, final int nameServerAddrStreamIndex, final DnsQuestion question, final DnsQueryLifecycleObserver queryLifecycleObserver, final Promise<List<T>> promise, final Throwable cause)
  {
    String nameServerName = PlatformDependent.javaVersion() >= 7 ? nameServerAddr.getHostString() : nameServerAddr.getHostName();
    assert (nameServerName != null);
    


    final Future<AddressedEnvelope<DnsResponse, InetSocketAddress>> resolveFuture = parent.executor().newSucceededFuture(null);
    queriesInProgress.add(resolveFuture);
    
    Promise<List<InetAddress>> resolverPromise = parent.executor().newPromise();
    resolverPromise.addListener(new FutureListener()
    {
      public void operationComplete(Future<List<InetAddress>> future)
      {
        queriesInProgress.remove(resolveFuture);
        
        if (future.isSuccess()) {
          List<InetAddress> resolvedAddresses = (List)future.getNow();
          DnsServerAddressStream addressStream = new DnsResolveContext.CombinedDnsServerAddressStream(DnsResolveContext.this, nameServerAddr, resolvedAddresses, nameServerAddrStream);
          
          DnsResolveContext.this.query(addressStream, nameServerAddrStreamIndex, question, queryLifecycleObserver, true, promise, cause);
        }
        else
        {
          DnsResolveContext.this.query(nameServerAddrStream, nameServerAddrStreamIndex + 1, question, queryLifecycleObserver, true, promise, cause);
        }
        
      }
    });
    DnsCache resolveCache = resolveCache();
    if (!DnsNameResolver.doResolveAllCached(nameServerName, additionals, resolverPromise, resolveCache, parent
      .resolvedInternetProtocolFamiliesUnsafe()))
    {







      new DnsAddressResolveContext(parent, originalPromise, nameServerName, additionals, parent.newNameServerAddressStream(nameServerName), allowedQueries, resolveCache, redirectAuthoritativeDnsServerCache(authoritativeDnsServerCache()), false).resolve(resolverPromise);
    }
  }
  


  private static AuthoritativeDnsServerCache redirectAuthoritativeDnsServerCache(AuthoritativeDnsServerCache authoritativeDnsServerCache)
  {
    if ((authoritativeDnsServerCache instanceof RedirectAuthoritativeDnsServerCache)) {
      return authoritativeDnsServerCache;
    }
    return new RedirectAuthoritativeDnsServerCache(authoritativeDnsServerCache);
  }
  
  private static final class RedirectAuthoritativeDnsServerCache implements AuthoritativeDnsServerCache {
    private final AuthoritativeDnsServerCache wrapped;
    
    RedirectAuthoritativeDnsServerCache(AuthoritativeDnsServerCache authoritativeDnsServerCache) {
      wrapped = authoritativeDnsServerCache;
    }
    


    public DnsServerAddressStream get(String hostname)
    {
      return null;
    }
    
    public void cache(String hostname, InetSocketAddress address, long originalTtl, EventLoop loop)
    {
      wrapped.cache(hostname, address, originalTtl, loop);
    }
    
    public void clear()
    {
      wrapped.clear();
    }
    
    public boolean clear(String hostname)
    {
      return wrapped.clear(hostname);
    }
  }
  

  private void onResponse(DnsServerAddressStream nameServerAddrStream, int nameServerAddrStreamIndex, DnsQuestion question, AddressedEnvelope<DnsResponse, InetSocketAddress> envelope, DnsQueryLifecycleObserver queryLifecycleObserver, Promise<List<T>> promise)
  {
    try
    {
      DnsResponse res = (DnsResponse)envelope.content();
      DnsResponseCode code = res.code();
      if (code == DnsResponseCode.NOERROR) {
        if (handleRedirect(question, envelope, queryLifecycleObserver, promise))
        {
          return;
        }
        DnsRecordType type = question.type();
        
        if (type == DnsRecordType.CNAME) {
          onResponseCNAME(question, buildAliasMap((DnsResponse)envelope.content(), cnameCache(), parent.executor()), queryLifecycleObserver, promise);
          
          return;
        }
        
        for (DnsRecordType expectedType : expectedTypes) {
          if (type == expectedType) {
            onExpectedResponse(question, envelope, queryLifecycleObserver, promise);
            return;
          }
        }
        
        queryLifecycleObserver.queryFailed(UNRECOGNIZED_TYPE_QUERY_FAILED_EXCEPTION);
        return;
      }
      

      if (code != DnsResponseCode.NXDOMAIN) {
        query(nameServerAddrStream, nameServerAddrStreamIndex + 1, question, queryLifecycleObserver
          .queryNoAnswer(code), true, promise, null);
      } else {
        queryLifecycleObserver.queryFailed(NXDOMAIN_QUERY_FAILED_EXCEPTION);
        


















        if (!res.isAuthoritativeAnswer()) {
          query(nameServerAddrStream, nameServerAddrStreamIndex + 1, question, 
            newDnsQueryLifecycleObserver(question), true, promise, null);
        }
      }
    } finally {
      ReferenceCountUtil.safeRelease(envelope);
    }
  }
  




  private boolean handleRedirect(DnsQuestion question, AddressedEnvelope<DnsResponse, InetSocketAddress> envelope, DnsQueryLifecycleObserver queryLifecycleObserver, Promise<List<T>> promise)
  {
    DnsResponse res = (DnsResponse)envelope.content();
    

    if (res.count(DnsSection.ANSWER) == 0) {
      AuthoritativeNameServerList serverNames = extractAuthoritativeNameServers(question.name(), res);
      if (serverNames != null) {
        int additionalCount = res.count(DnsSection.ADDITIONAL);
        
        AuthoritativeDnsServerCache authoritativeDnsServerCache = authoritativeDnsServerCache();
        for (int i = 0; i < additionalCount; i++) {
          DnsRecord r = res.recordAt(DnsSection.ADDITIONAL, i);
          
          if (((r.type() != DnsRecordType.A) || (parent.supportsARecords())) && (
            (r.type() != DnsRecordType.AAAA) || (parent.supportsAAAARecords())))
          {




            serverNames.handleWithAdditional(parent, r, authoritativeDnsServerCache);
          }
        }
        
        serverNames.handleWithoutAdditionals(parent, resolveCache(), authoritativeDnsServerCache);
        
        List<InetSocketAddress> addresses = serverNames.addressList();
        

        DnsServerAddressStream serverStream = parent.newRedirectDnsServerStream(question
          .name(), addresses);
        
        if (serverStream != null) {
          query(serverStream, 0, question, queryLifecycleObserver
            .queryRedirected(new DnsAddressStreamList(serverStream)), true, promise, null);
          
          return true;
        }
      }
    }
    return false;
  }
  
  private static final class DnsAddressStreamList extends AbstractList<InetSocketAddress>
  {
    private final DnsServerAddressStream duplicate;
    private List<InetSocketAddress> addresses;
    
    DnsAddressStreamList(DnsServerAddressStream stream) {
      duplicate = stream.duplicate();
    }
    
    public InetSocketAddress get(int index)
    {
      if (addresses == null) {
        DnsServerAddressStream stream = duplicate.duplicate();
        addresses = new ArrayList(size());
        for (int i = 0; i < stream.size(); i++) {
          addresses.add(stream.next());
        }
      }
      return (InetSocketAddress)addresses.get(index);
    }
    
    public int size()
    {
      return duplicate.size();
    }
    
    public Iterator<InetSocketAddress> iterator()
    {
      new Iterator() {
        private final DnsServerAddressStream stream = duplicate.duplicate();
        private int i;
        
        public boolean hasNext()
        {
          return i < stream.size();
        }
        
        public InetSocketAddress next()
        {
          if (!hasNext()) {
            throw new NoSuchElementException();
          }
          i += 1;
          return stream.next();
        }
        
        public void remove()
        {
          throw new UnsupportedOperationException();
        }
      };
    }
  }
  



  private static AuthoritativeNameServerList extractAuthoritativeNameServers(String questionName, DnsResponse res)
  {
    int authorityCount = res.count(DnsSection.AUTHORITY);
    if (authorityCount == 0) {
      return null;
    }
    
    AuthoritativeNameServerList serverNames = new AuthoritativeNameServerList(questionName);
    for (int i = 0; i < authorityCount; i++) {
      serverNames.add(res.recordAt(DnsSection.AUTHORITY, i));
    }
    return serverNames.isEmpty() ? null : serverNames;
  }
  



  private void onExpectedResponse(DnsQuestion question, AddressedEnvelope<DnsResponse, InetSocketAddress> envelope, DnsQueryLifecycleObserver queryLifecycleObserver, Promise<List<T>> promise)
  {
    DnsResponse response = (DnsResponse)envelope.content();
    Map<String, String> cnames = buildAliasMap(response, cnameCache(), parent.executor());
    int answerCount = response.count(DnsSection.ANSWER);
    
    boolean found = false;
    boolean completeEarly = this.completeEarly;
    for (int i = 0; i < answerCount; i++) {
      DnsRecord r = response.recordAt(DnsSection.ANSWER, i);
      DnsRecordType type = r.type();
      boolean matches = false;
      for (DnsRecordType expectedType : expectedTypes) {
        if (type == expectedType) {
          matches = true;
          break;
        }
      }
      
      if (matches)
      {


        String questionName = question.name().toLowerCase(Locale.US);
        String recordName = r.name().toLowerCase(Locale.US);
        

        if (!recordName.equals(questionName)) {
          Object cnamesCopy = new HashMap(cnames);
          
          String resolved = questionName;
          do {
            resolved = (String)((Map)cnamesCopy).remove(resolved);
          } while ((!recordName.equals(resolved)) && 
          

            (resolved != null));
          
          if (resolved == null) {
            assert ((questionName.isEmpty()) || (questionName.charAt(questionName.length() - 1) == '.'));
            
            for (String searchDomain : parent.searchDomains())
              if (!searchDomain.isEmpty())
              {
                String fqdn;
                
                String fqdn;
                if (searchDomain.charAt(searchDomain.length() - 1) == '.') {
                  fqdn = questionName + searchDomain;
                } else {
                  fqdn = questionName + searchDomain + '.';
                }
                if (recordName.equals(fqdn)) {
                  resolved = recordName;
                  break;
                }
              }
            if (resolved == null) {
              if (!logger.isDebugEnabled()) continue;
              logger.debug("Ignoring record {} as it contains a different name than the question name [{}]. Cnames: {}, Search domains: {}", new Object[] {r
              
                .toString(), questionName, cnames, parent.searchDomains() }); continue;
            }
          }
        }
        


        Object converted = convertRecord(r, hostname, additionals, parent.executor());
        if (converted == null) {
          if (logger.isDebugEnabled()) {
            logger.debug("Ignoring record {} as the converted record is null. hostname [{}], Additionals: {}", new Object[] {r
              .toString(), hostname, additionals });
          }
        }
        else
        {
          boolean shouldRelease = false;
          

          if (!completeEarly) {
            completeEarly = isCompleteEarly(converted);
          }
          





          if (finalResult == null) {
            finalResult = new ArrayList(8);
            finalResult.add(converted);
          } else if ((isDuplicateAllowed()) || (!finalResult.contains(converted))) {
            finalResult.add(converted);
          } else {
            shouldRelease = true;
          }
          
          cache(hostname, additionals, r, converted);
          found = true;
          
          if (shouldRelease) {
            ReferenceCountUtil.release(converted);
          }
        }
      }
    }
    if (cnames.isEmpty()) {
      if (found) {
        if (completeEarly) {
          this.completeEarly = true;
        }
        queryLifecycleObserver.querySucceed();
        return;
      }
      queryLifecycleObserver.queryFailed(NO_MATCHING_RECORD_QUERY_FAILED_EXCEPTION);
    } else {
      queryLifecycleObserver.querySucceed();
      
      onResponseCNAME(question, cnames, newDnsQueryLifecycleObserver(question), promise);
    }
  }
  




  private void onResponseCNAME(DnsQuestion question, Map<String, String> cnames, DnsQueryLifecycleObserver queryLifecycleObserver, Promise<List<T>> promise)
  {
    String resolved = question.name().toLowerCase(Locale.US);
    boolean found = false;
    while (!cnames.isEmpty())
    {

      String next = (String)cnames.remove(resolved);
      if (next == null) break;
      found = true;
      resolved = next;
    }
    



    if (found) {
      followCname(question, resolved, queryLifecycleObserver, promise);
    } else {
      queryLifecycleObserver.queryFailed(CNAME_NOT_FOUND_QUERY_FAILED_EXCEPTION);
    }
  }
  
  private static Map<String, String> buildAliasMap(DnsResponse response, DnsCnameCache cache, EventLoop loop) {
    int answerCount = response.count(DnsSection.ANSWER);
    Map<String, String> cnames = null;
    for (int i = 0; i < answerCount; i++) {
      DnsRecord r = response.recordAt(DnsSection.ANSWER, i);
      DnsRecordType type = r.type();
      if (type == DnsRecordType.CNAME)
      {


        if ((r instanceof DnsRawRecord))
        {


          ByteBuf recordContent = ((ByteBufHolder)r).content();
          String domainName = decodeDomainName(recordContent);
          if (domainName != null)
          {


            if (cnames == null) {
              cnames = new HashMap(Math.min(8, answerCount));
            }
            
            String name = r.name().toLowerCase(Locale.US);
            String mapping = domainName.toLowerCase(Locale.US);
            

            String nameWithDot = hostnameWithDot(name);
            String mappingWithDot = hostnameWithDot(mapping);
            if (!nameWithDot.equalsIgnoreCase(mappingWithDot)) {
              cache.cache(nameWithDot, mappingWithDot, r.timeToLive(), loop);
              cnames.put(name, mapping);
            }
          }
        } } }
    return cnames != null ? cnames : Collections.emptyMap();
  }
  






  private void tryToFinishResolve(DnsServerAddressStream nameServerAddrStream, int nameServerAddrStreamIndex, DnsQuestion question, DnsQueryLifecycleObserver queryLifecycleObserver, Promise<List<T>> promise, Throwable cause)
  {
    if ((!completeEarly) && (!queriesInProgress.isEmpty())) {
      queryLifecycleObserver.queryCancelled(allowedQueries);
      


      return;
    }
    

    if (finalResult == null) {
      if (nameServerAddrStreamIndex < nameServerAddrStream.size()) {
        if (queryLifecycleObserver == NoopDnsQueryLifecycleObserver.INSTANCE)
        {

          query(nameServerAddrStream, nameServerAddrStreamIndex + 1, question, 
            newDnsQueryLifecycleObserver(question), true, promise, cause);
        } else {
          query(nameServerAddrStream, nameServerAddrStreamIndex + 1, question, queryLifecycleObserver, true, promise, cause);
        }
        
        return;
      }
      
      queryLifecycleObserver.queryFailed(NAME_SERVERS_EXHAUSTED_EXCEPTION);
      





      if ((cause == null) && (!triedCNAME) && (
        (question.type() == DnsRecordType.A) || (question.type() == DnsRecordType.AAAA)))
      {
        triedCNAME = true;
        
        query(hostname, DnsRecordType.CNAME, getNameServers(hostname), true, promise);
      }
    }
    else {
      queryLifecycleObserver.queryCancelled(allowedQueries);
    }
    

    finishResolve(promise, cause);
  }
  
  private void finishResolve(Promise<List<T>> promise, Throwable cause)
  {
    Future<AddressedEnvelope<DnsResponse, InetSocketAddress>> f;
    if ((!completeEarly) && (!queriesInProgress.isEmpty()))
    {
      Iterator<Future<AddressedEnvelope<DnsResponse, InetSocketAddress>>> i = queriesInProgress.iterator();
      while (i.hasNext()) {
        f = (Future)i.next();
        i.remove();
        
        f.cancel(false);
      }
    }
    
    if (finalResult != null) {
      if (!promise.isDone())
      {
        List<T> result = filterResults(finalResult);
        if (!DnsNameResolver.trySuccess(promise, result)) {
          for (T item : result) {
            ReferenceCountUtil.safeRelease(item);
          }
        }
      }
      return;
    }
    

    int maxAllowedQueries = parent.maxQueriesPerResolve();
    int tries = maxAllowedQueries - allowedQueries;
    StringBuilder buf = new StringBuilder(64);
    
    buf.append("failed to resolve '").append(hostname).append('\'');
    if (tries > 1) {
      if (tries < maxAllowedQueries)
      {

        buf.append(" after ").append(tries).append(" queries ");
      }
      else
      {
        buf.append(". Exceeded max queries per resolve ").append(maxAllowedQueries).append(' ');
      }
    }
    UnknownHostException unknownHostException = new UnknownHostException(buf.toString());
    if (cause == null)
    {

      cache(hostname, additionals, unknownHostException);
    } else {
      unknownHostException.initCause(cause);
    }
    promise.tryFailure(unknownHostException);
  }
  
  static String decodeDomainName(ByteBuf in) {
    in.markReaderIndex();
    try {
      return DefaultDnsRecordDecoder.decodeName(in);
    }
    catch (CorruptedFrameException e) {
      return null;
    } finally {
      in.resetReaderIndex();
    }
  }
  
  private DnsServerAddressStream getNameServers(String name) {
    DnsServerAddressStream stream = getNameServersFromCache(name);
    if (stream == null)
    {


      if (name.equals(hostname)) {
        return nameServerAddrs.duplicate();
      }
      return parent.newNameServerAddressStream(name);
    }
    return stream;
  }
  

  private void followCname(DnsQuestion question, String cname, DnsQueryLifecycleObserver queryLifecycleObserver, Promise<List<T>> promise)
  {
    try
    {
      cname = cnameResolveFromCache(cnameCache(), cname);
      DnsServerAddressStream stream = getNameServers(cname);
      cnameQuestion = new DefaultDnsQuestion(cname, question.type(), dnsClass);
    } catch (Throwable cause) { DnsQuestion cnameQuestion;
      queryLifecycleObserver.queryFailed(cause);
      PlatformDependent.throwException(cause); return; }
    DnsServerAddressStream stream;
    DnsQuestion cnameQuestion;
    query(stream, 0, cnameQuestion, queryLifecycleObserver.queryCNAMEd(cnameQuestion), true, promise, null);
  }
  

  private boolean query(String hostname, DnsRecordType type, DnsServerAddressStream dnsServerAddressStream, boolean flush, Promise<List<T>> promise)
  {
    try
    {
      question = new DefaultDnsQuestion(hostname, type, dnsClass);
    }
    catch (Throwable cause) {
      DnsQuestion question;
      promise.tryFailure(new IllegalArgumentException("Unable to create DNS Question for: [" + hostname + ", " + type + ']', cause));
      
      return false; }
    DnsQuestion question;
    query(dnsServerAddressStream, 0, question, newDnsQueryLifecycleObserver(question), flush, promise, null);
    return true;
  }
  
  private DnsQueryLifecycleObserver newDnsQueryLifecycleObserver(DnsQuestion question) {
    return parent.dnsQueryLifecycleObserverFactory().newDnsQueryLifecycleObserver(question);
  }
  
  private final class CombinedDnsServerAddressStream implements DnsServerAddressStream
  {
    private final InetSocketAddress replaced;
    private final DnsServerAddressStream originalStream;
    private final List<InetAddress> resolvedAddresses;
    private Iterator<InetAddress> resolved;
    
    CombinedDnsServerAddressStream(List<InetAddress> replaced, DnsServerAddressStream resolvedAddresses) {
      this.replaced = replaced;
      this.resolvedAddresses = resolvedAddresses;
      this.originalStream = originalStream;
      resolved = resolvedAddresses.iterator();
    }
    
    public InetSocketAddress next()
    {
      if (resolved.hasNext()) {
        return nextResolved0();
      }
      InetSocketAddress address = originalStream.next();
      if (address.equals(replaced)) {
        resolved = resolvedAddresses.iterator();
        return nextResolved0();
      }
      return address;
    }
    
    private InetSocketAddress nextResolved0() {
      return parent.newRedirectServerAddress((InetAddress)resolved.next());
    }
    
    public int size()
    {
      return originalStream.size() + resolvedAddresses.size() - 1;
    }
    
    public DnsServerAddressStream duplicate()
    {
      return new CombinedDnsServerAddressStream(DnsResolveContext.this, replaced, resolvedAddresses, originalStream.duplicate());
    }
  }
  


  private static final class AuthoritativeNameServerList
  {
    private final String questionName;
    
    private DnsResolveContext.AuthoritativeNameServer head;
    
    private int nameServerCount;
    

    AuthoritativeNameServerList(String questionName)
    {
      this.questionName = questionName.toLowerCase(Locale.US);
    }
    
    void add(DnsRecord r) {
      if ((r.type() != DnsRecordType.NS) || (!(r instanceof DnsRawRecord))) {
        return;
      }
      

      if (questionName.length() < r.name().length()) {
        return;
      }
      
      String recordName = r.name().toLowerCase(Locale.US);
      
      int dots = 0;
      int a = recordName.length() - 1; for (int b = questionName.length() - 1; a >= 0; b--) {
        char c = recordName.charAt(a);
        if (questionName.charAt(b) != c) {
          return;
        }
        if (c == '.') {
          dots++;
        }
        a--;
      }
      







      if ((head != null) && (head.dots > dots))
      {
        return;
      }
      
      ByteBuf recordContent = ((ByteBufHolder)r).content();
      String domainName = DnsResolveContext.decodeDomainName(recordContent);
      if (domainName == null)
      {
        return;
      }
      


      if ((head == null) || (head.dots < dots)) {
        nameServerCount = 1;
        head = new DnsResolveContext.AuthoritativeNameServer(dots, r.timeToLive(), recordName, domainName);
      } else if (head.dots == dots) {
        DnsResolveContext.AuthoritativeNameServer serverName = head;
        while (next != null) {
          serverName = next;
        }
        next = new DnsResolveContext.AuthoritativeNameServer(dots, r.timeToLive(), recordName, domainName);
        nameServerCount += 1;
      }
    }
    

    void handleWithAdditional(DnsNameResolver parent, DnsRecord r, AuthoritativeDnsServerCache authoritativeCache)
    {
      DnsResolveContext.AuthoritativeNameServer serverName = head;
      
      String nsName = r.name();
      InetAddress resolved = DnsAddressDecoder.decodeAddress(r, nsName, parent.isDecodeIdn());
      if (resolved == null)
      {
        return;
      }
      
      while (serverName != null) {
        if (nsName.equalsIgnoreCase(nsName)) {
          if (address != null)
          {

            while ((next != null) && (next.isCopy)) {
              serverName = next;
            }
            DnsResolveContext.AuthoritativeNameServer server = new DnsResolveContext.AuthoritativeNameServer(serverName);
            next = next;
            next = server;
            serverName = server;
            
            nameServerCount += 1;
          }
          

          serverName.update(parent.newRedirectServerAddress(resolved), r.timeToLive());
          

          cache(serverName, authoritativeCache, parent.executor());
          return;
        }
        serverName = next;
      }
    }
    

    void handleWithoutAdditionals(DnsNameResolver parent, DnsCache cache, AuthoritativeDnsServerCache authoritativeCache)
    {
      DnsResolveContext.AuthoritativeNameServer serverName = head;
      
      while (serverName != null) {
        if (address == null)
        {
          cacheUnresolved(serverName, authoritativeCache, parent.executor());
          


          List<? extends DnsCacheEntry> entries = cache.get(nsName, null);
          if ((entries != null) && (!entries.isEmpty())) {
            InetAddress address = ((DnsCacheEntry)entries.get(0)).address();
            

            if (address != null) {
              serverName.update(parent.newRedirectServerAddress(address));
              
              for (int i = 1; i < entries.size(); i++) {
                address = ((DnsCacheEntry)entries.get(i)).address();
                
                assert (address != null) : "Cache returned a cached failure, should never return anything else";
                

                DnsResolveContext.AuthoritativeNameServer server = new DnsResolveContext.AuthoritativeNameServer(serverName);
                next = next;
                next = server;
                serverName = server;
                serverName.update(parent.newRedirectServerAddress(address));
                
                nameServerCount += 1;
              }
            }
          }
        }
        serverName = next;
      }
    }
    

    private static void cacheUnresolved(DnsResolveContext.AuthoritativeNameServer server, AuthoritativeDnsServerCache authoritativeCache, EventLoop loop)
    {
      address = InetSocketAddress.createUnresolved(nsName, 53);
      


      cache(server, authoritativeCache, loop);
    }
    
    private static void cache(DnsResolveContext.AuthoritativeNameServer server, AuthoritativeDnsServerCache cache, EventLoop loop)
    {
      if (!server.isRootServer()) {
        cache.cache(domainName, address, ttl, loop);
      }
    }
    


    boolean isEmpty()
    {
      return nameServerCount == 0;
    }
    


    List<InetSocketAddress> addressList()
    {
      List<InetSocketAddress> addressList = new ArrayList(nameServerCount);
      
      DnsResolveContext.AuthoritativeNameServer server = head;
      while (server != null) {
        if (address != null) {
          addressList.add(address);
        }
        server = next;
      }
      return addressList;
    }
  }
  
  private static final class AuthoritativeNameServer
  {
    private final int dots;
    private final String domainName;
    final boolean isCopy;
    final String nsName;
    private long ttl;
    private InetSocketAddress address;
    AuthoritativeNameServer next;
    
    AuthoritativeNameServer(int dots, long ttl, String domainName, String nsName)
    {
      this.dots = dots;
      this.ttl = ttl;
      this.nsName = nsName;
      this.domainName = domainName;
      isCopy = false;
    }
    
    AuthoritativeNameServer(AuthoritativeNameServer server) {
      dots = dots;
      ttl = ttl;
      nsName = nsName;
      domainName = domainName;
      isCopy = true;
    }
    


    boolean isRootServer()
    {
      return dots == 1;
    }
    


    void update(InetSocketAddress address, long ttl)
    {
      assert ((this.address == null) || (this.address.isUnresolved()));
      this.address = address;
      this.ttl = Math.min(this.ttl, ttl);
    }
    
    void update(InetSocketAddress address) {
      update(address, Long.MAX_VALUE);
    }
  }
}
