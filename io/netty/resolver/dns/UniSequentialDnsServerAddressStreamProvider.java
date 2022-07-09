package io.netty.resolver.dns;

import io.netty.util.internal.ObjectUtil;
















abstract class UniSequentialDnsServerAddressStreamProvider
  implements DnsServerAddressStreamProvider
{
  private final DnsServerAddresses addresses;
  
  UniSequentialDnsServerAddressStreamProvider(DnsServerAddresses addresses)
  {
    this.addresses = ((DnsServerAddresses)ObjectUtil.checkNotNull(addresses, "addresses"));
  }
  
  public final DnsServerAddressStream nameServerAddressStream(String hostname)
  {
    return addresses.stream();
  }
}
