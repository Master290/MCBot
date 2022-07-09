package io.netty.util.concurrent;

import io.netty.util.internal.ObjectUtil;













final class FastThreadLocalRunnable
  implements Runnable
{
  private final Runnable runnable;
  
  private FastThreadLocalRunnable(Runnable runnable)
  {
    this.runnable = ((Runnable)ObjectUtil.checkNotNull(runnable, "runnable"));
  }
  
  public void run()
  {
    try {
      runnable.run();
      
      FastThreadLocal.removeAll(); } finally { FastThreadLocal.removeAll();
    }
  }
  
  static Runnable wrap(Runnable runnable) {
    return (runnable instanceof FastThreadLocalRunnable) ? runnable : new FastThreadLocalRunnable(runnable);
  }
}
