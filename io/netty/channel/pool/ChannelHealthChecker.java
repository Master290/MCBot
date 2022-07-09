package io.netty.channel.pool;

import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.Future;
























public abstract interface ChannelHealthChecker
{
  public static final ChannelHealthChecker ACTIVE = new ChannelHealthChecker()
  {
    public Future<Boolean> isHealthy(Channel channel) {
      EventLoop loop = channel.eventLoop();
      return channel.isActive() ? loop.newSucceededFuture(Boolean.TRUE) : loop.newSucceededFuture(Boolean.FALSE);
    }
  };
  
  public abstract Future<Boolean> isHealthy(Channel paramChannel);
}
