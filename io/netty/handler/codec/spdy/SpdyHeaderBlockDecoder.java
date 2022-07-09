package io.netty.handler.codec.spdy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;













abstract class SpdyHeaderBlockDecoder
{
  SpdyHeaderBlockDecoder() {}
  
  static SpdyHeaderBlockDecoder newInstance(SpdyVersion spdyVersion, int maxHeaderSize)
  {
    return new SpdyHeaderBlockZlibDecoder(spdyVersion, maxHeaderSize);
  }
  
  abstract void decode(ByteBufAllocator paramByteBufAllocator, ByteBuf paramByteBuf, SpdyHeadersFrame paramSpdyHeadersFrame)
    throws Exception;
  
  abstract void endHeaderBlock(SpdyHeadersFrame paramSpdyHeadersFrame)
    throws Exception;
  
  abstract void end();
}
