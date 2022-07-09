package io.netty.handler.codec.dns;

import io.netty.buffer.ByteBuf;
import io.netty.channel.AddressedEnvelope;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.util.internal.StringUtil;
import java.net.SocketAddress;



















final class DnsMessageUtil
{
  static StringBuilder appendQuery(StringBuilder buf, DnsQuery query)
  {
    appendQueryHeader(buf, query);
    appendAllRecords(buf, query);
    return buf;
  }
  
  static StringBuilder appendResponse(StringBuilder buf, DnsResponse response) {
    appendResponseHeader(buf, response);
    appendAllRecords(buf, response);
    return buf; }
  
  static StringBuilder appendRecordClass(StringBuilder buf, int dnsClass) { String name;
    String name;
    String name;
    String name; String name; String name; String name; switch (dnsClass &= 0xFFFF) {
    case 1: 
      name = "IN";
      break;
    case 2: 
      name = "CSNET";
      break;
    case 3: 
      name = "CHAOS";
      break;
    case 4: 
      name = "HESIOD";
      break;
    case 254: 
      name = "NONE";
      break;
    case 255: 
      name = "ANY";
      break;
    default: 
      name = null;
    }
    
    
    if (name != null) {
      buf.append(name);
    } else {
      buf.append("UNKNOWN(").append(dnsClass).append(')');
    }
    
    return buf;
  }
  
  private static void appendQueryHeader(StringBuilder buf, DnsQuery msg)
  {
    buf.append(StringUtil.simpleClassName(msg)).append('(');
    
    appendAddresses(buf, msg)
      .append(msg.id())
      .append(", ")
      .append(msg.opCode());
    
    if (msg.isRecursionDesired()) {
      buf.append(", RD");
    }
    if (msg.z() != 0)
    {
      buf.append(", Z: ").append(msg.z());
    }
    buf.append(')');
  }
  
  private static void appendResponseHeader(StringBuilder buf, DnsResponse msg)
  {
    buf.append(StringUtil.simpleClassName(msg)).append('(');
    
    appendAddresses(buf, msg)
      .append(msg.id())
      .append(", ")
      .append(msg.opCode())
      .append(", ")
      .append(msg.code())
      .append(',');
    
    boolean hasComma = true;
    if (msg.isRecursionDesired()) {
      hasComma = false;
      buf.append(" RD");
    }
    if (msg.isAuthoritativeAnswer()) {
      hasComma = false;
      buf.append(" AA");
    }
    if (msg.isTruncated()) {
      hasComma = false;
      buf.append(" TC");
    }
    if (msg.isRecursionAvailable()) {
      hasComma = false;
      buf.append(" RA");
    }
    if (msg.z() != 0) {
      if (!hasComma) {
        buf.append(',');
      }
      
      buf.append(" Z: ").append(msg.z());
    }
    
    if (hasComma) {
      buf.setCharAt(buf.length() - 1, ')');
    } else {
      buf.append(')');
    }
  }
  
  private static StringBuilder appendAddresses(StringBuilder buf, DnsMessage msg)
  {
    if (!(msg instanceof AddressedEnvelope)) {
      return buf;
    }
    

    AddressedEnvelope<?, SocketAddress> envelope = (AddressedEnvelope)msg;
    
    SocketAddress addr = envelope.sender();
    if (addr != null)
    {

      buf.append("from: ").append(addr).append(", ");
    }
    
    addr = envelope.recipient();
    if (addr != null)
    {

      buf.append("to: ").append(addr).append(", ");
    }
    
    return buf;
  }
  
  private static void appendAllRecords(StringBuilder buf, DnsMessage msg) {
    appendRecords(buf, msg, DnsSection.QUESTION);
    appendRecords(buf, msg, DnsSection.ANSWER);
    appendRecords(buf, msg, DnsSection.AUTHORITY);
    appendRecords(buf, msg, DnsSection.ADDITIONAL);
  }
  
  private static void appendRecords(StringBuilder buf, DnsMessage message, DnsSection section) {
    int count = message.count(section);
    for (int i = 0; i < count; i++)
    {

      buf.append(StringUtil.NEWLINE).append('\t').append(message.recordAt(section, i));
    }
  }
  
  static DnsQuery decodeDnsQuery(DnsRecordDecoder decoder, ByteBuf buf, DnsQueryFactory supplier) throws Exception {
    DnsQuery query = newQuery(buf, supplier);
    boolean success = false;
    try {
      int questionCount = buf.readUnsignedShort();
      int answerCount = buf.readUnsignedShort();
      int authorityRecordCount = buf.readUnsignedShort();
      int additionalRecordCount = buf.readUnsignedShort();
      decodeQuestions(decoder, query, buf, questionCount);
      decodeRecords(decoder, query, DnsSection.ANSWER, buf, answerCount);
      decodeRecords(decoder, query, DnsSection.AUTHORITY, buf, authorityRecordCount);
      decodeRecords(decoder, query, DnsSection.ADDITIONAL, buf, additionalRecordCount);
      success = true;
      return query;
    } finally {
      if (!success) {
        query.release();
      }
    }
  }
  
  private static DnsQuery newQuery(ByteBuf buf, DnsQueryFactory supplier) {
    int id = buf.readUnsignedShort();
    int flags = buf.readUnsignedShort();
    if (flags >> 15 == 1) {
      throw new CorruptedFrameException("not a query");
    }
    
    DnsQuery query = supplier.newQuery(id, DnsOpCode.valueOf((byte)(flags >> 11 & 0xF)));
    query.setRecursionDesired((flags >> 8 & 0x1) == 1);
    query.setZ(flags >> 4 & 0x7);
    return query;
  }
  
  private static void decodeQuestions(DnsRecordDecoder decoder, DnsQuery query, ByteBuf buf, int questionCount) throws Exception
  {
    for (int i = questionCount; i > 0; i--) {
      query.addRecord(DnsSection.QUESTION, decoder.decodeQuestion(buf));
    }
  }
  
  private static void decodeRecords(DnsRecordDecoder decoder, DnsQuery query, DnsSection section, ByteBuf buf, int count) throws Exception
  {
    for (int i = count; i > 0; i--) {
      DnsRecord r = decoder.decodeRecord(buf);
      if (r == null) {
        break;
      }
      query.addRecord(section, r);
    }
  }
  
  static void encodeDnsResponse(DnsRecordEncoder encoder, DnsResponse response, ByteBuf buf) throws Exception {
    boolean success = false;
    try {
      encodeHeader(response, buf);
      encodeQuestions(encoder, response, buf);
      encodeRecords(encoder, response, DnsSection.ANSWER, buf);
      encodeRecords(encoder, response, DnsSection.AUTHORITY, buf);
      encodeRecords(encoder, response, DnsSection.ADDITIONAL, buf);
      success = true;
    } finally {
      if (!success) {
        buf.release();
      }
    }
  }
  





  private static void encodeHeader(DnsResponse response, ByteBuf buf)
  {
    buf.writeShort(response.id());
    int flags = 32768;
    flags |= (response.opCode().byteValue() & 0xFF) << 11;
    if (response.isAuthoritativeAnswer()) {
      flags |= 0x400;
    }
    if (response.isTruncated()) {
      flags |= 0x200;
    }
    if (response.isRecursionDesired()) {
      flags |= 0x100;
    }
    if (response.isRecursionAvailable()) {
      flags |= 0x80;
    }
    flags |= response.z() << 4;
    flags |= response.code().intValue();
    buf.writeShort(flags);
    buf.writeShort(response.count(DnsSection.QUESTION));
    buf.writeShort(response.count(DnsSection.ANSWER));
    buf.writeShort(response.count(DnsSection.AUTHORITY));
    buf.writeShort(response.count(DnsSection.ADDITIONAL));
  }
  
  private static void encodeQuestions(DnsRecordEncoder encoder, DnsResponse response, ByteBuf buf) throws Exception {
    int count = response.count(DnsSection.QUESTION);
    for (int i = 0; i < count; i++) {
      encoder.encodeQuestion((DnsQuestion)response.recordAt(DnsSection.QUESTION, i), buf);
    }
  }
  
  private static void encodeRecords(DnsRecordEncoder encoder, DnsResponse response, DnsSection section, ByteBuf buf) throws Exception
  {
    int count = response.count(section);
    for (int i = 0; i < count; i++) {
      encoder.encodeRecord(response.recordAt(section, i), buf);
    }
  }
  
  private DnsMessageUtil() {}
  
  static abstract interface DnsQueryFactory
  {
    public abstract DnsQuery newQuery(int paramInt, DnsOpCode paramDnsOpCode);
  }
}
