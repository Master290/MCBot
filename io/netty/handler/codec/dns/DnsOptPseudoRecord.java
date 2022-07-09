package io.netty.handler.codec.dns;

public abstract interface DnsOptPseudoRecord
  extends DnsRecord
{
  public abstract int extendedRcode();
  
  public abstract int version();
  
  public abstract int flags();
}
