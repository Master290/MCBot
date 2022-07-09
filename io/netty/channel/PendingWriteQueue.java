package io.netty.channel;

import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.PromiseCombiner;
import io.netty.util.internal.ObjectPool;
import io.netty.util.internal.ObjectPool.Handle;
import io.netty.util.internal.ObjectPool.ObjectCreator;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;



















public final class PendingWriteQueue
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(PendingWriteQueue.class);
  




  private static final int PENDING_WRITE_OVERHEAD = SystemPropertyUtil.getInt("io.netty.transport.pendingWriteSizeOverhead", 64);
  
  private final ChannelOutboundInvoker invoker;
  
  private final EventExecutor executor;
  private final PendingBytesTracker tracker;
  private PendingWrite head;
  private PendingWrite tail;
  private int size;
  private long bytes;
  
  public PendingWriteQueue(ChannelHandlerContext ctx)
  {
    tracker = PendingBytesTracker.newTracker(ctx.channel());
    invoker = ctx;
    executor = ctx.executor();
  }
  
  public PendingWriteQueue(Channel channel) {
    tracker = PendingBytesTracker.newTracker(channel);
    invoker = channel;
    executor = channel.eventLoop();
  }
  


  public boolean isEmpty()
  {
    assert (executor.inEventLoop());
    return head == null;
  }
  


  public int size()
  {
    assert (executor.inEventLoop());
    return size;
  }
  



  public long bytes()
  {
    assert (executor.inEventLoop());
    return bytes;
  }
  

  private int size(Object msg)
  {
    int messageSize = tracker.size(msg);
    if (messageSize < 0)
    {
      messageSize = 0;
    }
    return messageSize + PENDING_WRITE_OVERHEAD;
  }
  


  public void add(Object msg, ChannelPromise promise)
  {
    assert (executor.inEventLoop());
    ObjectUtil.checkNotNull(msg, "msg");
    ObjectUtil.checkNotNull(promise, "promise");
    

    int messageSize = size(msg);
    
    PendingWrite write = PendingWrite.newInstance(msg, messageSize, promise);
    PendingWrite currentTail = tail;
    if (currentTail == null) {
      tail = (this.head = write);
    } else {
      next = write;
      tail = write;
    }
    size += 1;
    bytes += messageSize;
    tracker.incrementPendingOutboundBytes(size);
  }
  






  public ChannelFuture removeAndWriteAll()
  {
    assert (executor.inEventLoop());
    
    if (isEmpty()) {
      return null;
    }
    
    ChannelPromise p = invoker.newPromise();
    PromiseCombiner combiner = new PromiseCombiner(executor);
    
    try
    {
      for (PendingWrite write = head; write != null; write = head) {
        head = (this.tail = null);
        size = 0;
        bytes = 0L;
        
        while (write != null) {
          PendingWrite next = next;
          Object msg = msg;
          ChannelPromise promise = promise;
          recycle(write, false);
          if (!(promise instanceof VoidChannelPromise)) {
            combiner.add(promise);
          }
          invoker.write(msg, promise);
          write = next;
        }
      }
      combiner.finish(p);
    } catch (Throwable cause) {
      p.setFailure(cause);
    }
    assertEmpty();
    return p;
  }
  



  public void removeAndFailAll(Throwable cause)
  {
    assert (executor.inEventLoop());
    ObjectUtil.checkNotNull(cause, "cause");
    

    for (PendingWrite write = head; write != null; write = head) {
      head = (this.tail = null);
      size = 0;
      bytes = 0L;
      while (write != null) {
        PendingWrite next = next;
        ReferenceCountUtil.safeRelease(msg);
        ChannelPromise promise = promise;
        recycle(write, false);
        safeFail(promise, cause);
        write = next;
      }
    }
    assertEmpty();
  }
  



  public void removeAndFail(Throwable cause)
  {
    assert (executor.inEventLoop());
    ObjectUtil.checkNotNull(cause, "cause");
    
    PendingWrite write = head;
    if (write == null) {
      return;
    }
    ReferenceCountUtil.safeRelease(msg);
    ChannelPromise promise = promise;
    safeFail(promise, cause);
    recycle(write, true);
  }
  
  private void assertEmpty() {
    assert ((tail == null) && (head == null) && (size == 0));
  }
  






  public ChannelFuture removeAndWrite()
  {
    assert (executor.inEventLoop());
    PendingWrite write = head;
    if (write == null) {
      return null;
    }
    Object msg = msg;
    ChannelPromise promise = promise;
    recycle(write, true);
    return invoker.write(msg, promise);
  }
  





  public ChannelPromise remove()
  {
    assert (executor.inEventLoop());
    PendingWrite write = head;
    if (write == null) {
      return null;
    }
    ChannelPromise promise = promise;
    ReferenceCountUtil.safeRelease(msg);
    recycle(write, true);
    return promise;
  }
  


  public Object current()
  {
    assert (executor.inEventLoop());
    PendingWrite write = head;
    if (write == null) {
      return null;
    }
    return msg;
  }
  
  private void recycle(PendingWrite write, boolean update) {
    PendingWrite next = next;
    long writeSize = size;
    
    if (update) {
      if (next == null)
      {

        head = (this.tail = null);
        size = 0;
        bytes = 0L;
      } else {
        head = next;
        size -= 1;
        bytes -= writeSize;
        assert ((size > 0) && (bytes >= 0L));
      }
    }
    
    write.recycle();
    tracker.decrementPendingOutboundBytes(writeSize);
  }
  
  private static void safeFail(ChannelPromise promise, Throwable cause) {
    if ((!(promise instanceof VoidChannelPromise)) && (!promise.tryFailure(cause))) {
      logger.warn("Failed to mark a promise as failure because it's done already: {}", promise, cause);
    }
  }
  


  static final class PendingWrite
  {
    private static final ObjectPool<PendingWrite> RECYCLER = ObjectPool.newPool(new ObjectPool.ObjectCreator()
    {
      public PendingWriteQueue.PendingWrite newObject(ObjectPool.Handle<PendingWriteQueue.PendingWrite> handle) {
        return new PendingWriteQueue.PendingWrite(handle, null);
      }
    });
    
    private final ObjectPool.Handle<PendingWrite> handle;
    
    private PendingWrite next;
    
    private long size;
    
    private ChannelPromise promise;
    
    private Object msg;
    
    private PendingWrite(ObjectPool.Handle<PendingWrite> handle)
    {
      this.handle = handle;
    }
    
    static PendingWrite newInstance(Object msg, int size, ChannelPromise promise) {
      PendingWrite write = (PendingWrite)RECYCLER.get();
      size = size;
      msg = msg;
      promise = promise;
      return write;
    }
    
    private void recycle() {
      size = 0L;
      next = null;
      msg = null;
      promise = null;
      handle.recycle(this);
    }
  }
}
