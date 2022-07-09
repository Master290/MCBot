package io.netty.util.internal;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.FastThreadLocal;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;




















public final class ThreadExecutorMap
{
  private static final FastThreadLocal<EventExecutor> mappings = new FastThreadLocal();
  

  private ThreadExecutorMap() {}
  

  public static EventExecutor currentExecutor()
  {
    return (EventExecutor)mappings.get();
  }
  


  private static void setCurrentEventExecutor(EventExecutor executor)
  {
    mappings.set(executor);
  }
  



  public static Executor apply(Executor executor, final EventExecutor eventExecutor)
  {
    ObjectUtil.checkNotNull(executor, "executor");
    ObjectUtil.checkNotNull(eventExecutor, "eventExecutor");
    new Executor()
    {
      public void execute(Runnable command) {
        val$executor.execute(ThreadExecutorMap.apply(command, eventExecutor));
      }
    };
  }
  



  public static Runnable apply(final Runnable command, EventExecutor eventExecutor)
  {
    ObjectUtil.checkNotNull(command, "command");
    ObjectUtil.checkNotNull(eventExecutor, "eventExecutor");
    new Runnable()
    {
      public void run() {
        ThreadExecutorMap.setCurrentEventExecutor(val$eventExecutor);
        try {
          command.run();
          
          ThreadExecutorMap.setCurrentEventExecutor(null); } finally { ThreadExecutorMap.setCurrentEventExecutor(null);
        }
      }
    };
  }
  



  public static ThreadFactory apply(ThreadFactory threadFactory, final EventExecutor eventExecutor)
  {
    ObjectUtil.checkNotNull(threadFactory, "command");
    ObjectUtil.checkNotNull(eventExecutor, "eventExecutor");
    new ThreadFactory()
    {
      public Thread newThread(Runnable r) {
        return val$threadFactory.newThread(ThreadExecutorMap.apply(r, eventExecutor));
      }
    };
  }
}
