package io.netty.resolver.dns;

import io.netty.resolver.NameResolver;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;
import java.util.List;
import java.util.concurrent.ConcurrentMap;





















final class InflightNameResolver<T>
  implements NameResolver<T>
{
  private final EventExecutor executor;
  private final NameResolver<T> delegate;
  private final ConcurrentMap<String, Promise<T>> resolvesInProgress;
  private final ConcurrentMap<String, Promise<List<T>>> resolveAllsInProgress;
  
  InflightNameResolver(EventExecutor executor, NameResolver<T> delegate, ConcurrentMap<String, Promise<T>> resolvesInProgress, ConcurrentMap<String, Promise<List<T>>> resolveAllsInProgress)
  {
    this.executor = ((EventExecutor)ObjectUtil.checkNotNull(executor, "executor"));
    this.delegate = ((NameResolver)ObjectUtil.checkNotNull(delegate, "delegate"));
    this.resolvesInProgress = ((ConcurrentMap)ObjectUtil.checkNotNull(resolvesInProgress, "resolvesInProgress"));
    this.resolveAllsInProgress = ((ConcurrentMap)ObjectUtil.checkNotNull(resolveAllsInProgress, "resolveAllsInProgress"));
  }
  
  public Future<T> resolve(String inetHost)
  {
    return resolve(inetHost, executor.newPromise());
  }
  
  public Future<List<T>> resolveAll(String inetHost)
  {
    return resolveAll(inetHost, executor.newPromise());
  }
  
  public void close()
  {
    delegate.close();
  }
  
  public Promise<T> resolve(String inetHost, Promise<T> promise)
  {
    return resolve(resolvesInProgress, inetHost, promise, false);
  }
  
  public Promise<List<T>> resolveAll(String inetHost, Promise<List<T>> promise)
  {
    return resolve(resolveAllsInProgress, inetHost, promise, true);
  }
  


  private <U> Promise<U> resolve(final ConcurrentMap<String, Promise<U>> resolveMap, final String inetHost, final Promise<U> promise, boolean resolveAll)
  {
    Promise<U> earlyPromise = (Promise)resolveMap.putIfAbsent(inetHost, promise);
    if (earlyPromise != null)
    {
      if (earlyPromise.isDone()) {
        transferResult(earlyPromise, promise);
      } else {
        earlyPromise.addListener(new FutureListener()
        {
          public void operationComplete(Future<U> f) throws Exception {
            InflightNameResolver.transferResult(f, promise);
          }
        });
      }
    } else {
      try {
        if (resolveAll)
        {
          Promise<List<T>> castPromise = promise;
          delegate.resolveAll(inetHost, castPromise);
        }
        else {
          Promise<T> castPromise = promise;
          delegate.resolve(inetHost, castPromise);
        }
      } finally {
        if (promise.isDone()) {
          resolveMap.remove(inetHost);
        } else {
          promise.addListener(new FutureListener()
          {
            public void operationComplete(Future<U> f) throws Exception {
              resolveMap.remove(inetHost);
            }
          });
        }
      }
    }
    
    return promise;
  }
  
  private static <T> void transferResult(Future<T> src, Promise<T> dst) {
    if (src.isSuccess()) {
      dst.trySuccess(src.getNow());
    } else {
      dst.tryFailure(src.cause());
    }
  }
  
  public String toString()
  {
    return StringUtil.simpleClassName(this) + '(' + delegate + ')';
  }
}
