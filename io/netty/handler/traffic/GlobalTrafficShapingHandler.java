package io.netty.handler.traffic;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;































































@ChannelHandler.Sharable
public class GlobalTrafficShapingHandler
  extends AbstractTrafficShapingHandler
{
  private final ConcurrentMap<Integer, PerChannel> channelQueues = PlatformDependent.newConcurrentHashMap();
  



  private final AtomicLong queuesSize = new AtomicLong();
  




  long maxGlobalWriteSize = 419430400L;
  










  void createGlobalTrafficCounter(ScheduledExecutorService executor)
  {
    TrafficCounter tc = new TrafficCounter(this, (ScheduledExecutorService)ObjectUtil.checkNotNull(executor, "executor"), "GlobalTC", checkInterval);
    


    setTrafficCounter(tc);
    tc.start();
  }
  
  protected int userDefinedWritabilityIndex()
  {
    return 2;
  }
  















  public GlobalTrafficShapingHandler(ScheduledExecutorService executor, long writeLimit, long readLimit, long checkInterval, long maxTime)
  {
    super(writeLimit, readLimit, checkInterval, maxTime);
    createGlobalTrafficCounter(executor);
  }
  














  public GlobalTrafficShapingHandler(ScheduledExecutorService executor, long writeLimit, long readLimit, long checkInterval)
  {
    super(writeLimit, readLimit, checkInterval);
    createGlobalTrafficCounter(executor);
  }
  











  public GlobalTrafficShapingHandler(ScheduledExecutorService executor, long writeLimit, long readLimit)
  {
    super(writeLimit, readLimit);
    createGlobalTrafficCounter(executor);
  }
  









  public GlobalTrafficShapingHandler(ScheduledExecutorService executor, long checkInterval)
  {
    super(checkInterval);
    createGlobalTrafficCounter(executor);
  }
  






  public GlobalTrafficShapingHandler(EventExecutor executor)
  {
    createGlobalTrafficCounter(executor);
  }
  


  public long getMaxGlobalWriteSize()
  {
    return maxGlobalWriteSize;
  }
  










  public void setMaxGlobalWriteSize(long maxGlobalWriteSize)
  {
    this.maxGlobalWriteSize = maxGlobalWriteSize;
  }
  


  public long queuesSize()
  {
    return queuesSize.get();
  }
  


  public final void release()
  {
    trafficCounter.stop();
  }
  
  private PerChannel getOrSetPerChannel(ChannelHandlerContext ctx)
  {
    Channel channel = ctx.channel();
    Integer key = Integer.valueOf(channel.hashCode());
    PerChannel perChannel = (PerChannel)channelQueues.get(key);
    if (perChannel == null) {
      perChannel = new PerChannel(null);
      messagesQueue = new ArrayDeque();
      queueSize = 0L;
      lastReadTimestamp = TrafficCounter.milliSecondFromNano();
      lastWriteTimestamp = lastReadTimestamp;
      channelQueues.put(key, perChannel);
    }
    return perChannel;
  }
  
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception
  {
    getOrSetPerChannel(ctx);
    super.handlerAdded(ctx);
  }
  
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception
  {
    Channel channel = ctx.channel();
    Integer key = Integer.valueOf(channel.hashCode());
    PerChannel perChannel = (PerChannel)channelQueues.remove(key);
    if (perChannel != null)
    {
      synchronized (perChannel) {
        if (channel.isActive()) {
          for (ToSend toSend : messagesQueue) {
            long size = calculateSize(toSend);
            trafficCounter.bytesRealWriteFlowControl(size);
            queueSize -= size;
            queuesSize.addAndGet(-size);
            ctx.write(toSend, promise);
          }
        } else {
          queuesSize.addAndGet(-queueSize);
          for (ToSend toSend : messagesQueue) {
            if ((toSend instanceof ByteBuf)) {
              ((ByteBuf)toSend).release();
            }
          }
        }
        messagesQueue.clear();
      }
    }
    releaseWriteSuspended(ctx);
    releaseReadSuspended(ctx);
    super.handlerRemoved(ctx);
  }
  
  long checkWaitReadTime(ChannelHandlerContext ctx, long wait, long now)
  {
    Integer key = Integer.valueOf(ctx.channel().hashCode());
    PerChannel perChannel = (PerChannel)channelQueues.get(key);
    if ((perChannel != null) && 
      (wait > maxTime) && (now + wait - lastReadTimestamp > maxTime)) {
      wait = maxTime;
    }
    
    return wait;
  }
  
  void informReadOperation(ChannelHandlerContext ctx, long now)
  {
    Integer key = Integer.valueOf(ctx.channel().hashCode());
    PerChannel perChannel = (PerChannel)channelQueues.get(key);
    if (perChannel != null) {
      lastReadTimestamp = now;
    }
  }
  
  private static final class ToSend {
    final long relativeTimeAction;
    final Object toSend;
    final long size;
    final ChannelPromise promise;
    
    private ToSend(long delay, Object toSend, long size, ChannelPromise promise) {
      relativeTimeAction = delay;
      this.toSend = toSend;
      this.size = size;
      this.promise = promise;
    }
  }
  


  void submitWrite(final ChannelHandlerContext ctx, Object msg, long size, long writedelay, long now, ChannelPromise promise)
  {
    Channel channel = ctx.channel();
    Integer key = Integer.valueOf(channel.hashCode());
    PerChannel perChannel = (PerChannel)channelQueues.get(key);
    if (perChannel == null)
    {

      perChannel = getOrSetPerChannel(ctx);
    }
    
    long delay = writedelay;
    boolean globalSizeExceeded = false;
    
    synchronized (perChannel) {
      if ((writedelay == 0L) && (messagesQueue.isEmpty())) {
        trafficCounter.bytesRealWriteFlowControl(size);
        ctx.write(msg, promise);
        lastWriteTimestamp = now;
        return;
      }
      if ((delay > maxTime) && (now + delay - lastWriteTimestamp > maxTime)) {
        delay = maxTime;
      }
      ToSend newToSend = new ToSend(delay + now, msg, size, promise, null);
      messagesQueue.addLast(newToSend);
      queueSize += size;
      queuesSize.addAndGet(size);
      checkWriteSuspend(ctx, delay, queueSize);
      if (queuesSize.get() > maxGlobalWriteSize)
        globalSizeExceeded = true;
    }
    ToSend newToSend;
    if (globalSizeExceeded) {
      setUserDefinedWritability(ctx, false);
    }
    final long futureNow = relativeTimeAction;
    final PerChannel forSchedule = perChannel;
    ctx.executor().schedule(new Runnable()
    {

      public void run() { GlobalTrafficShapingHandler.this.sendAllValid(ctx, forSchedule, futureNow); } }, delay, TimeUnit.MILLISECONDS);
  }
  


  private void sendAllValid(ChannelHandlerContext ctx, PerChannel perChannel, long now)
  {
    synchronized (perChannel) {
      for (ToSend newToSend = (ToSend)messagesQueue.pollFirst(); 
          newToSend != null; newToSend = (ToSend)messagesQueue.pollFirst()) {
        if (relativeTimeAction <= now) {
          long size = size;
          trafficCounter.bytesRealWriteFlowControl(size);
          queueSize -= size;
          queuesSize.addAndGet(-size);
          ctx.write(toSend, promise);
          lastWriteTimestamp = now;
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
  
  private static final class PerChannel
  {
    ArrayDeque<GlobalTrafficShapingHandler.ToSend> messagesQueue;
    long queueSize;
    long lastWriteTimestamp;
    long lastReadTimestamp;
    
    private PerChannel() {}
  }
}
