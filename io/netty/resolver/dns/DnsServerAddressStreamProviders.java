package io.netty.resolver.dns;

import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;





















public final class DnsServerAddressStreamProviders
{
  private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(DnsServerAddressStreamProviders.class);
  private static final Constructor<? extends DnsServerAddressStreamProvider> STREAM_PROVIDER_CONSTRUCTOR;
  private static final String MACOS_PROVIDER_CLASS_NAME = "io.netty.resolver.dns.macos.MacOSDnsServerAddressStreamProvider";
  
  static
  {
    Constructor<? extends DnsServerAddressStreamProvider> constructor = null;
    if (PlatformDependent.isOsx())
    {
      try
      {
        Object maybeProvider = AccessController.doPrivileged(new PrivilegedAction()
        {
          public Object run() {
            try {
              return Class.forName("io.netty.resolver.dns.macos.MacOSDnsServerAddressStreamProvider", true, DnsServerAddressStreamProviders.class
              

                .getClassLoader());
            } catch (Throwable cause) {
              return cause;
            }
          }
        });
        if ((maybeProvider instanceof Class))
        {
          Class<? extends DnsServerAddressStreamProvider> providerClass = (Class)maybeProvider;
          
          constructor = providerClass.getConstructor(new Class[0]);
          constructor.newInstance(new Object[0]);
          LOGGER.debug("{}: available", "io.netty.resolver.dns.macos.MacOSDnsServerAddressStreamProvider");
        } else {
          throw ((Throwable)maybeProvider);
        }
      } catch (ClassNotFoundException cause) {
        LOGGER.warn("Can not find {} in the classpath, fallback to system defaults. This may result in incorrect DNS resolutions on MacOS.", "io.netty.resolver.dns.macos.MacOSDnsServerAddressStreamProvider");
      }
      catch (Throwable cause) {
        LOGGER.error("Unable to load {}, fallback to system defaults. This may result in incorrect DNS resolutions on MacOS.", "io.netty.resolver.dns.macos.MacOSDnsServerAddressStreamProvider", cause);
        
        constructor = null;
      }
    }
    STREAM_PROVIDER_CONSTRUCTOR = constructor;
  }
  









  public static DnsServerAddressStreamProvider platformDefault()
  {
    if (STREAM_PROVIDER_CONSTRUCTOR != null) {
      try {
        return (DnsServerAddressStreamProvider)STREAM_PROVIDER_CONSTRUCTOR.newInstance(new Object[0]);
      }
      catch (IllegalAccessException localIllegalAccessException) {}catch (InstantiationException localInstantiationException) {}catch (InvocationTargetException localInvocationTargetException) {}
    }
    




    return unixDefault();
  }
  
  public static DnsServerAddressStreamProvider unixDefault() {
    return DefaultProviderHolder.DEFAULT_DNS_SERVER_ADDRESS_STREAM_PROVIDER;
  }
  
  private DnsServerAddressStreamProviders() {}
  
  private static final class DefaultProviderHolder
  {
    private static final long REFRESH_INTERVAL = TimeUnit.MINUTES.toNanos(5L);
    


    static final DnsServerAddressStreamProvider DEFAULT_DNS_SERVER_ADDRESS_STREAM_PROVIDER = new DnsServerAddressStreamProvider()
    {
      private volatile DnsServerAddressStreamProvider currentProvider = provider();
      private final AtomicLong lastRefresh = new AtomicLong(System.nanoTime());
      
      public DnsServerAddressStream nameServerAddressStream(String hostname)
      {
        long last = lastRefresh.get();
        DnsServerAddressStreamProvider current = currentProvider;
        if (System.nanoTime() - last > DnsServerAddressStreamProviders.DefaultProviderHolder.REFRESH_INTERVAL)
        {

          if (lastRefresh.compareAndSet(last, System.nanoTime())) {
            current = this.currentProvider = provider();
          }
        }
        return current.nameServerAddressStream(hostname);
      }
      

      private DnsServerAddressStreamProvider provider()
      {
        return PlatformDependent.isWindows() ? DefaultDnsServerAddressStreamProvider.INSTANCE : 
          UnixResolverDnsServerAddressStreamProvider.parseSilently();
      }
    };
    
    private DefaultProviderHolder() {}
  }
}
