package io.netty.handler.codec.base64;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ByteProcessor;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import java.nio.ByteOrder;



































public final class Base64
{
  private static final int MAX_LINE_LENGTH = 76;
  private static final byte EQUALS_SIGN = 61;
  private static final byte NEW_LINE = 10;
  private static final byte WHITE_SPACE_ENC = -5;
  private static final byte EQUALS_SIGN_ENC = -1;
  
  private static byte[] alphabet(Base64Dialect dialect)
  {
    return checkNotNull"dialect"alphabet;
  }
  
  private static byte[] decodabet(Base64Dialect dialect) {
    return checkNotNull"dialect"decodabet;
  }
  
  private static boolean breakLines(Base64Dialect dialect) {
    return checkNotNull"dialect"breakLinesByDefault;
  }
  
  public static ByteBuf encode(ByteBuf src) {
    return encode(src, Base64Dialect.STANDARD);
  }
  
  public static ByteBuf encode(ByteBuf src, Base64Dialect dialect) {
    return encode(src, breakLines(dialect), dialect);
  }
  
  public static ByteBuf encode(ByteBuf src, boolean breakLines) {
    return encode(src, breakLines, Base64Dialect.STANDARD);
  }
  
  public static ByteBuf encode(ByteBuf src, boolean breakLines, Base64Dialect dialect) {
    ObjectUtil.checkNotNull(src, "src");
    
    ByteBuf dest = encode(src, src.readerIndex(), src.readableBytes(), breakLines, dialect);
    src.readerIndex(src.writerIndex());
    return dest;
  }
  
  public static ByteBuf encode(ByteBuf src, int off, int len) {
    return encode(src, off, len, Base64Dialect.STANDARD);
  }
  
  public static ByteBuf encode(ByteBuf src, int off, int len, Base64Dialect dialect) {
    return encode(src, off, len, breakLines(dialect), dialect);
  }
  
  public static ByteBuf encode(ByteBuf src, int off, int len, boolean breakLines)
  {
    return encode(src, off, len, breakLines, Base64Dialect.STANDARD);
  }
  
  public static ByteBuf encode(ByteBuf src, int off, int len, boolean breakLines, Base64Dialect dialect)
  {
    return encode(src, off, len, breakLines, dialect, src.alloc());
  }
  
  public static ByteBuf encode(ByteBuf src, int off, int len, boolean breakLines, Base64Dialect dialect, ByteBufAllocator allocator)
  {
    ObjectUtil.checkNotNull(src, "src");
    ObjectUtil.checkNotNull(dialect, "dialect");
    
    ByteBuf dest = allocator.buffer(encodedBufferSize(len, breakLines)).order(src.order());
    byte[] alphabet = alphabet(dialect);
    int d = 0;
    int e = 0;
    int len2 = len - 2;
    int lineLength = 0;
    for (; d < len2; e += 4) {
      encode3to4(src, d + off, 3, dest, e, alphabet);
      
      lineLength += 4;
      
      if ((breakLines) && (lineLength == 76)) {
        dest.setByte(e + 4, 10);
        e++;
        lineLength = 0;
      }
      d += 3;
    }
    









    if (d < len) {
      encode3to4(src, d + off, len - d, dest, e, alphabet);
      e += 4;
    }
    

    if ((e > 1) && (dest.getByte(e - 1) == 10)) {
      e--;
    }
    
    return dest.slice(0, e);
  }
  











  private static void encode3to4(ByteBuf src, int srcOffset, int numSigBytes, ByteBuf dest, int destOffset, byte[] alphabet)
  {
    if (src.order() == ByteOrder.BIG_ENDIAN) { int inBuff;
      int inBuff;
      int inBuff; switch (numSigBytes) {
      case 1: 
        inBuff = toInt(src.getByte(srcOffset));
        break;
      case 2: 
        inBuff = toIntBE(src.getShort(srcOffset));
        break;
      default: 
        inBuff = numSigBytes <= 0 ? 0 : toIntBE(src.getMedium(srcOffset));
      }
      
      encode3to4BigEndian(inBuff, numSigBytes, dest, destOffset, alphabet); } else { int inBuff;
      int inBuff;
      int inBuff;
      switch (numSigBytes) {
      case 1: 
        inBuff = toInt(src.getByte(srcOffset));
        break;
      case 2: 
        inBuff = toIntLE(src.getShort(srcOffset));
        break;
      default: 
        inBuff = numSigBytes <= 0 ? 0 : toIntLE(src.getMedium(srcOffset));
      }
      
      encode3to4LittleEndian(inBuff, numSigBytes, dest, destOffset, alphabet);
    }
  }
  

  static int encodedBufferSize(int len, boolean breakLines)
  {
    long len43 = (len << 2) / 3L;
    

    long ret = len43 + 3L & 0xFFFFFFFFFFFFFFFC;
    
    if (breakLines) {
      ret += len43 / 76L;
    }
    
    return ret < 2147483647L ? (int)ret : Integer.MAX_VALUE;
  }
  
  private static int toInt(byte value) {
    return (value & 0xFF) << 16;
  }
  
  private static int toIntBE(short value) {
    return (value & 0xFF00) << 8 | (value & 0xFF) << 8;
  }
  
  private static int toIntLE(short value) {
    return (value & 0xFF) << 16 | value & 0xFF00;
  }
  
  private static int toIntBE(int mediumValue) {
    return mediumValue & 0xFF0000 | mediumValue & 0xFF00 | mediumValue & 0xFF;
  }
  
  private static int toIntLE(int mediumValue) {
    return (mediumValue & 0xFF) << 16 | mediumValue & 0xFF00 | (mediumValue & 0xFF0000) >>> 16;
  }
  

  private static void encode3to4BigEndian(int inBuff, int numSigBytes, ByteBuf dest, int destOffset, byte[] alphabet)
  {
    switch (numSigBytes) {
    case 3: 
      dest.setInt(destOffset, alphabet[(inBuff >>> 18)] << 24 | alphabet[(inBuff >>> 12 & 0x3F)] << 16 | alphabet[(inBuff >>> 6 & 0x3F)] << 8 | alphabet[(inBuff & 0x3F)]);
      


      break;
    case 2: 
      dest.setInt(destOffset, alphabet[(inBuff >>> 18)] << 24 | alphabet[(inBuff >>> 12 & 0x3F)] << 16 | alphabet[(inBuff >>> 6 & 0x3F)] << 8 | 0x3D);
      


      break;
    case 1: 
      dest.setInt(destOffset, alphabet[(inBuff >>> 18)] << 24 | alphabet[(inBuff >>> 12 & 0x3F)] << 16 | 0x3D00 | 0x3D);
      


      break;
    }
    
  }
  



  private static void encode3to4LittleEndian(int inBuff, int numSigBytes, ByteBuf dest, int destOffset, byte[] alphabet)
  {
    switch (numSigBytes) {
    case 3: 
      dest.setInt(destOffset, alphabet[(inBuff >>> 18)] | alphabet[(inBuff >>> 12 & 0x3F)] << 8 | alphabet[(inBuff >>> 6 & 0x3F)] << 16 | alphabet[(inBuff & 0x3F)] << 24);
      


      break;
    case 2: 
      dest.setInt(destOffset, alphabet[(inBuff >>> 18)] | alphabet[(inBuff >>> 12 & 0x3F)] << 8 | alphabet[(inBuff >>> 6 & 0x3F)] << 16 | 0x3D000000);
      


      break;
    case 1: 
      dest.setInt(destOffset, alphabet[(inBuff >>> 18)] | alphabet[(inBuff >>> 12 & 0x3F)] << 8 | 0x3D0000 | 0x3D000000);
      


      break;
    }
    
  }
  

  public static ByteBuf decode(ByteBuf src)
  {
    return decode(src, Base64Dialect.STANDARD);
  }
  
  public static ByteBuf decode(ByteBuf src, Base64Dialect dialect) {
    ObjectUtil.checkNotNull(src, "src");
    
    ByteBuf dest = decode(src, src.readerIndex(), src.readableBytes(), dialect);
    src.readerIndex(src.writerIndex());
    return dest;
  }
  
  public static ByteBuf decode(ByteBuf src, int off, int len)
  {
    return decode(src, off, len, Base64Dialect.STANDARD);
  }
  
  public static ByteBuf decode(ByteBuf src, int off, int len, Base64Dialect dialect)
  {
    return decode(src, off, len, dialect, src.alloc());
  }
  
  public static ByteBuf decode(ByteBuf src, int off, int len, Base64Dialect dialect, ByteBufAllocator allocator)
  {
    ObjectUtil.checkNotNull(src, "src");
    ObjectUtil.checkNotNull(dialect, "dialect");
    

    return new Decoder(null).decode(src, off, len, allocator, dialect);
  }
  


  static int decodedBufferSize(int len) { return len - (len >>> 2); }
  
  private Base64() {}
  
  private static final class Decoder implements ByteProcessor { private final byte[] b4 = new byte[4];
    private int b4Posn;
    private byte[] decodabet;
    
    private Decoder() {}
    
    ByteBuf decode(ByteBuf src, int off, int len, ByteBufAllocator allocator, Base64Dialect dialect) {
      dest = allocator.buffer(Base64.decodedBufferSize(len)).order(src.order());
      
      decodabet = Base64.decodabet(dialect);
      try {
        src.forEachByte(off, len, this);
        return dest.slice(0, outBuffPosn);
      } catch (Throwable cause) {
        dest.release();
        PlatformDependent.throwException(cause); }
      return null;
    }
    
    private int outBuffPosn;
    private ByteBuf dest;
    public boolean process(byte value) throws Exception {
      if (value > 0) {
        byte sbiDecode = decodabet[value];
        if (sbiDecode >= -5) {
          if (sbiDecode >= -1) {
            b4[(b4Posn++)] = value;
            if (b4Posn > 3) {
              outBuffPosn += decode4to3(b4, dest, outBuffPosn, decodabet);
              b4Posn = 0;
              

              return value != 61;
            }
          }
          return true;
        }
      }
      throw new IllegalArgumentException("invalid Base64 input character: " + (short)(value & 0xFF) + " (decimal)");
    }
    
    private static int decode4to3(byte[] src, ByteBuf dest, int destOffset, byte[] decodabet)
    {
      byte src0 = src[0];
      byte src1 = src[1];
      byte src2 = src[2];
      
      if (src2 == 61)
      {
        try {
          decodedValue = (decodabet[src0] & 0xFF) << 2 | (decodabet[src1] & 0xFF) >>> 4;
        } catch (IndexOutOfBoundsException ignored) { int decodedValue;
          throw new IllegalArgumentException("not encoded in Base64"); }
        int decodedValue;
        dest.setByte(destOffset, decodedValue);
        return 1;
      }
      
      byte src3 = src[3];
      if (src3 == 61)
      {
        byte b1 = decodabet[src1];
        try {
          int decodedValue;
          if (dest.order() == ByteOrder.BIG_ENDIAN)
          {

            decodedValue = ((decodabet[src0] & 0x3F) << 2 | (b1 & 0xF0) >> 4) << 8 | (b1 & 0xF) << 4 | (decodabet[src2] & 0xFC) >>> 2;
          }
          else
          {
            decodedValue = (decodabet[src0] & 0x3F) << 2 | (b1 & 0xF0) >> 4 | ((b1 & 0xF) << 4 | (decodabet[src2] & 0xFC) >>> 2) << 8;
          }
        } catch (IndexOutOfBoundsException ignored) {
          int decodedValue;
          throw new IllegalArgumentException("not encoded in Base64"); }
        int decodedValue;
        dest.setShort(destOffset, decodedValue);
        return 2;
      }
      try
      {
        int decodedValue;
        if (dest.order() == ByteOrder.BIG_ENDIAN) {
          decodedValue = (decodabet[src0] & 0x3F) << 18 | (decodabet[src1] & 0xFF) << 12 | (decodabet[src2] & 0xFF) << 6 | decodabet[src3] & 0xFF;

        }
        else
        {
          byte b1 = decodabet[src1];
          byte b2 = decodabet[src2];
          




          decodedValue = (decodabet[src0] & 0x3F) << 2 | (b1 & 0xF) << 12 | (b1 & 0xF0) >>> 4 | (b2 & 0x3) << 22 | (b2 & 0xFC) << 6 | (decodabet[src3] & 0xFF) << 16;
        }
      }
      catch (IndexOutOfBoundsException ignored)
      {
        int decodedValue;
        





        throw new IllegalArgumentException("not encoded in Base64"); }
      int decodedValue;
      dest.setMedium(destOffset, decodedValue);
      return 3;
    }
  }
}
