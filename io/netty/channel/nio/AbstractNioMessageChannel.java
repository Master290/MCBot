package io.netty.channel.nio;

import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.RecvByteBufAllocator.Handle;
import io.netty.channel.ServerChannel;
import java.io.IOException;
import java.net.PortUnreachableException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.List;




















public abstract class AbstractNioMessageChannel
  extends AbstractNioChannel
{
  boolean inputShutdown;
  
  protected AbstractNioMessageChannel(Channel parent, SelectableChannel ch, int readInterestOp)
  {
    super(parent, ch, readInterestOp);
  }
  
  protected AbstractNioChannel.AbstractNioUnsafe newUnsafe()
  {
    return new NioMessageUnsafe(null);
  }
  
  protected void doBeginRead() throws Exception
  {
    if (inputShutdown) {
      return;
    }
    super.doBeginRead();
  }
  

  protected boolean continueReading(RecvByteBufAllocator.Handle allocHandle) { return allocHandle.continueReading(); }
  
  private final class NioMessageUnsafe extends AbstractNioChannel.AbstractNioUnsafe {
    private NioMessageUnsafe() { super(); }
    
    private final List<Object> readBuf = new ArrayList();
    
    public void read()
    {
      assert (eventLoop().inEventLoop());
      ChannelConfig config = config();
      ChannelPipeline pipeline = pipeline();
      RecvByteBufAllocator.Handle allocHandle = unsafe().recvBufAllocHandle();
      allocHandle.reset(config);
      
      boolean closed = false;
      Throwable exception = null;
      try {
        try {
          do {
            int localRead = doReadMessages(readBuf);
            if (localRead == 0) {
              break;
            }
            if (localRead < 0) {
              closed = true;
              break;
            }
            
            allocHandle.incMessagesRead(localRead);
          } while (continueReading(allocHandle));
        } catch (Throwable t) {
          exception = t;
        }
        
        int size = readBuf.size();
        for (int i = 0; i < size; i++) {
          readPending = false;
          pipeline.fireChannelRead(readBuf.get(i));
        }
        readBuf.clear();
        allocHandle.readComplete();
        pipeline.fireChannelReadComplete();
        
        if (exception != null) {
          closed = closeOnReadError(exception);
          
          pipeline.fireExceptionCaught(exception);
        }
        
        if (closed) {
          inputShutdown = true;
          if (isOpen()) {
            close(voidPromise());
          }
          
        }
        

      }
      finally
      {

        if ((!readPending) && (!config.isAutoRead())) {
          removeReadOp();
        }
      }
    }
  }
  
  protected void doWrite(ChannelOutboundBuffer in) throws Exception
  {
    SelectionKey key = selectionKey();
    int interestOps = key.interestOps();
    
    int maxMessagesPerWrite = maxMessagesPerWrite();
    while (maxMessagesPerWrite > 0) {
      Object msg = in.current();
      if (msg == null) {
        break;
      }
      try {
        boolean done = false;
        for (int i = config().getWriteSpinCount() - 1; i >= 0; i--) {
          if (doWriteMessage(msg, in)) {
            done = true;
            break;
          }
        }
        
        if (done) {
          maxMessagesPerWrite--;
          in.remove();
        } else {
          break;
        }
      } catch (Exception e) {
        if (continueOnWriteError()) {
          maxMessagesPerWrite--;
          in.remove(e);
        } else {
          throw e;
        }
      }
    }
    if (in.isEmpty())
    {
      if ((interestOps & 0x4) != 0) {
        key.interestOps(interestOps & 0xFFFFFFFB);
      }
      
    }
    else if ((interestOps & 0x4) == 0) {
      key.interestOps(interestOps | 0x4);
    }
  }
  



  protected boolean continueOnWriteError()
  {
    return false;
  }
  
  protected boolean closeOnReadError(Throwable cause) {
    if (!isActive())
    {
      return true;
    }
    if ((cause instanceof PortUnreachableException)) {
      return false;
    }
    if ((cause instanceof IOException))
    {

      return !(this instanceof ServerChannel);
    }
    return true;
  }
  
  protected abstract int doReadMessages(List<Object> paramList)
    throws Exception;
  
  protected abstract boolean doWriteMessage(Object paramObject, ChannelOutboundBuffer paramChannelOutboundBuffer)
    throws Exception;
}
