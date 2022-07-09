package io.netty.resolver.dns;

public abstract interface DnsServerAddressStreamProvider
{
  public abstract DnsServerAddressStream nameServerAddressStream(String paramString);
}
