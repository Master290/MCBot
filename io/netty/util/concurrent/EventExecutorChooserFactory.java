package io.netty.util.concurrent;

public abstract interface EventExecutorChooserFactory
{
  public abstract EventExecutorChooser newChooser(EventExecutor[] paramArrayOfEventExecutor);
  
  public static abstract interface EventExecutorChooser
  {
    public abstract EventExecutor next();
  }
}
