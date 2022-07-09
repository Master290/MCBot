package io.netty.util.concurrent;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;


















public abstract class AbstractEventExecutor
  extends AbstractExecutorService
  implements EventExecutor
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractEventExecutor.class);
  
  static final long DEFAULT_SHUTDOWN_QUIET_PERIOD = 2L;
  
  static final long DEFAULT_SHUTDOWN_TIMEOUT = 15L;
  private final EventExecutorGroup parent;
  private final Collection<EventExecutor> selfCollection = Collections.singleton(this);
  
  protected AbstractEventExecutor() {
    this(null);
  }
  
  protected AbstractEventExecutor(EventExecutorGroup parent) {
    this.parent = parent;
  }
  
  public EventExecutorGroup parent()
  {
    return parent;
  }
  
  public EventExecutor next()
  {
    return this;
  }
  
  public boolean inEventLoop()
  {
    return inEventLoop(Thread.currentThread());
  }
  
  public Iterator<EventExecutor> iterator()
  {
    return selfCollection.iterator();
  }
  
  public Future<?> shutdownGracefully()
  {
    return shutdownGracefully(2L, 15L, TimeUnit.SECONDS);
  }
  




  @Deprecated
  public abstract void shutdown();
  



  @Deprecated
  public List<Runnable> shutdownNow()
  {
    shutdown();
    return Collections.emptyList();
  }
  
  public <V> Promise<V> newPromise()
  {
    return new DefaultPromise(this);
  }
  
  public <V> ProgressivePromise<V> newProgressivePromise()
  {
    return new DefaultProgressivePromise(this);
  }
  
  public <V> Future<V> newSucceededFuture(V result)
  {
    return new SucceededFuture(this, result);
  }
  
  public <V> Future<V> newFailedFuture(Throwable cause)
  {
    return new FailedFuture(this, cause);
  }
  
  public Future<?> submit(Runnable task)
  {
    return (Future)super.submit(task);
  }
  
  public <T> Future<T> submit(Runnable task, T result)
  {
    return (Future)super.submit(task, result);
  }
  
  public <T> Future<T> submit(Callable<T> task)
  {
    return (Future)super.submit(task);
  }
  
  protected final <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value)
  {
    return new PromiseTask(this, runnable, value);
  }
  
  protected final <T> RunnableFuture<T> newTaskFor(Callable<T> callable)
  {
    return new PromiseTask(this, callable);
  }
  

  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit)
  {
    throw new UnsupportedOperationException();
  }
  
  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit)
  {
    throw new UnsupportedOperationException();
  }
  
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit)
  {
    throw new UnsupportedOperationException();
  }
  
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit)
  {
    throw new UnsupportedOperationException();
  }
  

  protected static void safeExecute(Runnable task)
  {
    try
    {
      task.run();
    } catch (Throwable t) {
      logger.warn("A task raised an exception. Task: {}", task, t);
    }
  }
  









  public void lazyExecute(Runnable task)
  {
    execute(task);
  }
  
  public static abstract interface LazyRunnable
    extends Runnable
  {}
}
