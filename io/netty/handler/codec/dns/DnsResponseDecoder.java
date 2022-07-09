package io.netty.handler.codec.dns;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.util.internal.ObjectUtil;
import java.net.SocketAddress;




















abstract class DnsResponseDecoder<A extends SocketAddress>
{
  private final DnsRecordDecoder recordDecoder;
  
  DnsResponseDecoder(DnsRecordDecoder recordDecoder)
  {
    this.recordDecoder = ((DnsRecordDecoder)ObjectUtil.checkNotNull(recordDecoder, "recordDecoder"));
  }
  
  final DnsResponse decode(A sender, A recipient, ByteBuf buffer) throws Exception {
    int id = buffer.readUnsignedShort();
    
    int flags = buffer.readUnsignedShort();
    if (flags >> 15 == 0) {
      throw new CorruptedFrameException("not a response");
    }
    
    DnsResponse response = newResponse(sender, recipient, id, 
    


      DnsOpCode.valueOf((byte)(flags >> 11 & 0xF)), DnsResponseCode.valueOf((byte)(flags & 0xF)));
    
    response.setRecursionDesired((flags >> 8 & 0x1) == 1);
    response.setAuthoritativeAnswer((flags >> 10 & 0x1) == 1);
    response.setTruncated((flags >> 9 & 0x1) == 1);
    response.setRecursionAvailable((flags >> 7 & 0x1) == 1);
    response.setZ(flags >> 4 & 0x7);
    
    boolean success = false;
    try {
      int questionCount = buffer.readUnsignedShort();
      int answerCount = buffer.readUnsignedShort();
      int authorityRecordCount = buffer.readUnsignedShort();
      int additionalRecordCount = buffer.readUnsignedShort();
      
      decodeQuestions(response, buffer, questionCount);
      DnsResponse localDnsResponse1; if (!decodeRecords(response, DnsSection.ANSWER, buffer, answerCount)) {
        success = true;
        return response;
      }
      if (!decodeRecords(response, DnsSection.AUTHORITY, buffer, authorityRecordCount)) {
        success = true;
        return response;
      }
      
      decodeRecords(response, DnsSection.ADDITIONAL, buffer, additionalRecordCount);
      success = true;
      return response;
    } finally {
      if (!success) {
        response.release();
      }
    }
  }
  
  protected abstract DnsResponse newResponse(A paramA1, A paramA2, int paramInt, DnsOpCode paramDnsOpCode, DnsResponseCode paramDnsResponseCode) throws Exception;
  
  private void decodeQuestions(DnsResponse response, ByteBuf buf, int questionCount) throws Exception
  {
    for (int i = questionCount; i > 0; i--) {
      response.addRecord(DnsSection.QUESTION, recordDecoder.decodeQuestion(buf));
    }
  }
  
  private boolean decodeRecords(DnsResponse response, DnsSection section, ByteBuf buf, int count) throws Exception
  {
    for (int i = count; i > 0; i--) {
      DnsRecord r = recordDecoder.decodeRecord(buf);
      if (r == null)
      {
        return false;
      }
      
      response.addRecord(section, r);
    }
    return true;
  }
}
