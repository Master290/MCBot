package io.netty.resolver.dns;

import io.netty.handler.codec.dns.DnsQuestion;
import io.netty.util.internal.ObjectUtil;






















public final class BiDnsQueryLifecycleObserverFactory
  implements DnsQueryLifecycleObserverFactory
{
  private final DnsQueryLifecycleObserverFactory a;
  private final DnsQueryLifecycleObserverFactory b;
  
  public BiDnsQueryLifecycleObserverFactory(DnsQueryLifecycleObserverFactory a, DnsQueryLifecycleObserverFactory b)
  {
    this.a = ((DnsQueryLifecycleObserverFactory)ObjectUtil.checkNotNull(a, "a"));
    this.b = ((DnsQueryLifecycleObserverFactory)ObjectUtil.checkNotNull(b, "b"));
  }
  
  public DnsQueryLifecycleObserver newDnsQueryLifecycleObserver(DnsQuestion question)
  {
    return new BiDnsQueryLifecycleObserver(a.newDnsQueryLifecycleObserver(question), b
      .newDnsQueryLifecycleObserver(question));
  }
}
