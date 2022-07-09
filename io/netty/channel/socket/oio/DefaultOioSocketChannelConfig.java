package io.netty.channel.socket.oio;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelOption;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.PreferHeapByteBufAllocator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.socket.DefaultSocketChannelConfig;
import io.netty.channel.socket.SocketChannel;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;




















@Deprecated
public class DefaultOioSocketChannelConfig
  extends DefaultSocketChannelConfig
  implements OioSocketChannelConfig
{
  @Deprecated
  public DefaultOioSocketChannelConfig(SocketChannel channel, Socket javaSocket)
  {
    super(channel, javaSocket);
    setAllocator(new PreferHeapByteBufAllocator(getAllocator()));
  }
  
  DefaultOioSocketChannelConfig(OioSocketChannel channel, Socket javaSocket) {
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
  
  public OioSocketChannelConfig setSoTimeout(int timeout)
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
  
  public OioSocketChannelConfig setTcpNoDelay(boolean tcpNoDelay)
  {
    super.setTcpNoDelay(tcpNoDelay);
    return this;
  }
  
  public OioSocketChannelConfig setSoLinger(int soLinger)
  {
    super.setSoLinger(soLinger);
    return this;
  }
  
  public OioSocketChannelConfig setSendBufferSize(int sendBufferSize)
  {
    super.setSendBufferSize(sendBufferSize);
    return this;
  }
  
  public OioSocketChannelConfig setReceiveBufferSize(int receiveBufferSize)
  {
    super.setReceiveBufferSize(receiveBufferSize);
    return this;
  }
  
  public OioSocketChannelConfig setKeepAlive(boolean keepAlive)
  {
    super.setKeepAlive(keepAlive);
    return this;
  }
  
  public OioSocketChannelConfig setTrafficClass(int trafficClass)
  {
    super.setTrafficClass(trafficClass);
    return this;
  }
  
  public OioSocketChannelConfig setReuseAddress(boolean reuseAddress)
  {
    super.setReuseAddress(reuseAddress);
    return this;
  }
  
  public OioSocketChannelConfig setPerformancePreferences(int connectionTime, int latency, int bandwidth)
  {
    super.setPerformancePreferences(connectionTime, latency, bandwidth);
    return this;
  }
  
  public OioSocketChannelConfig setAllowHalfClosure(boolean allowHalfClosure)
  {
    super.setAllowHalfClosure(allowHalfClosure);
    return this;
  }
  
  public OioSocketChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis)
  {
    super.setConnectTimeoutMillis(connectTimeoutMillis);
    return this;
  }
  
  @Deprecated
  public OioSocketChannelConfig setMaxMessagesPerRead(int maxMessagesPerRead)
  {
    super.setMaxMessagesPerRead(maxMessagesPerRead);
    return this;
  }
  
  public OioSocketChannelConfig setWriteSpinCount(int writeSpinCount)
  {
    super.setWriteSpinCount(writeSpinCount);
    return this;
  }
  
  public OioSocketChannelConfig setAllocator(ByteBufAllocator allocator)
  {
    super.setAllocator(allocator);
    return this;
  }
  
  public OioSocketChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator allocator)
  {
    super.setRecvByteBufAllocator(allocator);
    return this;
  }
  
  public OioSocketChannelConfig setAutoRead(boolean autoRead)
  {
    super.setAutoRead(autoRead);
    return this;
  }
  
  protected void autoReadCleared()
  {
    if ((channel instanceof OioSocketChannel)) {
      ((OioSocketChannel)channel).clearReadPending0();
    }
  }
  
  public OioSocketChannelConfig setAutoClose(boolean autoClose)
  {
    super.setAutoClose(autoClose);
    return this;
  }
  
  public OioSocketChannelConfig setWriteBufferHighWaterMark(int writeBufferHighWaterMark)
  {
    super.setWriteBufferHighWaterMark(writeBufferHighWaterMark);
    return this;
  }
  
  public OioSocketChannelConfig setWriteBufferLowWaterMark(int writeBufferLowWaterMark)
  {
    super.setWriteBufferLowWaterMark(writeBufferLowWaterMark);
    return this;
  }
  
  public OioSocketChannelConfig setWriteBufferWaterMark(WriteBufferWaterMark writeBufferWaterMark)
  {
    super.setWriteBufferWaterMark(writeBufferWaterMark);
    return this;
  }
  
  public OioSocketChannelConfig setMessageSizeEstimator(MessageSizeEstimator estimator)
  {
    super.setMessageSizeEstimator(estimator);
    return this;
  }
}
