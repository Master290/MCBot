package io.netty.channel.oio;

import io.netty.channel.Channel;
import io.netty.channel.Channel.Unsafe;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.RecvByteBufAllocator.Handle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;




















@Deprecated
public abstract class AbstractOioMessageChannel
  extends AbstractOioChannel
{
  private final List<Object> readBuf = new ArrayList();
  
  protected AbstractOioMessageChannel(Channel parent) {
    super(parent);
  }
  
  protected void doRead()
  {
    if (!readPending)
    {

      return;
    }
    

    readPending = false;
    
    ChannelConfig config = config();
    ChannelPipeline pipeline = pipeline();
    RecvByteBufAllocator.Handle allocHandle = unsafe().recvBufAllocHandle();
    allocHandle.reset(config);
    
    boolean closed = false;
    Throwable exception = null;
    try
    {
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
      } while (allocHandle.continueReading());
    } catch (Throwable t) {
      exception = t;
    }
    
    boolean readData = false;
    int size = readBuf.size();
    if (size > 0) {
      readData = true;
      for (int i = 0; i < size; i++) {
        readPending = false;
        pipeline.fireChannelRead(readBuf.get(i));
      }
      readBuf.clear();
      allocHandle.readComplete();
      pipeline.fireChannelReadComplete();
    }
    
    if (exception != null) {
      if ((exception instanceof IOException)) {
        closed = true;
      }
      
      pipeline.fireExceptionCaught(exception);
    }
    
    if (closed) {
      if (isOpen()) {
        unsafe().close(unsafe().voidPromise());
      }
    } else if ((readPending) || (config.isAutoRead()) || ((!readData) && (isActive())))
    {

      read();
    }
  }
  
  protected abstract int doReadMessages(List<Object> paramList)
    throws Exception;
}
