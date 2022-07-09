package io.netty.handler.stream;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelProgressivePromise;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayDeque;
import java.util.Queue;


















































public class ChunkedWriteHandler
  extends ChannelDuplexHandler
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(ChunkedWriteHandler.class);
  
  private final Queue<PendingWrite> queue = new ArrayDeque();
  
  private volatile ChannelHandlerContext ctx;
  

  public ChunkedWriteHandler() {}
  

  @Deprecated
  public ChunkedWriteHandler(int maxPendingWrites)
  {
    ObjectUtil.checkPositive(maxPendingWrites, "maxPendingWrites");
  }
  
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception
  {
    this.ctx = ctx;
  }
  


  public void resumeTransfer()
  {
    final ChannelHandlerContext ctx = this.ctx;
    if (ctx == null) {
      return;
    }
    if (ctx.executor().inEventLoop()) {
      resumeTransfer0(ctx);
    }
    else {
      ctx.executor().execute(new Runnable()
      {
        public void run()
        {
          ChunkedWriteHandler.this.resumeTransfer0(ctx);
        }
      });
    }
  }
  
  private void resumeTransfer0(ChannelHandlerContext ctx) {
    try {
      doFlush(ctx);
    } catch (Exception e) {
      logger.warn("Unexpected exception while sending chunks.", e);
    }
  }
  
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception
  {
    queue.add(new PendingWrite(msg, promise));
  }
  
  public void flush(ChannelHandlerContext ctx) throws Exception
  {
    doFlush(ctx);
  }
  
  public void channelInactive(ChannelHandlerContext ctx) throws Exception
  {
    doFlush(ctx);
    ctx.fireChannelInactive();
  }
  
  public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception
  {
    if (ctx.channel().isWritable())
    {
      doFlush(ctx);
    }
    ctx.fireChannelWritabilityChanged();
  }
  
  private void discard(Throwable cause) {
    for (;;) {
      PendingWrite currentWrite = (PendingWrite)queue.poll();
      
      if (currentWrite == null) {
        break;
      }
      Object message = msg;
      if ((message instanceof ChunkedInput)) {
        ChunkedInput<?> in = (ChunkedInput)message;
        
        try
        {
          boolean endOfInput = in.isEndOfInput();
          long inputLength = in.length();
          closeInput(in);
        } catch (Exception e) {
          closeInput(in);
          currentWrite.fail(e);
          if (logger.isWarnEnabled())
            logger.warn(ChunkedInput.class.getSimpleName() + " failed", e);
        }
        continue;
        long inputLength;
        boolean endOfInput;
        if (!endOfInput) {
          if (cause == null) {
            cause = new ClosedChannelException();
          }
          currentWrite.fail(cause);
        } else {
          currentWrite.success(inputLength);
        }
      } else {
        if (cause == null) {
          cause = new ClosedChannelException();
        }
        currentWrite.fail(cause);
      }
    }
  }
  
  private void doFlush(ChannelHandlerContext ctx) {
    Channel channel = ctx.channel();
    if (!channel.isActive()) {
      discard(null);
      return;
    }
    
    boolean requiresFlush = true;
    ByteBufAllocator allocator = ctx.alloc();
    while (channel.isWritable()) {
      final PendingWrite currentWrite = (PendingWrite)queue.peek();
      
      if (currentWrite == null) {
        break;
      }
      
      if (promise.isDone())
      {








        queue.remove();
      }
      else
      {
        Object pendingMessage = msg;
        
        if ((pendingMessage instanceof ChunkedInput)) {
          ChunkedInput<?> chunks = (ChunkedInput)pendingMessage;
          

          Object message = null;
          try {
            message = chunks.readChunk(allocator);
            boolean endOfInput = chunks.isEndOfInput();
            boolean suspend;
            if (message == null)
            {
              suspend = !endOfInput;
            } else
              suspend = false;
          } catch (Throwable t) {
            boolean suspend;
            queue.remove();
            
            if (message != null) {
              ReferenceCountUtil.release(message);
            }
            
            closeInput(chunks);
            currentWrite.fail(t);
            break; }
          boolean suspend;
          boolean endOfInput;
          if (suspend) {
            break;
          }
          



          if (message == null)
          {

            message = Unpooled.EMPTY_BUFFER;
          }
          
          if (endOfInput)
          {

            queue.remove();
          }
          
          ChannelFuture f = ctx.writeAndFlush(message);
          if (endOfInput) {
            if (f.isDone()) {
              handleEndOfInputFuture(f, currentWrite);


            }
            else
            {

              f.addListener(new ChannelFutureListener()
              {
                public void operationComplete(ChannelFuture future) {
                  ChunkedWriteHandler.handleEndOfInputFuture(future, currentWrite);
                }
              });
            }
          } else {
            final boolean resume = !channel.isWritable();
            if (f.isDone()) {
              handleFuture(f, currentWrite, resume);
            } else {
              f.addListener(new ChannelFutureListener()
              {
                public void operationComplete(ChannelFuture future) {
                  ChunkedWriteHandler.this.handleFuture(future, currentWrite, resume);
                }
              });
            }
          }
          requiresFlush = false;
        } else {
          queue.remove();
          ctx.write(pendingMessage, promise);
          requiresFlush = true;
        }
        
        if (!channel.isActive()) {
          discard(new ClosedChannelException());
          break;
        }
      }
    }
    if (requiresFlush) {
      ctx.flush();
    }
  }
  
  private static void handleEndOfInputFuture(ChannelFuture future, PendingWrite currentWrite) {
    ChunkedInput<?> input = (ChunkedInput)msg;
    if (!future.isSuccess()) {
      closeInput(input);
      currentWrite.fail(future.cause());
    }
    else {
      long inputProgress = input.progress();
      long inputLength = input.length();
      closeInput(input);
      currentWrite.progress(inputProgress, inputLength);
      currentWrite.success(inputLength);
    }
  }
  
  private void handleFuture(ChannelFuture future, PendingWrite currentWrite, boolean resume) {
    ChunkedInput<?> input = (ChunkedInput)msg;
    if (!future.isSuccess()) {
      closeInput(input);
      currentWrite.fail(future.cause());
    } else {
      currentWrite.progress(input.progress(), input.length());
      if ((resume) && (future.channel().isWritable())) {
        resumeTransfer();
      }
    }
  }
  
  private static void closeInput(ChunkedInput<?> chunks) {
    try {
      chunks.close();
    } catch (Throwable t) {
      if (logger.isWarnEnabled()) {
        logger.warn("Failed to close a chunked input.", t);
      }
    }
  }
  
  private static final class PendingWrite {
    final Object msg;
    final ChannelPromise promise;
    
    PendingWrite(Object msg, ChannelPromise promise) {
      this.msg = msg;
      this.promise = promise;
    }
    
    void fail(Throwable cause) {
      ReferenceCountUtil.release(msg);
      promise.tryFailure(cause);
    }
    
    void success(long total) {
      if (promise.isDone())
      {
        return;
      }
      progress(total, total);
      promise.trySuccess();
    }
    
    void progress(long progress, long total) {
      if ((promise instanceof ChannelProgressivePromise)) {
        ((ChannelProgressivePromise)promise).tryProgress(progress, total);
      }
    }
  }
}
