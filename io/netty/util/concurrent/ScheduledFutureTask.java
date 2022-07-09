package io.netty.util.concurrent;

import io.netty.util.internal.DefaultPriorityQueue;
import io.netty.util.internal.PriorityQueue;
import io.netty.util.internal.PriorityQueueNode;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;















final class ScheduledFutureTask<V>
  extends PromiseTask<V>
  implements ScheduledFuture<V>, PriorityQueueNode
{
  private static final long START_TIME = System.nanoTime();
  private long id;
  
  static long nanoTime() { return System.nanoTime() - START_TIME; }
  
  static long deadlineNanos(long delay)
  {
    long deadlineNanos = nanoTime() + delay;
    
    return deadlineNanos < 0L ? Long.MAX_VALUE : deadlineNanos;
  }
  
  static long initialNanoTime() {
    return START_TIME;
  }
  


  private long deadlineNanos;
  

  private final long periodNanos;
  
  private int queueIndex = -1;
  

  ScheduledFutureTask(AbstractScheduledEventExecutor executor, Runnable runnable, long nanoTime)
  {
    super(executor, runnable);
    deadlineNanos = nanoTime;
    periodNanos = 0L;
  }
  

  ScheduledFutureTask(AbstractScheduledEventExecutor executor, Runnable runnable, long nanoTime, long period)
  {
    super(executor, runnable);
    deadlineNanos = nanoTime;
    periodNanos = validatePeriod(period);
  }
  

  ScheduledFutureTask(AbstractScheduledEventExecutor executor, Callable<V> callable, long nanoTime, long period)
  {
    super(executor, callable);
    deadlineNanos = nanoTime;
    periodNanos = validatePeriod(period);
  }
  

  ScheduledFutureTask(AbstractScheduledEventExecutor executor, Callable<V> callable, long nanoTime)
  {
    super(executor, callable);
    deadlineNanos = nanoTime;
    periodNanos = 0L;
  }
  
  private static long validatePeriod(long period) {
    if (period == 0L) {
      throw new IllegalArgumentException("period: 0 (expected: != 0)");
    }
    return period;
  }
  
  ScheduledFutureTask<V> setId(long id) {
    if (this.id == 0L) {
      this.id = id;
    }
    return this;
  }
  
  protected EventExecutor executor()
  {
    return super.executor();
  }
  
  public long deadlineNanos() {
    return deadlineNanos;
  }
  

  void setConsumed()
  {
    if (periodNanos == 0L) {
      assert (nanoTime() >= deadlineNanos);
      deadlineNanos = 0L;
    }
  }
  
  public long delayNanos() {
    return deadlineToDelayNanos(deadlineNanos());
  }
  
  static long deadlineToDelayNanos(long deadlineNanos) {
    return deadlineNanos == 0L ? 0L : Math.max(0L, deadlineNanos - nanoTime());
  }
  
  public long delayNanos(long currentTimeNanos) {
    return deadlineNanos == 0L ? 0L : 
      Math.max(0L, deadlineNanos() - (currentTimeNanos - START_TIME));
  }
  
  public long getDelay(TimeUnit unit)
  {
    return unit.convert(delayNanos(), TimeUnit.NANOSECONDS);
  }
  
  public int compareTo(Delayed o)
  {
    if (this == o) {
      return 0;
    }
    
    ScheduledFutureTask<?> that = (ScheduledFutureTask)o;
    long d = deadlineNanos() - that.deadlineNanos();
    if (d < 0L)
      return -1;
    if (d > 0L)
      return 1;
    if (id < id) {
      return -1;
    }
    assert (id != id);
    return 1;
  }
  

  public void run()
  {
    assert (executor().inEventLoop());
    try {
      if (delayNanos() > 0L)
      {
        if (isCancelled()) {
          scheduledExecutor().scheduledTaskQueue().removeTyped(this);
        } else {
          scheduledExecutor().scheduleFromEventLoop(this);
        }
        return;
      }
      if (periodNanos == 0L) {
        if (setUncancellableInternal()) {
          V result = runTask();
          setSuccessInternal(result);
        }
        
      }
      else if (!isCancelled()) {
        runTask();
        if (!executor().isShutdown()) {
          if (periodNanos > 0L) {
            deadlineNanos += periodNanos;
          } else {
            deadlineNanos = (nanoTime() - periodNanos);
          }
          if (!isCancelled()) {
            scheduledExecutor().scheduledTaskQueue().add(this);
          }
        }
      }
    }
    catch (Throwable cause) {
      setFailureInternal(cause);
    }
  }
  
  private AbstractScheduledEventExecutor scheduledExecutor() {
    return (AbstractScheduledEventExecutor)executor();
  }
  





  public boolean cancel(boolean mayInterruptIfRunning)
  {
    boolean canceled = super.cancel(mayInterruptIfRunning);
    if (canceled) {
      scheduledExecutor().removeScheduled(this);
    }
    return canceled;
  }
  
  boolean cancelWithoutRemove(boolean mayInterruptIfRunning) {
    return super.cancel(mayInterruptIfRunning);
  }
  
  protected StringBuilder toStringBuilder()
  {
    StringBuilder buf = super.toStringBuilder();
    buf.setCharAt(buf.length() - 1, ',');
    
    return buf.append(" deadline: ")
      .append(deadlineNanos)
      .append(", period: ")
      .append(periodNanos)
      .append(')');
  }
  
  public int priorityQueueIndex(DefaultPriorityQueue<?> queue)
  {
    return queueIndex;
  }
  
  public void priorityQueueIndex(DefaultPriorityQueue<?> queue, int i)
  {
    queueIndex = i;
  }
}
