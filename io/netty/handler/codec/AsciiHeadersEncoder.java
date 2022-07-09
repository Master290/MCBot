package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.ObjectUtil;
import java.util.Map.Entry;




















public final class AsciiHeadersEncoder
{
  private final ByteBuf buf;
  private final SeparatorType separatorType;
  private final NewlineType newlineType;
  
  public static enum SeparatorType
  {
    COLON, 
    


    COLON_SPACE;
    


    private SeparatorType() {}
  }
  

  public static enum NewlineType
  {
    LF, 
    


    CRLF;
    

    private NewlineType() {}
  }
  
  public AsciiHeadersEncoder(ByteBuf buf)
  {
    this(buf, SeparatorType.COLON_SPACE, NewlineType.CRLF);
  }
  
  public AsciiHeadersEncoder(ByteBuf buf, SeparatorType separatorType, NewlineType newlineType) {
    this.buf = ((ByteBuf)ObjectUtil.checkNotNull(buf, "buf"));
    this.separatorType = ((SeparatorType)ObjectUtil.checkNotNull(separatorType, "separatorType"));
    this.newlineType = ((NewlineType)ObjectUtil.checkNotNull(newlineType, "newlineType"));
  }
  
  public void encode(Map.Entry<CharSequence, CharSequence> entry) {
    CharSequence name = (CharSequence)entry.getKey();
    CharSequence value = (CharSequence)entry.getValue();
    ByteBuf buf = this.buf;
    int nameLen = name.length();
    int valueLen = value.length();
    int entryLen = nameLen + valueLen + 4;
    int offset = buf.writerIndex();
    buf.ensureWritable(entryLen);
    writeAscii(buf, offset, name);
    offset += nameLen;
    
    switch (separatorType) {
    case COLON: 
      buf.setByte(offset++, 58);
      break;
    case COLON_SPACE: 
      buf.setByte(offset++, 58);
      buf.setByte(offset++, 32);
      break;
    default: 
      throw new Error();
    }
    
    writeAscii(buf, offset, value);
    offset += valueLen;
    
    switch (newlineType) {
    case LF: 
      buf.setByte(offset++, 10);
      break;
    case CRLF: 
      buf.setByte(offset++, 13);
      buf.setByte(offset++, 10);
      break;
    default: 
      throw new Error();
    }
    
    buf.writerIndex(offset);
  }
  
  private static void writeAscii(ByteBuf buf, int offset, CharSequence value) {
    if ((value instanceof AsciiString)) {
      ByteBufUtil.copy((AsciiString)value, 0, buf, offset, value.length());
    } else {
      buf.setCharSequence(offset, value, CharsetUtil.US_ASCII);
    }
  }
}
