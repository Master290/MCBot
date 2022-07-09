package io.netty.channel;

import io.netty.util.internal.ObjectUtil;
import java.util.ArrayDeque;
import java.util.Queue;























public final class ChannelFlushPromiseNotifier
{
  private long writeCounter;
  private final Queue<FlushCheckpoint> flushCheckpoints = new ArrayDeque();
  



  private final boolean tryNotify;
  



  public ChannelFlushPromiseNotifier(boolean tryNotify)
  {
    this.tryNotify = tryNotify;
  }
  



  public ChannelFlushPromiseNotifier()
  {
    this(false);
  }
  


  @Deprecated
  public ChannelFlushPromiseNotifier add(ChannelPromise promise, int pendingDataSize)
  {
    return add(promise, pendingDataSize);
  }
  



  public ChannelFlushPromiseNotifier add(ChannelPromise promise, long pendingDataSize)
  {
    ObjectUtil.checkNotNull(promise, "promise");
    ObjectUtil.checkPositiveOrZero(pendingDataSize, "pendingDataSize");
    long checkpoint = writeCounter + pendingDataSize;
    if ((promise instanceof FlushCheckpoint)) {
      FlushCheckpoint cp = (FlushCheckpoint)promise;
      cp.flushCheckpoint(checkpoint);
      flushCheckpoints.add(cp);
    } else {
      flushCheckpoints.add(new DefaultFlushCheckpoint(checkpoint, promise));
    }
    return this;
  }
  

  public ChannelFlushPromiseNotifier increaseWriteCounter(long delta)
  {
    ObjectUtil.checkPositiveOrZero(delta, "delta");
    writeCounter += delta;
    return this;
  }
  


  public long writeCounter()
  {
    return writeCounter;
  }
  






  public ChannelFlushPromiseNotifier notifyPromises()
  {
    notifyPromises0(null);
    return this;
  }
  


  @Deprecated
  public ChannelFlushPromiseNotifier notifyFlushFutures()
  {
    return notifyPromises();
  }
  










  public ChannelFlushPromiseNotifier notifyPromises(Throwable cause)
  {
    notifyPromises();
    for (;;) {
      FlushCheckpoint cp = (FlushCheckpoint)flushCheckpoints.poll();
      if (cp == null) {
        break;
      }
      if (tryNotify) {
        cp.promise().tryFailure(cause);
      } else {
        cp.promise().setFailure(cause);
      }
    }
    return this;
  }
  


  @Deprecated
  public ChannelFlushPromiseNotifier notifyFlushFutures(Throwable cause)
  {
    return notifyPromises(cause);
  }
  















  public ChannelFlushPromiseNotifier notifyPromises(Throwable cause1, Throwable cause2)
  {
    notifyPromises0(cause1);
    for (;;) {
      FlushCheckpoint cp = (FlushCheckpoint)flushCheckpoints.poll();
      if (cp == null) {
        break;
      }
      if (tryNotify) {
        cp.promise().tryFailure(cause2);
      } else {
        cp.promise().setFailure(cause2);
      }
    }
    return this;
  }
  


  @Deprecated
  public ChannelFlushPromiseNotifier notifyFlushFutures(Throwable cause1, Throwable cause2)
  {
    return notifyPromises(cause1, cause2);
  }
  
  private void notifyPromises0(Throwable cause) {
    if (flushCheckpoints.isEmpty()) {
      this.writeCounter = 0L;
      return;
    }
    
    long writeCounter = this.writeCounter;
    for (;;) {
      FlushCheckpoint cp = (FlushCheckpoint)flushCheckpoints.peek();
      if (cp == null)
      {
        this.writeCounter = 0L;
        break;
      }
      
      if (cp.flushCheckpoint() > writeCounter) {
        if ((writeCounter <= 0L) || (flushCheckpoints.size() != 1)) break;
        this.writeCounter = 0L;
        cp.flushCheckpoint(cp.flushCheckpoint() - writeCounter); break;
      }
      


      flushCheckpoints.remove();
      ChannelPromise promise = cp.promise();
      if (cause == null) {
        if (tryNotify) {
          promise.trySuccess();
        } else {
          promise.setSuccess();
        }
      }
      else if (tryNotify) {
        promise.tryFailure(cause);
      } else {
        promise.setFailure(cause);
      }
    }
    


    long newWriteCounter = this.writeCounter;
    if (newWriteCounter >= 549755813888L)
    {

      this.writeCounter = 0L;
      for (FlushCheckpoint cp : flushCheckpoints) {
        cp.flushCheckpoint(cp.flushCheckpoint() - newWriteCounter);
      }
    }
  }
  

  private static class DefaultFlushCheckpoint
    implements ChannelFlushPromiseNotifier.FlushCheckpoint
  {
    private long checkpoint;
    
    private final ChannelPromise future;
    

    DefaultFlushCheckpoint(long checkpoint, ChannelPromise future)
    {
      this.checkpoint = checkpoint;
      this.future = future;
    }
    
    public long flushCheckpoint()
    {
      return checkpoint;
    }
    
    public void flushCheckpoint(long checkpoint)
    {
      this.checkpoint = checkpoint;
    }
    
    public ChannelPromise promise()
    {
      return future;
    }
  }
  
  static abstract interface FlushCheckpoint
  {
    public abstract long flushCheckpoint();
    
    public abstract void flushCheckpoint(long paramLong);
    
    public abstract ChannelPromise promise();
  }
}
