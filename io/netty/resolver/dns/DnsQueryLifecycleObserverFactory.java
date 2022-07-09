package io.netty.resolver.dns;

import io.netty.handler.codec.dns.DnsQuestion;

public abstract interface DnsQueryLifecycleObserverFactory
{
  public abstract DnsQueryLifecycleObserver newDnsQueryLifecycleObserver(DnsQuestion paramDnsQuestion);
}
