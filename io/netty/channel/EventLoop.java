package io.netty.channel;

import io.netty.util.concurrent.OrderedEventExecutor;

public abstract interface EventLoop
  extends OrderedEventExecutor, EventLoopGroup
{
  public abstract EventLoopGroup parent();
}
