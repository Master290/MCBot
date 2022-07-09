package io.netty.channel;

import io.netty.util.concurrent.AbstractEventExecutor;

















public abstract class AbstractEventLoop
  extends AbstractEventExecutor
  implements EventLoop
{
  protected AbstractEventLoop() {}
  
  protected AbstractEventLoop(EventLoopGroup parent)
  {
    super(parent);
  }
  
  public EventLoopGroup parent()
  {
    return (EventLoopGroup)super.parent();
  }
  
  public EventLoop next()
  {
    return (EventLoop)super.next();
  }
}
