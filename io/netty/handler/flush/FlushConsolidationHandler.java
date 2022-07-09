package io.netty.handler.flush;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.util.internal.ObjectUtil;
import java.util.concurrent.Future;























































public class FlushConsolidationHandler
  extends ChannelDuplexHandler
{
  private final int explicitFlushAfterFlushes;
  private final boolean consolidateWhenNoReadInProgress;
  private final Runnable flushTask;
  private int flushPendingCount;
  private boolean readInProgress;
  private ChannelHandlerContext ctx;
  private Future<?> nextScheduledFlush;
  public static final int DEFAULT_EXPLICIT_FLUSH_AFTER_FLUSHES = 256;
  
  public FlushConsolidationHandler()
  {
    this(256, false);
  }
  




  public FlushConsolidationHandler(int explicitFlushAfterFlushes)
  {
    this(explicitFlushAfterFlushes, false);
  }
  







  public FlushConsolidationHandler(int explicitFlushAfterFlushes, boolean consolidateWhenNoReadInProgress)
  {
    this.explicitFlushAfterFlushes = ObjectUtil.checkPositive(explicitFlushAfterFlushes, "explicitFlushAfterFlushes");
    this.consolidateWhenNoReadInProgress = consolidateWhenNoReadInProgress;
    flushTask = (consolidateWhenNoReadInProgress ? new Runnable()
    {
      public void run()
      {
        if ((flushPendingCount > 0) && (!readInProgress)) {
          flushPendingCount = 0;
          nextScheduledFlush = null;
          ctx.flush(); } } } : null);
  }
  



  public void handlerAdded(ChannelHandlerContext ctx)
    throws Exception
  {
    this.ctx = ctx;
  }
  
  public void flush(ChannelHandlerContext ctx) throws Exception
  {
    if (readInProgress)
    {

      if (++flushPendingCount == explicitFlushAfterFlushes) {
        flushNow(ctx);
      }
    } else if (consolidateWhenNoReadInProgress)
    {
      if (++flushPendingCount == explicitFlushAfterFlushes) {
        flushNow(ctx);
      } else {
        scheduleFlush(ctx);
      }
    }
    else {
      flushNow(ctx);
    }
  }
  
  public void channelReadComplete(ChannelHandlerContext ctx)
    throws Exception
  {
    resetReadAndFlushIfNeeded(ctx);
    ctx.fireChannelReadComplete();
  }
  
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
  {
    readInProgress = true;
    ctx.fireChannelRead(msg);
  }
  
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    throws Exception
  {
    resetReadAndFlushIfNeeded(ctx);
    ctx.fireExceptionCaught(cause);
  }
  
  public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise)
    throws Exception
  {
    resetReadAndFlushIfNeeded(ctx);
    ctx.disconnect(promise);
  }
  
  public void close(ChannelHandlerContext ctx, ChannelPromise promise)
    throws Exception
  {
    resetReadAndFlushIfNeeded(ctx);
    ctx.close(promise);
  }
  
  public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception
  {
    if (!ctx.channel().isWritable())
    {
      flushIfNeeded(ctx);
    }
    ctx.fireChannelWritabilityChanged();
  }
  
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception
  {
    flushIfNeeded(ctx);
  }
  
  private void resetReadAndFlushIfNeeded(ChannelHandlerContext ctx) {
    readInProgress = false;
    flushIfNeeded(ctx);
  }
  
  private void flushIfNeeded(ChannelHandlerContext ctx) {
    if (flushPendingCount > 0) {
      flushNow(ctx);
    }
  }
  
  private void flushNow(ChannelHandlerContext ctx) {
    cancelScheduledFlush();
    flushPendingCount = 0;
    ctx.flush();
  }
  
  private void scheduleFlush(ChannelHandlerContext ctx) {
    if (nextScheduledFlush == null)
    {
      nextScheduledFlush = ctx.channel().eventLoop().submit(flushTask);
    }
  }
  
  private void cancelScheduledFlush() {
    if (nextScheduledFlush != null) {
      nextScheduledFlush.cancel(false);
      nextScheduledFlush = null;
    }
  }
}
