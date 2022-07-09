package io.netty.handler.codec.dns;

public abstract interface DnsOptEcsRecord
  extends DnsOptPseudoRecord
{
  public abstract int sourcePrefixLength();
  
  public abstract int scopePrefixLength();
  
  public abstract byte[] address();
}
