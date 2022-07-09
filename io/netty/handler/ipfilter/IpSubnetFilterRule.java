package io.netty.handler.ipfilter;

import io.netty.util.NetUtil;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.SocketUtils;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;


















public final class IpSubnetFilterRule
  implements IpFilterRule, Comparable<IpSubnetFilterRule>
{
  private final IpFilterRule filterRule;
  private final String ipAddress;
  
  public IpSubnetFilterRule(String ipAddress, int cidrPrefix, IpFilterRuleType ruleType)
  {
    try
    {
      this.ipAddress = ipAddress;
      filterRule = selectFilterRule(SocketUtils.addressByName(ipAddress), cidrPrefix, ruleType);
    } catch (UnknownHostException e) {
      throw new IllegalArgumentException("ipAddress", e);
    }
  }
  
  public IpSubnetFilterRule(InetAddress ipAddress, int cidrPrefix, IpFilterRuleType ruleType) {
    this.ipAddress = ipAddress.getHostAddress();
    filterRule = selectFilterRule(ipAddress, cidrPrefix, ruleType);
  }
  
  private static IpFilterRule selectFilterRule(InetAddress ipAddress, int cidrPrefix, IpFilterRuleType ruleType) {
    ObjectUtil.checkNotNull(ipAddress, "ipAddress");
    ObjectUtil.checkNotNull(ruleType, "ruleType");
    
    if ((ipAddress instanceof Inet4Address))
      return new Ip4SubnetFilterRule((Inet4Address)ipAddress, cidrPrefix, ruleType, null);
    if ((ipAddress instanceof Inet6Address)) {
      return new Ip6SubnetFilterRule((Inet6Address)ipAddress, cidrPrefix, ruleType, null);
    }
    throw new IllegalArgumentException("Only IPv4 and IPv6 addresses are supported");
  }
  

  public boolean matches(InetSocketAddress remoteAddress)
  {
    return filterRule.matches(remoteAddress);
  }
  
  public IpFilterRuleType ruleType()
  {
    return filterRule.ruleType();
  }
  


  String getIpAddress()
  {
    return ipAddress;
  }
  


  IpFilterRule getFilterRule()
  {
    return filterRule;
  }
  
  public int compareTo(IpSubnetFilterRule ipSubnetFilterRule)
  {
    if ((filterRule instanceof Ip4SubnetFilterRule)) {
      return compareInt(filterRule).networkAddress, 
        filterRule).networkAddress);
    }
    return 
      filterRule).networkAddress.compareTo(filterRule).networkAddress);
  }
  







  int compareTo(InetSocketAddress inetSocketAddress)
  {
    if ((filterRule instanceof Ip4SubnetFilterRule)) {
      Ip4SubnetFilterRule ip4SubnetFilterRule = (Ip4SubnetFilterRule)filterRule;
      return compareInt(networkAddress, NetUtil.ipv4AddressToInt(
        (Inet4Address)inetSocketAddress.getAddress()) & subnetMask);
    }
    Ip6SubnetFilterRule ip6SubnetFilterRule = (Ip6SubnetFilterRule)filterRule;
    return networkAddress
      .compareTo(Ip6SubnetFilterRule.ipToInt((Inet6Address)inetSocketAddress.getAddress())
      .and(networkAddress));
  }
  



  private static int compareInt(int x, int y)
  {
    return x == y ? 0 : x < y ? -1 : 1;
  }
  
  static final class Ip4SubnetFilterRule implements IpFilterRule
  {
    private final int networkAddress;
    private final int subnetMask;
    private final IpFilterRuleType ruleType;
    
    private Ip4SubnetFilterRule(Inet4Address ipAddress, int cidrPrefix, IpFilterRuleType ruleType) {
      if ((cidrPrefix < 0) || (cidrPrefix > 32)) {
        throw new IllegalArgumentException(String.format("IPv4 requires the subnet prefix to be in range of [0,32]. The prefix was: %d", new Object[] {
          Integer.valueOf(cidrPrefix) }));
      }
      
      subnetMask = prefixToSubnetMask(cidrPrefix);
      networkAddress = (NetUtil.ipv4AddressToInt(ipAddress) & subnetMask);
      this.ruleType = ruleType;
    }
    
    public boolean matches(InetSocketAddress remoteAddress)
    {
      InetAddress inetAddress = remoteAddress.getAddress();
      if ((inetAddress instanceof Inet4Address)) {
        int ipAddress = NetUtil.ipv4AddressToInt((Inet4Address)inetAddress);
        return (ipAddress & subnetMask) == networkAddress;
      }
      return false;
    }
    
    public IpFilterRuleType ruleType()
    {
      return ruleType;
    }
    









    private static int prefixToSubnetMask(int cidrPrefix)
    {
      return (int)(-1L << 32 - cidrPrefix & 0xFFFFFFFFFFFFFFFF);
    }
  }
  
  static final class Ip6SubnetFilterRule implements IpFilterRule
  {
    private static final BigInteger MINUS_ONE = BigInteger.valueOf(-1L);
    private final BigInteger networkAddress;
    private final BigInteger subnetMask;
    private final IpFilterRuleType ruleType;
    
    private Ip6SubnetFilterRule(Inet6Address ipAddress, int cidrPrefix, IpFilterRuleType ruleType)
    {
      if ((cidrPrefix < 0) || (cidrPrefix > 128)) {
        throw new IllegalArgumentException(String.format("IPv6 requires the subnet prefix to be in range of [0,128]. The prefix was: %d", new Object[] {
          Integer.valueOf(cidrPrefix) }));
      }
      
      subnetMask = prefixToSubnetMask(cidrPrefix);
      networkAddress = ipToInt(ipAddress).and(subnetMask);
      this.ruleType = ruleType;
    }
    
    public boolean matches(InetSocketAddress remoteAddress)
    {
      InetAddress inetAddress = remoteAddress.getAddress();
      if ((inetAddress instanceof Inet6Address)) {
        BigInteger ipAddress = ipToInt((Inet6Address)inetAddress);
        return (ipAddress.and(subnetMask).equals(subnetMask)) || (ipAddress.and(subnetMask).equals(networkAddress));
      }
      return false;
    }
    
    public IpFilterRuleType ruleType()
    {
      return ruleType;
    }
    
    private static BigInteger ipToInt(Inet6Address ipAddress) {
      byte[] octets = ipAddress.getAddress();
      assert (octets.length == 16);
      
      return new BigInteger(octets);
    }
    
    private static BigInteger prefixToSubnetMask(int cidrPrefix) {
      return MINUS_ONE.shiftLeft(128 - cidrPrefix);
    }
  }
}
