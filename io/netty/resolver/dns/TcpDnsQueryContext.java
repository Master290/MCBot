package io.netty.resolver.dns;

import io.netty.channel.AddressedEnvelope;
import io.netty.channel.Channel;
import io.netty.handler.codec.dns.DefaultDnsQuery;
import io.netty.handler.codec.dns.DnsQuery;
import io.netty.handler.codec.dns.DnsQuestion;
import io.netty.handler.codec.dns.DnsRecord;
import io.netty.handler.codec.dns.DnsResponse;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
















final class TcpDnsQueryContext
  extends DnsQueryContext
{
  private final Channel channel;
  
  TcpDnsQueryContext(DnsNameResolver parent, Channel channel, InetSocketAddress nameServerAddr, DnsQuestion question, DnsRecord[] additionals, Promise<AddressedEnvelope<DnsResponse, InetSocketAddress>> promise)
  {
    super(parent, nameServerAddr, question, additionals, promise);
    this.channel = channel;
  }
  
  protected DnsQuery newQuery(int id)
  {
    return new DefaultDnsQuery(id);
  }
  
  protected Channel channel()
  {
    return channel;
  }
  
  protected String protocol()
  {
    return "TCP";
  }
}
