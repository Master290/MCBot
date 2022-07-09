package io.netty.resolver.dns;

import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.dns.DnsQuestion;
import io.netty.handler.codec.dns.DnsResponseCode;
import java.net.InetSocketAddress;
import java.util.List;















final class NoopDnsQueryLifecycleObserver
  implements DnsQueryLifecycleObserver
{
  static final NoopDnsQueryLifecycleObserver INSTANCE = new NoopDnsQueryLifecycleObserver();
  


  private NoopDnsQueryLifecycleObserver() {}
  

  public void queryWritten(InetSocketAddress dnsServerAddress, ChannelFuture future) {}
  

  public void queryCancelled(int queriesRemaining) {}
  

  public DnsQueryLifecycleObserver queryRedirected(List<InetSocketAddress> nameServers)
  {
    return this;
  }
  
  public DnsQueryLifecycleObserver queryCNAMEd(DnsQuestion cnameQuestion)
  {
    return this;
  }
  
  public DnsQueryLifecycleObserver queryNoAnswer(DnsResponseCode code)
  {
    return this;
  }
  
  public void queryFailed(Throwable cause) {}
  
  public void querySucceed() {}
}
