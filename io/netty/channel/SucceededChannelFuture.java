package io.netty.channel;

import io.netty.util.concurrent.EventExecutor;
























final class SucceededChannelFuture
  extends CompleteChannelFuture
{
  SucceededChannelFuture(Channel channel, EventExecutor executor)
  {
    super(channel, executor);
  }
  
  public Throwable cause()
  {
    return null;
  }
  
  public boolean isSuccess()
  {
    return true;
  }
}
