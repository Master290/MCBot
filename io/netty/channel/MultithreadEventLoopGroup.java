package io.netty.channel;

import io.netty.util.NettyRuntime;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorChooserFactory;
import io.netty.util.concurrent.MultithreadEventExecutorGroup;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;



















public abstract class MultithreadEventLoopGroup
  extends MultithreadEventExecutorGroup
  implements EventLoopGroup
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(MultithreadEventLoopGroup.class);
  



  private static final int DEFAULT_EVENT_LOOP_THREADS = Math.max(1, SystemPropertyUtil.getInt("io.netty.eventLoopThreads", 
    NettyRuntime.availableProcessors() * 2));
  
  static { if (logger.isDebugEnabled()) {
      logger.debug("-Dio.netty.eventLoopThreads: {}", Integer.valueOf(DEFAULT_EVENT_LOOP_THREADS));
    }
  }
  


  protected MultithreadEventLoopGroup(int nThreads, Executor executor, Object... args)
  {
    super(nThreads == 0 ? DEFAULT_EVENT_LOOP_THREADS : nThreads, executor, args);
  }
  


  protected MultithreadEventLoopGroup(int nThreads, ThreadFactory threadFactory, Object... args)
  {
    super(nThreads == 0 ? DEFAULT_EVENT_LOOP_THREADS : nThreads, threadFactory, args);
  }
  




  protected MultithreadEventLoopGroup(int nThreads, Executor executor, EventExecutorChooserFactory chooserFactory, Object... args)
  {
    super(nThreads == 0 ? DEFAULT_EVENT_LOOP_THREADS : nThreads, executor, chooserFactory, args);
  }
  
  protected ThreadFactory newDefaultThreadFactory()
  {
    return new DefaultThreadFactory(getClass(), 10);
  }
  
  public EventLoop next()
  {
    return (EventLoop)super.next();
  }
  



  public ChannelFuture register(Channel channel)
  {
    return next().register(channel);
  }
  
  public ChannelFuture register(ChannelPromise promise)
  {
    return next().register(promise);
  }
  
  @Deprecated
  public ChannelFuture register(Channel channel, ChannelPromise promise)
  {
    return next().register(channel, promise);
  }
  
  protected abstract EventLoop newChild(Executor paramExecutor, Object... paramVarArgs)
    throws Exception;
}
