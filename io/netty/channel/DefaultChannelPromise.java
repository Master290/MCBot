package io.netty.channel;

import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.ObjectUtil;

























public class DefaultChannelPromise
  extends DefaultPromise<Void>
  implements ChannelPromise, ChannelFlushPromiseNotifier.FlushCheckpoint
{
  private final Channel channel;
  private long checkpoint;
  
  public DefaultChannelPromise(Channel channel)
  {
    this.channel = ((Channel)ObjectUtil.checkNotNull(channel, "channel"));
  }
  





  public DefaultChannelPromise(Channel channel, EventExecutor executor)
  {
    super(executor);
    this.channel = ((Channel)ObjectUtil.checkNotNull(channel, "channel"));
  }
  
  protected EventExecutor executor()
  {
    EventExecutor e = super.executor();
    if (e == null) {
      return channel().eventLoop();
    }
    return e;
  }
  

  public Channel channel()
  {
    return channel;
  }
  
  public ChannelPromise setSuccess()
  {
    return setSuccess(null);
  }
  
  public ChannelPromise setSuccess(Void result)
  {
    super.setSuccess(result);
    return this;
  }
  
  public boolean trySuccess()
  {
    return trySuccess(null);
  }
  
  public ChannelPromise setFailure(Throwable cause)
  {
    super.setFailure(cause);
    return this;
  }
  
  public ChannelPromise addListener(GenericFutureListener<? extends Future<? super Void>> listener)
  {
    super.addListener(listener);
    return this;
  }
  
  public ChannelPromise addListeners(GenericFutureListener<? extends Future<? super Void>>... listeners)
  {
    super.addListeners(listeners);
    return this;
  }
  
  public ChannelPromise removeListener(GenericFutureListener<? extends Future<? super Void>> listener)
  {
    super.removeListener(listener);
    return this;
  }
  
  public ChannelPromise removeListeners(GenericFutureListener<? extends Future<? super Void>>... listeners)
  {
    super.removeListeners(listeners);
    return this;
  }
  
  public ChannelPromise sync() throws InterruptedException
  {
    super.sync();
    return this;
  }
  
  public ChannelPromise syncUninterruptibly()
  {
    super.syncUninterruptibly();
    return this;
  }
  
  public ChannelPromise await() throws InterruptedException
  {
    super.await();
    return this;
  }
  
  public ChannelPromise awaitUninterruptibly()
  {
    super.awaitUninterruptibly();
    return this;
  }
  
  public long flushCheckpoint()
  {
    return checkpoint;
  }
  
  public void flushCheckpoint(long checkpoint)
  {
    this.checkpoint = checkpoint;
  }
  
  public ChannelPromise promise()
  {
    return this;
  }
  
  protected void checkDeadLock()
  {
    if (channel().isRegistered()) {
      super.checkDeadLock();
    }
  }
  
  public ChannelPromise unvoid()
  {
    return this;
  }
  
  public boolean isVoid()
  {
    return false;
  }
}
