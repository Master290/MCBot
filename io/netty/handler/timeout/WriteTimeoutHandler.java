package io.netty.handler.timeout;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.internal.ObjectUtil;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;




















































public class WriteTimeoutHandler
  extends ChannelOutboundHandlerAdapter
{
  private static final long MIN_TIMEOUT_NANOS = TimeUnit.MILLISECONDS.toNanos(1L);
  


  private final long timeoutNanos;
  


  private WriteTimeoutTask lastTask;
  


  private boolean closed;
  


  public WriteTimeoutHandler(int timeoutSeconds)
  {
    this(timeoutSeconds, TimeUnit.SECONDS);
  }
  







  public WriteTimeoutHandler(long timeout, TimeUnit unit)
  {
    ObjectUtil.checkNotNull(unit, "unit");
    
    if (timeout <= 0L) {
      timeoutNanos = 0L;
    } else {
      timeoutNanos = Math.max(unit.toNanos(timeout), MIN_TIMEOUT_NANOS);
    }
  }
  
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception
  {
    if (timeoutNanos > 0L) {
      promise = promise.unvoid();
      scheduleTimeout(ctx, promise);
    }
    ctx.write(msg, promise);
  }
  
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception
  {
    assert (ctx.executor().inEventLoop());
    WriteTimeoutTask task = lastTask;
    lastTask = null;
    while (task != null) {
      assert (ctx.executor().inEventLoop());
      scheduledFuture.cancel(false);
      WriteTimeoutTask prev = prev;
      prev = null;
      next = null;
      task = prev;
    }
  }
  
  private void scheduleTimeout(ChannelHandlerContext ctx, ChannelPromise promise)
  {
    WriteTimeoutTask task = new WriteTimeoutTask(ctx, promise);
    scheduledFuture = ctx.executor().schedule(task, timeoutNanos, TimeUnit.NANOSECONDS);
    
    if (!scheduledFuture.isDone()) {
      addWriteTimeoutTask(task);
      

      promise.addListener(task);
    }
  }
  
  private void addWriteTimeoutTask(WriteTimeoutTask task) {
    assert (ctx.executor().inEventLoop());
    if (lastTask != null) {
      lastTask.next = task;
      prev = lastTask;
    }
    lastTask = task;
  }
  
  private void removeWriteTimeoutTask(WriteTimeoutTask task) {
    assert (ctx.executor().inEventLoop());
    if (task == lastTask)
    {
      assert (next == null);
      lastTask = lastTask.prev;
      if (lastTask != null)
        lastTask.next = null;
    } else {
      if ((prev == null) && (next == null))
      {
        return; }
      if (prev == null)
      {
        next.prev = null;
      } else {
        prev.next = next;
        next.prev = prev;
      } }
    prev = null;
    next = null;
  }
  

  protected void writeTimedOut(ChannelHandlerContext ctx)
    throws Exception
  {
    if (!closed) {
      ctx.fireExceptionCaught(WriteTimeoutException.INSTANCE);
      ctx.close();
      closed = true;
    }
  }
  

  private final class WriteTimeoutTask
    implements Runnable, ChannelFutureListener
  {
    private final ChannelHandlerContext ctx;
    private final ChannelPromise promise;
    WriteTimeoutTask prev;
    WriteTimeoutTask next;
    ScheduledFuture<?> scheduledFuture;
    
    WriteTimeoutTask(ChannelHandlerContext ctx, ChannelPromise promise)
    {
      this.ctx = ctx;
      this.promise = promise;
    }
    



    public void run()
    {
      if (!promise.isDone()) {
        try {
          writeTimedOut(ctx);
        } catch (Throwable t) {
          ctx.fireExceptionCaught(t);
        }
      }
      WriteTimeoutHandler.this.removeWriteTimeoutTask(this);
    }
    
    public void operationComplete(ChannelFuture future)
      throws Exception
    {
      scheduledFuture.cancel(false);
      


      if (ctx.executor().inEventLoop()) {
        WriteTimeoutHandler.this.removeWriteTimeoutTask(this);

      }
      else
      {

        assert (promise.isDone());
        ctx.executor().execute(this);
      }
    }
  }
}
