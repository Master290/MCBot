package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.ObjectUtil;

















public class Http2FrameListenerDecorator
  implements Http2FrameListener
{
  protected final Http2FrameListener listener;
  
  public Http2FrameListenerDecorator(Http2FrameListener listener)
  {
    this.listener = ((Http2FrameListener)ObjectUtil.checkNotNull(listener, "listener"));
  }
  
  public int onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream)
    throws Http2Exception
  {
    return listener.onDataRead(ctx, streamId, data, padding, endOfStream);
  }
  
  public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding, boolean endStream)
    throws Http2Exception
  {
    listener.onHeadersRead(ctx, streamId, headers, padding, endStream);
  }
  
  public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int streamDependency, short weight, boolean exclusive, int padding, boolean endStream)
    throws Http2Exception
  {
    listener.onHeadersRead(ctx, streamId, headers, streamDependency, weight, exclusive, padding, endStream);
  }
  
  public void onPriorityRead(ChannelHandlerContext ctx, int streamId, int streamDependency, short weight, boolean exclusive)
    throws Http2Exception
  {
    listener.onPriorityRead(ctx, streamId, streamDependency, weight, exclusive);
  }
  
  public void onRstStreamRead(ChannelHandlerContext ctx, int streamId, long errorCode) throws Http2Exception
  {
    listener.onRstStreamRead(ctx, streamId, errorCode);
  }
  
  public void onSettingsAckRead(ChannelHandlerContext ctx) throws Http2Exception
  {
    listener.onSettingsAckRead(ctx);
  }
  
  public void onSettingsRead(ChannelHandlerContext ctx, Http2Settings settings) throws Http2Exception
  {
    listener.onSettingsRead(ctx, settings);
  }
  
  public void onPingRead(ChannelHandlerContext ctx, long data) throws Http2Exception
  {
    listener.onPingRead(ctx, data);
  }
  
  public void onPingAckRead(ChannelHandlerContext ctx, long data) throws Http2Exception
  {
    listener.onPingAckRead(ctx, data);
  }
  
  public void onPushPromiseRead(ChannelHandlerContext ctx, int streamId, int promisedStreamId, Http2Headers headers, int padding)
    throws Http2Exception
  {
    listener.onPushPromiseRead(ctx, streamId, promisedStreamId, headers, padding);
  }
  
  public void onGoAwayRead(ChannelHandlerContext ctx, int lastStreamId, long errorCode, ByteBuf debugData)
    throws Http2Exception
  {
    listener.onGoAwayRead(ctx, lastStreamId, errorCode, debugData);
  }
  
  public void onWindowUpdateRead(ChannelHandlerContext ctx, int streamId, int windowSizeIncrement)
    throws Http2Exception
  {
    listener.onWindowUpdateRead(ctx, streamId, windowSizeIncrement);
  }
  
  public void onUnknownFrame(ChannelHandlerContext ctx, byte frameType, int streamId, Http2Flags flags, ByteBuf payload)
    throws Http2Exception
  {
    listener.onUnknownFrame(ctx, frameType, streamId, flags, payload);
  }
}
