package io.netty.util.concurrent;

import io.netty.util.internal.ObjectUtil;






























public final class PromiseCombiner
{
  private int expectedCount;
  private int doneCount;
  private Promise<Void> aggregatePromise;
  private Throwable cause;
  private final GenericFutureListener<Future<?>> listener = new GenericFutureListener()
  {
    public void operationComplete(final Future<?> future) {
      if (executor.inEventLoop()) {
        operationComplete0(future);
      } else {
        executor.execute(new Runnable()
        {
          public void run() {
            PromiseCombiner.1.this.operationComplete0(future);
          }
        });
      }
    }
    
    private void operationComplete0(Future<?> future) {
      assert (executor.inEventLoop());
      PromiseCombiner.access$204(PromiseCombiner.this);
      if ((!future.isSuccess()) && (cause == null)) {
        cause = future.cause();
      }
      if ((doneCount == expectedCount) && (aggregatePromise != null)) {
        PromiseCombiner.this.tryPromise();
      }
    }
  };
  

  private final EventExecutor executor;
  

  @Deprecated
  public PromiseCombiner()
  {
    this(ImmediateEventExecutor.INSTANCE);
  }
  





  public PromiseCombiner(EventExecutor executor)
  {
    this.executor = ((EventExecutor)ObjectUtil.checkNotNull(executor, "executor"));
  }
  







  @Deprecated
  public void add(Promise promise)
  {
    add(promise);
  }
  






  public void add(Future future)
  {
    checkAddAllowed();
    checkInEventLoop();
    expectedCount += 1;
    future.addListener(listener);
  }
  







  @Deprecated
  public void addAll(Promise... promises)
  {
    addAll((Future[])promises);
  }
  






  public void addAll(Future... futures)
  {
    for (Future future : futures) {
      add(future);
    }
  }
  










  public void finish(Promise<Void> aggregatePromise)
  {
    ObjectUtil.checkNotNull(aggregatePromise, "aggregatePromise");
    checkInEventLoop();
    if (this.aggregatePromise != null) {
      throw new IllegalStateException("Already finished");
    }
    this.aggregatePromise = aggregatePromise;
    if (doneCount == expectedCount) {
      tryPromise();
    }
  }
  
  private void checkInEventLoop() {
    if (!executor.inEventLoop()) {
      throw new IllegalStateException("Must be called from EventExecutor thread");
    }
  }
  
  private boolean tryPromise() {
    return cause == null ? aggregatePromise.trySuccess(null) : aggregatePromise.tryFailure(cause);
  }
  
  private void checkAddAllowed() {
    if (aggregatePromise != null) {
      throw new IllegalStateException("Adding promises is not allowed after finished adding");
    }
  }
}
