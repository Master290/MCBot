package io.netty.util.concurrent;

import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;

























public final class FailedFuture<V>
  extends CompleteFuture<V>
{
  private final Throwable cause;
  
  public FailedFuture(EventExecutor executor, Throwable cause)
  {
    super(executor);
    this.cause = ((Throwable)ObjectUtil.checkNotNull(cause, "cause"));
  }
  
  public Throwable cause()
  {
    return cause;
  }
  
  public boolean isSuccess()
  {
    return false;
  }
  
  public Future<V> sync()
  {
    PlatformDependent.throwException(cause);
    return this;
  }
  
  public Future<V> syncUninterruptibly()
  {
    PlatformDependent.throwException(cause);
    return this;
  }
  
  public V getNow()
  {
    return null;
  }
}
