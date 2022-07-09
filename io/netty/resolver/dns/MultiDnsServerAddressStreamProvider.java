package io.netty.resolver.dns;

import java.util.List;





















public final class MultiDnsServerAddressStreamProvider
  implements DnsServerAddressStreamProvider
{
  private final DnsServerAddressStreamProvider[] providers;
  
  public MultiDnsServerAddressStreamProvider(List<DnsServerAddressStreamProvider> providers)
  {
    this.providers = ((DnsServerAddressStreamProvider[])providers.toArray(new DnsServerAddressStreamProvider[0]));
  }
  



  public MultiDnsServerAddressStreamProvider(DnsServerAddressStreamProvider... providers)
  {
    this.providers = ((DnsServerAddressStreamProvider[])providers.clone());
  }
  
  public DnsServerAddressStream nameServerAddressStream(String hostname)
  {
    for (DnsServerAddressStreamProvider provider : providers) {
      DnsServerAddressStream stream = provider.nameServerAddressStream(hostname);
      if (stream != null) {
        return stream;
      }
    }
    return null;
  }
}
