package io.netty.util.concurrent;

import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PriorityQueue;
import io.netty.util.internal.ThreadExecutorMap;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;




















public final class GlobalEventExecutor
  extends AbstractScheduledEventExecutor
  implements OrderedEventExecutor
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(GlobalEventExecutor.class);
  
  private static final long SCHEDULE_QUIET_PERIOD_INTERVAL = TimeUnit.SECONDS.toNanos(1L);
  
  public static final GlobalEventExecutor INSTANCE = new GlobalEventExecutor();
  
  final BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue();
  final ScheduledFutureTask<Void> quietPeriodTask = new ScheduledFutureTask(this, 
    Executors.callable(new Runnable() { public void run() {}
  }, null), 
  



    ScheduledFutureTask.deadlineNanos(SCHEDULE_QUIET_PERIOD_INTERVAL), -SCHEDULE_QUIET_PERIOD_INTERVAL);
  


  final ThreadFactory threadFactory;
  

  private final TaskRunner taskRunner = new TaskRunner();
  private final AtomicBoolean started = new AtomicBoolean();
  
  volatile Thread thread;
  private final Future<?> terminationFuture = new FailedFuture(this, new UnsupportedOperationException());
  
  private GlobalEventExecutor() {
    scheduledTaskQueue().add(quietPeriodTask);
    threadFactory = ThreadExecutorMap.apply(new DefaultThreadFactory(
      DefaultThreadFactory.toPoolName(getClass()), false, 5, null), this);
  }
  




  Runnable takeTask()
  {
    BlockingQueue<Runnable> taskQueue = this.taskQueue;
    for (;;) {
      ScheduledFutureTask<?> scheduledTask = peekScheduledTask();
      if (scheduledTask == null) {
        Runnable task = null;
        try {
          task = (Runnable)taskQueue.take();
        }
        catch (InterruptedException localInterruptedException1) {}
        
        return task;
      }
      long delayNanos = scheduledTask.delayNanos();
      Runnable task = null;
      if (delayNanos > 0L) {
        try {
          task = (Runnable)taskQueue.poll(delayNanos, TimeUnit.NANOSECONDS);
        }
        catch (InterruptedException e) {
          return null;
        }
      }
      if (task == null)
      {



        fetchFromScheduledTaskQueue();
        task = (Runnable)taskQueue.poll();
      }
      
      if (task != null) {
        return task;
      }
    }
  }
  
  private void fetchFromScheduledTaskQueue()
  {
    long nanoTime = AbstractScheduledEventExecutor.nanoTime();
    Runnable scheduledTask = pollScheduledTask(nanoTime);
    while (scheduledTask != null) {
      taskQueue.add(scheduledTask);
      scheduledTask = pollScheduledTask(nanoTime);
    }
  }
  


  public int pendingTasks()
  {
    return taskQueue.size();
  }
  



  private void addTask(Runnable task)
  {
    taskQueue.add(ObjectUtil.checkNotNull(task, "task"));
  }
  
  public boolean inEventLoop(Thread thread)
  {
    return thread == this.thread;
  }
  
  public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit)
  {
    return terminationFuture();
  }
  
  public Future<?> terminationFuture()
  {
    return terminationFuture;
  }
  
  @Deprecated
  public void shutdown()
  {
    throw new UnsupportedOperationException();
  }
  
  public boolean isShuttingDown()
  {
    return false;
  }
  
  public boolean isShutdown()
  {
    return false;
  }
  
  public boolean isTerminated()
  {
    return false;
  }
  
  public boolean awaitTermination(long timeout, TimeUnit unit)
  {
    return false;
  }
  






  public boolean awaitInactivity(long timeout, TimeUnit unit)
    throws InterruptedException
  {
    ObjectUtil.checkNotNull(unit, "unit");
    
    Thread thread = this.thread;
    if (thread == null) {
      throw new IllegalStateException("thread was not started");
    }
    thread.join(unit.toMillis(timeout));
    return !thread.isAlive();
  }
  
  public void execute(Runnable task)
  {
    addTask((Runnable)ObjectUtil.checkNotNull(task, "task"));
    if (!inEventLoop()) {
      startThread();
    }
  }
  
  private void startThread() {
    if (started.compareAndSet(false, true)) {
      final Thread t = threadFactory.newThread(taskRunner);
      




      AccessController.doPrivileged(new PrivilegedAction()
      {
        public Void run() {
          t.setContextClassLoader(null);
          return null;

        }
        


      });
      thread = t;
      t.start();
    }
  }
  
  final class TaskRunner implements Runnable {
    TaskRunner() {}
    
    public void run() {
      for (;;) { Runnable task = takeTask();
        if (task != null) {
          try {
            task.run();
          } catch (Throwable t) {
            GlobalEventExecutor.logger.warn("Unexpected exception from the global event executor: ", t);
          }
          
          if (task != quietPeriodTask) {}

        }
        else
        {
          Queue<ScheduledFutureTask<?>> scheduledTaskQueue = GlobalEventExecutor.this.scheduledTaskQueue;
          
          if ((taskQueue.isEmpty()) && ((scheduledTaskQueue == null) || (scheduledTaskQueue.size() == 1)))
          {


            boolean stopped = started.compareAndSet(true, false);
            assert (stopped);
            



            if (taskQueue.isEmpty()) {
              break;
            }
            





            if (!started.compareAndSet(false, true)) {
              break;
            }
          }
        }
      }
    }
  }
}
