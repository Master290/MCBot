package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.util.AsciiString;
import io.netty.util.ByteProcessor;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
































final class HpackHuffmanEncoder
{
  private final int[] codes;
  private final byte[] lengths;
  private final EncodedLengthProcessor encodedLengthProcessor = new EncodedLengthProcessor(null);
  private final EncodeProcessor encodeProcessor = new EncodeProcessor(null);
  
  HpackHuffmanEncoder() {
    this(HpackUtil.HUFFMAN_CODES, HpackUtil.HUFFMAN_CODE_LENGTHS);
  }
  





  private HpackHuffmanEncoder(int[] codes, byte[] lengths)
  {
    this.codes = codes;
    this.lengths = lengths;
  }
  





  public void encode(ByteBuf out, CharSequence data)
  {
    ObjectUtil.checkNotNull(out, "out");
    if ((data instanceof AsciiString)) {
      AsciiString string = (AsciiString)data;
      try {
        encodeProcessor.out = out;
        string.forEachByte(encodeProcessor);
      } catch (Exception e) {
        PlatformDependent.throwException(e);
      } finally {
        encodeProcessor.end();
      }
    } else {
      encodeSlowPath(out, data);
    }
  }
  
  private void encodeSlowPath(ByteBuf out, CharSequence data) {
    long current = 0L;
    int n = 0;
    
    for (int i = 0; i < data.length(); i++) {
      int b = data.charAt(i) & 0xFF;
      int code = codes[b];
      int nbits = lengths[b];
      
      current <<= nbits;
      current |= code;
      n += nbits;
      
      while (n >= 8) {
        n -= 8;
        out.writeByte((int)(current >> n));
      }
    }
    
    if (n > 0) {
      current <<= 8 - n;
      current |= 255 >>> n;
      out.writeByte((int)current);
    }
  }
  





  int getEncodedLength(CharSequence data)
  {
    if ((data instanceof AsciiString)) {
      AsciiString string = (AsciiString)data;
      try {
        encodedLengthProcessor.reset();
        string.forEachByte(encodedLengthProcessor);
        return encodedLengthProcessor.length();
      } catch (Exception e) {
        PlatformDependent.throwException(e);
        return -1;
      }
    }
    return getEncodedLengthSlowPath(data);
  }
  
  private int getEncodedLengthSlowPath(CharSequence data)
  {
    long len = 0L;
    for (int i = 0; i < data.length(); i++) {
      len += lengths[(data.charAt(i) & 0xFF)];
    }
    return (int)(len + 7L >> 3);
  }
  
  private final class EncodeProcessor implements ByteProcessor {
    ByteBuf out;
    private long current;
    private int n;
    
    private EncodeProcessor() {}
    
    public boolean process(byte value) { int b = value & 0xFF;
      int nbits = lengths[b];
      
      current <<= nbits;
      current |= codes[b];
      n += nbits;
      
      while (n >= 8) {
        n -= 8;
        out.writeByte((int)(current >> n));
      }
      return true;
    }
    
    void end() {
      try {
        if (n > 0) {
          current <<= 8 - n;
          current |= 255 >>> n;
          out.writeByte((int)current);
        }
        
        out = null;
        current = 0L;
        n = 0;
      }
      finally
      {
        out = null;
        current = 0L;
        n = 0;
      }
    }
  }
  
  private final class EncodedLengthProcessor implements ByteProcessor {
    private long len;
    
    private EncodedLengthProcessor() {}
    
    public boolean process(byte value) { len += lengths[(value & 0xFF)];
      return true;
    }
    
    void reset() {
      len = 0L;
    }
    
    int length() {
      return (int)(len + 7L >> 3);
    }
  }
}
