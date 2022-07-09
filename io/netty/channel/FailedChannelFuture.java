package io.netty.channel;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;

























final class FailedChannelFuture
  extends CompleteChannelFuture
{
  private final Throwable cause;
  
  FailedChannelFuture(Channel channel, EventExecutor executor, Throwable cause)
  {
    super(channel, executor);
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
  
  public ChannelFuture sync()
  {
    PlatformDependent.throwException(cause);
    return this;
  }
  
  public ChannelFuture syncUninterruptibly()
  {
    PlatformDependent.throwException(cause);
    return this;
  }
}
