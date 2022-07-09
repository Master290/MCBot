package io.netty.channel.epoll;

import io.netty.channel.DefaultSelectStrategyFactory;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopTaskQueueFactory;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.channel.SelectStrategyFactory;
import io.netty.util.concurrent.EventExecutorChooserFactory;
import io.netty.util.concurrent.RejectedExecutionHandler;
import io.netty.util.concurrent.RejectedExecutionHandlers;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;




























public final class EpollEventLoopGroup
  extends MultithreadEventLoopGroup
{
  public EpollEventLoopGroup()
  {
    this(0);
  }
  


  public EpollEventLoopGroup(int nThreads)
  {
    this(nThreads, (ThreadFactory)null);
  }
  



  public EpollEventLoopGroup(ThreadFactory threadFactory)
  {
    this(0, threadFactory, 0);
  }
  



  public EpollEventLoopGroup(int nThreads, SelectStrategyFactory selectStrategyFactory)
  {
    this(nThreads, (ThreadFactory)null, selectStrategyFactory);
  }
  



  public EpollEventLoopGroup(int nThreads, ThreadFactory threadFactory)
  {
    this(nThreads, threadFactory, 0);
  }
  
  public EpollEventLoopGroup(int nThreads, Executor executor) {
    this(nThreads, executor, DefaultSelectStrategyFactory.INSTANCE);
  }
  



  public EpollEventLoopGroup(int nThreads, ThreadFactory threadFactory, SelectStrategyFactory selectStrategyFactory)
  {
    this(nThreads, threadFactory, 0, selectStrategyFactory);
  }
  





  @Deprecated
  public EpollEventLoopGroup(int nThreads, ThreadFactory threadFactory, int maxEventsAtOnce)
  {
    this(nThreads, threadFactory, maxEventsAtOnce, DefaultSelectStrategyFactory.INSTANCE);
  }
  







  @Deprecated
  public EpollEventLoopGroup(int nThreads, ThreadFactory threadFactory, int maxEventsAtOnce, SelectStrategyFactory selectStrategyFactory)
  {
    super(nThreads, threadFactory, new Object[] { Integer.valueOf(maxEventsAtOnce), selectStrategyFactory, RejectedExecutionHandlers.reject() });Epoll.ensureAvailability();
  }
  
  public EpollEventLoopGroup(int nThreads, Executor executor, SelectStrategyFactory selectStrategyFactory) {
    super(nThreads, executor, new Object[] { Integer.valueOf(0), selectStrategyFactory, RejectedExecutionHandlers.reject() });Epoll.ensureAvailability();
  }
  
  public EpollEventLoopGroup(int nThreads, Executor executor, EventExecutorChooserFactory chooserFactory, SelectStrategyFactory selectStrategyFactory)
  {
    super(nThreads, executor, chooserFactory, new Object[] { Integer.valueOf(0), selectStrategyFactory, RejectedExecutionHandlers.reject() });Epoll.ensureAvailability();
  }
  

  public EpollEventLoopGroup(int nThreads, Executor executor, EventExecutorChooserFactory chooserFactory, SelectStrategyFactory selectStrategyFactory, RejectedExecutionHandler rejectedExecutionHandler)
  {
    super(nThreads, executor, chooserFactory, new Object[] { Integer.valueOf(0), selectStrategyFactory, rejectedExecutionHandler });Epoll.ensureAvailability();
  }
  


  public EpollEventLoopGroup(int nThreads, Executor executor, EventExecutorChooserFactory chooserFactory, SelectStrategyFactory selectStrategyFactory, RejectedExecutionHandler rejectedExecutionHandler, EventLoopTaskQueueFactory queueFactory)
  {
    super(nThreads, executor, chooserFactory, new Object[] { Integer.valueOf(0), selectStrategyFactory, rejectedExecutionHandler, queueFactory });Epoll.ensureAvailability();
  }
  
















  public EpollEventLoopGroup(int nThreads, Executor executor, EventExecutorChooserFactory chooserFactory, SelectStrategyFactory selectStrategyFactory, RejectedExecutionHandler rejectedExecutionHandler, EventLoopTaskQueueFactory taskQueueFactory, EventLoopTaskQueueFactory tailTaskQueueFactory)
  {
    super(nThreads, executor, chooserFactory, new Object[] { Integer.valueOf(0), selectStrategyFactory, rejectedExecutionHandler, taskQueueFactory, tailTaskQueueFactory });Epoll.ensureAvailability();
  }
  



  @Deprecated
  public void setIoRatio(int ioRatio)
  {
    if ((ioRatio <= 0) || (ioRatio > 100)) {
      throw new IllegalArgumentException("ioRatio: " + ioRatio + " (expected: 0 < ioRatio <= 100)");
    }
  }
  
  protected EventLoop newChild(Executor executor, Object... args) throws Exception
  {
    Integer maxEvents = (Integer)args[0];
    SelectStrategyFactory selectStrategyFactory = (SelectStrategyFactory)args[1];
    RejectedExecutionHandler rejectedExecutionHandler = (RejectedExecutionHandler)args[2];
    EventLoopTaskQueueFactory taskQueueFactory = null;
    EventLoopTaskQueueFactory tailTaskQueueFactory = null;
    
    int argsLength = args.length;
    if (argsLength > 3) {
      taskQueueFactory = (EventLoopTaskQueueFactory)args[3];
    }
    if (argsLength > 4) {
      tailTaskQueueFactory = (EventLoopTaskQueueFactory)args[4];
    }
    return new EpollEventLoop(this, executor, maxEvents.intValue(), selectStrategyFactory
      .newSelectStrategy(), rejectedExecutionHandler, taskQueueFactory, tailTaskQueueFactory);
  }
}
