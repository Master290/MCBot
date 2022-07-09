package io.netty.channel.kqueue;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelOption;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.socket.ServerSocketChannelConfig;
import io.netty.channel.unix.UnixChannelOption;
import java.io.IOException;
import java.util.Map;

















public class KQueueServerSocketChannelConfig
  extends KQueueServerChannelConfig
  implements ServerSocketChannelConfig
{
  KQueueServerSocketChannelConfig(KQueueServerSocketChannel channel)
  {
    super(channel);
    



    setReuseAddress(true);
  }
  
  public Map<ChannelOption<?>, Object> getOptions()
  {
    return getOptions(super.getOptions(), new ChannelOption[] { UnixChannelOption.SO_REUSEPORT, KQueueChannelOption.SO_ACCEPTFILTER });
  }
  

  public <T> T getOption(ChannelOption<T> option)
  {
    if (option == UnixChannelOption.SO_REUSEPORT) {
      return Boolean.valueOf(isReusePort());
    }
    if (option == KQueueChannelOption.SO_ACCEPTFILTER) {
      return getAcceptFilter();
    }
    return super.getOption(option);
  }
  
  public <T> boolean setOption(ChannelOption<T> option, T value)
  {
    validate(option, value);
    
    if (option == UnixChannelOption.SO_REUSEPORT) {
      setReusePort(((Boolean)value).booleanValue());
    } else if (option == KQueueChannelOption.SO_ACCEPTFILTER) {
      setAcceptFilter((AcceptFilter)value);
    } else {
      return super.setOption(option, value);
    }
    
    return true;
  }
  
  public KQueueServerSocketChannelConfig setReusePort(boolean reusePort) {
    try {
      channel).socket.setReusePort(reusePort);
      return this;
    } catch (IOException e) {
      throw new ChannelException(e);
    }
  }
  
  public boolean isReusePort() {
    try {
      return channel).socket.isReusePort();
    } catch (IOException e) {
      throw new ChannelException(e);
    }
  }
  
  public KQueueServerSocketChannelConfig setAcceptFilter(AcceptFilter acceptFilter) {
    try {
      channel).socket.setAcceptFilter(acceptFilter);
      return this;
    } catch (IOException e) {
      throw new ChannelException(e);
    }
  }
  
  public AcceptFilter getAcceptFilter() {
    try {
      return channel).socket.getAcceptFilter();
    } catch (IOException e) {
      throw new ChannelException(e);
    }
  }
  
  public KQueueServerSocketChannelConfig setRcvAllocTransportProvidesGuess(boolean transportProvidesGuess)
  {
    super.setRcvAllocTransportProvidesGuess(transportProvidesGuess);
    return this;
  }
  
  public KQueueServerSocketChannelConfig setReuseAddress(boolean reuseAddress)
  {
    super.setReuseAddress(reuseAddress);
    return this;
  }
  
  public KQueueServerSocketChannelConfig setReceiveBufferSize(int receiveBufferSize)
  {
    super.setReceiveBufferSize(receiveBufferSize);
    return this;
  }
  
  public KQueueServerSocketChannelConfig setPerformancePreferences(int connectionTime, int latency, int bandwidth)
  {
    return this;
  }
  
  public KQueueServerSocketChannelConfig setBacklog(int backlog)
  {
    super.setBacklog(backlog);
    return this;
  }
  
  public KQueueServerSocketChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis)
  {
    super.setConnectTimeoutMillis(connectTimeoutMillis);
    return this;
  }
  
  @Deprecated
  public KQueueServerSocketChannelConfig setMaxMessagesPerRead(int maxMessagesPerRead)
  {
    super.setMaxMessagesPerRead(maxMessagesPerRead);
    return this;
  }
  
  public KQueueServerSocketChannelConfig setWriteSpinCount(int writeSpinCount)
  {
    super.setWriteSpinCount(writeSpinCount);
    return this;
  }
  
  public KQueueServerSocketChannelConfig setAllocator(ByteBufAllocator allocator)
  {
    super.setAllocator(allocator);
    return this;
  }
  
  public KQueueServerSocketChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator allocator)
  {
    super.setRecvByteBufAllocator(allocator);
    return this;
  }
  
  public KQueueServerSocketChannelConfig setAutoRead(boolean autoRead)
  {
    super.setAutoRead(autoRead);
    return this;
  }
  
  @Deprecated
  public KQueueServerSocketChannelConfig setWriteBufferHighWaterMark(int writeBufferHighWaterMark)
  {
    super.setWriteBufferHighWaterMark(writeBufferHighWaterMark);
    return this;
  }
  
  @Deprecated
  public KQueueServerSocketChannelConfig setWriteBufferLowWaterMark(int writeBufferLowWaterMark)
  {
    super.setWriteBufferLowWaterMark(writeBufferLowWaterMark);
    return this;
  }
  
  public KQueueServerSocketChannelConfig setWriteBufferWaterMark(WriteBufferWaterMark writeBufferWaterMark)
  {
    super.setWriteBufferWaterMark(writeBufferWaterMark);
    return this;
  }
  
  public KQueueServerSocketChannelConfig setMessageSizeEstimator(MessageSizeEstimator estimator)
  {
    super.setMessageSizeEstimator(estimator);
    return this;
  }
}
