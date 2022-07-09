package io.netty.channel.oio;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.Channel.Unsafe;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.FileRegion;
import io.netty.channel.RecvByteBufAllocator.Handle;
import io.netty.channel.socket.ChannelInputShutdownEvent;
import io.netty.channel.socket.ChannelInputShutdownReadComplete;
import io.netty.util.internal.StringUtil;
import java.io.IOException;

















/**
 * @deprecated
 */
public abstract class AbstractOioByteChannel
  extends AbstractOioChannel
{
  private static final ChannelMetadata METADATA = new ChannelMetadata(false);
  private static final String EXPECTED_TYPES = " (expected: " + 
    StringUtil.simpleClassName(ByteBuf.class) + ", " + 
    StringUtil.simpleClassName(FileRegion.class) + ')';
  


  protected AbstractOioByteChannel(Channel parent)
  {
    super(parent);
  }
  
  public ChannelMetadata metadata()
  {
    return METADATA;
  }
  



  protected abstract boolean isInputShutdown();
  


  protected abstract ChannelFuture shutdownInput();
  


  private void closeOnRead(ChannelPipeline pipeline)
  {
    if (isOpen()) {
      if (Boolean.TRUE.equals(config().getOption(ChannelOption.ALLOW_HALF_CLOSURE))) {
        shutdownInput();
        pipeline.fireUserEventTriggered(ChannelInputShutdownEvent.INSTANCE);
      } else {
        unsafe().close(unsafe().voidPromise());
      }
      pipeline.fireUserEventTriggered(ChannelInputShutdownReadComplete.INSTANCE);
    }
  }
  
  private void handleReadException(ChannelPipeline pipeline, ByteBuf byteBuf, Throwable cause, boolean close, RecvByteBufAllocator.Handle allocHandle)
  {
    if (byteBuf != null) {
      if (byteBuf.isReadable()) {
        readPending = false;
        pipeline.fireChannelRead(byteBuf);
      } else {
        byteBuf.release();
      }
    }
    allocHandle.readComplete();
    pipeline.fireChannelReadComplete();
    pipeline.fireExceptionCaught(cause);
    


    if ((close) || ((cause instanceof OutOfMemoryError)) || ((cause instanceof IOException))) {
      closeOnRead(pipeline);
    }
  }
  
  protected void doRead()
  {
    ChannelConfig config = config();
    if ((isInputShutdown()) || (!readPending))
    {

      return;
    }
    

    readPending = false;
    
    ChannelPipeline pipeline = pipeline();
    ByteBufAllocator allocator = config.getAllocator();
    RecvByteBufAllocator.Handle allocHandle = unsafe().recvBufAllocHandle();
    allocHandle.reset(config);
    
    ByteBuf byteBuf = null;
    boolean close = false;
    boolean readData = false;
    try {
      byteBuf = allocHandle.allocate(allocator);
      do {
        allocHandle.lastBytesRead(doReadBytes(byteBuf));
        if (allocHandle.lastBytesRead() <= 0) {
          if (byteBuf.isReadable()) break;
          byteBuf.release();
          byteBuf = null;
          close = allocHandle.lastBytesRead() < 0;
          if (!close)
            break;
          readPending = false; break;
        }
        


        readData = true;
        

        int available = available();
        if (available <= 0) {
          break;
        }
        

        if (!byteBuf.isWritable()) {
          int capacity = byteBuf.capacity();
          int maxCapacity = byteBuf.maxCapacity();
          if (capacity == maxCapacity) {
            allocHandle.incMessagesRead(1);
            readPending = false;
            pipeline.fireChannelRead(byteBuf);
            byteBuf = allocHandle.allocate(allocator);
          } else {
            int writerIndex = byteBuf.writerIndex();
            if (writerIndex + available > maxCapacity) {
              byteBuf.capacity(maxCapacity);
            } else {
              byteBuf.ensureWritable(available);
            }
          }
        }
      } while (allocHandle.continueReading());
      
      if (byteBuf != null)
      {

        if (byteBuf.isReadable()) {
          readPending = false;
          pipeline.fireChannelRead(byteBuf);
        } else {
          byteBuf.release();
        }
        byteBuf = null;
      }
      
      if (readData) {
        allocHandle.readComplete();
        pipeline.fireChannelReadComplete();
      }
      
      if (close) {
        closeOnRead(pipeline);
      }
    } catch (Throwable t) {
      handleReadException(pipeline, byteBuf, t, close, allocHandle);
    } finally {
      if ((readPending) || (config.isAutoRead()) || ((!readData) && (isActive())))
      {

        read();
      }
    }
  }
  
  protected void doWrite(ChannelOutboundBuffer in) throws Exception
  {
    for (;;) {
      Object msg = in.current();
      if (msg == null) {
        break;
      }
      
      if ((msg instanceof ByteBuf)) {
        ByteBuf buf = (ByteBuf)msg;
        int readableBytes = buf.readableBytes();
        while (readableBytes > 0) {
          doWriteBytes(buf);
          int newReadableBytes = buf.readableBytes();
          in.progress(readableBytes - newReadableBytes);
          readableBytes = newReadableBytes;
        }
        in.remove();
      } else if ((msg instanceof FileRegion)) {
        FileRegion region = (FileRegion)msg;
        long transferred = region.transferred();
        doWriteFileRegion(region);
        in.progress(region.transferred() - transferred);
        in.remove();
      } else {
        in.remove(new UnsupportedOperationException("unsupported message type: " + 
          StringUtil.simpleClassName(msg)));
      }
    }
  }
  
  protected final Object filterOutboundMessage(Object msg) throws Exception
  {
    if (((msg instanceof ByteBuf)) || ((msg instanceof FileRegion))) {
      return msg;
    }
    

    throw new UnsupportedOperationException("unsupported message type: " + StringUtil.simpleClassName(msg) + EXPECTED_TYPES);
  }
  
  protected abstract int available();
  
  protected abstract int doReadBytes(ByteBuf paramByteBuf)
    throws Exception;
  
  protected abstract void doWriteBytes(ByteBuf paramByteBuf)
    throws Exception;
  
  protected abstract void doWriteFileRegion(FileRegion paramFileRegion)
    throws Exception;
}
