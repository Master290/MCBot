package io.netty.handler.traffic;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.EventExecutor;
import java.util.ArrayDeque;
import java.util.concurrent.TimeUnit;





















































public class ChannelTrafficShapingHandler
  extends AbstractTrafficShapingHandler
{
  private final ArrayDeque<ToSend> messagesQueue = new ArrayDeque();
  






  private long queueSize;
  






  public ChannelTrafficShapingHandler(long writeLimit, long readLimit, long checkInterval, long maxTime)
  {
    super(writeLimit, readLimit, checkInterval, maxTime);
  }
  












  public ChannelTrafficShapingHandler(long writeLimit, long readLimit, long checkInterval)
  {
    super(writeLimit, readLimit, checkInterval);
  }
  









  public ChannelTrafficShapingHandler(long writeLimit, long readLimit)
  {
    super(writeLimit, readLimit);
  }
  







  public ChannelTrafficShapingHandler(long checkInterval)
  {
    super(checkInterval);
  }
  
  public void handlerAdded(ChannelHandlerContext ctx)
    throws Exception
  {
    TrafficCounter trafficCounter = new TrafficCounter(this, ctx.executor(), "ChannelTC" + ctx.channel().hashCode(), checkInterval);
    setTrafficCounter(trafficCounter);
    trafficCounter.start();
    super.handlerAdded(ctx);
  }
  
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception
  {
    trafficCounter.stop();
    
    synchronized (this) {
      if (ctx.channel().isActive()) {
        for (ToSend toSend : messagesQueue) {
          long size = calculateSize(toSend);
          trafficCounter.bytesRealWriteFlowControl(size);
          queueSize -= size;
          ctx.write(toSend, promise);
        }
      } else {
        for (ToSend toSend : messagesQueue) {
          if ((toSend instanceof ByteBuf)) {
            ((ByteBuf)toSend).release();
          }
        }
      }
      messagesQueue.clear();
    }
    releaseWriteSuspended(ctx);
    releaseReadSuspended(ctx);
    super.handlerRemoved(ctx);
  }
  
  private static final class ToSend {
    final long relativeTimeAction;
    final Object toSend;
    final ChannelPromise promise;
    
    private ToSend(long delay, Object toSend, ChannelPromise promise) {
      relativeTimeAction = delay;
      this.toSend = toSend;
      this.promise = promise;
    }
  }
  




  void submitWrite(final ChannelHandlerContext ctx, Object msg, long size, long delay, long now, ChannelPromise promise)
  {
    synchronized (this) {
      if ((delay == 0L) && (messagesQueue.isEmpty())) {
        trafficCounter.bytesRealWriteFlowControl(size);
        ctx.write(msg, promise);
        return;
      }
      ToSend newToSend = new ToSend(delay + now, msg, promise, null);
      messagesQueue.addLast(newToSend);
      queueSize += size;
      checkWriteSuspend(ctx, delay, queueSize); }
    ToSend newToSend;
    final long futureNow = relativeTimeAction;
    ctx.executor().schedule(new Runnable()
    {

      public void run() { ChannelTrafficShapingHandler.this.sendAllValid(ctx, futureNow); } }, delay, TimeUnit.MILLISECONDS);
  }
  


  private void sendAllValid(ChannelHandlerContext ctx, long now)
  {
    synchronized (this) {
      for (ToSend newToSend = (ToSend)messagesQueue.pollFirst(); 
          newToSend != null; newToSend = (ToSend)messagesQueue.pollFirst()) {
        if (relativeTimeAction <= now) {
          long size = calculateSize(toSend);
          trafficCounter.bytesRealWriteFlowControl(size);
          queueSize -= size;
          ctx.write(toSend, promise);
        } else {
          messagesQueue.addFirst(newToSend);
          break;
        }
      }
      if (messagesQueue.isEmpty()) {
        releaseWriteSuspended(ctx);
      }
    }
    ctx.flush();
  }
  


  public long queueSize()
  {
    return queueSize;
  }
}
