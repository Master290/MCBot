package io.netty.handler.codec.dns;

import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;

































public class DefaultDnsPtrRecord
  extends AbstractDnsRecord
  implements DnsPtrRecord
{
  private final String hostname;
  
  public DefaultDnsPtrRecord(String name, int dnsClass, long timeToLive, String hostname)
  {
    super(name, DnsRecordType.PTR, dnsClass, timeToLive);
    this.hostname = ((String)ObjectUtil.checkNotNull(hostname, "hostname"));
  }
  
  public String hostname()
  {
    return hostname;
  }
  
  public String toString()
  {
    StringBuilder buf = new StringBuilder(64).append(StringUtil.simpleClassName(this)).append('(');
    DnsRecordType type = type();
    buf.append(name().isEmpty() ? "<root>" : name())
      .append(' ')
      .append(timeToLive())
      .append(' ');
    
    DnsMessageUtil.appendRecordClass(buf, dnsClass())
      .append(' ')
      .append(type.name());
    
    buf.append(' ')
      .append(hostname);
    
    return buf.toString();
  }
}
