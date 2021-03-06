package io.netty.handler.codec.compression;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.io.InputStream;
import lzma.sdk.lzma.Encoder;

























public class LzmaFrameEncoder
  extends MessageToByteEncoder<ByteBuf>
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(LzmaFrameEncoder.class);
  


  private static final int MEDIUM_DICTIONARY_SIZE = 65536;
  


  private static final int MIN_FAST_BYTES = 5;
  


  private static final int MEDIUM_FAST_BYTES = 32;
  


  private static final int MAX_FAST_BYTES = 273;
  


  private static final int DEFAULT_MATCH_FINDER = 1;
  

  private static final int DEFAULT_LC = 3;
  

  private static final int DEFAULT_LP = 0;
  

  private static final int DEFAULT_PB = 2;
  

  private final Encoder encoder;
  

  private final byte properties;
  

  private final int littleEndianDictionarySize;
  

  private static boolean warningLogged;
  


  public LzmaFrameEncoder()
  {
    this(65536);
  }
  



  public LzmaFrameEncoder(int lc, int lp, int pb)
  {
    this(lc, lp, pb, 65536);
  }
  





  public LzmaFrameEncoder(int dictionarySize)
  {
    this(3, 0, 2, dictionarySize);
  }
  


  public LzmaFrameEncoder(int lc, int lp, int pb, int dictionarySize)
  {
    this(lc, lp, pb, dictionarySize, false, 32);
  }
  




















  public LzmaFrameEncoder(int lc, int lp, int pb, int dictionarySize, boolean endMarkerMode, int numFastBytes)
  {
    if ((lc < 0) || (lc > 8)) {
      throw new IllegalArgumentException("lc: " + lc + " (expected: 0-8)");
    }
    if ((lp < 0) || (lp > 4)) {
      throw new IllegalArgumentException("lp: " + lp + " (expected: 0-4)");
    }
    if ((pb < 0) || (pb > 4)) {
      throw new IllegalArgumentException("pb: " + pb + " (expected: 0-4)");
    }
    if ((lc + lp > 4) && 
      (!warningLogged)) {
      logger.warn("The latest versions of LZMA libraries (for example, XZ Utils) has an additional requirement: lc + lp <= 4. Data which don't follow this requirement cannot be decompressed with this libraries.");
      

      warningLogged = true;
    }
    
    if (dictionarySize < 0) {
      throw new IllegalArgumentException("dictionarySize: " + dictionarySize + " (expected: 0+)");
    }
    if ((numFastBytes < 5) || (numFastBytes > 273)) {
      throw new IllegalArgumentException(String.format("numFastBytes: %d (expected: %d-%d)", new Object[] {
        Integer.valueOf(numFastBytes), Integer.valueOf(5), Integer.valueOf(273) }));
    }
    

    encoder = new Encoder();
    encoder.setDictionarySize(dictionarySize);
    encoder.setEndMarkerMode(endMarkerMode);
    encoder.setMatchFinder(1);
    encoder.setNumFastBytes(numFastBytes);
    encoder.setLcLpPb(lc, lp, pb);
    
    properties = ((byte)((pb * 5 + lp) * 9 + lc));
    littleEndianDictionarySize = Integer.reverseBytes(dictionarySize);
  }
  
  protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws Exception
  {
    int length = in.readableBytes();
    InputStream bbIn = null;
    ByteBufOutputStream bbOut = null;
    try {
      bbIn = new ByteBufInputStream(in);
      bbOut = new ByteBufOutputStream(out);
      bbOut.writeByte(properties);
      bbOut.writeInt(littleEndianDictionarySize);
      bbOut.writeLong(Long.reverseBytes(length));
      encoder.code(bbIn, bbOut, -1L, -1L, null);
    } finally {
      if (bbIn != null) {
        bbIn.close();
      }
      if (bbOut != null) {
        bbOut.close();
      }
    }
  }
  
  protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, ByteBuf in, boolean preferDirect) throws Exception
  {
    int length = in.readableBytes();
    int maxOutputLength = maxOutputBufferLength(length);
    return ctx.alloc().ioBuffer(maxOutputLength);
  }
  

  private static int maxOutputBufferLength(int inputLength)
  {
    double factor;
    double factor;
    if (inputLength < 200) {
      factor = 1.5D; } else { double factor;
      if (inputLength < 500) {
        factor = 1.2D; } else { double factor;
        if (inputLength < 1000) {
          factor = 1.1D; } else { double factor;
          if (inputLength < 10000) {
            factor = 1.05D;
          } else
            factor = 1.02D;
        } } }
    return 13 + (int)(inputLength * factor);
  }
}
