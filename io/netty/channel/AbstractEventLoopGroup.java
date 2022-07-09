package io.netty.channel;

import io.netty.util.concurrent.AbstractEventExecutorGroup;

public abstract class AbstractEventLoopGroup
  extends AbstractEventExecutorGroup
  implements EventLoopGroup
{
  public AbstractEventLoopGroup() {}
  
  public abstract EventLoop next();
}
