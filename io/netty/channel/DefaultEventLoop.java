package io.netty.channel;

import io.netty.util.concurrent.DefaultThreadFactory;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;















public class DefaultEventLoop
  extends SingleThreadEventLoop
{
  public DefaultEventLoop()
  {
    this((EventLoopGroup)null);
  }
  
  public DefaultEventLoop(ThreadFactory threadFactory) {
    this(null, threadFactory);
  }
  
  public DefaultEventLoop(Executor executor) {
    this(null, executor);
  }
  
  public DefaultEventLoop(EventLoopGroup parent) {
    this(parent, new DefaultThreadFactory(DefaultEventLoop.class));
  }
  
  public DefaultEventLoop(EventLoopGroup parent, ThreadFactory threadFactory) {
    super(parent, threadFactory, true);
  }
  
  public DefaultEventLoop(EventLoopGroup parent, Executor executor) {
    super(parent, executor, true);
  }
  
  protected void run()
  {
    for (;;) {
      Runnable task = takeTask();
      if (task != null) {
        task.run();
        updateLastExecutionTime();
      }
      
      if (confirmShutdown()) {
        break;
      }
    }
  }
}
