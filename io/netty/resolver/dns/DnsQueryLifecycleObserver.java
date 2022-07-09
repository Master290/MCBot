package io.netty.resolver.dns;

import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.dns.DnsQuestion;
import io.netty.handler.codec.dns.DnsResponseCode;
import java.net.InetSocketAddress;
import java.util.List;

public abstract interface DnsQueryLifecycleObserver
{
  public abstract void queryWritten(InetSocketAddress paramInetSocketAddress, ChannelFuture paramChannelFuture);
  
  public abstract void queryCancelled(int paramInt);
  
  public abstract DnsQueryLifecycleObserver queryRedirected(List<InetSocketAddress> paramList);
  
  public abstract DnsQueryLifecycleObserver queryCNAMEd(DnsQuestion paramDnsQuestion);
  
  public abstract DnsQueryLifecycleObserver queryNoAnswer(DnsResponseCode paramDnsResponseCode);
  
  public abstract void queryFailed(Throwable paramThrowable);
  
  public abstract void querySucceed();
}
