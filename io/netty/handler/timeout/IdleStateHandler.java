package io.netty.handler.timeout;

import io.netty.channel.Channel;
import io.netty.channel.Channel.Unsafe;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.internal.ObjectUtil;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


















































































public class IdleStateHandler
  extends ChannelDuplexHandler
{
  private static final long MIN_TIMEOUT_NANOS = TimeUnit.MILLISECONDS.toNanos(1L);
  

  private final ChannelFutureListener writeListener = new ChannelFutureListener()
  {
    public void operationComplete(ChannelFuture future) throws Exception {
      lastWriteTime = ticksInNanos();
      firstWriterIdleEvent = IdleStateHandler.access$202(IdleStateHandler.this, true);
    }
  };
  
  private final boolean observeOutput;
  
  private final long readerIdleTimeNanos;
  private final long writerIdleTimeNanos;
  private final long allIdleTimeNanos;
  private ScheduledFuture<?> readerIdleTimeout;
  private long lastReadTime;
  private boolean firstReaderIdleEvent = true;
  
  private ScheduledFuture<?> writerIdleTimeout;
  private long lastWriteTime;
  private boolean firstWriterIdleEvent = true;
  
  private ScheduledFuture<?> allIdleTimeout;
  private boolean firstAllIdleEvent = true;
  


  private byte state;
  


  private boolean reading;
  


  private long lastChangeCheckTimeStamp;
  


  private int lastMessageHashCode;
  


  private long lastPendingWriteBytes;
  


  private long lastFlushProgress;
  



  public IdleStateHandler(int readerIdleTimeSeconds, int writerIdleTimeSeconds, int allIdleTimeSeconds)
  {
    this(readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds, TimeUnit.SECONDS);
  }
  





  public IdleStateHandler(long readerIdleTime, long writerIdleTime, long allIdleTime, TimeUnit unit)
  {
    this(false, readerIdleTime, writerIdleTime, allIdleTime, unit);
  }
  























  public IdleStateHandler(boolean observeOutput, long readerIdleTime, long writerIdleTime, long allIdleTime, TimeUnit unit)
  {
    ObjectUtil.checkNotNull(unit, "unit");
    
    this.observeOutput = observeOutput;
    
    if (readerIdleTime <= 0L) {
      readerIdleTimeNanos = 0L;
    } else {
      readerIdleTimeNanos = Math.max(unit.toNanos(readerIdleTime), MIN_TIMEOUT_NANOS);
    }
    if (writerIdleTime <= 0L) {
      writerIdleTimeNanos = 0L;
    } else {
      writerIdleTimeNanos = Math.max(unit.toNanos(writerIdleTime), MIN_TIMEOUT_NANOS);
    }
    if (allIdleTime <= 0L) {
      allIdleTimeNanos = 0L;
    } else {
      allIdleTimeNanos = Math.max(unit.toNanos(allIdleTime), MIN_TIMEOUT_NANOS);
    }
  }
  



  public long getReaderIdleTimeInMillis()
  {
    return TimeUnit.NANOSECONDS.toMillis(readerIdleTimeNanos);
  }
  



  public long getWriterIdleTimeInMillis()
  {
    return TimeUnit.NANOSECONDS.toMillis(writerIdleTimeNanos);
  }
  



  public long getAllIdleTimeInMillis()
  {
    return TimeUnit.NANOSECONDS.toMillis(allIdleTimeNanos);
  }
  
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception
  {
    if ((ctx.channel().isActive()) && (ctx.channel().isRegistered()))
    {

      initialize(ctx);
    }
  }
  


  public void handlerRemoved(ChannelHandlerContext ctx)
    throws Exception
  {
    destroy();
  }
  
  public void channelRegistered(ChannelHandlerContext ctx)
    throws Exception
  {
    if (ctx.channel().isActive()) {
      initialize(ctx);
    }
    super.channelRegistered(ctx);
  }
  


  public void channelActive(ChannelHandlerContext ctx)
    throws Exception
  {
    initialize(ctx);
    super.channelActive(ctx);
  }
  
  public void channelInactive(ChannelHandlerContext ctx) throws Exception
  {
    destroy();
    super.channelInactive(ctx);
  }
  
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
  {
    if ((readerIdleTimeNanos > 0L) || (allIdleTimeNanos > 0L)) {
      reading = true;
      firstReaderIdleEvent = (this.firstAllIdleEvent = 1);
    }
    ctx.fireChannelRead(msg);
  }
  
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception
  {
    if (((readerIdleTimeNanos > 0L) || (allIdleTimeNanos > 0L)) && (reading)) {
      lastReadTime = ticksInNanos();
      reading = false;
    }
    ctx.fireChannelReadComplete();
  }
  
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
    throws Exception
  {
    if ((writerIdleTimeNanos > 0L) || (allIdleTimeNanos > 0L)) {
      ctx.write(msg, promise.unvoid()).addListener(writeListener);
    } else {
      ctx.write(msg, promise);
    }
  }
  

  private void initialize(ChannelHandlerContext ctx)
  {
    switch (state) {
    case 1: 
    case 2: 
      return;
    }
    
    

    state = 1;
    initOutputChanged(ctx);
    
    lastReadTime = (this.lastWriteTime = ticksInNanos());
    if (readerIdleTimeNanos > 0L) {
      readerIdleTimeout = schedule(ctx, new ReaderIdleTimeoutTask(ctx), readerIdleTimeNanos, TimeUnit.NANOSECONDS);
    }
    
    if (writerIdleTimeNanos > 0L) {
      writerIdleTimeout = schedule(ctx, new WriterIdleTimeoutTask(ctx), writerIdleTimeNanos, TimeUnit.NANOSECONDS);
    }
    
    if (allIdleTimeNanos > 0L) {
      allIdleTimeout = schedule(ctx, new AllIdleTimeoutTask(ctx), allIdleTimeNanos, TimeUnit.NANOSECONDS);
    }
  }
  



  long ticksInNanos()
  {
    return System.nanoTime();
  }
  


  ScheduledFuture<?> schedule(ChannelHandlerContext ctx, Runnable task, long delay, TimeUnit unit)
  {
    return ctx.executor().schedule(task, delay, unit);
  }
  
  private void destroy() {
    state = 2;
    
    if (readerIdleTimeout != null) {
      readerIdleTimeout.cancel(false);
      readerIdleTimeout = null;
    }
    if (writerIdleTimeout != null) {
      writerIdleTimeout.cancel(false);
      writerIdleTimeout = null;
    }
    if (allIdleTimeout != null) {
      allIdleTimeout.cancel(false);
      allIdleTimeout = null;
    }
  }
  


  protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt)
    throws Exception
  {
    ctx.fireUserEventTriggered(evt);
  }
  


  protected IdleStateEvent newIdleStateEvent(IdleState state, boolean first)
  {
    switch (2.$SwitchMap$io$netty$handler$timeout$IdleState[state.ordinal()]) {
    case 1: 
      return first ? IdleStateEvent.FIRST_ALL_IDLE_STATE_EVENT : IdleStateEvent.ALL_IDLE_STATE_EVENT;
    case 2: 
      return first ? IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT : IdleStateEvent.READER_IDLE_STATE_EVENT;
    case 3: 
      return first ? IdleStateEvent.FIRST_WRITER_IDLE_STATE_EVENT : IdleStateEvent.WRITER_IDLE_STATE_EVENT;
    }
    throw new IllegalArgumentException("Unhandled: state=" + state + ", first=" + first);
  }
  



  private void initOutputChanged(ChannelHandlerContext ctx)
  {
    if (observeOutput) {
      Channel channel = ctx.channel();
      Channel.Unsafe unsafe = channel.unsafe();
      ChannelOutboundBuffer buf = unsafe.outboundBuffer();
      
      if (buf != null) {
        lastMessageHashCode = System.identityHashCode(buf.current());
        lastPendingWriteBytes = buf.totalPendingWriteBytes();
        lastFlushProgress = buf.currentProgress();
      }
    }
  }
  






  private boolean hasOutputChanged(ChannelHandlerContext ctx, boolean first)
  {
    if (observeOutput)
    {





      if (lastChangeCheckTimeStamp != lastWriteTime) {
        lastChangeCheckTimeStamp = lastWriteTime;
        

        if (!first) {
          return true;
        }
      }
      
      Channel channel = ctx.channel();
      Channel.Unsafe unsafe = channel.unsafe();
      ChannelOutboundBuffer buf = unsafe.outboundBuffer();
      
      if (buf != null) {
        int messageHashCode = System.identityHashCode(buf.current());
        long pendingWriteBytes = buf.totalPendingWriteBytes();
        
        if ((messageHashCode != lastMessageHashCode) || (pendingWriteBytes != lastPendingWriteBytes)) {
          lastMessageHashCode = messageHashCode;
          lastPendingWriteBytes = pendingWriteBytes;
          
          if (!first) {
            return true;
          }
        }
        
        long flushProgress = buf.currentProgress();
        if (flushProgress != lastFlushProgress) {
          lastFlushProgress = flushProgress;
          
          if (!first) {
            return true;
          }
        }
      }
    }
    
    return false;
  }
  
  private static abstract class AbstractIdleTask implements Runnable
  {
    private final ChannelHandlerContext ctx;
    
    AbstractIdleTask(ChannelHandlerContext ctx) {
      this.ctx = ctx;
    }
    
    public void run()
    {
      if (!ctx.channel().isOpen()) {
        return;
      }
      
      run(ctx);
    }
    
    protected abstract void run(ChannelHandlerContext paramChannelHandlerContext);
  }
  
  private final class ReaderIdleTimeoutTask extends IdleStateHandler.AbstractIdleTask
  {
    ReaderIdleTimeoutTask(ChannelHandlerContext ctx) {
      super();
    }
    
    protected void run(ChannelHandlerContext ctx)
    {
      long nextDelay = readerIdleTimeNanos;
      if (!reading) {
        nextDelay -= ticksInNanos() - lastReadTime;
      }
      
      if (nextDelay <= 0L)
      {
        readerIdleTimeout = schedule(ctx, this, readerIdleTimeNanos, TimeUnit.NANOSECONDS);
        
        boolean first = firstReaderIdleEvent;
        firstReaderIdleEvent = false;
        try
        {
          IdleStateEvent event = newIdleStateEvent(IdleState.READER_IDLE, first);
          channelIdle(ctx, event);
        } catch (Throwable t) {
          ctx.fireExceptionCaught(t);
        }
      }
      else {
        readerIdleTimeout = schedule(ctx, this, nextDelay, TimeUnit.NANOSECONDS);
      }
    }
  }
  
  private final class WriterIdleTimeoutTask extends IdleStateHandler.AbstractIdleTask
  {
    WriterIdleTimeoutTask(ChannelHandlerContext ctx) {
      super();
    }
    

    protected void run(ChannelHandlerContext ctx)
    {
      long lastWriteTime = IdleStateHandler.this.lastWriteTime;
      long nextDelay = writerIdleTimeNanos - (ticksInNanos() - lastWriteTime);
      if (nextDelay <= 0L)
      {
        writerIdleTimeout = schedule(ctx, this, writerIdleTimeNanos, TimeUnit.NANOSECONDS);
        
        boolean first = firstWriterIdleEvent;
        firstWriterIdleEvent = false;
        try
        {
          if (IdleStateHandler.this.hasOutputChanged(ctx, first)) {
            return;
          }
          
          IdleStateEvent event = newIdleStateEvent(IdleState.WRITER_IDLE, first);
          channelIdle(ctx, event);
        } catch (Throwable t) {
          ctx.fireExceptionCaught(t);
        }
      }
      else {
        writerIdleTimeout = schedule(ctx, this, nextDelay, TimeUnit.NANOSECONDS);
      }
    }
  }
  
  private final class AllIdleTimeoutTask extends IdleStateHandler.AbstractIdleTask
  {
    AllIdleTimeoutTask(ChannelHandlerContext ctx) {
      super();
    }
    

    protected void run(ChannelHandlerContext ctx)
    {
      long nextDelay = allIdleTimeNanos;
      if (!reading) {
        nextDelay -= ticksInNanos() - Math.max(lastReadTime, lastWriteTime);
      }
      if (nextDelay <= 0L)
      {

        allIdleTimeout = schedule(ctx, this, allIdleTimeNanos, TimeUnit.NANOSECONDS);
        
        boolean first = firstAllIdleEvent;
        firstAllIdleEvent = false;
        try
        {
          if (IdleStateHandler.this.hasOutputChanged(ctx, first)) {
            return;
          }
          
          IdleStateEvent event = newIdleStateEvent(IdleState.ALL_IDLE, first);
          channelIdle(ctx, event);
        } catch (Throwable t) {
          ctx.fireExceptionCaught(t);
        }
      }
      else
      {
        allIdleTimeout = schedule(ctx, this, nextDelay, TimeUnit.NANOSECONDS);
      }
    }
  }
}
