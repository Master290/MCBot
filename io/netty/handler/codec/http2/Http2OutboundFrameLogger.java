package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.internal.ObjectUtil;




















public class Http2OutboundFrameLogger
  implements Http2FrameWriter
{
  private final Http2FrameWriter writer;
  private final Http2FrameLogger logger;
  
  public Http2OutboundFrameLogger(Http2FrameWriter writer, Http2FrameLogger logger)
  {
    this.writer = ((Http2FrameWriter)ObjectUtil.checkNotNull(writer, "writer"));
    this.logger = ((Http2FrameLogger)ObjectUtil.checkNotNull(logger, "logger"));
  }
  

  public ChannelFuture writeData(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endStream, ChannelPromise promise)
  {
    logger.logData(Http2FrameLogger.Direction.OUTBOUND, ctx, streamId, data, padding, endStream);
    return writer.writeData(ctx, streamId, data, padding, endStream, promise);
  }
  

  public ChannelFuture writeHeaders(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding, boolean endStream, ChannelPromise promise)
  {
    logger.logHeaders(Http2FrameLogger.Direction.OUTBOUND, ctx, streamId, headers, padding, endStream);
    return writer.writeHeaders(ctx, streamId, headers, padding, endStream, promise);
  }
  


  public ChannelFuture writeHeaders(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int streamDependency, short weight, boolean exclusive, int padding, boolean endStream, ChannelPromise promise)
  {
    logger.logHeaders(Http2FrameLogger.Direction.OUTBOUND, ctx, streamId, headers, streamDependency, weight, exclusive, padding, endStream);
    
    return writer.writeHeaders(ctx, streamId, headers, streamDependency, weight, exclusive, padding, endStream, promise);
  }
  


  public ChannelFuture writePriority(ChannelHandlerContext ctx, int streamId, int streamDependency, short weight, boolean exclusive, ChannelPromise promise)
  {
    logger.logPriority(Http2FrameLogger.Direction.OUTBOUND, ctx, streamId, streamDependency, weight, exclusive);
    return writer.writePriority(ctx, streamId, streamDependency, weight, exclusive, promise);
  }
  

  public ChannelFuture writeRstStream(ChannelHandlerContext ctx, int streamId, long errorCode, ChannelPromise promise)
  {
    logger.logRstStream(Http2FrameLogger.Direction.OUTBOUND, ctx, streamId, errorCode);
    return writer.writeRstStream(ctx, streamId, errorCode, promise);
  }
  

  public ChannelFuture writeSettings(ChannelHandlerContext ctx, Http2Settings settings, ChannelPromise promise)
  {
    logger.logSettings(Http2FrameLogger.Direction.OUTBOUND, ctx, settings);
    return writer.writeSettings(ctx, settings, promise);
  }
  
  public ChannelFuture writeSettingsAck(ChannelHandlerContext ctx, ChannelPromise promise)
  {
    logger.logSettingsAck(Http2FrameLogger.Direction.OUTBOUND, ctx);
    return writer.writeSettingsAck(ctx, promise);
  }
  

  public ChannelFuture writePing(ChannelHandlerContext ctx, boolean ack, long data, ChannelPromise promise)
  {
    if (ack) {
      logger.logPingAck(Http2FrameLogger.Direction.OUTBOUND, ctx, data);
    } else {
      logger.logPing(Http2FrameLogger.Direction.OUTBOUND, ctx, data);
    }
    return writer.writePing(ctx, ack, data, promise);
  }
  

  public ChannelFuture writePushPromise(ChannelHandlerContext ctx, int streamId, int promisedStreamId, Http2Headers headers, int padding, ChannelPromise promise)
  {
    logger.logPushPromise(Http2FrameLogger.Direction.OUTBOUND, ctx, streamId, promisedStreamId, headers, padding);
    return writer.writePushPromise(ctx, streamId, promisedStreamId, headers, padding, promise);
  }
  

  public ChannelFuture writeGoAway(ChannelHandlerContext ctx, int lastStreamId, long errorCode, ByteBuf debugData, ChannelPromise promise)
  {
    logger.logGoAway(Http2FrameLogger.Direction.OUTBOUND, ctx, lastStreamId, errorCode, debugData);
    return writer.writeGoAway(ctx, lastStreamId, errorCode, debugData, promise);
  }
  

  public ChannelFuture writeWindowUpdate(ChannelHandlerContext ctx, int streamId, int windowSizeIncrement, ChannelPromise promise)
  {
    logger.logWindowsUpdate(Http2FrameLogger.Direction.OUTBOUND, ctx, streamId, windowSizeIncrement);
    return writer.writeWindowUpdate(ctx, streamId, windowSizeIncrement, promise);
  }
  

  public ChannelFuture writeFrame(ChannelHandlerContext ctx, byte frameType, int streamId, Http2Flags flags, ByteBuf payload, ChannelPromise promise)
  {
    logger.logUnknownFrame(Http2FrameLogger.Direction.OUTBOUND, ctx, frameType, streamId, flags, payload);
    return writer.writeFrame(ctx, frameType, streamId, flags, payload, promise);
  }
  
  public void close()
  {
    writer.close();
  }
  
  public Http2FrameWriter.Configuration configuration()
  {
    return writer.configuration();
  }
}
