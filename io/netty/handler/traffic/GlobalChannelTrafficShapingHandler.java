package io.netty.handler.traffic;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.Attribute;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.util.AbstractCollection;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


































































@ChannelHandler.Sharable
public class GlobalChannelTrafficShapingHandler
  extends AbstractTrafficShapingHandler
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(GlobalChannelTrafficShapingHandler.class);
  


  final ConcurrentMap<Integer, PerChannel> channelQueues = PlatformDependent.newConcurrentHashMap();
  



  private final AtomicLong queuesSize = new AtomicLong();
  



  private final AtomicLong cumulativeWrittenBytes = new AtomicLong();
  



  private final AtomicLong cumulativeReadBytes = new AtomicLong();
  




  volatile long maxGlobalWriteSize = 419430400L;
  

  private volatile long writeChannelLimit;
  

  private volatile long readChannelLimit;
  

  private static final float DEFAULT_DEVIATION = 0.1F;
  

  private static final float MAX_DEVIATION = 0.4F;
  

  private static final float DEFAULT_SLOWDOWN = 0.4F;
  

  private static final float DEFAULT_ACCELERATION = -0.1F;
  

  private volatile float maxDeviation;
  

  private volatile float accelerationFactor;
  
  private volatile float slowDownFactor;
  
  private volatile boolean readDeviationActive;
  
  private volatile boolean writeDeviationActive;
  

  void createGlobalTrafficCounter(ScheduledExecutorService executor)
  {
    setMaxDeviation(0.1F, 0.4F, -0.1F);
    ObjectUtil.checkNotNullWithIAE(executor, "executor");
    TrafficCounter tc = new GlobalChannelTrafficCounter(this, executor, "GlobalChannelTC", checkInterval);
    setTrafficCounter(tc);
    tc.start();
  }
  
  protected int userDefinedWritabilityIndex()
  {
    return 3;
  }
  





















  public GlobalChannelTrafficShapingHandler(ScheduledExecutorService executor, long writeGlobalLimit, long readGlobalLimit, long writeChannelLimit, long readChannelLimit, long checkInterval, long maxTime)
  {
    super(writeGlobalLimit, readGlobalLimit, checkInterval, maxTime);
    createGlobalTrafficCounter(executor);
    this.writeChannelLimit = writeChannelLimit;
    this.readChannelLimit = readChannelLimit;
  }
  



















  public GlobalChannelTrafficShapingHandler(ScheduledExecutorService executor, long writeGlobalLimit, long readGlobalLimit, long writeChannelLimit, long readChannelLimit, long checkInterval)
  {
    super(writeGlobalLimit, readGlobalLimit, checkInterval);
    this.writeChannelLimit = writeChannelLimit;
    this.readChannelLimit = readChannelLimit;
    createGlobalTrafficCounter(executor);
  }
  















  public GlobalChannelTrafficShapingHandler(ScheduledExecutorService executor, long writeGlobalLimit, long readGlobalLimit, long writeChannelLimit, long readChannelLimit)
  {
    super(writeGlobalLimit, readGlobalLimit);
    this.writeChannelLimit = writeChannelLimit;
    this.readChannelLimit = readChannelLimit;
    createGlobalTrafficCounter(executor);
  }
  








  public GlobalChannelTrafficShapingHandler(ScheduledExecutorService executor, long checkInterval)
  {
    super(checkInterval);
    createGlobalTrafficCounter(executor);
  }
  





  public GlobalChannelTrafficShapingHandler(ScheduledExecutorService executor)
  {
    createGlobalTrafficCounter(executor);
  }
  


  public float maxDeviation()
  {
    return maxDeviation;
  }
  


  public float accelerationFactor()
  {
    return accelerationFactor;
  }
  


  public float slowDownFactor()
  {
    return slowDownFactor;
  }
  










  public void setMaxDeviation(float maxDeviation, float slowDownFactor, float accelerationFactor)
  {
    if (maxDeviation > 0.4F) {
      throw new IllegalArgumentException("maxDeviation must be <= 0.4");
    }
    ObjectUtil.checkPositiveOrZero(slowDownFactor, "slowDownFactor");
    if (accelerationFactor > 0.0F) {
      throw new IllegalArgumentException("accelerationFactor must be <= 0");
    }
    this.maxDeviation = maxDeviation;
    this.accelerationFactor = (1.0F + accelerationFactor);
    this.slowDownFactor = (1.0F + slowDownFactor);
  }
  
  private void computeDeviationCumulativeBytes()
  {
    long maxWrittenBytes = 0L;
    long maxReadBytes = 0L;
    long minWrittenBytes = Long.MAX_VALUE;
    long minReadBytes = Long.MAX_VALUE;
    for (PerChannel perChannel : channelQueues.values()) {
      long value = channelTrafficCounter.cumulativeWrittenBytes();
      if (maxWrittenBytes < value) {
        maxWrittenBytes = value;
      }
      if (minWrittenBytes > value) {
        minWrittenBytes = value;
      }
      value = channelTrafficCounter.cumulativeReadBytes();
      if (maxReadBytes < value) {
        maxReadBytes = value;
      }
      if (minReadBytes > value) {
        minReadBytes = value;
      }
    }
    boolean multiple = channelQueues.size() > 1;
    readDeviationActive = ((multiple) && (minReadBytes < maxReadBytes / 2L));
    writeDeviationActive = ((multiple) && (minWrittenBytes < maxWrittenBytes / 2L));
    cumulativeWrittenBytes.set(maxWrittenBytes);
    cumulativeReadBytes.set(maxReadBytes);
  }
  
  protected void doAccounting(TrafficCounter counter)
  {
    computeDeviationCumulativeBytes();
    super.doAccounting(counter);
  }
  
  private long computeBalancedWait(float maxLocal, float maxGlobal, long wait) {
    if (maxGlobal == 0.0F)
    {
      return wait;
    }
    float ratio = maxLocal / maxGlobal;
    
    if (ratio > maxDeviation) {
      if (ratio < 1.0F - maxDeviation) {
        return wait;
      }
      ratio = slowDownFactor;
      if (wait < 10L) {
        wait = 10L;
      }
    }
    else {
      ratio = accelerationFactor;
    }
    return ((float)wait * ratio);
  }
  


  public long getMaxGlobalWriteSize()
  {
    return maxGlobalWriteSize;
  }
  









  public void setMaxGlobalWriteSize(long maxGlobalWriteSize)
  {
    this.maxGlobalWriteSize = ObjectUtil.checkPositive(maxGlobalWriteSize, "maxGlobalWriteSize");
  }
  


  public long queuesSize()
  {
    return queuesSize.get();
  }
  



  public void configureChannel(long newWriteLimit, long newReadLimit)
  {
    writeChannelLimit = newWriteLimit;
    readChannelLimit = newReadLimit;
    long now = TrafficCounter.milliSecondFromNano();
    for (PerChannel perChannel : channelQueues.values()) {
      channelTrafficCounter.resetAccounting(now);
    }
  }
  


  public long getWriteChannelLimit()
  {
    return writeChannelLimit;
  }
  


  public void setWriteChannelLimit(long writeLimit)
  {
    writeChannelLimit = writeLimit;
    long now = TrafficCounter.milliSecondFromNano();
    for (PerChannel perChannel : channelQueues.values()) {
      channelTrafficCounter.resetAccounting(now);
    }
  }
  


  public long getReadChannelLimit()
  {
    return readChannelLimit;
  }
  


  public void setReadChannelLimit(long readLimit)
  {
    readChannelLimit = readLimit;
    long now = TrafficCounter.milliSecondFromNano();
    for (PerChannel perChannel : channelQueues.values()) {
      channelTrafficCounter.resetAccounting(now);
    }
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
      perChannel = new PerChannel();
      messagesQueue = new ArrayDeque();
      

      channelTrafficCounter = new TrafficCounter(this, null, "ChannelTC" + ctx.channel().hashCode(), checkInterval);
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
    trafficCounter.resetCumulativeTime();
    super.handlerAdded(ctx);
  }
  
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception
  {
    trafficCounter.resetCumulativeTime();
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
            channelTrafficCounter.bytesRealWriteFlowControl(size);
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
  
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
  {
    long size = calculateSize(msg);
    long now = TrafficCounter.milliSecondFromNano();
    if (size > 0L)
    {
      long waitGlobal = trafficCounter.readTimeToWait(size, getReadLimit(), maxTime, now);
      Integer key = Integer.valueOf(ctx.channel().hashCode());
      PerChannel perChannel = (PerChannel)channelQueues.get(key);
      long wait = 0L;
      if (perChannel != null) {
        wait = channelTrafficCounter.readTimeToWait(size, readChannelLimit, maxTime, now);
        if (readDeviationActive)
        {

          long maxLocalRead = channelTrafficCounter.cumulativeReadBytes();
          long maxGlobalRead = cumulativeReadBytes.get();
          if (maxLocalRead <= 0L) {
            maxLocalRead = 0L;
          }
          if (maxGlobalRead < maxLocalRead) {
            maxGlobalRead = maxLocalRead;
          }
          wait = computeBalancedWait((float)maxLocalRead, (float)maxGlobalRead, wait);
        }
      }
      if (wait < waitGlobal) {
        wait = waitGlobal;
      }
      wait = checkWaitReadTime(ctx, wait, now);
      if (wait >= 10L)
      {

        Channel channel = ctx.channel();
        ChannelConfig config = channel.config();
        if (logger.isDebugEnabled()) {
          logger.debug("Read Suspend: " + wait + ':' + config.isAutoRead() + ':' + 
            isHandlerActive(ctx));
        }
        if ((config.isAutoRead()) && (isHandlerActive(ctx))) {
          config.setAutoRead(false);
          channel.attr(READ_SUSPENDED).set(Boolean.valueOf(true));
          

          Attribute<Runnable> attr = channel.attr(REOPEN_TASK);
          Runnable reopenTask = (Runnable)attr.get();
          if (reopenTask == null) {
            reopenTask = new AbstractTrafficShapingHandler.ReopenReadTimerTask(ctx);
            attr.set(reopenTask);
          }
          ctx.executor().schedule(reopenTask, wait, TimeUnit.MILLISECONDS);
          if (logger.isDebugEnabled()) {
            logger.debug("Suspend final status => " + config.isAutoRead() + ':' + 
              isHandlerActive(ctx) + " will reopened at: " + wait);
          }
        }
      }
    }
    informReadOperation(ctx, now);
    ctx.fireChannelRead(msg);
  }
  
  protected long checkWaitReadTime(ChannelHandlerContext ctx, long wait, long now)
  {
    Integer key = Integer.valueOf(ctx.channel().hashCode());
    PerChannel perChannel = (PerChannel)channelQueues.get(key);
    if ((perChannel != null) && 
      (wait > maxTime) && (now + wait - lastReadTimestamp > maxTime)) {
      wait = maxTime;
    }
    
    return wait;
  }
  
  protected void informReadOperation(ChannelHandlerContext ctx, long now)
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
    final ChannelPromise promise;
    final long size;
    
    private ToSend(long delay, Object toSend, long size, ChannelPromise promise) {
      relativeTimeAction = delay;
      this.toSend = toSend;
      this.size = size;
      this.promise = promise;
    }
  }
  
  protected long maximumCumulativeWrittenBytes() {
    return cumulativeWrittenBytes.get();
  }
  
  protected long maximumCumulativeReadBytes() {
    return cumulativeReadBytes.get();
  }
  



  public Collection<TrafficCounter> channelTrafficCounters()
  {
    new AbstractCollection()
    {
      public Iterator<TrafficCounter> iterator() {
        new Iterator() {
          final Iterator<GlobalChannelTrafficShapingHandler.PerChannel> iter = channelQueues.values().iterator();
          
          public boolean hasNext() {
            return iter.hasNext();
          }
          
          public TrafficCounter next() {
            return iter.next()).channelTrafficCounter;
          }
          
          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      }
      
      public int size() {
        return channelQueues.size();
      }
    };
  }
  
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
    throws Exception
  {
    long size = calculateSize(msg);
    long now = TrafficCounter.milliSecondFromNano();
    if (size > 0L)
    {
      long waitGlobal = trafficCounter.writeTimeToWait(size, getWriteLimit(), maxTime, now);
      Integer key = Integer.valueOf(ctx.channel().hashCode());
      PerChannel perChannel = (PerChannel)channelQueues.get(key);
      long wait = 0L;
      if (perChannel != null) {
        wait = channelTrafficCounter.writeTimeToWait(size, writeChannelLimit, maxTime, now);
        if (writeDeviationActive)
        {

          long maxLocalWrite = channelTrafficCounter.cumulativeWrittenBytes();
          long maxGlobalWrite = cumulativeWrittenBytes.get();
          if (maxLocalWrite <= 0L) {
            maxLocalWrite = 0L;
          }
          if (maxGlobalWrite < maxLocalWrite) {
            maxGlobalWrite = maxLocalWrite;
          }
          wait = computeBalancedWait((float)maxLocalWrite, (float)maxGlobalWrite, wait);
        }
      }
      if (wait < waitGlobal) {
        wait = waitGlobal;
      }
      if (wait >= 10L) {
        if (logger.isDebugEnabled()) {
          logger.debug("Write suspend: " + wait + ':' + ctx.channel().config().isAutoRead() + ':' + 
            isHandlerActive(ctx));
        }
        submitWrite(ctx, msg, size, wait, now, promise);
        return;
      }
    }
    
    submitWrite(ctx, msg, size, 0L, now, promise);
  }
  


  protected void submitWrite(final ChannelHandlerContext ctx, Object msg, long size, long writedelay, long now, ChannelPromise promise)
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
        channelTrafficCounter.bytesRealWriteFlowControl(size);
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

      public void run() { GlobalChannelTrafficShapingHandler.this.sendAllValid(ctx, forSchedule, futureNow); } }, delay, TimeUnit.MILLISECONDS);
  }
  


  private void sendAllValid(ChannelHandlerContext ctx, PerChannel perChannel, long now)
  {
    synchronized (perChannel) {
      for (ToSend newToSend = (ToSend)messagesQueue.pollFirst(); 
          newToSend != null; newToSend = (ToSend)messagesQueue.pollFirst()) {
        if (relativeTimeAction <= now) {
          long size = size;
          trafficCounter.bytesRealWriteFlowControl(size);
          channelTrafficCounter.bytesRealWriteFlowControl(size);
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
  
  public String toString()
  {
    return 
    
      340 + super.toString() + " Write Channel Limit: " + writeChannelLimit + " Read Channel Limit: " + readChannelLimit;
  }
  
  static final class PerChannel
  {
    ArrayDeque<GlobalChannelTrafficShapingHandler.ToSend> messagesQueue;
    TrafficCounter channelTrafficCounter;
    long queueSize;
    long lastWriteTimestamp;
    long lastReadTimestamp;
    
    PerChannel() {}
  }
}
