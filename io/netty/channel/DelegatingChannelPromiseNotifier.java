package io.netty.channel;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PromiseNotificationUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;



















public final class DelegatingChannelPromiseNotifier
  implements ChannelPromise, ChannelFutureListener
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(DelegatingChannelPromiseNotifier.class);
  private final ChannelPromise delegate;
  private final boolean logNotifyFailure;
  
  public DelegatingChannelPromiseNotifier(ChannelPromise delegate) {
    this(delegate, !(delegate instanceof VoidChannelPromise));
  }
  
  public DelegatingChannelPromiseNotifier(ChannelPromise delegate, boolean logNotifyFailure) {
    this.delegate = ((ChannelPromise)ObjectUtil.checkNotNull(delegate, "delegate"));
    this.logNotifyFailure = logNotifyFailure;
  }
  
  public void operationComplete(ChannelFuture future) throws Exception
  {
    InternalLogger internalLogger = logNotifyFailure ? logger : null;
    if (future.isSuccess()) {
      Void result = (Void)future.get();
      PromiseNotificationUtil.trySuccess(delegate, result, internalLogger);
    } else if (future.isCancelled()) {
      PromiseNotificationUtil.tryCancel(delegate, internalLogger);
    } else {
      Throwable cause = future.cause();
      PromiseNotificationUtil.tryFailure(delegate, cause, internalLogger);
    }
  }
  
  public Channel channel()
  {
    return delegate.channel();
  }
  
  public ChannelPromise setSuccess(Void result)
  {
    delegate.setSuccess(result);
    return this;
  }
  
  public ChannelPromise setSuccess()
  {
    delegate.setSuccess();
    return this;
  }
  
  public boolean trySuccess()
  {
    return delegate.trySuccess();
  }
  
  public boolean trySuccess(Void result)
  {
    return delegate.trySuccess(result);
  }
  
  public ChannelPromise setFailure(Throwable cause)
  {
    delegate.setFailure(cause);
    return this;
  }
  
  public ChannelPromise addListener(GenericFutureListener<? extends Future<? super Void>> listener)
  {
    delegate.addListener(listener);
    return this;
  }
  
  public ChannelPromise addListeners(GenericFutureListener<? extends Future<? super Void>>... listeners)
  {
    delegate.addListeners(listeners);
    return this;
  }
  
  public ChannelPromise removeListener(GenericFutureListener<? extends Future<? super Void>> listener)
  {
    delegate.removeListener(listener);
    return this;
  }
  
  public ChannelPromise removeListeners(GenericFutureListener<? extends Future<? super Void>>... listeners)
  {
    delegate.removeListeners(listeners);
    return this;
  }
  
  public boolean tryFailure(Throwable cause)
  {
    return delegate.tryFailure(cause);
  }
  
  public boolean setUncancellable()
  {
    return delegate.setUncancellable();
  }
  
  public ChannelPromise await() throws InterruptedException
  {
    delegate.await();
    return this;
  }
  
  public ChannelPromise awaitUninterruptibly()
  {
    delegate.awaitUninterruptibly();
    return this;
  }
  
  public boolean isVoid()
  {
    return delegate.isVoid();
  }
  
  public ChannelPromise unvoid()
  {
    return isVoid() ? new DelegatingChannelPromiseNotifier(delegate.unvoid()) : this;
  }
  
  public boolean await(long timeout, TimeUnit unit) throws InterruptedException
  {
    return delegate.await(timeout, unit);
  }
  
  public boolean await(long timeoutMillis) throws InterruptedException
  {
    return delegate.await(timeoutMillis);
  }
  
  public boolean awaitUninterruptibly(long timeout, TimeUnit unit)
  {
    return delegate.awaitUninterruptibly(timeout, unit);
  }
  
  public boolean awaitUninterruptibly(long timeoutMillis)
  {
    return delegate.awaitUninterruptibly(timeoutMillis);
  }
  
  public Void getNow()
  {
    return (Void)delegate.getNow();
  }
  
  public boolean cancel(boolean mayInterruptIfRunning)
  {
    return delegate.cancel(mayInterruptIfRunning);
  }
  
  public boolean isCancelled()
  {
    return delegate.isCancelled();
  }
  
  public boolean isDone()
  {
    return delegate.isDone();
  }
  
  public Void get() throws InterruptedException, ExecutionException
  {
    return (Void)delegate.get();
  }
  
  public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
  {
    return (Void)delegate.get(timeout, unit);
  }
  
  public ChannelPromise sync() throws InterruptedException
  {
    delegate.sync();
    return this;
  }
  
  public ChannelPromise syncUninterruptibly()
  {
    delegate.syncUninterruptibly();
    return this;
  }
  
  public boolean isSuccess()
  {
    return delegate.isSuccess();
  }
  
  public boolean isCancellable()
  {
    return delegate.isCancellable();
  }
  
  public Throwable cause()
  {
    return delegate.cause();
  }
}
