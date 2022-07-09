package io.netty.resolver.dns;

import io.netty.channel.EventLoop;
import io.netty.handler.codec.dns.DnsQuestion;
import io.netty.handler.codec.dns.DnsRecord;
import io.netty.handler.codec.dns.DnsRecordType;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Promise;
import java.net.UnknownHostException;
import java.util.List;
















final class DnsRecordResolveContext
  extends DnsResolveContext<DnsRecord>
{
  DnsRecordResolveContext(DnsNameResolver parent, Promise<?> originalPromise, DnsQuestion question, DnsRecord[] additionals, DnsServerAddressStream nameServerAddrs, int allowedQueries)
  {
    this(parent, originalPromise, question.name(), question.dnsClass(), new DnsRecordType[] {question
      .type() }, additionals, nameServerAddrs, allowedQueries);
  }
  




  private DnsRecordResolveContext(DnsNameResolver parent, Promise<?> originalPromise, String hostname, int dnsClass, DnsRecordType[] expectedTypes, DnsRecord[] additionals, DnsServerAddressStream nameServerAddrs, int allowedQueries)
  {
    super(parent, originalPromise, hostname, dnsClass, expectedTypes, additionals, nameServerAddrs, allowedQueries);
  }
  





  DnsResolveContext<DnsRecord> newResolverContext(DnsNameResolver parent, Promise<?> originalPromise, String hostname, int dnsClass, DnsRecordType[] expectedTypes, DnsRecord[] additionals, DnsServerAddressStream nameServerAddrs, int allowedQueries)
  {
    return new DnsRecordResolveContext(parent, originalPromise, hostname, dnsClass, expectedTypes, additionals, nameServerAddrs, allowedQueries);
  }
  

  DnsRecord convertRecord(DnsRecord record, String hostname, DnsRecord[] additionals, EventLoop eventLoop)
  {
    return (DnsRecord)ReferenceCountUtil.retain(record);
  }
  
  List<DnsRecord> filterResults(List<DnsRecord> unfiltered)
  {
    return unfiltered;
  }
  
  boolean isCompleteEarly(DnsRecord resolved)
  {
    return false;
  }
  
  boolean isDuplicateAllowed()
  {
    return true;
  }
  



  void cache(String hostname, DnsRecord[] additionals, DnsRecord result, DnsRecord convertedResult) {}
  



  void cache(String hostname, DnsRecord[] additionals, UnknownHostException cause) {}
  



  DnsCnameCache cnameCache()
  {
    return NoopDnsCnameCache.INSTANCE;
  }
}
