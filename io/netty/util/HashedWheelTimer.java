package io.netty.util;

import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.util.Collections;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;






























































public class HashedWheelTimer
  implements Timer
{
  static final InternalLogger logger = InternalLoggerFactory.getInstance(HashedWheelTimer.class);
  
  private static final AtomicInteger INSTANCE_COUNTER = new AtomicInteger();
  private static final AtomicBoolean WARNED_TOO_MANY_INSTANCES = new AtomicBoolean();
  private static final int INSTANCE_COUNT_LIMIT = 64;
  private static final long MILLISECOND_NANOS = TimeUnit.MILLISECONDS.toNanos(1L);
  private static final ResourceLeakDetector<HashedWheelTimer> leakDetector = ResourceLeakDetectorFactory.instance()
    .newResourceLeakDetector(HashedWheelTimer.class, 1);
  

  private static final AtomicIntegerFieldUpdater<HashedWheelTimer> WORKER_STATE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(HashedWheelTimer.class, "workerState");
  
  private final ResourceLeakTracker<HashedWheelTimer> leak;
  private final Worker worker = new Worker(null);
  
  private final Thread workerThread;
  
  public static final int WORKER_STATE_INIT = 0;
  
  public static final int WORKER_STATE_STARTED = 1;
  public static final int WORKER_STATE_SHUTDOWN = 2;
  private volatile int workerState;
  private final long tickDuration;
  private final HashedWheelBucket[] wheel;
  private final int mask;
  private final CountDownLatch startTimeInitialized = new CountDownLatch(1);
  private final Queue<HashedWheelTimeout> timeouts = PlatformDependent.newMpscQueue();
  private final Queue<HashedWheelTimeout> cancelledTimeouts = PlatformDependent.newMpscQueue();
  private final AtomicLong pendingTimeouts = new AtomicLong(0L);
  

  private final long maxPendingTimeouts;
  

  private volatile long startTime;
  

  public HashedWheelTimer()
  {
    this(Executors.defaultThreadFactory());
  }
  









  public HashedWheelTimer(long tickDuration, TimeUnit unit)
  {
    this(Executors.defaultThreadFactory(), tickDuration, unit);
  }
  









  public HashedWheelTimer(long tickDuration, TimeUnit unit, int ticksPerWheel)
  {
    this(Executors.defaultThreadFactory(), tickDuration, unit, ticksPerWheel);
  }
  








  public HashedWheelTimer(ThreadFactory threadFactory)
  {
    this(threadFactory, 100L, TimeUnit.MILLISECONDS);
  }
  











  public HashedWheelTimer(ThreadFactory threadFactory, long tickDuration, TimeUnit unit)
  {
    this(threadFactory, tickDuration, unit, 512);
  }
  













  public HashedWheelTimer(ThreadFactory threadFactory, long tickDuration, TimeUnit unit, int ticksPerWheel)
  {
    this(threadFactory, tickDuration, unit, ticksPerWheel, true);
  }
  
















  public HashedWheelTimer(ThreadFactory threadFactory, long tickDuration, TimeUnit unit, int ticksPerWheel, boolean leakDetection)
  {
    this(threadFactory, tickDuration, unit, ticksPerWheel, leakDetection, -1L);
  }
  























  public HashedWheelTimer(ThreadFactory threadFactory, long tickDuration, TimeUnit unit, int ticksPerWheel, boolean leakDetection, long maxPendingTimeouts)
  {
    ObjectUtil.checkNotNull(threadFactory, "threadFactory");
    ObjectUtil.checkNotNull(unit, "unit");
    ObjectUtil.checkPositive(tickDuration, "tickDuration");
    ObjectUtil.checkPositive(ticksPerWheel, "ticksPerWheel");
    

    wheel = createWheel(ticksPerWheel);
    mask = (wheel.length - 1);
    

    long duration = unit.toNanos(tickDuration);
    

    if (duration >= Long.MAX_VALUE / wheel.length) {
      throw new IllegalArgumentException(String.format("tickDuration: %d (expected: 0 < tickDuration in nanos < %d", new Object[] {
      
        Long.valueOf(tickDuration), Long.valueOf(Long.MAX_VALUE / wheel.length) }));
    }
    
    if (duration < MILLISECOND_NANOS) {
      logger.warn("Configured tickDuration {} smaller then {}, using 1ms.", 
        Long.valueOf(tickDuration), Long.valueOf(MILLISECOND_NANOS));
      this.tickDuration = MILLISECOND_NANOS;
    } else {
      this.tickDuration = duration;
    }
    
    workerThread = threadFactory.newThread(worker);
    
    leak = ((leakDetection) || (!workerThread.isDaemon()) ? leakDetector.track(this) : null);
    
    this.maxPendingTimeouts = maxPendingTimeouts;
    
    if ((INSTANCE_COUNTER.incrementAndGet() > 64) && 
      (WARNED_TOO_MANY_INSTANCES.compareAndSet(false, true))) {
      reportTooManyInstances();
    }
  }
  
  protected void finalize() throws Throwable
  {
    try {
      super.finalize();
      


      if (WORKER_STATE_UPDATER.getAndSet(this, 2) != 2) {
        INSTANCE_COUNTER.decrementAndGet();
      }
    }
    finally
    {
      if (WORKER_STATE_UPDATER.getAndSet(this, 2) != 2) {
        INSTANCE_COUNTER.decrementAndGet();
      }
    }
  }
  
  private static HashedWheelBucket[] createWheel(int ticksPerWheel)
  {
    ObjectUtil.checkInRange(ticksPerWheel, 1, 1073741824, "ticksPerWheel");
    
    ticksPerWheel = normalizeTicksPerWheel(ticksPerWheel);
    HashedWheelBucket[] wheel = new HashedWheelBucket[ticksPerWheel];
    for (int i = 0; i < wheel.length; i++) {
      wheel[i] = new HashedWheelBucket(null);
    }
    return wheel;
  }
  
  private static int normalizeTicksPerWheel(int ticksPerWheel) {
    int normalizedTicksPerWheel = 1;
    while (normalizedTicksPerWheel < ticksPerWheel) {
      normalizedTicksPerWheel <<= 1;
    }
    return normalizedTicksPerWheel;
  }
  






  public void start()
  {
    switch (WORKER_STATE_UPDATER.get(this)) {
    case 0: 
      if (WORKER_STATE_UPDATER.compareAndSet(this, 0, 1)) {
        workerThread.start();
      }
      break;
    case 1: 
      break;
    case 2: 
      throw new IllegalStateException("cannot be started once stopped");
    default: 
      throw new Error("Invalid WorkerState");
    }
    
    
    while (startTime == 0L) {
      try {
        startTimeInitialized.await();
      }
      catch (InterruptedException localInterruptedException) {}
    }
  }
  

  public Set<Timeout> stop()
  {
    if (Thread.currentThread() == workerThread)
    {


      throw new IllegalStateException(HashedWheelTimer.class.getSimpleName() + ".stop() cannot be called from " + TimerTask.class.getSimpleName());
    }
    
    if (!WORKER_STATE_UPDATER.compareAndSet(this, 1, 2))
    {
      if (WORKER_STATE_UPDATER.getAndSet(this, 2) != 2) {
        INSTANCE_COUNTER.decrementAndGet();
        if (leak != null) {
          boolean closed = leak.close(this);
          assert (closed);
        }
      }
      
      return Collections.emptySet();
    }
    try
    {
      boolean interrupted = false;
      while (workerThread.isAlive()) {
        workerThread.interrupt();
        try {
          workerThread.join(100L);
        } catch (InterruptedException ignored) {
          interrupted = true;
        }
      }
      
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
      
      INSTANCE_COUNTER.decrementAndGet();
      if (leak != null) {
        boolean closed = leak.close(this);
        if ((!$assertionsDisabled) && (!closed)) throw new AssertionError();
      }
    }
    finally
    {
      INSTANCE_COUNTER.decrementAndGet();
      if (leak != null) {
        boolean closed = leak.close(this);
        if ((!$assertionsDisabled) && (!closed)) throw new AssertionError();
      }
    }
    return worker.unprocessedTimeouts();
  }
  
  public Timeout newTimeout(TimerTask task, long delay, TimeUnit unit)
  {
    ObjectUtil.checkNotNull(task, "task");
    ObjectUtil.checkNotNull(unit, "unit");
    
    long pendingTimeoutsCount = pendingTimeouts.incrementAndGet();
    
    if ((maxPendingTimeouts > 0L) && (pendingTimeoutsCount > maxPendingTimeouts)) {
      pendingTimeouts.decrementAndGet();
      throw new RejectedExecutionException("Number of pending timeouts (" + pendingTimeoutsCount + ") is greater than or equal to maximum allowed pending timeouts (" + maxPendingTimeouts + ")");
    }
    


    start();
    


    long deadline = System.nanoTime() + unit.toNanos(delay) - startTime;
    

    if ((delay > 0L) && (deadline < 0L)) {
      deadline = Long.MAX_VALUE;
    }
    HashedWheelTimeout timeout = new HashedWheelTimeout(this, task, deadline);
    timeouts.add(timeout);
    return timeout;
  }
  


  public long pendingTimeouts()
  {
    return pendingTimeouts.get();
  }
  
  private static void reportTooManyInstances() {
    if (logger.isErrorEnabled()) {
      String resourceType = StringUtil.simpleClassName(HashedWheelTimer.class);
      logger.error("You are creating too many " + resourceType + " instances. " + resourceType + " is a shared resource that must be reused across the JVM, so that only a few instances are created.");
    }
  }
  
  private final class Worker
    implements Runnable
  {
    private final Set<Timeout> unprocessedTimeouts = new HashSet();
    private long tick;
    
    private Worker() {}
    
    public void run()
    {
      startTime = System.nanoTime();
      if (startTime == 0L)
      {
        startTime = 1L;
      }
      

      startTimeInitialized.countDown();
      long deadline;
      int idx;
      do { deadline = waitForNextTick();
        if (deadline > 0L) {
          idx = (int)(tick & mask);
          processCancelledTasks();
          
          HashedWheelTimer.HashedWheelBucket bucket = wheel[idx];
          transferTimeoutsToBuckets();
          bucket.expireTimeouts(deadline);
          tick += 1L;
        }
      } while (HashedWheelTimer.WORKER_STATE_UPDATER.get(HashedWheelTimer.this) == 1);
      

      for (HashedWheelTimer.HashedWheelBucket bucket : wheel) {
        bucket.clearTimeouts(unprocessedTimeouts);
      }
      for (;;) {
        HashedWheelTimer.HashedWheelTimeout timeout = (HashedWheelTimer.HashedWheelTimeout)timeouts.poll();
        if (timeout == null) {
          break;
        }
        if (!timeout.isCancelled()) {
          unprocessedTimeouts.add(timeout);
        }
      }
      processCancelledTasks();
    }
    

    private void transferTimeoutsToBuckets()
    {
      for (int i = 0; i < 100000; i++) {
        HashedWheelTimer.HashedWheelTimeout timeout = (HashedWheelTimer.HashedWheelTimeout)timeouts.poll();
        if (timeout == null) {
          break;
        }
        
        if (timeout.state() != 1)
        {



          long calculated = deadline / tickDuration;
          remainingRounds = ((calculated - tick) / wheel.length);
          
          long ticks = Math.max(calculated, tick);
          int stopIndex = (int)(ticks & mask);
          
          HashedWheelTimer.HashedWheelBucket bucket = wheel[stopIndex];
          bucket.addTimeout(timeout);
        }
      }
    }
    
    private void processCancelledTasks() {
      for (;;) { HashedWheelTimer.HashedWheelTimeout timeout = (HashedWheelTimer.HashedWheelTimeout)cancelledTimeouts.poll();
        if (timeout == null) {
          break;
        }
        try
        {
          timeout.remove();
        } catch (Throwable t) {
          if (HashedWheelTimer.logger.isWarnEnabled()) {
            HashedWheelTimer.logger.warn("An exception was thrown while process a cancellation task", t);
          }
        }
      }
    }
    





    private long waitForNextTick()
    {
      long deadline = tickDuration * (tick + 1L);
      for (;;)
      {
        long currentTime = System.nanoTime() - startTime;
        long sleepTimeMs = (deadline - currentTime + 999999L) / 1000000L;
        
        if (sleepTimeMs <= 0L) {
          if (currentTime == Long.MIN_VALUE) {
            return -9223372036854775807L;
          }
          return currentTime;
        }
        






        if (PlatformDependent.isWindows()) {
          sleepTimeMs = sleepTimeMs / 10L * 10L;
          if (sleepTimeMs == 0L) {
            sleepTimeMs = 1L;
          }
        }
        try
        {
          Thread.sleep(sleepTimeMs);
        } catch (InterruptedException ignored) {
          if (HashedWheelTimer.WORKER_STATE_UPDATER.get(HashedWheelTimer.this) == 2) {
            return Long.MIN_VALUE;
          }
        }
      }
    }
    
    public Set<Timeout> unprocessedTimeouts() {
      return Collections.unmodifiableSet(unprocessedTimeouts);
    }
  }
  
  private static final class HashedWheelTimeout
    implements Timeout
  {
    private static final int ST_INIT = 0;
    private static final int ST_CANCELLED = 1;
    private static final int ST_EXPIRED = 2;
    private static final AtomicIntegerFieldUpdater<HashedWheelTimeout> STATE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(HashedWheelTimeout.class, "state");
    
    private final HashedWheelTimer timer;
    
    private final TimerTask task;
    private final long deadline;
    private volatile int state = 0;
    

    long remainingRounds;
    

    HashedWheelTimeout next;
    

    HashedWheelTimeout prev;
    
    HashedWheelTimer.HashedWheelBucket bucket;
    

    HashedWheelTimeout(HashedWheelTimer timer, TimerTask task, long deadline)
    {
      this.timer = timer;
      this.task = task;
      this.deadline = deadline;
    }
    
    public Timer timer()
    {
      return timer;
    }
    
    public TimerTask task()
    {
      return task;
    }
    

    public boolean cancel()
    {
      if (!compareAndSetState(0, 1)) {
        return false;
      }
      


      timer.cancelledTimeouts.add(this);
      return true;
    }
    
    void remove() {
      HashedWheelTimer.HashedWheelBucket bucket = this.bucket;
      if (bucket != null) {
        bucket.remove(this);
      } else {
        timer.pendingTimeouts.decrementAndGet();
      }
    }
    
    public boolean compareAndSetState(int expected, int state) {
      return STATE_UPDATER.compareAndSet(this, expected, state);
    }
    
    public int state() {
      return state;
    }
    
    public boolean isCancelled()
    {
      return state() == 1;
    }
    
    public boolean isExpired()
    {
      return state() == 2;
    }
    
    public void expire() {
      if (!compareAndSetState(0, 2)) {
        return;
      }
      try
      {
        task.run(this);
      } catch (Throwable t) {
        if (HashedWheelTimer.logger.isWarnEnabled()) {
          HashedWheelTimer.logger.warn("An exception was thrown by " + TimerTask.class.getSimpleName() + '.', t);
        }
      }
    }
    
    public String toString()
    {
      long currentTime = System.nanoTime();
      long remaining = deadline - currentTime + timer.startTime;
      



      StringBuilder buf = new StringBuilder(192).append(StringUtil.simpleClassName(this)).append('(').append("deadline: ");
      if (remaining > 0L)
      {
        buf.append(remaining).append(" ns later");
      } else if (remaining < 0L)
      {
        buf.append(-remaining).append(" ns ago");
      } else {
        buf.append("now");
      }
      
      if (isCancelled()) {
        buf.append(", cancelled");
      }
      
      return 
      
        ", task: " + task() + ')';
    }
  }
  


  private static final class HashedWheelBucket
  {
    private HashedWheelTimer.HashedWheelTimeout head;
    

    private HashedWheelTimer.HashedWheelTimeout tail;
    

    private HashedWheelBucket() {}
    

    public void addTimeout(HashedWheelTimer.HashedWheelTimeout timeout)
    {
      assert (bucket == null);
      bucket = this;
      if (head == null) {
        head = (this.tail = timeout);
      } else {
        tail.next = timeout;
        prev = tail;
        tail = timeout;
      }
    }
    


    public void expireTimeouts(long deadline)
    {
      HashedWheelTimer.HashedWheelTimeout timeout = head;
      

      while (timeout != null) {
        HashedWheelTimer.HashedWheelTimeout next = next;
        if (remainingRounds <= 0L) {
          next = remove(timeout);
          if (HashedWheelTimer.HashedWheelTimeout.access$800(timeout) <= deadline) {
            timeout.expire();
          }
          else {
            throw new IllegalStateException(String.format("timeout.deadline (%d) > deadline (%d)", new Object[] {
              Long.valueOf(HashedWheelTimer.HashedWheelTimeout.access$800(timeout)), Long.valueOf(deadline) }));
          }
        } else if (timeout.isCancelled()) {
          next = remove(timeout);
        } else {
          remainingRounds -= 1L;
        }
        timeout = next;
      }
    }
    
    public HashedWheelTimer.HashedWheelTimeout remove(HashedWheelTimer.HashedWheelTimeout timeout) {
      HashedWheelTimer.HashedWheelTimeout next = next;
      
      if (prev != null) {
        prev.next = next;
      }
      if (next != null) {
        next.prev = prev;
      }
      
      if (timeout == head)
      {
        if (timeout == tail) {
          tail = null;
          head = null;
        } else {
          head = next;
        }
      } else if (timeout == tail)
      {
        tail = prev;
      }
      
      prev = null;
      next = null;
      bucket = null;
      access$1200pendingTimeouts.decrementAndGet();
      return next;
    }
    

    public void clearTimeouts(Set<Timeout> set)
    {
      for (;;)
      {
        HashedWheelTimer.HashedWheelTimeout timeout = pollTimeout();
        if (timeout == null) {
          return;
        }
        if ((!timeout.isExpired()) && (!timeout.isCancelled()))
        {

          set.add(timeout); }
      }
    }
    
    private HashedWheelTimer.HashedWheelTimeout pollTimeout() {
      HashedWheelTimer.HashedWheelTimeout head = this.head;
      if (head == null) {
        return null;
      }
      HashedWheelTimer.HashedWheelTimeout next = next;
      if (next == null) {
        tail = (this.head = null);
      } else {
        this.head = next;
        prev = null;
      }
      

      next = null;
      prev = null;
      bucket = null;
      return head;
    }
  }
}
