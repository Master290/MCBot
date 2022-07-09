package io.netty.channel;

import io.netty.util.concurrent.AbstractEventExecutorGroup;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.ThreadPerTaskExecutor;
import io.netty.util.internal.EmptyArrays;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.ReadOnlyIterator;
import java.util.Collections;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;






















@Deprecated
public class ThreadPerChannelEventLoopGroup
  extends AbstractEventExecutorGroup
  implements EventLoopGroup
{
  private final Object[] childArgs;
  private final int maxChannels;
  final Executor executor;
  final Set<EventLoop> activeChildren = Collections.newSetFromMap(PlatformDependent.newConcurrentHashMap());
  final Queue<EventLoop> idleChildren = new ConcurrentLinkedQueue();
  
  private final ChannelException tooManyChannels;
  private volatile boolean shuttingDown;
  private final Promise<?> terminationFuture = new DefaultPromise(GlobalEventExecutor.INSTANCE);
  private final FutureListener<Object> childTerminationListener = new FutureListener()
  {
    public void operationComplete(Future<Object> future) throws Exception
    {
      if (isTerminated()) {
        terminationFuture.trySuccess(null);
      }
    }
  };
  


  protected ThreadPerChannelEventLoopGroup()
  {
    this(0);
  }
  








  protected ThreadPerChannelEventLoopGroup(int maxChannels)
  {
    this(maxChannels, (ThreadFactory)null, new Object[0]);
  }
  











  protected ThreadPerChannelEventLoopGroup(int maxChannels, ThreadFactory threadFactory, Object... args)
  {
    this(maxChannels, threadFactory == null ? null : new ThreadPerTaskExecutor(threadFactory), args);
  }
  











  protected ThreadPerChannelEventLoopGroup(int maxChannels, Executor executor, Object... args)
  {
    ObjectUtil.checkPositiveOrZero(maxChannels, "maxChannels");
    if (executor == null) {
      executor = new ThreadPerTaskExecutor(new DefaultThreadFactory(getClass()));
    }
    
    if (args == null) {
      childArgs = EmptyArrays.EMPTY_OBJECTS;
    } else {
      childArgs = ((Object[])args.clone());
    }
    
    this.maxChannels = maxChannels;
    this.executor = executor;
    

    tooManyChannels = ChannelException.newStatic("too many channels (max: " + maxChannels + ')', ThreadPerChannelEventLoopGroup.class, "nextChild()");
  }
  


  protected EventLoop newChild(Object... args)
    throws Exception
  {
    return new ThreadPerChannelEventLoop(this);
  }
  
  public Iterator<EventExecutor> iterator()
  {
    return new ReadOnlyIterator(activeChildren.iterator());
  }
  
  public EventLoop next()
  {
    throw new UnsupportedOperationException();
  }
  
  public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit)
  {
    shuttingDown = true;
    
    for (EventLoop l : activeChildren) {
      l.shutdownGracefully(quietPeriod, timeout, unit);
    }
    for (EventLoop l : idleChildren) {
      l.shutdownGracefully(quietPeriod, timeout, unit);
    }
    

    if (isTerminated()) {
      terminationFuture.trySuccess(null);
    }
    
    return terminationFuture();
  }
  
  public Future<?> terminationFuture()
  {
    return terminationFuture;
  }
  
  @Deprecated
  public void shutdown()
  {
    shuttingDown = true;
    
    for (EventLoop l : activeChildren) {
      l.shutdown();
    }
    for (EventLoop l : idleChildren) {
      l.shutdown();
    }
    

    if (isTerminated()) {
      terminationFuture.trySuccess(null);
    }
  }
  
  public boolean isShuttingDown()
  {
    for (EventLoop l : activeChildren) {
      if (!l.isShuttingDown()) {
        return false;
      }
    }
    for (EventLoop l : idleChildren) {
      if (!l.isShuttingDown()) {
        return false;
      }
    }
    return true;
  }
  
  public boolean isShutdown()
  {
    for (EventLoop l : activeChildren) {
      if (!l.isShutdown()) {
        return false;
      }
    }
    for (EventLoop l : idleChildren) {
      if (!l.isShutdown()) {
        return false;
      }
    }
    return true;
  }
  
  public boolean isTerminated()
  {
    for (EventLoop l : activeChildren) {
      if (!l.isTerminated()) {
        return false;
      }
    }
    for (EventLoop l : idleChildren) {
      if (!l.isTerminated()) {
        return false;
      }
    }
    return true;
  }
  
  public boolean awaitTermination(long timeout, TimeUnit unit)
    throws InterruptedException
  {
    long deadline = System.nanoTime() + unit.toNanos(timeout);
    for (EventLoop l : activeChildren) {
      for (;;) {
        long timeLeft = deadline - System.nanoTime();
        if (timeLeft <= 0L) {
          return isTerminated();
        }
        if (l.awaitTermination(timeLeft, TimeUnit.NANOSECONDS)) {
          break;
        }
      }
    }
    for (EventLoop l : idleChildren) {
      for (;;) {
        long timeLeft = deadline - System.nanoTime();
        if (timeLeft <= 0L) {
          return isTerminated();
        }
        if (l.awaitTermination(timeLeft, TimeUnit.NANOSECONDS)) {
          break;
        }
      }
    }
    return isTerminated();
  }
  
  public ChannelFuture register(Channel channel)
  {
    ObjectUtil.checkNotNull(channel, "channel");
    try {
      EventLoop l = nextChild();
      return l.register(new DefaultChannelPromise(channel, l));
    } catch (Throwable t) {
      return new FailedChannelFuture(channel, GlobalEventExecutor.INSTANCE, t);
    }
  }
  
  public ChannelFuture register(ChannelPromise promise)
  {
    try {
      return nextChild().register(promise);
    } catch (Throwable t) {
      promise.setFailure(t); }
    return promise;
  }
  

  @Deprecated
  public ChannelFuture register(Channel channel, ChannelPromise promise)
  {
    ObjectUtil.checkNotNull(channel, "channel");
    try {
      return nextChild().register(channel, promise);
    } catch (Throwable t) {
      promise.setFailure(t); }
    return promise;
  }
  
  private EventLoop nextChild() throws Exception
  {
    if (shuttingDown) {
      throw new RejectedExecutionException("shutting down");
    }
    
    EventLoop loop = (EventLoop)idleChildren.poll();
    if (loop == null) {
      if ((maxChannels > 0) && (activeChildren.size() >= maxChannels)) {
        throw tooManyChannels;
      }
      loop = newChild(childArgs);
      loop.terminationFuture().addListener(childTerminationListener);
    }
    activeChildren.add(loop);
    return loop;
  }
}
