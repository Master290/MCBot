package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import java.io.Closeable;

public abstract interface Http2FrameReader
  extends Closeable
{
  public abstract void readFrame(ChannelHandlerContext paramChannelHandlerContext, ByteBuf paramByteBuf, Http2FrameListener paramHttp2FrameListener)
    throws Http2Exception;
  
  public abstract Configuration configuration();
  
  public abstract void close();
  
  public static abstract interface Configuration
  {
    public abstract Http2HeadersDecoder.Configuration headersConfiguration();
    
    public abstract Http2FrameSizePolicy frameSizePolicy();
  }
}
