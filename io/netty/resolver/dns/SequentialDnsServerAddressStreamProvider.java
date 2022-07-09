package io.netty.resolver.dns;

import java.net.InetSocketAddress;























public final class SequentialDnsServerAddressStreamProvider
  extends UniSequentialDnsServerAddressStreamProvider
{
  public SequentialDnsServerAddressStreamProvider(InetSocketAddress... addresses)
  {
    super(DnsServerAddresses.sequential(addresses));
  }
  




  public SequentialDnsServerAddressStreamProvider(Iterable<? extends InetSocketAddress> addresses)
  {
    super(DnsServerAddresses.sequential(addresses));
  }
}
