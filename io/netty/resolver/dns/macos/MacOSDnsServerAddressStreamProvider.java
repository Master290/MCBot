package io.netty.resolver.dns.macos;

import io.netty.resolver.dns.DnsServerAddressStream;
import io.netty.resolver.dns.DnsServerAddressStreamProvider;
import io.netty.resolver.dns.DnsServerAddressStreamProviders;
import io.netty.resolver.dns.DnsServerAddresses;
import io.netty.util.internal.ClassInitializerUtil;
import io.netty.util.internal.NativeLibraryLoader;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.ThrowableUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;






















public final class MacOSDnsServerAddressStreamProvider
  implements DnsServerAddressStreamProvider
{
  private static final Comparator<DnsResolver> RESOLVER_COMPARATOR = new Comparator()
  {

    public int compare(DnsResolver r1, DnsResolver r2)
    {

      return r1.searchOrder() == r2.searchOrder() ? 0 : r1.searchOrder() < r2.searchOrder() ? 1 : -1;
    }
  };
  

  private static final Throwable UNAVAILABILITY_CAUSE;
  
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(MacOSDnsServerAddressStreamProvider.class);
  

  private static final long REFRESH_INTERVAL = TimeUnit.SECONDS.toNanos(10L);
  




  static
  {
    ClassInitializerUtil.tryLoadClasses(MacOSDnsServerAddressStreamProvider.class, new Class[] { [B.class, String.class });
    



    Throwable cause = null;
    try {
      loadNativeLibrary();
    } catch (Throwable error) {
      cause = error;
    }
    UNAVAILABILITY_CAUSE = cause;
  }
  
  private static void loadNativeLibrary() {
    if (!PlatformDependent.isOsx()) {
      throw new IllegalStateException("Only supported on MacOS/OSX");
    }
    String staticLibName = "netty_resolver_dns_native_macos";
    String sharedLibName = staticLibName + '_' + PlatformDependent.normalizedArch();
    ClassLoader cl = PlatformDependent.getClassLoader(MacOSDnsServerAddressStreamProvider.class);
    try {
      NativeLibraryLoader.load(sharedLibName, cl);
    } catch (UnsatisfiedLinkError e1) {
      try {
        NativeLibraryLoader.load(staticLibName, cl);
        logger.debug("Failed to load {}", sharedLibName, e1);
      } catch (UnsatisfiedLinkError e2) {
        ThrowableUtil.addSuppressed(e1, e2);
        throw e1;
      }
    }
  }
  
  public static boolean isAvailable() {
    return UNAVAILABILITY_CAUSE == null;
  }
  
  public static void ensureAvailability() {
    if (UNAVAILABILITY_CAUSE != null)
    {
      throw ((Error)new UnsatisfiedLinkError("failed to load the required native library").initCause(UNAVAILABILITY_CAUSE));
    }
  }
  
  public static Throwable unavailabilityCause() {
    return UNAVAILABILITY_CAUSE;
  }
  
  public MacOSDnsServerAddressStreamProvider() {
    ensureAvailability();
  }
  
  private volatile Map<String, DnsServerAddresses> currentMappings = retrieveCurrentMappings();
  private final AtomicLong lastRefresh = new AtomicLong(System.nanoTime());
  
  private static Map<String, DnsServerAddresses> retrieveCurrentMappings() {
    DnsResolver[] resolvers = resolvers();
    
    if ((resolvers == null) || (resolvers.length == 0)) {
      return Collections.emptyMap();
    }
    Arrays.sort(resolvers, RESOLVER_COMPARATOR);
    Map<String, DnsServerAddresses> resolverMap = new HashMap(resolvers.length);
    for (DnsResolver resolver : resolvers)
    {
      if (!"mdns".equalsIgnoreCase(resolver.options()))
      {

        InetSocketAddress[] nameservers = resolver.nameservers();
        if ((nameservers != null) && (nameservers.length != 0))
        {

          String domain = resolver.domain();
          if (domain == null)
          {
            domain = "";
          }
          InetSocketAddress[] servers = resolver.nameservers();
          for (int a = 0; a < servers.length; a++) {
            InetSocketAddress address = servers[a];
            
            if (address.getPort() == 0) {
              int port = resolver.port();
              if (port == 0) {
                port = 53;
              }
              servers[a] = new InetSocketAddress(address.getAddress(), port);
            }
          }
          
          resolverMap.put(domain, DnsServerAddresses.sequential(servers));
        } } }
    return resolverMap;
  }
  
  public DnsServerAddressStream nameServerAddressStream(String hostname)
  {
    long last = lastRefresh.get();
    Map<String, DnsServerAddresses> resolverMap = currentMappings;
    if (System.nanoTime() - last > REFRESH_INTERVAL)
    {

      if (lastRefresh.compareAndSet(last, System.nanoTime())) {
        resolverMap = this.currentMappings = retrieveCurrentMappings();
      }
    }
    
    String originalHostname = hostname;
    for (;;) {
      int i = hostname.indexOf('.', 1);
      if ((i < 0) || (i == hostname.length() - 1))
      {
        DnsServerAddresses addresses = (DnsServerAddresses)resolverMap.get("");
        if (addresses != null) {
          return addresses.stream();
        }
        return DnsServerAddressStreamProviders.unixDefault().nameServerAddressStream(originalHostname);
      }
      
      DnsServerAddresses addresses = (DnsServerAddresses)resolverMap.get(hostname);
      if (addresses != null) {
        return addresses.stream();
      }
      
      hostname = hostname.substring(i + 1);
    }
  }
  
  private static native DnsResolver[] resolvers();
}
