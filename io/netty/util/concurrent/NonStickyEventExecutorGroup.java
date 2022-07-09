package io.netty.util.concurrent;

import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;



























public final class NonStickyEventExecutorGroup
  implements EventExecutorGroup
{
  private final EventExecutorGroup group;
  private final int maxTaskExecutePerRun;
  
  public NonStickyEventExecutorGroup(EventExecutorGroup group)
  {
    this(group, 1024);
  }
  



  public NonStickyEventExecutorGroup(EventExecutorGroup group, int maxTaskExecutePerRun)
  {
    this.group = verify(group);
    this.maxTaskExecutePerRun = ObjectUtil.checkPositive(maxTaskExecutePerRun, "maxTaskExecutePerRun");
  }
  
  private static EventExecutorGroup verify(EventExecutorGroup group) {
    Iterator<EventExecutor> executors = ((EventExecutorGroup)ObjectUtil.checkNotNull(group, "group")).iterator();
    while (executors.hasNext()) {
      EventExecutor executor = (EventExecutor)executors.next();
      if ((executor instanceof OrderedEventExecutor)) {
        throw new IllegalArgumentException("EventExecutorGroup " + group + " contains OrderedEventExecutors: " + executor);
      }
    }
    
    return group;
  }
  
  private NonStickyOrderedEventExecutor newExecutor(EventExecutor executor) {
    return new NonStickyOrderedEventExecutor(executor, maxTaskExecutePerRun);
  }
  
  public boolean isShuttingDown()
  {
    return group.isShuttingDown();
  }
  
  public Future<?> shutdownGracefully()
  {
    return group.shutdownGracefully();
  }
  
  public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit)
  {
    return group.shutdownGracefully(quietPeriod, timeout, unit);
  }
  
  public Future<?> terminationFuture()
  {
    return group.terminationFuture();
  }
  

  public void shutdown()
  {
    group.shutdown();
  }
  

  public List<Runnable> shutdownNow()
  {
    return group.shutdownNow();
  }
  
  public EventExecutor next()
  {
    return newExecutor(group.next());
  }
  
  public Iterator<EventExecutor> iterator()
  {
    final Iterator<EventExecutor> itr = group.iterator();
    new Iterator()
    {
      public boolean hasNext() {
        return itr.hasNext();
      }
      
      public EventExecutor next()
      {
        return NonStickyEventExecutorGroup.this.newExecutor((EventExecutor)itr.next());
      }
      
      public void remove()
      {
        itr.remove();
      }
    };
  }
  
  public Future<?> submit(Runnable task)
  {
    return group.submit(task);
  }
  
  public <T> Future<T> submit(Runnable task, T result)
  {
    return group.submit(task, result);
  }
  
  public <T> Future<T> submit(Callable<T> task)
  {
    return group.submit(task);
  }
  
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit)
  {
    return group.schedule(command, delay, unit);
  }
  
  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit)
  {
    return group.schedule(callable, delay, unit);
  }
  
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit)
  {
    return group.scheduleAtFixedRate(command, initialDelay, period, unit);
  }
  
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit)
  {
    return group.scheduleWithFixedDelay(command, initialDelay, delay, unit);
  }
  
  public boolean isShutdown()
  {
    return group.isShutdown();
  }
  
  public boolean isTerminated()
  {
    return group.isTerminated();
  }
  
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException
  {
    return group.awaitTermination(timeout, unit);
  }
  
  public <T> List<java.util.concurrent.Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
    throws InterruptedException
  {
    return group.invokeAll(tasks);
  }
  
  public <T> List<java.util.concurrent.Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
    throws InterruptedException
  {
    return group.invokeAll(tasks, timeout, unit);
  }
  
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException
  {
    return group.invokeAny(tasks);
  }
  
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
    throws InterruptedException, ExecutionException, TimeoutException
  {
    return group.invokeAny(tasks, timeout, unit);
  }
  
  public void execute(Runnable command)
  {
    group.execute(command);
  }
  
  private static final class NonStickyOrderedEventExecutor extends AbstractEventExecutor implements Runnable, OrderedEventExecutor
  {
    private final EventExecutor executor;
    private final Queue<Runnable> tasks = PlatformDependent.newMpscQueue();
    
    private static final int NONE = 0;
    
    private static final int SUBMITTED = 1;
    private static final int RUNNING = 2;
    private final AtomicInteger state = new AtomicInteger();
    private final int maxTaskExecutePerRun;
    
    NonStickyOrderedEventExecutor(EventExecutor executor, int maxTaskExecutePerRun) {
      super();
      this.executor = executor;
      this.maxTaskExecutePerRun = maxTaskExecutePerRun;
    }
    
    public void run()
    {
      if (!state.compareAndSet(1, 2)) {
        return;
      }
      for (;;) {
        int i = 0;
        try {
          for (; i < maxTaskExecutePerRun; i++) {
            Runnable task = (Runnable)tasks.poll();
            if (task == null) {
              break;
            }
            safeExecute(task);
          }
        } finally {
          if (i == maxTaskExecutePerRun) {
            try {
              state.set(1);
              executor.execute(this);
              return;
            }
            catch (Throwable ignore) {
              state.set(2);
            }
            
          }
          else
          {
            state.set(0);
            














            if ((tasks.isEmpty()) || (!state.compareAndSet(0, 2))) {
              return;
            }
          }
        }
      }
    }
    
    public boolean inEventLoop(Thread thread)
    {
      return false;
    }
    
    public boolean inEventLoop()
    {
      return false;
    }
    
    public boolean isShuttingDown()
    {
      return executor.isShutdown();
    }
    
    public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit)
    {
      return executor.shutdownGracefully(quietPeriod, timeout, unit);
    }
    
    public Future<?> terminationFuture()
    {
      return executor.terminationFuture();
    }
    
    public void shutdown()
    {
      executor.shutdown();
    }
    
    public boolean isShutdown()
    {
      return executor.isShutdown();
    }
    
    public boolean isTerminated()
    {
      return executor.isTerminated();
    }
    
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException
    {
      return executor.awaitTermination(timeout, unit);
    }
    
    public void execute(Runnable command)
    {
      if (!tasks.offer(command)) {
        throw new RejectedExecutionException();
      }
      if (state.compareAndSet(0, 1))
      {

        executor.execute(this);
      }
    }
  }
}
