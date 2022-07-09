package io.netty.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.util.ArrayDeque;



















public abstract class AbstractCoalescingBufferQueue
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractCoalescingBufferQueue.class);
  

  private final ArrayDeque<Object> bufAndListenerPairs;
  

  private final PendingBytesTracker tracker;
  
  private int readableBytes;
  

  protected AbstractCoalescingBufferQueue(Channel channel, int initSize)
  {
    bufAndListenerPairs = new ArrayDeque(initSize);
    tracker = (channel == null ? null : PendingBytesTracker.newTracker(channel));
  }
  





  public final void addFirst(ByteBuf buf, ChannelPromise promise)
  {
    addFirst(buf, toChannelFutureListener(promise));
  }
  
  private void addFirst(ByteBuf buf, ChannelFutureListener listener) {
    if (listener != null) {
      bufAndListenerPairs.addFirst(listener);
    }
    bufAndListenerPairs.addFirst(buf);
    incrementReadableBytes(buf.readableBytes());
  }
  


  public final void add(ByteBuf buf)
  {
    add(buf, (ChannelFutureListener)null);
  }
  







  public final void add(ByteBuf buf, ChannelPromise promise)
  {
    add(buf, toChannelFutureListener(promise));
  }
  







  public final void add(ByteBuf buf, ChannelFutureListener listener)
  {
    bufAndListenerPairs.add(buf);
    if (listener != null) {
      bufAndListenerPairs.add(listener);
    }
    incrementReadableBytes(buf.readableBytes());
  }
  




  public final ByteBuf removeFirst(ChannelPromise aggregatePromise)
  {
    Object entry = bufAndListenerPairs.poll();
    if (entry == null) {
      return null;
    }
    assert ((entry instanceof ByteBuf));
    ByteBuf result = (ByteBuf)entry;
    
    decrementReadableBytes(result.readableBytes());
    
    entry = bufAndListenerPairs.peek();
    if ((entry instanceof ChannelFutureListener)) {
      aggregatePromise.addListener((ChannelFutureListener)entry);
      bufAndListenerPairs.poll();
    }
    return result;
  }
  










  public final ByteBuf remove(ByteBufAllocator alloc, int bytes, ChannelPromise aggregatePromise)
  {
    ObjectUtil.checkPositiveOrZero(bytes, "bytes");
    ObjectUtil.checkNotNull(aggregatePromise, "aggregatePromise");
    

    if (bufAndListenerPairs.isEmpty()) {
      assert (readableBytes == 0);
      return removeEmptyValue();
    }
    bytes = Math.min(bytes, readableBytes);
    
    ByteBuf toReturn = null;
    ByteBuf entryBuffer = null;
    int originalBytes = bytes;
    try {
      for (;;) {
        Object entry = bufAndListenerPairs.poll();
        if (entry == null) {
          break;
        }
        if ((entry instanceof ChannelFutureListener)) {
          aggregatePromise.addListener((ChannelFutureListener)entry);
        }
        else {
          entryBuffer = (ByteBuf)entry;
          if (entryBuffer.readableBytes() > bytes)
          {
            bufAndListenerPairs.addFirst(entryBuffer);
            if (bytes <= 0)
              break;
            entryBuffer = entryBuffer.readRetainedSlice(bytes);
            
            toReturn = toReturn == null ? composeFirst(alloc, entryBuffer) : compose(alloc, toReturn, entryBuffer);
            bytes = 0; break;
          }
          

          bytes -= entryBuffer.readableBytes();
          
          toReturn = toReturn == null ? composeFirst(alloc, entryBuffer) : compose(alloc, toReturn, entryBuffer);
          
          entryBuffer = null;
        }
      }
    } catch (Throwable cause) { ReferenceCountUtil.safeRelease(entryBuffer);
      ReferenceCountUtil.safeRelease(toReturn);
      aggregatePromise.setFailure(cause);
      PlatformDependent.throwException(cause);
    }
    decrementReadableBytes(originalBytes - bytes);
    return toReturn;
  }
  


  public final int readableBytes()
  {
    return readableBytes;
  }
  


  public final boolean isEmpty()
  {
    return bufAndListenerPairs.isEmpty();
  }
  


  public final void releaseAndFailAll(ChannelOutboundInvoker invoker, Throwable cause)
  {
    releaseAndCompleteAll(invoker.newFailedFuture(cause));
  }
  



  public final void copyTo(AbstractCoalescingBufferQueue dest)
  {
    bufAndListenerPairs.addAll(bufAndListenerPairs);
    dest.incrementReadableBytes(readableBytes);
  }
  



  public final void writeAndRemoveAll(ChannelHandlerContext ctx)
  {
    Throwable pending = null;
    ByteBuf previousBuf = null;
    for (;;) {
      Object entry = bufAndListenerPairs.poll();
      try {
        if (entry == null) {
          if (previousBuf != null) {
            decrementReadableBytes(previousBuf.readableBytes());
            ctx.write(previousBuf, ctx.voidPromise());
          }
          break;
        }
        
        if ((entry instanceof ByteBuf)) {
          if (previousBuf != null) {
            decrementReadableBytes(previousBuf.readableBytes());
            ctx.write(previousBuf, ctx.voidPromise());
          }
          previousBuf = (ByteBuf)entry;
        } else if ((entry instanceof ChannelPromise)) {
          decrementReadableBytes(previousBuf.readableBytes());
          ctx.write(previousBuf, (ChannelPromise)entry);
          previousBuf = null;
        } else {
          decrementReadableBytes(previousBuf.readableBytes());
          ctx.write(previousBuf).addListener((ChannelFutureListener)entry);
          previousBuf = null;
        }
      } catch (Throwable t) {
        if (pending == null) {
          pending = t;
        } else {
          logger.info("Throwable being suppressed because Throwable {} is already pending", pending, t);
        }
      }
    }
    if (pending != null) {
      throw new IllegalStateException(pending);
    }
  }
  
  public String toString()
  {
    return "bytes: " + readableBytes + " buffers: " + (size() >> 1);
  }
  




  protected abstract ByteBuf compose(ByteBufAllocator paramByteBufAllocator, ByteBuf paramByteBuf1, ByteBuf paramByteBuf2);
  



  protected final ByteBuf composeIntoComposite(ByteBufAllocator alloc, ByteBuf cumulation, ByteBuf next)
  {
    CompositeByteBuf composite = alloc.compositeBuffer(size() + 2);
    try {
      composite.addComponent(true, cumulation);
      composite.addComponent(true, next);
    } catch (Throwable cause) {
      composite.release();
      ReferenceCountUtil.safeRelease(next);
      PlatformDependent.throwException(cause);
    }
    return composite;
  }
  






  protected final ByteBuf copyAndCompose(ByteBufAllocator alloc, ByteBuf cumulation, ByteBuf next)
  {
    ByteBuf newCumulation = alloc.ioBuffer(cumulation.readableBytes() + next.readableBytes());
    try {
      newCumulation.writeBytes(cumulation).writeBytes(next);
    } catch (Throwable cause) {
      newCumulation.release();
      ReferenceCountUtil.safeRelease(next);
      PlatformDependent.throwException(cause);
    }
    cumulation.release();
    next.release();
    return newCumulation;
  }
  



  protected ByteBuf composeFirst(ByteBufAllocator allocator, ByteBuf first)
  {
    return first;
  }
  




  protected abstract ByteBuf removeEmptyValue();
  



  protected final int size()
  {
    return bufAndListenerPairs.size();
  }
  
  private void releaseAndCompleteAll(ChannelFuture future) {
    Throwable pending = null;
    for (;;) {
      Object entry = bufAndListenerPairs.poll();
      if (entry == null) {
        break;
      }
      try {
        if ((entry instanceof ByteBuf)) {
          ByteBuf buffer = (ByteBuf)entry;
          decrementReadableBytes(buffer.readableBytes());
          ReferenceCountUtil.safeRelease(buffer);
        } else {
          ((ChannelFutureListener)entry).operationComplete(future);
        }
      } catch (Throwable t) {
        if (pending == null) {
          pending = t;
        } else {
          logger.info("Throwable being suppressed because Throwable {} is already pending", pending, t);
        }
      }
    }
    if (pending != null) {
      throw new IllegalStateException(pending);
    }
  }
  
  private void incrementReadableBytes(int increment) {
    int nextReadableBytes = readableBytes + increment;
    if (nextReadableBytes < readableBytes) {
      throw new IllegalStateException("buffer queue length overflow: " + readableBytes + " + " + increment);
    }
    readableBytes = nextReadableBytes;
    if (tracker != null) {
      tracker.incrementPendingOutboundBytes(increment);
    }
  }
  
  private void decrementReadableBytes(int decrement) {
    readableBytes -= decrement;
    assert (readableBytes >= 0);
    if (tracker != null) {
      tracker.decrementPendingOutboundBytes(decrement);
    }
  }
  
  private static ChannelFutureListener toChannelFutureListener(ChannelPromise promise) {
    return promise.isVoid() ? null : new DelegatingChannelPromiseNotifier(promise);
  }
}
