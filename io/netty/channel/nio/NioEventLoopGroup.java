package io.netty.channel.nio;

import io.netty.channel.DefaultSelectStrategyFactory;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopTaskQueueFactory;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.channel.SelectStrategyFactory;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.EventExecutorChooserFactory;
import io.netty.util.concurrent.RejectedExecutionHandler;
import io.netty.util.concurrent.RejectedExecutionHandlers;
import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

























public class NioEventLoopGroup
  extends MultithreadEventLoopGroup
{
  public NioEventLoopGroup()
  {
    this(0);
  }
  



  public NioEventLoopGroup(int nThreads)
  {
    this(nThreads, (Executor)null);
  }
  



  public NioEventLoopGroup(ThreadFactory threadFactory)
  {
    this(0, threadFactory, SelectorProvider.provider());
  }
  



  public NioEventLoopGroup(int nThreads, ThreadFactory threadFactory)
  {
    this(nThreads, threadFactory, SelectorProvider.provider());
  }
  
  public NioEventLoopGroup(int nThreads, Executor executor) {
    this(nThreads, executor, SelectorProvider.provider());
  }
  




  public NioEventLoopGroup(int nThreads, ThreadFactory threadFactory, SelectorProvider selectorProvider)
  {
    this(nThreads, threadFactory, selectorProvider, DefaultSelectStrategyFactory.INSTANCE);
  }
  
  public NioEventLoopGroup(int nThreads, ThreadFactory threadFactory, SelectorProvider selectorProvider, SelectStrategyFactory selectStrategyFactory)
  {
    super(nThreads, threadFactory, new Object[] { selectorProvider, selectStrategyFactory, RejectedExecutionHandlers.reject() });
  }
  
  public NioEventLoopGroup(int nThreads, Executor executor, SelectorProvider selectorProvider)
  {
    this(nThreads, executor, selectorProvider, DefaultSelectStrategyFactory.INSTANCE);
  }
  
  public NioEventLoopGroup(int nThreads, Executor executor, SelectorProvider selectorProvider, SelectStrategyFactory selectStrategyFactory)
  {
    super(nThreads, executor, new Object[] { selectorProvider, selectStrategyFactory, RejectedExecutionHandlers.reject() });
  }
  

  public NioEventLoopGroup(int nThreads, Executor executor, EventExecutorChooserFactory chooserFactory, SelectorProvider selectorProvider, SelectStrategyFactory selectStrategyFactory)
  {
    super(nThreads, executor, chooserFactory, new Object[] { selectorProvider, selectStrategyFactory, 
      RejectedExecutionHandlers.reject() });
  }
  


  public NioEventLoopGroup(int nThreads, Executor executor, EventExecutorChooserFactory chooserFactory, SelectorProvider selectorProvider, SelectStrategyFactory selectStrategyFactory, RejectedExecutionHandler rejectedExecutionHandler)
  {
    super(nThreads, executor, chooserFactory, new Object[] { selectorProvider, selectStrategyFactory, rejectedExecutionHandler });
  }
  



  public NioEventLoopGroup(int nThreads, Executor executor, EventExecutorChooserFactory chooserFactory, SelectorProvider selectorProvider, SelectStrategyFactory selectStrategyFactory, RejectedExecutionHandler rejectedExecutionHandler, EventLoopTaskQueueFactory taskQueueFactory)
  {
    super(nThreads, executor, chooserFactory, new Object[] { selectorProvider, selectStrategyFactory, rejectedExecutionHandler, taskQueueFactory });
  }
  



















  public NioEventLoopGroup(int nThreads, Executor executor, EventExecutorChooserFactory chooserFactory, SelectorProvider selectorProvider, SelectStrategyFactory selectStrategyFactory, RejectedExecutionHandler rejectedExecutionHandler, EventLoopTaskQueueFactory taskQueueFactory, EventLoopTaskQueueFactory tailTaskQueueFactory)
  {
    super(nThreads, executor, chooserFactory, new Object[] { selectorProvider, selectStrategyFactory, rejectedExecutionHandler, taskQueueFactory, tailTaskQueueFactory });
  }
  




  public void setIoRatio(int ioRatio)
  {
    for (EventExecutor e : this) {
      ((NioEventLoop)e).setIoRatio(ioRatio);
    }
  }
  



  public void rebuildSelectors()
  {
    for (EventExecutor e : this) {
      ((NioEventLoop)e).rebuildSelector();
    }
  }
  
  protected EventLoop newChild(Executor executor, Object... args) throws Exception
  {
    SelectorProvider selectorProvider = (SelectorProvider)args[0];
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
    return new NioEventLoop(this, executor, selectorProvider, selectStrategyFactory
      .newSelectStrategy(), rejectedExecutionHandler, taskQueueFactory, tailTaskQueueFactory);
  }
}
