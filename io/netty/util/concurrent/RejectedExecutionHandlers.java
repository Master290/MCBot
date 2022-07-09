package io.netty.util.concurrent;

import io.netty.util.internal.ObjectUtil;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;



















public final class RejectedExecutionHandlers
{
  private static final RejectedExecutionHandler REJECT = new RejectedExecutionHandler()
  {
    public void rejected(Runnable task, SingleThreadEventExecutor executor) {
      throw new RejectedExecutionException();
    }
  };
  

  private RejectedExecutionHandlers() {}
  

  public static RejectedExecutionHandler reject()
  {
    return REJECT;
  }
  




  public static RejectedExecutionHandler backoff(int retries, long backoffAmount, TimeUnit unit)
  {
    ObjectUtil.checkPositive(retries, "retries");
    final long backOffNanos = unit.toNanos(backoffAmount);
    new RejectedExecutionHandler()
    {
      public void rejected(Runnable task, SingleThreadEventExecutor executor) {
        if (!executor.inEventLoop()) {
          for (int i = 0; i < val$retries; i++)
          {
            executor.wakeup(false);
            
            LockSupport.parkNanos(backOffNanos);
            if (executor.offerTask(task)) {
              return;
            }
          }
        }
        

        throw new RejectedExecutionException();
      }
    };
  }
}
