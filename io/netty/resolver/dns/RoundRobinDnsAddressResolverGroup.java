package io.netty.resolver.dns;

import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoop;
import io.netty.channel.socket.DatagramChannel;
import io.netty.resolver.AddressResolver;
import io.netty.resolver.NameResolver;
import io.netty.resolver.RoundRobinInetAddressResolver;
import java.net.InetAddress;
import java.net.InetSocketAddress;






















public class RoundRobinDnsAddressResolverGroup
  extends DnsAddressResolverGroup
{
  public RoundRobinDnsAddressResolverGroup(DnsNameResolverBuilder dnsResolverBuilder)
  {
    super(dnsResolverBuilder);
  }
  

  public RoundRobinDnsAddressResolverGroup(Class<? extends DatagramChannel> channelType, DnsServerAddressStreamProvider nameServerProvider)
  {
    super(channelType, nameServerProvider);
  }
  

  public RoundRobinDnsAddressResolverGroup(ChannelFactory<? extends DatagramChannel> channelFactory, DnsServerAddressStreamProvider nameServerProvider)
  {
    super(channelFactory, nameServerProvider);
  }
  








  protected final AddressResolver<InetSocketAddress> newAddressResolver(EventLoop eventLoop, NameResolver<InetAddress> resolver)
    throws Exception
  {
    return new RoundRobinInetAddressResolver(eventLoop, resolver).asAddressResolver();
  }
}
