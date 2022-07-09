package io.netty.handler.codec.spdy;

import com.jcraft.jzlib.Deflater;
import com.jcraft.jzlib.JZlib;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.compression.CompressionException;
import io.netty.util.internal.ObjectUtil;

















class SpdyHeaderBlockJZlibEncoder
  extends SpdyHeaderBlockRawEncoder
{
  private final Deflater z = new Deflater();
  
  private boolean finished;
  
  SpdyHeaderBlockJZlibEncoder(SpdyVersion version, int compressionLevel, int windowBits, int memLevel)
  {
    super(version);
    if ((compressionLevel < 0) || (compressionLevel > 9)) {
      throw new IllegalArgumentException("compressionLevel: " + compressionLevel + " (expected: 0-9)");
    }
    
    if ((windowBits < 9) || (windowBits > 15)) {
      throw new IllegalArgumentException("windowBits: " + windowBits + " (expected: 9-15)");
    }
    
    if ((memLevel < 1) || (memLevel > 9)) {
      throw new IllegalArgumentException("memLevel: " + memLevel + " (expected: 1-9)");
    }
    

    int resultCode = z.deflateInit(compressionLevel, windowBits, memLevel, JZlib.W_ZLIB);
    
    if (resultCode != 0) {
      throw new CompressionException("failed to initialize an SPDY header block deflater: " + resultCode);
    }
    
    resultCode = z.deflateSetDictionary(SpdyCodecUtil.SPDY_DICT, SpdyCodecUtil.SPDY_DICT.length);
    if (resultCode != 0) {
      throw new CompressionException("failed to set the SPDY dictionary: " + resultCode);
    }
  }
  

  private void setInput(ByteBuf decompressed)
  {
    int len = decompressed.readableBytes();
    int offset;
    byte[] in;
    int offset;
    if (decompressed.hasArray()) {
      byte[] in = decompressed.array();
      offset = decompressed.arrayOffset() + decompressed.readerIndex();
    } else {
      in = new byte[len];
      decompressed.getBytes(decompressed.readerIndex(), in);
      offset = 0;
    }
    z.next_in = in;
    z.next_in_index = offset;
    z.avail_in = len;
  }
  
  private ByteBuf encode(ByteBufAllocator alloc) {
    boolean release = true;
    ByteBuf out = null;
    try {
      int oldNextInIndex = z.next_in_index;
      int oldNextOutIndex = z.next_out_index;
      
      int maxOutputLength = (int)Math.ceil(z.next_in.length * 1.001D) + 12;
      out = alloc.heapBuffer(maxOutputLength);
      z.next_out = out.array();
      z.next_out_index = (out.arrayOffset() + out.writerIndex());
      z.avail_out = maxOutputLength;
      
      try
      {
        resultCode = z.deflate(2);
      } finally { int resultCode;
        out.skipBytes(z.next_in_index - oldNextInIndex); }
      int resultCode;
      if (resultCode != 0) {
        throw new CompressionException("compression failure: " + resultCode);
      }
      
      int outputLength = z.next_out_index - oldNextOutIndex;
      if (outputLength > 0) {
        out.writerIndex(out.writerIndex() + outputLength);
      }
      release = false;
      return out;

    }
    finally
    {

      z.next_in = null;
      z.next_out = null;
      if ((release) && (out != null)) {
        out.release();
      }
    }
  }
  
  public ByteBuf encode(ByteBufAllocator alloc, SpdyHeadersFrame frame) throws Exception
  {
    ObjectUtil.checkNotNullWithIAE(alloc, "alloc");
    ObjectUtil.checkNotNullWithIAE(frame, "frame");
    
    if (finished) {
      return Unpooled.EMPTY_BUFFER;
    }
    
    ByteBuf decompressed = super.encode(alloc, frame);
    try { ByteBuf localByteBuf1;
      if (!decompressed.isReadable()) {
        return Unpooled.EMPTY_BUFFER;
      }
      
      setInput(decompressed);
      return encode(alloc);
    } finally {
      decompressed.release();
    }
  }
  
  public void end()
  {
    if (finished) {
      return;
    }
    finished = true;
    z.deflateEnd();
    z.next_in = null;
    z.next_out = null;
  }
}
