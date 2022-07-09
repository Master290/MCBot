package io.netty.channel.socket;

import io.netty.util.NetUtil;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;



















public enum InternetProtocolFamily
{
  IPv4(Inet4Address.class, 1), 
  IPv6(Inet6Address.class, 2);
  
  private final Class<? extends InetAddress> addressType;
  private final int addressNumber;
  
  private InternetProtocolFamily(Class<? extends InetAddress> addressType, int addressNumber) {
    this.addressType = addressType;
    this.addressNumber = addressNumber;
  }
  


  public Class<? extends InetAddress> addressType()
  {
    return addressType;
  }
  




  public int addressNumber()
  {
    return addressNumber;
  }
  


  public InetAddress localhost()
  {
    switch (1.$SwitchMap$io$netty$channel$socket$InternetProtocolFamily[ordinal()]) {
    case 1: 
      return NetUtil.LOCALHOST4;
    case 2: 
      return NetUtil.LOCALHOST6;
    }
    throw new IllegalStateException("Unsupported family " + this);
  }
  



  public static InternetProtocolFamily of(InetAddress address)
  {
    if ((address instanceof Inet4Address)) {
      return IPv4;
    }
    if ((address instanceof Inet6Address)) {
      return IPv6;
    }
    throw new IllegalArgumentException("address " + address + " not supported");
  }
}
