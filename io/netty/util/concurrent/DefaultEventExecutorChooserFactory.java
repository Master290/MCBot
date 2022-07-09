package io.netty.util.concurrent;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;





















public final class DefaultEventExecutorChooserFactory
  implements EventExecutorChooserFactory
{
  public static final DefaultEventExecutorChooserFactory INSTANCE = new DefaultEventExecutorChooserFactory();
  
  private DefaultEventExecutorChooserFactory() {}
  
  public EventExecutorChooserFactory.EventExecutorChooser newChooser(EventExecutor[] executors)
  {
    if (isPowerOfTwo(executors.length)) {
      return new PowerOfTwoEventExecutorChooser(executors);
    }
    return new GenericEventExecutorChooser(executors);
  }
  
  private static boolean isPowerOfTwo(int val)
  {
    return (val & -val) == val;
  }
  
  private static final class PowerOfTwoEventExecutorChooser implements EventExecutorChooserFactory.EventExecutorChooser {
    private final AtomicInteger idx = new AtomicInteger();
    private final EventExecutor[] executors;
    
    PowerOfTwoEventExecutorChooser(EventExecutor[] executors) {
      this.executors = executors;
    }
    
    public EventExecutor next()
    {
      return executors[(idx.getAndIncrement() & executors.length - 1)];
    }
  }
  

  private static final class GenericEventExecutorChooser
    implements EventExecutorChooserFactory.EventExecutorChooser
  {
    private final AtomicLong idx = new AtomicLong();
    private final EventExecutor[] executors;
    
    GenericEventExecutorChooser(EventExecutor[] executors) {
      this.executors = executors;
    }
    
    public EventExecutor next()
    {
      return executors[((int)Math.abs(idx.getAndIncrement() % executors.length))];
    }
  }
}
