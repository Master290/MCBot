package io.netty.handler.codec.dns;

public abstract interface DnsQuery
  extends DnsMessage
{
  public abstract DnsQuery setId(int paramInt);
  
  public abstract DnsQuery setOpCode(DnsOpCode paramDnsOpCode);
  
  public abstract DnsQuery setRecursionDesired(boolean paramBoolean);
  
  public abstract DnsQuery setZ(int paramInt);
  
  public abstract DnsQuery setRecord(DnsSection paramDnsSection, DnsRecord paramDnsRecord);
  
  public abstract DnsQuery addRecord(DnsSection paramDnsSection, DnsRecord paramDnsRecord);
  
  public abstract DnsQuery addRecord(DnsSection paramDnsSection, int paramInt, DnsRecord paramDnsRecord);
  
  public abstract DnsQuery clear(DnsSection paramDnsSection);
  
  public abstract DnsQuery clear();
  
  public abstract DnsQuery touch();
  
  public abstract DnsQuery touch(Object paramObject);
  
  public abstract DnsQuery retain();
  
  public abstract DnsQuery retain(int paramInt);
}
