package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import java.io.Closeable;
import java.util.List;

public abstract interface Http2ConnectionDecoder
  extends Closeable
{
  public abstract void lifecycleManager(Http2LifecycleManager paramHttp2LifecycleManager);
  
  public abstract Http2Connection connection();
  
  public abstract Http2LocalFlowController flowController();
  
  public abstract void frameListener(Http2FrameListener paramHttp2FrameListener);
  
  public abstract Http2FrameListener frameListener();
  
  public abstract void decodeFrame(ChannelHandlerContext paramChannelHandlerContext, ByteBuf paramByteBuf, List<Object> paramList)
    throws Http2Exception;
  
  public abstract Http2Settings localSettings();
  
  public abstract boolean prefaceReceived();
  
  public abstract void close();
}
