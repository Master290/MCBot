package io.netty.channel;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;




















public class DefaultEventLoopGroup
  extends MultithreadEventLoopGroup
{
  public DefaultEventLoopGroup()
  {
    this(0);
  }
  




  public DefaultEventLoopGroup(int nThreads)
  {
    this(nThreads, (ThreadFactory)null);
  }
  




  public DefaultEventLoopGroup(ThreadFactory threadFactory)
  {
    this(0, threadFactory);
  }
  





  public DefaultEventLoopGroup(int nThreads, ThreadFactory threadFactory)
  {
    super(nThreads, threadFactory, new Object[0]);
  }
  





  public DefaultEventLoopGroup(int nThreads, Executor executor)
  {
    super(nThreads, executor, new Object[0]);
  }
  
  protected EventLoop newChild(Executor executor, Object... args) throws Exception
  {
    return new DefaultEventLoop(this, executor);
  }
}
