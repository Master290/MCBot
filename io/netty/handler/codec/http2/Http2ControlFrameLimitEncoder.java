package io.netty.handler.codec.http2;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;


















final class Http2ControlFrameLimitEncoder
  extends DecoratingHttp2ConnectionEncoder
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(Http2ControlFrameLimitEncoder.class);
  
  private final int maxOutstandingControlFrames;
  private final ChannelFutureListener outstandingControlFramesListener = new ChannelFutureListener()
  {
    public void operationComplete(ChannelFuture future) {
      Http2ControlFrameLimitEncoder.access$010(Http2ControlFrameLimitEncoder.this);
    }
  };
  private Http2LifecycleManager lifecycleManager;
  private int outstandingControlFrames;
  private boolean limitReached;
  
  Http2ControlFrameLimitEncoder(Http2ConnectionEncoder delegate, int maxOutstandingControlFrames) {
    super(delegate);
    this.maxOutstandingControlFrames = ObjectUtil.checkPositive(maxOutstandingControlFrames, "maxOutstandingControlFrames");
  }
  

  public void lifecycleManager(Http2LifecycleManager lifecycleManager)
  {
    this.lifecycleManager = lifecycleManager;
    super.lifecycleManager(lifecycleManager);
  }
  
  public ChannelFuture writeSettingsAck(ChannelHandlerContext ctx, ChannelPromise promise)
  {
    ChannelPromise newPromise = handleOutstandingControlFrames(ctx, promise);
    if (newPromise == null) {
      return promise;
    }
    return super.writeSettingsAck(ctx, newPromise);
  }
  

  public ChannelFuture writePing(ChannelHandlerContext ctx, boolean ack, long data, ChannelPromise promise)
  {
    if (ack) {
      ChannelPromise newPromise = handleOutstandingControlFrames(ctx, promise);
      if (newPromise == null) {
        return promise;
      }
      return super.writePing(ctx, ack, data, newPromise);
    }
    return super.writePing(ctx, ack, data, promise);
  }
  

  public ChannelFuture writeRstStream(ChannelHandlerContext ctx, int streamId, long errorCode, ChannelPromise promise)
  {
    ChannelPromise newPromise = handleOutstandingControlFrames(ctx, promise);
    if (newPromise == null) {
      return promise;
    }
    return super.writeRstStream(ctx, streamId, errorCode, newPromise);
  }
  
  private ChannelPromise handleOutstandingControlFrames(ChannelHandlerContext ctx, ChannelPromise promise) {
    if (!limitReached) {
      if (outstandingControlFrames == maxOutstandingControlFrames)
      {
        ctx.flush();
      }
      if (outstandingControlFrames == maxOutstandingControlFrames) {
        limitReached = true;
        Http2Exception exception = Http2Exception.connectionError(Http2Error.ENHANCE_YOUR_CALM, "Maximum number %d of outstanding control frames reached", new Object[] {
          Integer.valueOf(maxOutstandingControlFrames) });
        logger.info("Maximum number {} of outstanding control frames reached. Closing channel {}", new Object[] {
          Integer.valueOf(maxOutstandingControlFrames), ctx.channel(), exception });
        

        lifecycleManager.onError(ctx, true, exception);
        ctx.close();
      }
      outstandingControlFrames += 1;
      


      return promise.unvoid().addListener(outstandingControlFramesListener);
    }
    return promise;
  }
}
