package io.netty.resolver;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.ObjectUtil;
import java.util.List;
























public abstract class SimpleNameResolver<T>
  implements NameResolver<T>
{
  private final EventExecutor executor;
  
  protected SimpleNameResolver(EventExecutor executor)
  {
    this.executor = ((EventExecutor)ObjectUtil.checkNotNull(executor, "executor"));
  }
  



  protected EventExecutor executor()
  {
    return executor;
  }
  
  public final Future<T> resolve(String inetHost)
  {
    Promise<T> promise = executor().newPromise();
    return resolve(inetHost, promise);
  }
  
  public Future<T> resolve(String inetHost, Promise<T> promise)
  {
    ObjectUtil.checkNotNull(promise, "promise");
    try
    {
      doResolve(inetHost, promise);
      return promise;
    } catch (Exception e) {
      return promise.setFailure(e);
    }
  }
  
  public final Future<List<T>> resolveAll(String inetHost)
  {
    Promise<List<T>> promise = executor().newPromise();
    return resolveAll(inetHost, promise);
  }
  
  public Future<List<T>> resolveAll(String inetHost, Promise<List<T>> promise)
  {
    ObjectUtil.checkNotNull(promise, "promise");
    try
    {
      doResolveAll(inetHost, promise);
      return promise;
    } catch (Exception e) {
      return promise.setFailure(e);
    }
  }
  
  protected abstract void doResolve(String paramString, Promise<T> paramPromise)
    throws Exception;
  
  protected abstract void doResolveAll(String paramString, Promise<List<T>> paramPromise)
    throws Exception;
  
  public void close() {}
}
