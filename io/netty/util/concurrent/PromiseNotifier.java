package io.netty.util.concurrent;

import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PromiseNotificationUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
























public class PromiseNotifier<V, F extends Future<V>>
  implements GenericFutureListener<F>
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(PromiseNotifier.class);
  

  private final Promise<? super V>[] promises;
  
  private final boolean logNotifyFailure;
  

  @SafeVarargs
  public PromiseNotifier(Promise<? super V>... promises)
  {
    this(true, promises);
  }
  





  @SafeVarargs
  public PromiseNotifier(boolean logNotifyFailure, Promise<? super V>... promises)
  {
    ObjectUtil.checkNotNull(promises, "promises");
    for (Promise<? super V> promise : promises) {
      ObjectUtil.checkNotNullWithIAE(promise, "promise");
    }
    this.promises = ((Promise[])promises.clone());
    this.logNotifyFailure = logNotifyFailure;
  }
  










  public static <V, F extends Future<V>> F cascade(F future, Promise<? super V> promise)
  {
    return cascade(true, future, promise);
  }
  













  public static <V, F extends Future<V>> F cascade(boolean logNotifyFailure, final F future, final Promise<? super V> promise)
  {
    promise.addListener(new FutureListener()
    {
      public void operationComplete(Future f) {
        if (f.isCancelled()) {
          val$future.cancel(false);
        }
      }
    });
    future.addListener(new PromiseNotifier(logNotifyFailure, new Promise[] { promise })
    {
      public void operationComplete(Future f) throws Exception {
        if ((promise.isCancelled()) && (f.isCancelled()))
        {
          return;
        }
        super.operationComplete(future);
      }
    });
    return future;
  }
  
  public void operationComplete(F future) throws Exception
  {
    InternalLogger internalLogger = logNotifyFailure ? logger : null;
    V result; Promise<? super V> localPromise1; if (future.isSuccess()) {
      result = future.get();
      Promise[] arrayOfPromise1 = promises;localPromise1 = arrayOfPromise1.length; for (Promise<? super V> localPromise2 = 0; localPromise2 < localPromise1; localPromise2++) { Promise<? super V> p = arrayOfPromise1[localPromise2];
        PromiseNotificationUtil.trySuccess(p, result, internalLogger);
      } } else { Promise<? super V> p;
      if (future.isCancelled()) {
        result = promises;int i = result.length; for (localPromise1 = 0; localPromise1 < i; localPromise1++) { p = result[localPromise1];
          PromiseNotificationUtil.tryCancel(p, internalLogger);
        }
      } else {
        Throwable cause = future.cause();
        Promise[] arrayOfPromise2 = promises;localPromise1 = arrayOfPromise2.length; for (Promise<? super V> localPromise3 = 0; localPromise3 < localPromise1; localPromise3++) { Promise<? super V> p = arrayOfPromise2[localPromise3];
          PromiseNotificationUtil.tryFailure(p, cause, internalLogger);
        }
      }
    }
  }
}
