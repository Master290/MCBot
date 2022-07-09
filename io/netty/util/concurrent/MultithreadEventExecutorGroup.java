package io.netty.util.concurrent;

import io.netty.util.internal.ObjectUtil;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;




















public abstract class MultithreadEventExecutorGroup
  extends AbstractEventExecutorGroup
{
  private final EventExecutor[] children;
  private final Set<EventExecutor> readonlyChildren;
  private final AtomicInteger terminatedChildren = new AtomicInteger();
  private final Promise<?> terminationFuture = new DefaultPromise(GlobalEventExecutor.INSTANCE);
  


  private final EventExecutorChooserFactory.EventExecutorChooser chooser;
  



  protected MultithreadEventExecutorGroup(int nThreads, ThreadFactory threadFactory, Object... args)
  {
    this(nThreads, threadFactory == null ? null : new ThreadPerTaskExecutor(threadFactory), args);
  }
  






  protected MultithreadEventExecutorGroup(int nThreads, Executor executor, Object... args)
  {
    this(nThreads, executor, DefaultEventExecutorChooserFactory.INSTANCE, args);
  }
  








  protected MultithreadEventExecutorGroup(int nThreads, Executor executor, EventExecutorChooserFactory chooserFactory, Object... args)
  {
    ObjectUtil.checkPositive(nThreads, "nThreads");
    
    if (executor == null) {
      executor = new ThreadPerTaskExecutor(newDefaultThreadFactory());
    }
    
    children = new EventExecutor[nThreads];
    
    for (int i = 0; i < nThreads; i++) {
      success = false;
      try {
        children[i] = newChild(executor, args);
        success = true;
      } catch (Exception e) { int j;
        int j;
        throw new IllegalStateException("failed to create a child event loop", e);
      } finally {
        if (!success) {
          for (int j = 0; j < i; j++) {
            children[j].shutdownGracefully();
          }
          
          for (int j = 0; j < i; j++) {
            EventExecutor e = children[j];
            try {
              while (!e.isTerminated()) {
                e.awaitTermination(2147483647L, TimeUnit.SECONDS);
              }
            }
            catch (InterruptedException interrupted) {
              Thread.currentThread().interrupt();
              break;
            }
          }
        }
      }
    }
    
    chooser = chooserFactory.newChooser(children);
    
    FutureListener<Object> terminationListener = new FutureListener()
    {
      public void operationComplete(Future<Object> future) throws Exception {
        if (terminatedChildren.incrementAndGet() == children.length) {
          terminationFuture.setSuccess(null);
        }
        
      }
    };
    boolean success = children;e = success.length; for (EventExecutor e = 0; e < e; e++) { EventExecutor e = success[e];
      e.terminationFuture().addListener(terminationListener);
    }
    
    Set<EventExecutor> childrenSet = new LinkedHashSet(children.length);
    Collections.addAll(childrenSet, children);
    readonlyChildren = Collections.unmodifiableSet(childrenSet);
  }
  
  protected ThreadFactory newDefaultThreadFactory() {
    return new DefaultThreadFactory(getClass());
  }
  
  public EventExecutor next()
  {
    return chooser.next();
  }
  
  public Iterator<EventExecutor> iterator()
  {
    return readonlyChildren.iterator();
  }
  



  public final int executorCount()
  {
    return children.length;
  }
  


  protected abstract EventExecutor newChild(Executor paramExecutor, Object... paramVarArgs)
    throws Exception;
  


  public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit)
  {
    for (EventExecutor l : children) {
      l.shutdownGracefully(quietPeriod, timeout, unit);
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
    for (EventExecutor l : children) {
      l.shutdown();
    }
  }
  
  public boolean isShuttingDown()
  {
    for (EventExecutor l : children) {
      if (!l.isShuttingDown()) {
        return false;
      }
    }
    return true;
  }
  
  public boolean isShutdown()
  {
    for (EventExecutor l : children) {
      if (!l.isShutdown()) {
        return false;
      }
    }
    return true;
  }
  
  public boolean isTerminated()
  {
    for (EventExecutor l : children) {
      if (!l.isTerminated()) {
        return false;
      }
    }
    return true;
  }
  
  public boolean awaitTermination(long timeout, TimeUnit unit)
    throws InterruptedException
  {
    long deadline = System.nanoTime() + unit.toNanos(timeout);
    for (EventExecutor l : children) {
      for (;;) {
        long timeLeft = deadline - System.nanoTime();
        if (timeLeft <= 0L) {
          break label84;
        }
        if (l.awaitTermination(timeLeft, TimeUnit.NANOSECONDS))
          break;
      }
    }
    label84:
    return isTerminated();
  }
}
