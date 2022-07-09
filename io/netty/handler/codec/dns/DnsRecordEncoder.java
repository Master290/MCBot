package io.netty.handler.codec.dns;

import io.netty.buffer.ByteBuf;























public abstract interface DnsRecordEncoder
{
  public static final DnsRecordEncoder DEFAULT = new DefaultDnsRecordEncoder();
  
  public abstract void encodeQuestion(DnsQuestion paramDnsQuestion, ByteBuf paramByteBuf)
    throws Exception;
  
  public abstract void encodeRecord(DnsRecord paramDnsRecord, ByteBuf paramByteBuf)
    throws Exception;
}
