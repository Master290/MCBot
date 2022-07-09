package io.netty.handler.codec.dns;

public abstract interface DnsPtrRecord
  extends DnsRecord
{
  public abstract String hostname();
}
