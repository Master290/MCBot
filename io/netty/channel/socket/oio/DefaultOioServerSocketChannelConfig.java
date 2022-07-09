package io.netty.channel.socket.oio;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelOption;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.PreferHeapByteBufAllocator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.socket.DefaultServerSocketChannelConfig;
import io.netty.channel.socket.ServerSocketChannel;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;






















@Deprecated
public class DefaultOioServerSocketChannelConfig
  extends DefaultServerSocketChannelConfig
  implements OioServerSocketChannelConfig
{
  @Deprecated
  public DefaultOioServerSocketChannelConfig(ServerSocketChannel channel, ServerSocket javaSocket)
  {
    super(channel, javaSocket);
    setAllocator(new PreferHeapByteBufAllocator(getAllocator()));
  }
  
  DefaultOioServerSocketChannelConfig(OioServerSocketChannel channel, ServerSocket javaSocket) {
    super(channel, javaSocket);
    setAllocator(new PreferHeapByteBufAllocator(getAllocator()));
  }
  
  public Map<ChannelOption<?>, Object> getOptions()
  {
    return getOptions(
      super.getOptions(), new ChannelOption[] { ChannelOption.SO_TIMEOUT });
  }
  

  public <T> T getOption(ChannelOption<T> option)
  {
    if (option == ChannelOption.SO_TIMEOUT) {
      return Integer.valueOf(getSoTimeout());
    }
    return super.getOption(option);
  }
  
  public <T> boolean setOption(ChannelOption<T> option, T value)
  {
    validate(option, value);
    
    if (option == ChannelOption.SO_TIMEOUT) {
      setSoTimeout(((Integer)value).intValue());
    } else {
      return super.setOption(option, value);
    }
    return true;
  }
  
  public OioServerSocketChannelConfig setSoTimeout(int timeout)
  {
    try {
      javaSocket.setSoTimeout(timeout);
    } catch (IOException e) {
      throw new ChannelException(e);
    }
    return this;
  }
  
  public int getSoTimeout()
  {
    try {
      return javaSocket.getSoTimeout();
    } catch (IOException e) {
      throw new ChannelException(e);
    }
  }
  
  public OioServerSocketChannelConfig setBacklog(int backlog)
  {
    super.setBacklog(backlog);
    return this;
  }
  
  public OioServerSocketChannelConfig setReuseAddress(boolean reuseAddress)
  {
    super.setReuseAddress(reuseAddress);
    return this;
  }
  
  public OioServerSocketChannelConfig setReceiveBufferSize(int receiveBufferSize)
  {
    super.setReceiveBufferSize(receiveBufferSize);
    return this;
  }
  
  public OioServerSocketChannelConfig setPerformancePreferences(int connectionTime, int latency, int bandwidth)
  {
    super.setPerformancePreferences(connectionTime, latency, bandwidth);
    return this;
  }
  
  public OioServerSocketChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis)
  {
    super.setConnectTimeoutMillis(connectTimeoutMillis);
    return this;
  }
  
  @Deprecated
  public OioServerSocketChannelConfig setMaxMessagesPerRead(int maxMessagesPerRead)
  {
    super.setMaxMessagesPerRead(maxMessagesPerRead);
    return this;
  }
  
  public OioServerSocketChannelConfig setWriteSpinCount(int writeSpinCount)
  {
    super.setWriteSpinCount(writeSpinCount);
    return this;
  }
  
  public OioServerSocketChannelConfig setAllocator(ByteBufAllocator allocator)
  {
    super.setAllocator(allocator);
    return this;
  }
  
  public OioServerSocketChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator allocator)
  {
    super.setRecvByteBufAllocator(allocator);
    return this;
  }
  
  public OioServerSocketChannelConfig setAutoRead(boolean autoRead)
  {
    super.setAutoRead(autoRead);
    return this;
  }
  
  protected void autoReadCleared()
  {
    if ((channel instanceof OioServerSocketChannel)) {
      ((OioServerSocketChannel)channel).clearReadPending0();
    }
  }
  
  public OioServerSocketChannelConfig setAutoClose(boolean autoClose)
  {
    super.setAutoClose(autoClose);
    return this;
  }
  
  public OioServerSocketChannelConfig setWriteBufferHighWaterMark(int writeBufferHighWaterMark)
  {
    super.setWriteBufferHighWaterMark(writeBufferHighWaterMark);
    return this;
  }
  
  public OioServerSocketChannelConfig setWriteBufferLowWaterMark(int writeBufferLowWaterMark)
  {
    super.setWriteBufferLowWaterMark(writeBufferLowWaterMark);
    return this;
  }
  
  public OioServerSocketChannelConfig setWriteBufferWaterMark(WriteBufferWaterMark writeBufferWaterMark)
  {
    super.setWriteBufferWaterMark(writeBufferWaterMark);
    return this;
  }
  
  public OioServerSocketChannelConfig setMessageSizeEstimator(MessageSizeEstimator estimator)
  {
    super.setMessageSizeEstimator(estimator);
    return this;
  }
}
