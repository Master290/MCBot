package io.netty.channel.local;

import io.netty.channel.DefaultEventLoopGroup;
import java.util.concurrent.ThreadFactory;


























@Deprecated
public class LocalEventLoopGroup
  extends DefaultEventLoopGroup
{
  public LocalEventLoopGroup() {}
  
  public LocalEventLoopGroup(int nThreads)
  {
    super(nThreads);
  }
  




  public LocalEventLoopGroup(ThreadFactory threadFactory)
  {
    super(0, threadFactory);
  }
  





  public LocalEventLoopGroup(int nThreads, ThreadFactory threadFactory)
  {
    super(nThreads, threadFactory);
  }
}
