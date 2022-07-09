package io.netty.resolver.dns;

import io.netty.util.NetUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.SocketUtils;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.lang.reflect.Method;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
























public final class DefaultDnsServerAddressStreamProvider
  implements DnsServerAddressStreamProvider
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultDnsServerAddressStreamProvider.class);
  public static final DefaultDnsServerAddressStreamProvider INSTANCE = new DefaultDnsServerAddressStreamProvider();
  
  private static final List<InetSocketAddress> DEFAULT_NAME_SERVER_LIST;
  

  static
  {
    List<InetSocketAddress> defaultNameServers = new ArrayList(2);
    if (!PlatformDependent.isAndroid())
    {

      DirContextUtils.addNameServers(defaultNameServers, 53);
    }
    


    if ((PlatformDependent.javaVersion() < 9) && (defaultNameServers.isEmpty())) {
      try {
        Class<?> configClass = Class.forName("sun.net.dns.ResolverConfiguration");
        Method open = configClass.getMethod("open", new Class[0]);
        Method nameservers = configClass.getMethod("nameservers", new Class[0]);
        Object instance = open.invoke(null, new Object[0]);
        

        List<String> list = (List)nameservers.invoke(instance, new Object[0]);
        for (String a : list) {
          if (a != null) {
            defaultNameServers.add(new InetSocketAddress(SocketUtils.addressByName(a), 53));
          }
        }
      }
      catch (Exception localException) {}
    }
    


    if (!defaultNameServers.isEmpty()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Default DNS servers: {} (sun.net.dns.ResolverConfiguration)", defaultNameServers);
      }
      

    }
    else
    {
      if ((NetUtil.isIpV6AddressesPreferred()) || (((NetUtil.LOCALHOST instanceof Inet6Address)) && 
        (!NetUtil.isIpV4StackPreferred()))) {
        Collections.addAll(defaultNameServers, new InetSocketAddress[] {
        
          SocketUtils.socketAddress("2001:4860:4860::8888", 53), 
          SocketUtils.socketAddress("2001:4860:4860::8844", 53) });
      } else {
        Collections.addAll(defaultNameServers, new InetSocketAddress[] {
        
          SocketUtils.socketAddress("8.8.8.8", 53), 
          SocketUtils.socketAddress("8.8.4.4", 53) });
      }
      
      if (logger.isWarnEnabled()) {
        logger.warn("Default DNS servers: {} (Google Public DNS as a fallback)", defaultNameServers);
      }
    }
    

    DEFAULT_NAME_SERVER_LIST = Collections.unmodifiableList(defaultNameServers); }
  private static final DnsServerAddresses DEFAULT_NAME_SERVERS = DnsServerAddresses.sequential(DEFAULT_NAME_SERVER_LIST);
  

  static final int DNS_PORT = 53;
  

  public DnsServerAddressStream nameServerAddressStream(String hostname)
  {
    return DEFAULT_NAME_SERVERS.stream();
  }
  




  public static List<InetSocketAddress> defaultAddressList()
  {
    return DEFAULT_NAME_SERVER_LIST;
  }
  










  public static DnsServerAddresses defaultAddresses()
  {
    return DEFAULT_NAME_SERVERS;
  }
  
  private DefaultDnsServerAddressStreamProvider() {}
}
