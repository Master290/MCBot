package io.netty.handler.codec.dns;

public abstract interface DnsQuestion
  extends DnsRecord
{
  public abstract long timeToLive();
}
