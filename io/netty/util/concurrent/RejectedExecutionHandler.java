package io.netty.util.concurrent;

public abstract interface RejectedExecutionHandler
{
  public abstract void rejected(Runnable paramRunnable, SingleThreadEventExecutor paramSingleThreadEventExecutor);
}
