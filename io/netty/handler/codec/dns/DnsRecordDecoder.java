package io.netty.handler.codec.dns;

import io.netty.buffer.ByteBuf;























public abstract interface DnsRecordDecoder
{
  public static final DnsRecordDecoder DEFAULT = new DefaultDnsRecordDecoder();
  
  public abstract DnsQuestion decodeQuestion(ByteBuf paramByteBuf)
    throws Exception;
  
  public abstract <T extends DnsRecord> T decodeRecord(ByteBuf paramByteBuf)
    throws Exception;
}
