package io.netty.channel.group;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
















final class VoidChannelGroupFuture
  implements ChannelGroupFuture
{
  private static final Iterator<ChannelFuture> EMPTY = Collections.emptyList().iterator();
  private final ChannelGroup group;
  
  VoidChannelGroupFuture(ChannelGroup group) {
    this.group = group;
  }
  
  public ChannelGroup group()
  {
    return group;
  }
  
  public ChannelFuture find(Channel channel)
  {
    return null;
  }
  
  public boolean isSuccess()
  {
    return false;
  }
  
  public ChannelGroupException cause()
  {
    return null;
  }
  
  public boolean isPartialSuccess()
  {
    return false;
  }
  
  public boolean isPartialFailure()
  {
    return false;
  }
  
  public ChannelGroupFuture addListener(GenericFutureListener<? extends Future<? super Void>> listener)
  {
    throw reject();
  }
  
  public ChannelGroupFuture addListeners(GenericFutureListener<? extends Future<? super Void>>... listeners)
  {
    throw reject();
  }
  
  public ChannelGroupFuture removeListener(GenericFutureListener<? extends Future<? super Void>> listener)
  {
    throw reject();
  }
  
  public ChannelGroupFuture removeListeners(GenericFutureListener<? extends Future<? super Void>>... listeners)
  {
    throw reject();
  }
  
  public ChannelGroupFuture await()
  {
    throw reject();
  }
  
  public ChannelGroupFuture awaitUninterruptibly()
  {
    throw reject();
  }
  
  public ChannelGroupFuture syncUninterruptibly()
  {
    throw reject();
  }
  
  public ChannelGroupFuture sync()
  {
    throw reject();
  }
  
  public Iterator<ChannelFuture> iterator()
  {
    return EMPTY;
  }
  
  public boolean isCancellable()
  {
    return false;
  }
  
  public boolean await(long timeout, TimeUnit unit)
  {
    throw reject();
  }
  
  public boolean await(long timeoutMillis)
  {
    throw reject();
  }
  
  public boolean awaitUninterruptibly(long timeout, TimeUnit unit)
  {
    throw reject();
  }
  
  public boolean awaitUninterruptibly(long timeoutMillis)
  {
    throw reject();
  }
  
  public Void getNow()
  {
    return null;
  }
  





  public boolean cancel(boolean mayInterruptIfRunning)
  {
    return false;
  }
  
  public boolean isCancelled()
  {
    return false;
  }
  
  public boolean isDone()
  {
    return false;
  }
  
  public Void get()
  {
    throw reject();
  }
  
  public Void get(long timeout, TimeUnit unit)
  {
    throw reject();
  }
  
  private static RuntimeException reject() {
    return new IllegalStateException("void future");
  }
}
