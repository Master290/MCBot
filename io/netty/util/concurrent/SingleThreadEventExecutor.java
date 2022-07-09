package io.netty.util.concurrent;

import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.PriorityQueue;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.ThreadExecutorMap;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;




















public abstract class SingleThreadEventExecutor
  extends AbstractScheduledEventExecutor
  implements OrderedEventExecutor
{
  static final int DEFAULT_MAX_PENDING_EXECUTOR_TASKS = Math.max(16, 
    SystemPropertyUtil.getInt("io.netty.eventexecutor.maxPendingTasks", Integer.MAX_VALUE));
  

  private static final InternalLogger logger = InternalLoggerFactory.getInstance(SingleThreadEventExecutor.class);
  
  private static final int ST_NOT_STARTED = 1;
  
  private static final int ST_STARTED = 2;
  private static final int ST_SHUTTING_DOWN = 3;
  private static final int ST_SHUTDOWN = 4;
  private static final int ST_TERMINATED = 5;
  private static final Runnable NOOP_TASK = new Runnable()
  {
    public void run() {}
  };
  



  private static final AtomicIntegerFieldUpdater<SingleThreadEventExecutor> STATE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(SingleThreadEventExecutor.class, "state");
  
  private static final AtomicReferenceFieldUpdater<SingleThreadEventExecutor, ThreadProperties> PROPERTIES_UPDATER = AtomicReferenceFieldUpdater.newUpdater(SingleThreadEventExecutor.class, ThreadProperties.class, "threadProperties");
  
  private final Queue<Runnable> taskQueue;
  
  private volatile Thread thread;
  
  private volatile ThreadProperties threadProperties;
  
  private final Executor executor;
  
  private volatile boolean interrupted;
  private final CountDownLatch threadLock = new CountDownLatch(1);
  private final Set<Runnable> shutdownHooks = new LinkedHashSet();
  
  private final boolean addTaskWakesUp;
  
  private final int maxPendingTasks;
  private final RejectedExecutionHandler rejectedExecutionHandler;
  private long lastExecutionTime;
  private volatile int state = 1;
  
  private volatile long gracefulShutdownQuietPeriod;
  
  private volatile long gracefulShutdownTimeout;
  
  private long gracefulShutdownStartTime;
  private final Promise<?> terminationFuture = new DefaultPromise(GlobalEventExecutor.INSTANCE);
  








  protected SingleThreadEventExecutor(EventExecutorGroup parent, ThreadFactory threadFactory, boolean addTaskWakesUp)
  {
    this(parent, new ThreadPerTaskExecutor(threadFactory), addTaskWakesUp);
  }
  











  protected SingleThreadEventExecutor(EventExecutorGroup parent, ThreadFactory threadFactory, boolean addTaskWakesUp, int maxPendingTasks, RejectedExecutionHandler rejectedHandler)
  {
    this(parent, new ThreadPerTaskExecutor(threadFactory), addTaskWakesUp, maxPendingTasks, rejectedHandler);
  }
  







  protected SingleThreadEventExecutor(EventExecutorGroup parent, Executor executor, boolean addTaskWakesUp)
  {
    this(parent, executor, addTaskWakesUp, DEFAULT_MAX_PENDING_EXECUTOR_TASKS, RejectedExecutionHandlers.reject());
  }
  











  protected SingleThreadEventExecutor(EventExecutorGroup parent, Executor executor, boolean addTaskWakesUp, int maxPendingTasks, RejectedExecutionHandler rejectedHandler)
  {
    super(parent);
    this.addTaskWakesUp = addTaskWakesUp;
    this.maxPendingTasks = Math.max(16, maxPendingTasks);
    this.executor = ThreadExecutorMap.apply(executor, this);
    taskQueue = newTaskQueue(this.maxPendingTasks);
    rejectedExecutionHandler = ((RejectedExecutionHandler)ObjectUtil.checkNotNull(rejectedHandler, "rejectedHandler"));
  }
  

  protected SingleThreadEventExecutor(EventExecutorGroup parent, Executor executor, boolean addTaskWakesUp, Queue<Runnable> taskQueue, RejectedExecutionHandler rejectedHandler)
  {
    super(parent);
    this.addTaskWakesUp = addTaskWakesUp;
    maxPendingTasks = DEFAULT_MAX_PENDING_EXECUTOR_TASKS;
    this.executor = ThreadExecutorMap.apply(executor, this);
    this.taskQueue = ((Queue)ObjectUtil.checkNotNull(taskQueue, "taskQueue"));
    rejectedExecutionHandler = ((RejectedExecutionHandler)ObjectUtil.checkNotNull(rejectedHandler, "rejectedHandler"));
  }
  


  @Deprecated
  protected Queue<Runnable> newTaskQueue()
  {
    return newTaskQueue(maxPendingTasks);
  }
  





  protected Queue<Runnable> newTaskQueue(int maxPendingTasks)
  {
    return new LinkedBlockingQueue(maxPendingTasks);
  }
  


  protected void interruptThread()
  {
    Thread currentThread = thread;
    if (currentThread == null) {
      interrupted = true;
    } else {
      currentThread.interrupt();
    }
  }
  


  protected Runnable pollTask()
  {
    assert (inEventLoop());
    return pollTaskFrom(taskQueue);
  }
  
  protected static Runnable pollTaskFrom(Queue<Runnable> taskQueue) {
    for (;;) {
      Runnable task = (Runnable)taskQueue.poll();
      if (task != WAKEUP_TASK) {
        return task;
      }
    }
  }
  








  protected Runnable takeTask()
  {
    assert (inEventLoop());
    if (!(this.taskQueue instanceof BlockingQueue)) {
      throw new UnsupportedOperationException();
    }
    
    BlockingQueue<Runnable> taskQueue = (BlockingQueue)this.taskQueue;
    for (;;) {
      ScheduledFutureTask<?> scheduledTask = peekScheduledTask();
      if (scheduledTask == null) {
        Runnable task = null;
        try {
          task = (Runnable)taskQueue.take();
          if (task == WAKEUP_TASK) {
            task = null;
          }
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
  
  private boolean fetchFromScheduledTaskQueue()
  {
    if ((scheduledTaskQueue == null) || (scheduledTaskQueue.isEmpty())) {
      return true;
    }
    long nanoTime = AbstractScheduledEventExecutor.nanoTime();
    for (;;) {
      Runnable scheduledTask = pollScheduledTask(nanoTime);
      if (scheduledTask == null) {
        return true;
      }
      if (!taskQueue.offer(scheduledTask))
      {
        scheduledTaskQueue.add((ScheduledFutureTask)scheduledTask);
        return false;
      }
    }
  }
  


  private boolean executeExpiredScheduledTasks()
  {
    if ((scheduledTaskQueue == null) || (scheduledTaskQueue.isEmpty())) {
      return false;
    }
    long nanoTime = AbstractScheduledEventExecutor.nanoTime();
    Runnable scheduledTask = pollScheduledTask(nanoTime);
    if (scheduledTask == null) {
      return false;
    }
    do {
      safeExecute(scheduledTask);
    } while ((scheduledTask = pollScheduledTask(nanoTime)) != null);
    return true;
  }
  


  protected Runnable peekTask()
  {
    assert (inEventLoop());
    return (Runnable)taskQueue.peek();
  }
  


  protected boolean hasTasks()
  {
    assert (inEventLoop());
    return !taskQueue.isEmpty();
  }
  


  public int pendingTasks()
  {
    return taskQueue.size();
  }
  



  protected void addTask(Runnable task)
  {
    ObjectUtil.checkNotNull(task, "task");
    if (!offerTask(task)) {
      reject(task);
    }
  }
  
  final boolean offerTask(Runnable task) {
    if (isShutdown()) {
      reject();
    }
    return taskQueue.offer(task);
  }
  


  protected boolean removeTask(Runnable task)
  {
    return taskQueue.remove(ObjectUtil.checkNotNull(task, "task"));
  }
  




  protected boolean runAllTasks()
  {
    assert (inEventLoop());
    
    boolean ranAtLeastOne = false;
    boolean fetchedAll;
    do {
      fetchedAll = fetchFromScheduledTaskQueue();
      if (runAllTasksFrom(taskQueue)) {
        ranAtLeastOne = true;
      }
    } while (!fetchedAll);
    
    if (ranAtLeastOne) {
      lastExecutionTime = ScheduledFutureTask.nanoTime();
    }
    afterRunningAllTasks();
    return ranAtLeastOne;
  }
  







  protected final boolean runScheduledAndExecutorTasks(int maxDrainAttempts)
  {
    assert (inEventLoop());
    
    int drainAttempt = 0;
    
    do
    {
      boolean ranAtLeastOneTask = runExistingTasksFrom(taskQueue) | executeExpiredScheduledTasks();
      if (!ranAtLeastOneTask) break; drainAttempt++; } while (drainAttempt < maxDrainAttempts);
    
    if (drainAttempt > 0) {
      lastExecutionTime = ScheduledFutureTask.nanoTime();
    }
    afterRunningAllTasks();
    
    return drainAttempt > 0;
  }
  






  protected final boolean runAllTasksFrom(Queue<Runnable> taskQueue)
  {
    Runnable task = pollTaskFrom(taskQueue);
    if (task == null) {
      return false;
    }
    do {
      safeExecute(task);
      task = pollTaskFrom(taskQueue);
    } while (task != null);
    return true;
  }
  






  private boolean runExistingTasksFrom(Queue<Runnable> taskQueue)
  {
    Runnable task = pollTaskFrom(taskQueue);
    if (task == null) {
      return false;
    }
    int remaining = Math.min(maxPendingTasks, taskQueue.size());
    safeExecute(task);
    

    while ((remaining-- > 0) && ((task = (Runnable)taskQueue.poll()) != null)) {
      safeExecute(task);
    }
    return true;
  }
  



  protected boolean runAllTasks(long timeoutNanos)
  {
    fetchFromScheduledTaskQueue();
    Runnable task = pollTask();
    if (task == null) {
      afterRunningAllTasks();
      return false;
    }
    
    long deadline = timeoutNanos > 0L ? ScheduledFutureTask.nanoTime() + timeoutNanos : 0L;
    long runTasks = 0L;
    do
    {
      safeExecute(task);
      
      runTasks += 1L;
      


      if ((runTasks & 0x3F) == 0L) {
        long lastExecutionTime = ScheduledFutureTask.nanoTime();
        if (lastExecutionTime >= deadline) {
          break;
        }
      }
      
      task = pollTask();
    } while (task != null);
    long lastExecutionTime = ScheduledFutureTask.nanoTime();
    



    afterRunningAllTasks();
    this.lastExecutionTime = lastExecutionTime;
    return true;
  }
  



  protected void afterRunningAllTasks() {}
  



  protected long delayNanos(long currentTimeNanos)
  {
    ScheduledFutureTask<?> scheduledTask = peekScheduledTask();
    if (scheduledTask == null) {
      return SCHEDULE_PURGE_INTERVAL;
    }
    
    return scheduledTask.delayNanos(currentTimeNanos);
  }
  




  protected long deadlineNanos()
  {
    ScheduledFutureTask<?> scheduledTask = peekScheduledTask();
    if (scheduledTask == null) {
      return nanoTime() + SCHEDULE_PURGE_INTERVAL;
    }
    return scheduledTask.deadlineNanos();
  }
  






  protected void updateLastExecutionTime()
  {
    lastExecutionTime = ScheduledFutureTask.nanoTime();
  }
  



  protected abstract void run();
  


  protected void cleanup() {}
  


  protected void wakeup(boolean inEventLoop)
  {
    if (!inEventLoop)
    {

      taskQueue.offer(WAKEUP_TASK);
    }
  }
  
  public boolean inEventLoop(Thread thread)
  {
    return thread == this.thread;
  }
  


  public void addShutdownHook(final Runnable task)
  {
    if (inEventLoop()) {
      shutdownHooks.add(task);
    } else {
      execute(new Runnable()
      {
        public void run() {
          shutdownHooks.add(task);
        }
      });
    }
  }
  


  public void removeShutdownHook(final Runnable task)
  {
    if (inEventLoop()) {
      shutdownHooks.remove(task);
    } else {
      execute(new Runnable()
      {
        public void run() {
          shutdownHooks.remove(task);
        }
      });
    }
  }
  
  private boolean runShutdownHooks() {
    boolean ran = false;
    
    while (!shutdownHooks.isEmpty()) {
      List<Runnable> copy = new ArrayList(shutdownHooks);
      shutdownHooks.clear();
      for (Runnable task : copy) {
        try {
          task.run();
        } catch (Throwable t) {
          logger.warn("Shutdown hook raised an exception.", t);
        } finally {
          ran = true;
        }
      }
    }
    
    if (ran) {
      lastExecutionTime = ScheduledFutureTask.nanoTime();
    }
    
    return ran;
  }
  
  public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit)
  {
    ObjectUtil.checkPositiveOrZero(quietPeriod, "quietPeriod");
    if (timeout < quietPeriod) {
      throw new IllegalArgumentException("timeout: " + timeout + " (expected >= quietPeriod (" + quietPeriod + "))");
    }
    
    ObjectUtil.checkNotNull(unit, "unit");
    
    if (isShuttingDown()) {
      return terminationFuture();
    }
    
    boolean inEventLoop = inEventLoop();
    boolean wakeup;
    int oldState;
    for (;;) {
      if (isShuttingDown()) {
        return terminationFuture();
      }
      
      wakeup = true;
      oldState = state;
      int newState; int newState; if (inEventLoop) {
        newState = 3;
      } else { int newState;
        switch (oldState) {
        case 1: 
        case 2: 
          newState = 3;
          break;
        default: 
          newState = oldState;
          wakeup = false;
        }
      }
      if (STATE_UPDATER.compareAndSet(this, oldState, newState)) {
        break;
      }
    }
    gracefulShutdownQuietPeriod = unit.toNanos(quietPeriod);
    gracefulShutdownTimeout = unit.toNanos(timeout);
    
    if (ensureThreadStarted(oldState)) {
      return terminationFuture;
    }
    
    if (wakeup) {
      taskQueue.offer(WAKEUP_TASK);
      if (!addTaskWakesUp) {
        wakeup(inEventLoop);
      }
    }
    
    return terminationFuture();
  }
  
  public Future<?> terminationFuture()
  {
    return terminationFuture;
  }
  
  @Deprecated
  public void shutdown()
  {
    if (isShutdown()) {
      return;
    }
    
    boolean inEventLoop = inEventLoop();
    boolean wakeup;
    int oldState;
    for (;;) {
      if (isShuttingDown()) {
        return;
      }
      
      wakeup = true;
      oldState = state;
      int newState; int newState; if (inEventLoop) {
        newState = 4;
      } else { int newState;
        switch (oldState) {
        case 1: 
        case 2: 
        case 3: 
          newState = 4;
          break;
        default: 
          newState = oldState;
          wakeup = false;
        }
      }
      if (STATE_UPDATER.compareAndSet(this, oldState, newState)) {
        break;
      }
    }
    
    if (ensureThreadStarted(oldState)) {
      return;
    }
    
    if (wakeup) {
      taskQueue.offer(WAKEUP_TASK);
      if (!addTaskWakesUp) {
        wakeup(inEventLoop);
      }
    }
  }
  
  public boolean isShuttingDown()
  {
    return state >= 3;
  }
  
  public boolean isShutdown()
  {
    return state >= 4;
  }
  
  public boolean isTerminated()
  {
    return state == 5;
  }
  


  protected boolean confirmShutdown()
  {
    if (!isShuttingDown()) {
      return false;
    }
    
    if (!inEventLoop()) {
      throw new IllegalStateException("must be invoked from an event loop");
    }
    
    cancelScheduledTasks();
    
    if (gracefulShutdownStartTime == 0L) {
      gracefulShutdownStartTime = ScheduledFutureTask.nanoTime();
    }
    
    if ((runAllTasks()) || (runShutdownHooks())) {
      if (isShutdown())
      {
        return true;
      }
      



      if (gracefulShutdownQuietPeriod == 0L) {
        return true;
      }
      taskQueue.offer(WAKEUP_TASK);
      return false;
    }
    
    long nanoTime = ScheduledFutureTask.nanoTime();
    
    if ((isShutdown()) || (nanoTime - gracefulShutdownStartTime > gracefulShutdownTimeout)) {
      return true;
    }
    
    if (nanoTime - lastExecutionTime <= gracefulShutdownQuietPeriod)
    {

      taskQueue.offer(WAKEUP_TASK);
      try {
        Thread.sleep(100L);
      }
      catch (InterruptedException localInterruptedException) {}
      

      return false;
    }
    


    return true;
  }
  
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException
  {
    ObjectUtil.checkNotNull(unit, "unit");
    if (inEventLoop()) {
      throw new IllegalStateException("cannot await termination of the current thread");
    }
    
    threadLock.await(timeout, unit);
    
    return isTerminated();
  }
  
  public void execute(Runnable task)
  {
    ObjectUtil.checkNotNull(task, "task");
    execute(task, (!(task instanceof AbstractEventExecutor.LazyRunnable)) && (wakesUpForTask(task)));
  }
  
  public void lazyExecute(Runnable task)
  {
    execute((Runnable)ObjectUtil.checkNotNull(task, "task"), false);
  }
  
  private void execute(Runnable task, boolean immediate) {
    boolean inEventLoop = inEventLoop();
    addTask(task);
    if (!inEventLoop) {
      startThread();
      if (isShutdown()) {
        boolean reject = false;
        try {
          if (removeTask(task)) {
            reject = true;
          }
        }
        catch (UnsupportedOperationException localUnsupportedOperationException) {}
        


        if (reject) {
          reject();
        }
      }
    }
    
    if ((!addTaskWakesUp) && (immediate)) {
      wakeup(inEventLoop);
    }
  }
  
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException
  {
    throwIfInEventLoop("invokeAny");
    return super.invokeAny(tasks);
  }
  
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
    throws InterruptedException, ExecutionException, TimeoutException
  {
    throwIfInEventLoop("invokeAny");
    return super.invokeAny(tasks, timeout, unit);
  }
  
  public <T> List<java.util.concurrent.Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
    throws InterruptedException
  {
    throwIfInEventLoop("invokeAll");
    return super.invokeAll(tasks);
  }
  
  public <T> List<java.util.concurrent.Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
    throws InterruptedException
  {
    throwIfInEventLoop("invokeAll");
    return super.invokeAll(tasks, timeout, unit);
  }
  
  private void throwIfInEventLoop(String method) {
    if (inEventLoop()) {
      throw new RejectedExecutionException("Calling " + method + " from within the EventLoop is not allowed");
    }
  }
  




  public final ThreadProperties threadProperties()
  {
    ThreadProperties threadProperties = this.threadProperties;
    if (threadProperties == null) {
      Thread thread = this.thread;
      if (thread == null) {
        assert (!inEventLoop());
        submit(NOOP_TASK).syncUninterruptibly();
        thread = this.thread;
        assert (thread != null);
      }
      
      threadProperties = new DefaultThreadProperties(thread);
      if (!PROPERTIES_UPDATER.compareAndSet(this, null, threadProperties)) {
        threadProperties = this.threadProperties;
      }
    }
    
    return threadProperties;
  }
  









  protected boolean wakesUpForTask(Runnable task)
  {
    return true;
  }
  
  protected static void reject() {
    throw new RejectedExecutionException("event executor terminated");
  }
  




  protected final void reject(Runnable task)
  {
    rejectedExecutionHandler.rejected(task, this);
  }
  


  private static final long SCHEDULE_PURGE_INTERVAL = TimeUnit.SECONDS.toNanos(1L);
  
  private void startThread() {
    if ((state == 1) && 
      (STATE_UPDATER.compareAndSet(this, 1, 2))) {
      boolean success = false;
      try {
        doStartThread();
        success = true;
        
        if (!success) {
          STATE_UPDATER.compareAndSet(this, 2, 1);
        }
      }
      finally
      {
        if (!success) {
          STATE_UPDATER.compareAndSet(this, 2, 1);
        }
      }
    }
  }
  
  private boolean ensureThreadStarted(int oldState)
  {
    if (oldState == 1) {
      try {
        doStartThread();
      } catch (Throwable cause) {
        STATE_UPDATER.set(this, 5);
        terminationFuture.tryFailure(cause);
        
        if (!(cause instanceof Exception))
        {
          PlatformDependent.throwException(cause);
        }
        return true;
      }
    }
    return false;
  }
  
  private void doStartThread() {
    assert (thread == null);
    executor.execute(new Runnable()
    {
      public void run() {
        thread = Thread.currentThread();
        if (interrupted) {
          thread.interrupt();
        }
        
        boolean success = false;
        updateLastExecutionTime();
        try {
          SingleThreadEventExecutor.this.run();
          success = true;
          

          for (;;)
          {
            int oldState = state;
            if ((oldState >= 3) || (SingleThreadEventExecutor.STATE_UPDATER.compareAndSet(SingleThreadEventExecutor.this, oldState, 3))) {
              break;
            }
          }
          


          if ((success) && (gracefulShutdownStartTime == 0L) && 
            (SingleThreadEventExecutor.logger.isErrorEnabled())) {
            SingleThreadEventExecutor.logger.error("Buggy " + EventExecutor.class.getSimpleName() + " implementation; " + SingleThreadEventExecutor.class
              .getSimpleName() + ".confirmShutdown() must be called before run() implementation terminates.");
          }
          



          try
          {
            for (;;)
            {
              if (confirmShutdown()) {
                break;
              }
            }
            

            for (;;)
            {
              int oldState = state;
              if ((oldState >= 4) || (SingleThreadEventExecutor.STATE_UPDATER.compareAndSet(SingleThreadEventExecutor.this, oldState, 4))) {
                break;
              }
            }
            



            confirmShutdown();
          } finally {
            try {
              cleanup();
              




              FastThreadLocal.removeAll();
              
              SingleThreadEventExecutor.STATE_UPDATER.set(SingleThreadEventExecutor.this, 5);
              threadLock.countDown();
              int numUserTasks = drainTasks();
              if ((numUserTasks > 0) && (SingleThreadEventExecutor.logger.isWarnEnabled())) {
                SingleThreadEventExecutor.logger.warn("An event executor terminated with non-empty task queue (" + numUserTasks + ')');
              }
              
              terminationFuture.setSuccess(null);
            }
            finally
            {
              FastThreadLocal.removeAll();
              
              SingleThreadEventExecutor.STATE_UPDATER.set(SingleThreadEventExecutor.this, 5);
              threadLock.countDown();
              int numUserTasks = drainTasks();
              if ((numUserTasks > 0) && (SingleThreadEventExecutor.logger.isWarnEnabled())) {
                SingleThreadEventExecutor.logger.warn("An event executor terminated with non-empty task queue (" + numUserTasks + ')');
              }
              
              terminationFuture.setSuccess(null);
            }
            try
            {
              cleanup();
            }
            finally
            {
              int numUserTasks;
              
              FastThreadLocal.removeAll();
              
              SingleThreadEventExecutor.STATE_UPDATER.set(SingleThreadEventExecutor.this, 5);
              threadLock.countDown();
              int numUserTasks = drainTasks();
              if ((numUserTasks > 0) && (SingleThreadEventExecutor.logger.isWarnEnabled())) {
                SingleThreadEventExecutor.logger.warn("An event executor terminated with non-empty task queue (" + numUserTasks + ')');
              }
              
              terminationFuture.setSuccess(null);
            }
          }
          int oldState;
          int oldState;
          int numUserTasks;
          int numUserTasks;
          int numUserTasks;
          int numUserTasks;
          int oldState;
          int oldState;
          int numUserTasks;
          int numUserTasks;
          int numUserTasks;
          int numUserTasks;
          return;
        }
        catch (Throwable t)
        {
          SingleThreadEventExecutor.logger.warn("Unexpected exception from an event executor: ", t);
        } finally {
          for (;;) {
            oldState = state;
            if ((oldState >= 3) || (SingleThreadEventExecutor.STATE_UPDATER.compareAndSet(SingleThreadEventExecutor.this, oldState, 3))) {
              break;
            }
          }
          


          if ((success) && (gracefulShutdownStartTime == 0L) && 
            (SingleThreadEventExecutor.logger.isErrorEnabled())) {
            SingleThreadEventExecutor.logger.error("Buggy " + EventExecutor.class.getSimpleName() + " implementation; " + SingleThreadEventExecutor.class
              .getSimpleName() + ".confirmShutdown() must be called before run() implementation terminates.");
          }
          



          try
          {
            for (;;)
            {
              if (confirmShutdown()) {
                break;
              }
            }
            

            for (;;)
            {
              oldState = state;
              if ((oldState >= 4) || (SingleThreadEventExecutor.STATE_UPDATER.compareAndSet(SingleThreadEventExecutor.this, oldState, 4))) {
                break;
              }
            }
            



            confirmShutdown();
          } finally {
            try {
              cleanup();

            }
            finally
            {

              FastThreadLocal.removeAll();
              
              SingleThreadEventExecutor.STATE_UPDATER.set(SingleThreadEventExecutor.this, 5);
              threadLock.countDown();
              numUserTasks = drainTasks();
              if ((numUserTasks > 0) && (SingleThreadEventExecutor.logger.isWarnEnabled())) {
                SingleThreadEventExecutor.logger.warn("An event executor terminated with non-empty task queue (" + numUserTasks + ')');
              }
              
              terminationFuture.setSuccess(null);
            }
          }
        }
      }
    });
  }
  
  final int drainTasks() {
    int numTasks = 0;
    for (;;) {
      Runnable runnable = (Runnable)taskQueue.poll();
      if (runnable == null) {
        break;
      }
      

      if (WAKEUP_TASK != runnable) {
        numTasks++;
      }
    }
    return numTasks;
  }
  
  private static final class DefaultThreadProperties implements ThreadProperties {
    private final Thread t;
    
    DefaultThreadProperties(Thread t) {
      this.t = t;
    }
    
    public Thread.State state()
    {
      return t.getState();
    }
    
    public int priority()
    {
      return t.getPriority();
    }
    
    public boolean isInterrupted()
    {
      return t.isInterrupted();
    }
    
    public boolean isDaemon()
    {
      return t.isDaemon();
    }
    
    public String name()
    {
      return t.getName();
    }
    
    public long id()
    {
      return t.getId();
    }
    
    public StackTraceElement[] stackTrace()
    {
      return t.getStackTrace();
    }
    
    public boolean isAlive()
    {
      return t.isAlive();
    }
  }
  
  @Deprecated
  protected static abstract interface NonWakeupRunnable
    extends AbstractEventExecutor.LazyRunnable
  {}
}
