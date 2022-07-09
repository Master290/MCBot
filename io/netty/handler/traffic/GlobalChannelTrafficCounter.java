package io.netty.handler.traffic;

import io.netty.util.internal.ObjectUtil;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


























public class GlobalChannelTrafficCounter
  extends TrafficCounter
{
  public GlobalChannelTrafficCounter(GlobalChannelTrafficShapingHandler trafficShapingHandler, ScheduledExecutorService executor, String name, long checkInterval)
  {
    super(trafficShapingHandler, executor, name, checkInterval);
    ObjectUtil.checkNotNullWithIAE(executor, "executor");
  }
  





  private static class MixedTrafficMonitoringTask
    implements Runnable
  {
    private final GlobalChannelTrafficShapingHandler trafficShapingHandler1;
    




    private final TrafficCounter counter;
    




    MixedTrafficMonitoringTask(GlobalChannelTrafficShapingHandler trafficShapingHandler, TrafficCounter counter)
    {
      trafficShapingHandler1 = trafficShapingHandler;
      this.counter = counter;
    }
    
    public void run()
    {
      if (!counter.monitorActive) {
        return;
      }
      long newLastTime = TrafficCounter.milliSecondFromNano();
      counter.resetAccounting(newLastTime);
      for (GlobalChannelTrafficShapingHandler.PerChannel perChannel : trafficShapingHandler1.channelQueues.values()) {
        channelTrafficCounter.resetAccounting(newLastTime);
      }
      trafficShapingHandler1.doAccounting(counter);
    }
  }
  



  public synchronized void start()
  {
    if (monitorActive) {
      return;
    }
    lastTime.set(milliSecondFromNano());
    long localCheckInterval = checkInterval.get();
    if (localCheckInterval > 0L) {
      monitorActive = true;
      monitor = new MixedTrafficMonitoringTask((GlobalChannelTrafficShapingHandler)trafficShapingHandler, this);
      
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
    trafficShapingHandler.doAccounting(this);
    if (scheduledFuture != null) {
      scheduledFuture.cancel(true);
    }
  }
  

  public void resetCumulativeTime()
  {
    for (GlobalChannelTrafficShapingHandler.PerChannel perChannel : trafficShapingHandler).channelQueues.values()) {
      channelTrafficCounter.resetCumulativeTime();
    }
    super.resetCumulativeTime();
  }
}
