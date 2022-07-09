package io.netty.resolver.dns;

import io.netty.channel.AddressedEnvelope;
import io.netty.channel.Channel;
import io.netty.handler.codec.dns.DatagramDnsQuery;
import io.netty.handler.codec.dns.DnsQuery;
import io.netty.handler.codec.dns.DnsQuestion;
import io.netty.handler.codec.dns.DnsRecord;
import io.netty.handler.codec.dns.DnsResponse;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;

















final class DatagramDnsQueryContext
  extends DnsQueryContext
{
  DatagramDnsQueryContext(DnsNameResolver parent, InetSocketAddress nameServerAddr, DnsQuestion question, DnsRecord[] additionals, Promise<AddressedEnvelope<DnsResponse, InetSocketAddress>> promise)
  {
    super(parent, nameServerAddr, question, additionals, promise);
  }
  
  protected DnsQuery newQuery(int id)
  {
    return new DatagramDnsQuery(null, nameServerAddr(), id);
  }
  
  protected Channel channel()
  {
    return parentch;
  }
  
  protected String protocol()
  {
    return "UDP";
  }
}
