package io.netty.handler.codec.http.multipart;

import io.netty.buffer.ByteBuf;





































final class HttpPostBodyUtil
{
  public static final int chunkSize = 8096;
  public static final String DEFAULT_BINARY_CONTENT_TYPE = "application/octet-stream";
  public static final String DEFAULT_TEXT_CONTENT_TYPE = "text/plain";
  private HttpPostBodyUtil() {}
  
  public static enum TransferEncodingMechanism
  {
    BIT7("7bit"), 
    


    BIT8("8bit"), 
    


    BINARY("binary");
    
    private final String value;
    
    private TransferEncodingMechanism(String value) {
      this.value = value;
    }
    
    public String value() {
      return value;
    }
    
    public String toString()
    {
      return value;
    }
  }
  


  static class SeekAheadOptimize
  {
    byte[] bytes;
    
    int readerIndex;
    
    int pos;
    
    int origPos;
    
    int limit;
    
    ByteBuf buffer;
    

    SeekAheadOptimize(ByteBuf buffer)
    {
      if (!buffer.hasArray()) {
        throw new IllegalArgumentException("buffer hasn't backing byte array");
      }
      this.buffer = buffer;
      bytes = buffer.array();
      readerIndex = buffer.readerIndex();
      origPos = (this.pos = buffer.arrayOffset() + readerIndex);
      limit = (buffer.arrayOffset() + buffer.writerIndex());
    }
    




    void setReadPosition(int minus)
    {
      pos -= minus;
      readerIndex = getReadPosition(pos);
      buffer.readerIndex(readerIndex);
    }
    




    int getReadPosition(int index)
    {
      return index - origPos + readerIndex;
    }
  }
  




  static int findNonWhitespace(String sb, int offset)
  {
    for (int result = offset; result < sb.length(); result++) {
      if (!Character.isWhitespace(sb.charAt(result))) {
        break;
      }
    }
    return result;
  }
  




  static int findEndOfString(String sb)
  {
    for (int result = sb.length(); result > 0; result--) {
      if (!Character.isWhitespace(sb.charAt(result - 1))) {
        break;
      }
    }
    return result;
  }
  







  static int findLineBreak(ByteBuf buffer, int index)
  {
    int toRead = buffer.readableBytes() - (index - buffer.readerIndex());
    int posFirstChar = buffer.bytesBefore(index, toRead, (byte)10);
    if (posFirstChar == -1)
    {
      return -1;
    }
    if ((posFirstChar > 0) && (buffer.getByte(index + posFirstChar - 1) == 13)) {
      posFirstChar--;
    }
    return posFirstChar;
  }
  







  static int findLastLineBreak(ByteBuf buffer, int index)
  {
    int candidate = findLineBreak(buffer, index);
    int findCRLF = 0;
    if (candidate >= 0) {
      if (buffer.getByte(index + candidate) == 13) {
        findCRLF = 2;
      } else {
        findCRLF = 1;
      }
      candidate += findCRLF;
    }
    int next;
    while ((candidate > 0) && ((next = findLineBreak(buffer, index + candidate)) >= 0)) {
      candidate += next;
      if (buffer.getByte(index + candidate) == 13) {
        findCRLF = 2;
      } else {
        findCRLF = 1;
      }
      candidate += findCRLF;
    }
    return candidate - findCRLF;
  }
  












  static int findDelimiter(ByteBuf buffer, int index, byte[] delimiter, boolean precededByLineBreak)
  {
    int delimiterLength = delimiter.length;
    int readerIndex = buffer.readerIndex();
    int writerIndex = buffer.writerIndex();
    int toRead = writerIndex - index;
    int newOffset = index;
    boolean delimiterNotFound = true;
    while ((delimiterNotFound) && (delimiterLength <= toRead))
    {
      int posDelimiter = buffer.bytesBefore(newOffset, toRead, delimiter[0]);
      if (posDelimiter < 0) {
        return -1;
      }
      newOffset += posDelimiter;
      toRead -= posDelimiter;
      
      if (toRead >= delimiterLength) {
        delimiterNotFound = false;
        for (int i = 0; i < delimiterLength; i++) {
          if (buffer.getByte(newOffset + i) != delimiter[i]) {
            newOffset++;
            toRead--;
            delimiterNotFound = true;
            break;
          }
        }
      }
      if (!delimiterNotFound)
      {
        if ((precededByLineBreak) && (newOffset > readerIndex)) {
          if (buffer.getByte(newOffset - 1) == 10) {
            newOffset--;
            
            if ((newOffset > readerIndex) && (buffer.getByte(newOffset - 1) == 13)) {
              newOffset--;
            }
          }
          else {
            newOffset++;
            toRead--;
            delimiterNotFound = true;
            continue;
          }
        }
        return newOffset - readerIndex;
      }
    }
    return -1;
  }
}
