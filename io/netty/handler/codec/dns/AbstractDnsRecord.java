package io.netty.handler.codec.dns;

import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.StringUtil;
import java.net.IDN;





























public abstract class AbstractDnsRecord
  implements DnsRecord
{
  private final String name;
  private final DnsRecordType type;
  private final short dnsClass;
  private final long timeToLive;
  private int hashCode;
  
  protected AbstractDnsRecord(String name, DnsRecordType type, long timeToLive)
  {
    this(name, type, 1, timeToLive);
  }
  















  protected AbstractDnsRecord(String name, DnsRecordType type, int dnsClass, long timeToLive)
  {
    ObjectUtil.checkPositiveOrZero(timeToLive, "timeToLive");
    



    this.name = appendTrailingDot(IDNtoASCII(name));
    this.type = ((DnsRecordType)ObjectUtil.checkNotNull(type, "type"));
    this.dnsClass = ((short)dnsClass);
    this.timeToLive = timeToLive;
  }
  
  private static String IDNtoASCII(String name) {
    ObjectUtil.checkNotNull(name, "name");
    if ((PlatformDependent.isAndroid()) && (".".equals(name)))
    {


      return name;
    }
    return IDN.toASCII(name);
  }
  
  private static String appendTrailingDot(String name) {
    if ((name.length() > 0) && (name.charAt(name.length() - 1) != '.')) {
      return name + '.';
    }
    return name;
  }
  
  public String name()
  {
    return name;
  }
  
  public DnsRecordType type()
  {
    return type;
  }
  
  public int dnsClass()
  {
    return dnsClass & 0xFFFF;
  }
  
  public long timeToLive()
  {
    return timeToLive;
  }
  
  public boolean equals(Object obj)
  {
    if (this == obj) {
      return true;
    }
    
    if (!(obj instanceof DnsRecord)) {
      return false;
    }
    
    DnsRecord that = (DnsRecord)obj;
    int hashCode = this.hashCode;
    if ((hashCode != 0) && (hashCode != that.hashCode())) {
      return false;
    }
    
    return (type().intValue() == that.type().intValue()) && 
      (dnsClass() == that.dnsClass()) && 
      (name().equals(that.name()));
  }
  
  public int hashCode()
  {
    int hashCode = this.hashCode;
    if (hashCode != 0) {
      return hashCode;
    }
    
    return this.hashCode = name.hashCode() * 31 + type().intValue() * 31 + dnsClass();
  }
  
  public String toString()
  {
    StringBuilder buf = new StringBuilder(64);
    
    buf.append(StringUtil.simpleClassName(this))
      .append('(')
      .append(name())
      .append(' ')
      .append(timeToLive())
      .append(' ');
    
    DnsMessageUtil.appendRecordClass(buf, dnsClass())
      .append(' ')
      .append(type().name())
      .append(')');
    
    return buf.toString();
  }
}
