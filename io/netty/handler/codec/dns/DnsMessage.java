package io.netty.handler.codec.dns;

import io.netty.util.ReferenceCounted;

public abstract interface DnsMessage
  extends ReferenceCounted
{
  public abstract int id();
  
  public abstract DnsMessage setId(int paramInt);
  
  public abstract DnsOpCode opCode();
  
  public abstract DnsMessage setOpCode(DnsOpCode paramDnsOpCode);
  
  public abstract boolean isRecursionDesired();
  
  public abstract DnsMessage setRecursionDesired(boolean paramBoolean);
  
  public abstract int z();
  
  public abstract DnsMessage setZ(int paramInt);
  
  public abstract int count(DnsSection paramDnsSection);
  
  public abstract int count();
  
  public abstract <T extends DnsRecord> T recordAt(DnsSection paramDnsSection);
  
  public abstract <T extends DnsRecord> T recordAt(DnsSection paramDnsSection, int paramInt);
  
  public abstract DnsMessage setRecord(DnsSection paramDnsSection, DnsRecord paramDnsRecord);
  
  public abstract <T extends DnsRecord> T setRecord(DnsSection paramDnsSection, int paramInt, DnsRecord paramDnsRecord);
  
  public abstract DnsMessage addRecord(DnsSection paramDnsSection, DnsRecord paramDnsRecord);
  
  public abstract DnsMessage addRecord(DnsSection paramDnsSection, int paramInt, DnsRecord paramDnsRecord);
  
  public abstract <T extends DnsRecord> T removeRecord(DnsSection paramDnsSection, int paramInt);
  
  public abstract DnsMessage clear(DnsSection paramDnsSection);
  
  public abstract DnsMessage clear();
  
  public abstract DnsMessage touch();
  
  public abstract DnsMessage touch(Object paramObject);
  
  public abstract DnsMessage retain();
  
  public abstract DnsMessage retain(int paramInt);
}
