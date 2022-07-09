package io.netty.handler.codec.dns;

import io.netty.buffer.ByteBuf;
























public class DefaultDnsRecordDecoder
  implements DnsRecordDecoder
{
  static final String ROOT = ".";
  
  protected DefaultDnsRecordDecoder() {}
  
  public final DnsQuestion decodeQuestion(ByteBuf in)
    throws Exception
  {
    String name = decodeName(in);
    DnsRecordType type = DnsRecordType.valueOf(in.readUnsignedShort());
    int qClass = in.readUnsignedShort();
    return new DefaultDnsQuestion(name, type, qClass);
  }
  
  public final <T extends DnsRecord> T decodeRecord(ByteBuf in) throws Exception
  {
    int startOffset = in.readerIndex();
    String name = decodeName(in);
    
    int endOffset = in.writerIndex();
    if (endOffset - in.readerIndex() < 10)
    {
      in.readerIndex(startOffset);
      return null;
    }
    
    DnsRecordType type = DnsRecordType.valueOf(in.readUnsignedShort());
    int aClass = in.readUnsignedShort();
    long ttl = in.readUnsignedInt();
    int length = in.readUnsignedShort();
    int offset = in.readerIndex();
    
    if (endOffset - offset < length)
    {
      in.readerIndex(startOffset);
      return null;
    }
    

    T record = decodeRecord(name, type, aClass, ttl, in, offset, length);
    in.readerIndex(offset + length);
    return record;
  }
  


















  protected DnsRecord decodeRecord(String name, DnsRecordType type, int dnsClass, long timeToLive, ByteBuf in, int offset, int length)
    throws Exception
  {
    if (type == DnsRecordType.PTR) {
      return new DefaultDnsPtrRecord(name, dnsClass, timeToLive, 
        decodeName0(in.duplicate().setIndex(offset, offset + length)));
    }
    if ((type == DnsRecordType.CNAME) || (type == DnsRecordType.NS)) {
      return new DefaultDnsRawRecord(name, type, dnsClass, timeToLive, 
        DnsCodecUtil.decompressDomainName(in
        .duplicate().setIndex(offset, offset + length)));
    }
    return new DefaultDnsRawRecord(name, type, dnsClass, timeToLive, in
      .retainedDuplicate().setIndex(offset, offset + length));
  }
  







  protected String decodeName0(ByteBuf in)
  {
    return decodeName(in);
  }
  







  public static String decodeName(ByteBuf in)
  {
    return DnsCodecUtil.decodeDomainName(in);
  }
}
