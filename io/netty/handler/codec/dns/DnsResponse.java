package io.netty.handler.codec.dns;

public abstract interface DnsResponse
  extends DnsMessage
{
  public abstract boolean isAuthoritativeAnswer();
  
  public abstract DnsResponse setAuthoritativeAnswer(boolean paramBoolean);
  
  public abstract boolean isTruncated();
  
  public abstract DnsResponse setTruncated(boolean paramBoolean);
  
  public abstract boolean isRecursionAvailable();
  
  public abstract DnsResponse setRecursionAvailable(boolean paramBoolean);
  
  public abstract DnsResponseCode code();
  
  public abstract DnsResponse setCode(DnsResponseCode paramDnsResponseCode);
  
  public abstract DnsResponse setId(int paramInt);
  
  public abstract DnsResponse setOpCode(DnsOpCode paramDnsOpCode);
  
  public abstract DnsResponse setRecursionDesired(boolean paramBoolean);
  
  public abstract DnsResponse setZ(int paramInt);
  
  public abstract DnsResponse setRecord(DnsSection paramDnsSection, DnsRecord paramDnsRecord);
  
  public abstract DnsResponse addRecord(DnsSection paramDnsSection, DnsRecord paramDnsRecord);
  
  public abstract DnsResponse addRecord(DnsSection paramDnsSection, int paramInt, DnsRecord paramDnsRecord);
  
  public abstract DnsResponse clear(DnsSection paramDnsSection);
  
  public abstract DnsResponse clear();
  
  public abstract DnsResponse touch();
  
  public abstract DnsResponse touch(Object paramObject);
  
  public abstract DnsResponse retain();
  
  public abstract DnsResponse retain(int paramInt);
}
