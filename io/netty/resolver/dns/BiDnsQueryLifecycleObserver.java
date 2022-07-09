package io.netty.resolver.dns;

import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.dns.DnsQuestion;
import io.netty.handler.codec.dns.DnsResponseCode;
import io.netty.util.internal.ObjectUtil;
import java.net.InetSocketAddress;
import java.util.List;























public final class BiDnsQueryLifecycleObserver
  implements DnsQueryLifecycleObserver
{
  private final DnsQueryLifecycleObserver a;
  private final DnsQueryLifecycleObserver b;
  
  public BiDnsQueryLifecycleObserver(DnsQueryLifecycleObserver a, DnsQueryLifecycleObserver b)
  {
    this.a = ((DnsQueryLifecycleObserver)ObjectUtil.checkNotNull(a, "a"));
    this.b = ((DnsQueryLifecycleObserver)ObjectUtil.checkNotNull(b, "b"));
  }
  
  public void queryWritten(InetSocketAddress dnsServerAddress, ChannelFuture future)
  {
    try {
      a.queryWritten(dnsServerAddress, future);
      
      b.queryWritten(dnsServerAddress, future); } finally { b.queryWritten(dnsServerAddress, future);
    }
  }
  
  public void queryCancelled(int queriesRemaining)
  {
    try {
      a.queryCancelled(queriesRemaining);
      
      b.queryCancelled(queriesRemaining); } finally { b.queryCancelled(queriesRemaining);
    }
  }
  
  public DnsQueryLifecycleObserver queryRedirected(List<InetSocketAddress> nameServers)
  {
    try {
      a.queryRedirected(nameServers);
      
      b.queryRedirected(nameServers); } finally { b.queryRedirected(nameServers);
    }
    return this;
  }
  
  public DnsQueryLifecycleObserver queryCNAMEd(DnsQuestion cnameQuestion)
  {
    try {
      a.queryCNAMEd(cnameQuestion);
      
      b.queryCNAMEd(cnameQuestion); } finally { b.queryCNAMEd(cnameQuestion);
    }
    return this;
  }
  
  public DnsQueryLifecycleObserver queryNoAnswer(DnsResponseCode code)
  {
    try {
      a.queryNoAnswer(code);
      
      b.queryNoAnswer(code); } finally { b.queryNoAnswer(code);
    }
    return this;
  }
  
  public void queryFailed(Throwable cause)
  {
    try {
      a.queryFailed(cause);
      
      b.queryFailed(cause); } finally { b.queryFailed(cause);
    }
  }
  
  public void querySucceed()
  {
    try {
      a.querySucceed();
      
      b.querySucceed(); } finally { b.querySucceed();
    }
  }
}
