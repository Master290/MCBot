package io.netty.resolver.dns;

import io.netty.util.internal.ObjectUtil;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


























public abstract class DnsServerAddresses
{
  public DnsServerAddresses() {}
  
  @Deprecated
  public static List<InetSocketAddress> defaultAddressList()
  {
    return DefaultDnsServerAddressStreamProvider.defaultAddressList();
  }
  












  @Deprecated
  public static DnsServerAddresses defaultAddresses()
  {
    return DefaultDnsServerAddressStreamProvider.defaultAddresses();
  }
  



  public static DnsServerAddresses sequential(Iterable<? extends InetSocketAddress> addresses)
  {
    return sequential0(sanitize(addresses));
  }
  



  public static DnsServerAddresses sequential(InetSocketAddress... addresses)
  {
    return sequential0(sanitize(addresses));
  }
  
  private static DnsServerAddresses sequential0(List<InetSocketAddress> addresses) {
    if (addresses.size() == 1) {
      return singleton((InetSocketAddress)addresses.get(0));
    }
    
    new DefaultDnsServerAddresses("sequential", addresses)
    {
      public DnsServerAddressStream stream() {
        return new SequentialDnsServerAddressStream(addresses, 0);
      }
    };
  }
  



  public static DnsServerAddresses shuffled(Iterable<? extends InetSocketAddress> addresses)
  {
    return shuffled0(sanitize(addresses));
  }
  



  public static DnsServerAddresses shuffled(InetSocketAddress... addresses)
  {
    return shuffled0(sanitize(addresses));
  }
  
  private static DnsServerAddresses shuffled0(List<InetSocketAddress> addresses) {
    if (addresses.size() == 1) {
      return singleton((InetSocketAddress)addresses.get(0));
    }
    
    new DefaultDnsServerAddresses("shuffled", addresses)
    {
      public DnsServerAddressStream stream() {
        return new ShuffledDnsServerAddressStream(addresses);
      }
    };
  }
  





  public static DnsServerAddresses rotational(Iterable<? extends InetSocketAddress> addresses)
  {
    return rotational0(sanitize(addresses));
  }
  





  public static DnsServerAddresses rotational(InetSocketAddress... addresses)
  {
    return rotational0(sanitize(addresses));
  }
  
  private static DnsServerAddresses rotational0(List<InetSocketAddress> addresses) {
    if (addresses.size() == 1) {
      return singleton((InetSocketAddress)addresses.get(0));
    }
    
    return new RotationalDnsServerAddresses(addresses);
  }
  


  public static DnsServerAddresses singleton(InetSocketAddress address)
  {
    ObjectUtil.checkNotNull(address, "address");
    if (address.isUnresolved()) {
      throw new IllegalArgumentException("cannot use an unresolved DNS server address: " + address);
    }
    
    return new SingletonDnsServerAddresses(address);
  }
  
  private static List<InetSocketAddress> sanitize(Iterable<? extends InetSocketAddress> addresses) {
    ObjectUtil.checkNotNull(addresses, "addresses");
    List<InetSocketAddress> list;
    List<InetSocketAddress> list;
    if ((addresses instanceof Collection)) {
      list = new ArrayList(((Collection)addresses).size());
    } else {
      list = new ArrayList(4);
    }
    
    for (InetSocketAddress a : addresses) {
      if (a == null) {
        break;
      }
      if (a.isUnresolved()) {
        throw new IllegalArgumentException("cannot use an unresolved DNS server address: " + a);
      }
      list.add(a);
    }
    
    return (List)ObjectUtil.checkNonEmpty(list, "list");
  }
  
  private static List<InetSocketAddress> sanitize(InetSocketAddress[] addresses) {
    ObjectUtil.checkNotNull(addresses, "addresses");
    
    List<InetSocketAddress> list = new ArrayList(addresses.length);
    for (InetSocketAddress a : addresses) {
      if (a == null) {
        break;
      }
      if (a.isUnresolved()) {
        throw new IllegalArgumentException("cannot use an unresolved DNS server address: " + a);
      }
      list.add(a);
    }
    
    if (list.isEmpty()) {
      return DefaultDnsServerAddressStreamProvider.defaultAddressList();
    }
    
    return list;
  }
  
  public abstract DnsServerAddressStream stream();
}
