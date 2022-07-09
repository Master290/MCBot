package io.netty.channel;

import io.netty.util.concurrent.AbstractEventExecutor.LazyRunnable;
import io.netty.util.concurrent.RejectedExecutionHandler;
import io.netty.util.concurrent.RejectedExecutionHandlers;
import io.netty.util.concurrent.SingleThreadEventExecutor;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.SystemPropertyUtil;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;



















public abstract class SingleThreadEventLoop
  extends SingleThreadEventExecutor
  implements EventLoop
{
  protected static final int DEFAULT_MAX_PENDING_TASKS = Math.max(16, 
    SystemPropertyUtil.getInt("io.netty.eventLoop.maxPendingTasks", Integer.MAX_VALUE));
  private final Queue<Runnable> tailTasks;
  
  protected SingleThreadEventLoop(EventLoopGroup parent, ThreadFactory threadFactory, boolean addTaskWakesUp)
  {
    this(parent, threadFactory, addTaskWakesUp, DEFAULT_MAX_PENDING_TASKS, RejectedExecutionHandlers.reject());
  }
  
  protected SingleThreadEventLoop(EventLoopGroup parent, Executor executor, boolean addTaskWakesUp) {
    this(parent, executor, addTaskWakesUp, DEFAULT_MAX_PENDING_TASKS, RejectedExecutionHandlers.reject());
  }
  

  protected SingleThreadEventLoop(EventLoopGroup parent, ThreadFactory threadFactory, boolean addTaskWakesUp, int maxPendingTasks, RejectedExecutionHandler rejectedExecutionHandler)
  {
    super(parent, threadFactory, addTaskWakesUp, maxPendingTasks, rejectedExecutionHandler);
    tailTasks = newTaskQueue(maxPendingTasks);
  }
  

  protected SingleThreadEventLoop(EventLoopGroup parent, Executor executor, boolean addTaskWakesUp, int maxPendingTasks, RejectedExecutionHandler rejectedExecutionHandler)
  {
    super(parent, executor, addTaskWakesUp, maxPendingTasks, rejectedExecutionHandler);
    tailTasks = newTaskQueue(maxPendingTasks);
  }
  

  protected SingleThreadEventLoop(EventLoopGroup parent, Executor executor, boolean addTaskWakesUp, Queue<Runnable> taskQueue, Queue<Runnable> tailTaskQueue, RejectedExecutionHandler rejectedExecutionHandler)
  {
    super(parent, executor, addTaskWakesUp, taskQueue, rejectedExecutionHandler);
    tailTasks = ((Queue)ObjectUtil.checkNotNull(tailTaskQueue, "tailTaskQueue"));
  }
  
  public EventLoopGroup parent()
  {
    return (EventLoopGroup)super.parent();
  }
  
  public EventLoop next()
  {
    return (EventLoop)super.next();
  }
  
  public ChannelFuture register(Channel channel)
  {
    return register(new DefaultChannelPromise(channel, this));
  }
  
  public ChannelFuture register(ChannelPromise promise)
  {
    ObjectUtil.checkNotNull(promise, "promise");
    promise.channel().unsafe().register(this, promise);
    return promise;
  }
  
  @Deprecated
  public ChannelFuture register(Channel channel, ChannelPromise promise)
  {
    ObjectUtil.checkNotNull(promise, "promise");
    ObjectUtil.checkNotNull(channel, "channel");
    channel.unsafe().register(this, promise);
    return promise;
  }
  





  public final void executeAfterEventLoopIteration(Runnable task)
  {
    ObjectUtil.checkNotNull(task, "task");
    if (isShutdown()) {
      reject();
    }
    
    if (!tailTasks.offer(task)) {
      reject(task);
    }
    
    if ((!(task instanceof AbstractEventExecutor.LazyRunnable)) && (wakesUpForTask(task))) {
      wakeup(inEventLoop());
    }
  }
  







  final boolean removeAfterEventLoopIterationTask(Runnable task)
  {
    return tailTasks.remove(ObjectUtil.checkNotNull(task, "task"));
  }
  
  protected void afterRunningAllTasks()
  {
    runAllTasksFrom(tailTasks);
  }
  
  protected boolean hasTasks()
  {
    return (super.hasTasks()) || (!tailTasks.isEmpty());
  }
  
  public int pendingTasks()
  {
    return super.pendingTasks() + tailTasks.size();
  }
  





  public int registeredChannels()
  {
    return -1;
  }
}
