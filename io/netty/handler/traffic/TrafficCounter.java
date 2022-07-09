package io.netty.handler.traffic;

import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;




























public class TrafficCounter
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(TrafficCounter.class);
  


  public static long milliSecondFromNano()
  {
    return System.nanoTime() / 1000000L;
  }
  



  private final AtomicLong currentWrittenBytes = new AtomicLong();
  



  private final AtomicLong currentReadBytes = new AtomicLong();
  



  private long writingTime;
  



  private long readingTime;
  



  private final AtomicLong cumulativeWrittenBytes = new AtomicLong();
  



  private final AtomicLong cumulativeReadBytes = new AtomicLong();
  



  private long lastCumulativeTime;
  



  private long lastWriteThroughput;
  



  private long lastReadThroughput;
  



  final AtomicLong lastTime = new AtomicLong();
  



  private volatile long lastWrittenBytes;
  



  private volatile long lastReadBytes;
  



  private volatile long lastWritingTime;
  



  private volatile long lastReadingTime;
  



  private final AtomicLong realWrittenBytes = new AtomicLong();
  



  private long realWriteThroughput;
  



  final AtomicLong checkInterval = new AtomicLong(1000L);
  



  final String name;
  


  final AbstractTrafficShapingHandler trafficShapingHandler;
  


  final ScheduledExecutorService executor;
  


  Runnable monitor;
  


  volatile ScheduledFuture<?> scheduledFuture;
  


  volatile boolean monitorActive;
  



  private final class TrafficMonitoringTask
    implements Runnable
  {
    private TrafficMonitoringTask() {}
    



    public void run()
    {
      if (!monitorActive) {
        return;
      }
      resetAccounting(TrafficCounter.milliSecondFromNano());
      if (trafficShapingHandler != null) {
        trafficShapingHandler.doAccounting(TrafficCounter.this);
      }
    }
  }
  


  public synchronized void start()
  {
    if (monitorActive) {
      return;
    }
    lastTime.set(milliSecondFromNano());
    long localCheckInterval = checkInterval.get();
    
    if ((localCheckInterval > 0L) && (executor != null)) {
      monitorActive = true;
      monitor = new TrafficMonitoringTask(null);
      
      scheduledFuture = executor.scheduleAtFixedRate(monitor, 0L, localCheckInterval, TimeUnit.MILLISECONDS);
    }
  }
  


  public synchronized void stop()
  {
    if (!monitorActive) {
      return;
    }
    monitorActive = false;
    resetAccounting(milliSecondFromNano());
    if (trafficShapingHandler != null) {
      trafficShapingHandler.doAccounting(this);
    }
    if (scheduledFuture != null) {
      scheduledFuture.cancel(true);
    }
  }
  




  synchronized void resetAccounting(long newLastTime)
  {
    long interval = newLastTime - lastTime.getAndSet(newLastTime);
    if (interval == 0L)
    {
      return;
    }
    if ((logger.isDebugEnabled()) && (interval > checkInterval() << 1)) {
      logger.debug("Acct schedule not ok: " + interval + " > 2*" + checkInterval() + " from " + name);
    }
    lastReadBytes = currentReadBytes.getAndSet(0L);
    lastWrittenBytes = currentWrittenBytes.getAndSet(0L);
    lastReadThroughput = (lastReadBytes * 1000L / interval);
    
    lastWriteThroughput = (lastWrittenBytes * 1000L / interval);
    
    realWriteThroughput = (realWrittenBytes.getAndSet(0L) * 1000L / interval);
    lastWritingTime = Math.max(lastWritingTime, writingTime);
    lastReadingTime = Math.max(lastReadingTime, readingTime);
  }
  












  public TrafficCounter(ScheduledExecutorService executor, String name, long checkInterval)
  {
    this.name = ((String)ObjectUtil.checkNotNull(name, "name"));
    trafficShapingHandler = null;
    this.executor = executor;
    
    init(checkInterval);
  }
  















  public TrafficCounter(AbstractTrafficShapingHandler trafficShapingHandler, ScheduledExecutorService executor, String name, long checkInterval)
  {
    this.name = ((String)ObjectUtil.checkNotNull(name, "name"));
    this.trafficShapingHandler = ((AbstractTrafficShapingHandler)ObjectUtil.checkNotNullWithIAE(trafficShapingHandler, "trafficShapingHandler"));
    this.executor = executor;
    
    init(checkInterval);
  }
  
  private void init(long checkInterval)
  {
    lastCumulativeTime = System.currentTimeMillis();
    writingTime = milliSecondFromNano();
    readingTime = writingTime;
    lastWritingTime = writingTime;
    lastReadingTime = writingTime;
    configure(checkInterval);
  }
  




  public void configure(long newCheckInterval)
  {
    long newInterval = newCheckInterval / 10L * 10L;
    if (checkInterval.getAndSet(newInterval) != newInterval) {
      if (newInterval <= 0L) {
        stop();
        
        lastTime.set(milliSecondFromNano());
      }
      else {
        stop();
        start();
      }
    }
  }
  





  void bytesRecvFlowControl(long recv)
  {
    currentReadBytes.addAndGet(recv);
    cumulativeReadBytes.addAndGet(recv);
  }
  





  void bytesWriteFlowControl(long write)
  {
    currentWrittenBytes.addAndGet(write);
    cumulativeWrittenBytes.addAndGet(write);
  }
  





  void bytesRealWriteFlowControl(long write)
  {
    realWrittenBytes.addAndGet(write);
  }
  



  public long checkInterval()
  {
    return checkInterval.get();
  }
  


  public long lastReadThroughput()
  {
    return lastReadThroughput;
  }
  


  public long lastWriteThroughput()
  {
    return lastWriteThroughput;
  }
  


  public long lastReadBytes()
  {
    return lastReadBytes;
  }
  


  public long lastWrittenBytes()
  {
    return lastWrittenBytes;
  }
  


  public long currentReadBytes()
  {
    return currentReadBytes.get();
  }
  


  public long currentWrittenBytes()
  {
    return currentWrittenBytes.get();
  }
  


  public long lastTime()
  {
    return lastTime.get();
  }
  


  public long cumulativeWrittenBytes()
  {
    return cumulativeWrittenBytes.get();
  }
  


  public long cumulativeReadBytes()
  {
    return cumulativeReadBytes.get();
  }
  



  public long lastCumulativeTime()
  {
    return lastCumulativeTime;
  }
  


  public AtomicLong getRealWrittenBytes()
  {
    return realWrittenBytes;
  }
  


  public long getRealWriteThroughput()
  {
    return realWriteThroughput;
  }
  



  public void resetCumulativeTime()
  {
    lastCumulativeTime = System.currentTimeMillis();
    cumulativeReadBytes.set(0L);
    cumulativeWrittenBytes.set(0L);
  }
  


  public String name()
  {
    return name;
  }
  











  @Deprecated
  public long readTimeToWait(long size, long limitTraffic, long maxTime)
  {
    return readTimeToWait(size, limitTraffic, maxTime, milliSecondFromNano());
  }
  












  public long readTimeToWait(long size, long limitTraffic, long maxTime, long now)
  {
    bytesRecvFlowControl(size);
    if ((size == 0L) || (limitTraffic == 0L)) {
      return 0L;
    }
    long lastTimeCheck = lastTime.get();
    long sum = currentReadBytes.get();
    long localReadingTime = readingTime;
    long lastRB = lastReadBytes;
    long interval = now - lastTimeCheck;
    long pastDelay = Math.max(lastReadingTime - lastTimeCheck, 0L);
    if (interval > 10L)
    {
      long time = sum * 1000L / limitTraffic - interval + pastDelay;
      if (time > 10L) {
        if (logger.isDebugEnabled()) {
          logger.debug("Time: " + time + ':' + sum + ':' + interval + ':' + pastDelay);
        }
        if ((time > maxTime) && (now + time - localReadingTime > maxTime)) {
          time = maxTime;
        }
        readingTime = Math.max(localReadingTime, now + time);
        return time;
      }
      readingTime = Math.max(localReadingTime, now);
      return 0L;
    }
    
    long lastsum = sum + lastRB;
    long lastinterval = interval + checkInterval.get();
    long time = lastsum * 1000L / limitTraffic - lastinterval + pastDelay;
    if (time > 10L) {
      if (logger.isDebugEnabled()) {
        logger.debug("Time: " + time + ':' + lastsum + ':' + lastinterval + ':' + pastDelay);
      }
      if ((time > maxTime) && (now + time - localReadingTime > maxTime)) {
        time = maxTime;
      }
      readingTime = Math.max(localReadingTime, now + time);
      return time;
    }
    readingTime = Math.max(localReadingTime, now);
    return 0L;
  }
  











  @Deprecated
  public long writeTimeToWait(long size, long limitTraffic, long maxTime)
  {
    return writeTimeToWait(size, limitTraffic, maxTime, milliSecondFromNano());
  }
  












  public long writeTimeToWait(long size, long limitTraffic, long maxTime, long now)
  {
    bytesWriteFlowControl(size);
    if ((size == 0L) || (limitTraffic == 0L)) {
      return 0L;
    }
    long lastTimeCheck = lastTime.get();
    long sum = currentWrittenBytes.get();
    long lastWB = lastWrittenBytes;
    long localWritingTime = writingTime;
    long pastDelay = Math.max(lastWritingTime - lastTimeCheck, 0L);
    long interval = now - lastTimeCheck;
    if (interval > 10L)
    {
      long time = sum * 1000L / limitTraffic - interval + pastDelay;
      if (time > 10L) {
        if (logger.isDebugEnabled()) {
          logger.debug("Time: " + time + ':' + sum + ':' + interval + ':' + pastDelay);
        }
        if ((time > maxTime) && (now + time - localWritingTime > maxTime)) {
          time = maxTime;
        }
        writingTime = Math.max(localWritingTime, now + time);
        return time;
      }
      writingTime = Math.max(localWritingTime, now);
      return 0L;
    }
    
    long lastsum = sum + lastWB;
    long lastinterval = interval + checkInterval.get();
    long time = lastsum * 1000L / limitTraffic - lastinterval + pastDelay;
    if (time > 10L) {
      if (logger.isDebugEnabled()) {
        logger.debug("Time: " + time + ':' + lastsum + ':' + lastinterval + ':' + pastDelay);
      }
      if ((time > maxTime) && (now + time - localWritingTime > maxTime)) {
        time = maxTime;
      }
      writingTime = Math.max(localWritingTime, now + time);
      return time;
    }
    writingTime = Math.max(localWritingTime, now);
    return 0L;
  }
  
  public String toString()
  {
    return 
    




      165 + "Monitor " + name + " Current Speed Read: " + (lastReadThroughput >> 10) + " KB/s, " + "Asked Write: " + (lastWriteThroughput >> 10) + " KB/s, " + "Real Write: " + (realWriteThroughput >> 10) + " KB/s, " + "Current Read: " + (currentReadBytes.get() >> 10) + " KB, " + "Current asked Write: " + (currentWrittenBytes.get() >> 10) + " KB, " + "Current real Write: " + (realWrittenBytes.get() >> 10) + " KB";
  }
}
