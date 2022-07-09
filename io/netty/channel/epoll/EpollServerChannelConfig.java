package io.netty.channel.epoll;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelOption;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.socket.ServerSocketChannelConfig;
import io.netty.util.NetUtil;
import io.netty.util.internal.ObjectUtil;
import java.io.IOException;
import java.util.Map;


















public class EpollServerChannelConfig
  extends EpollChannelConfig
  implements ServerSocketChannelConfig
{
  private volatile int backlog = NetUtil.SOMAXCONN;
  private volatile int pendingFastOpenRequestsThreshold;
  
  EpollServerChannelConfig(AbstractEpollChannel channel) {
    super(channel);
  }
  
  public Map<ChannelOption<?>, Object> getOptions()
  {
    return getOptions(super.getOptions(), new ChannelOption[] { ChannelOption.SO_RCVBUF, ChannelOption.SO_REUSEADDR, ChannelOption.SO_BACKLOG, EpollChannelOption.TCP_FASTOPEN });
  }
  

  public <T> T getOption(ChannelOption<T> option)
  {
    if (option == ChannelOption.SO_RCVBUF) {
      return Integer.valueOf(getReceiveBufferSize());
    }
    if (option == ChannelOption.SO_REUSEADDR) {
      return Boolean.valueOf(isReuseAddress());
    }
    if (option == ChannelOption.SO_BACKLOG) {
      return Integer.valueOf(getBacklog());
    }
    if (option == EpollChannelOption.TCP_FASTOPEN) {
      return Integer.valueOf(getTcpFastopen());
    }
    return super.getOption(option);
  }
  
  public <T> boolean setOption(ChannelOption<T> option, T value)
  {
    validate(option, value);
    
    if (option == ChannelOption.SO_RCVBUF) {
      setReceiveBufferSize(((Integer)value).intValue());
    } else if (option == ChannelOption.SO_REUSEADDR) {
      setReuseAddress(((Boolean)value).booleanValue());
    } else if (option == ChannelOption.SO_BACKLOG) {
      setBacklog(((Integer)value).intValue());
    } else if (option == EpollChannelOption.TCP_FASTOPEN) {
      setTcpFastopen(((Integer)value).intValue());
    } else {
      return super.setOption(option, value);
    }
    
    return true;
  }
  
  public boolean isReuseAddress()
  {
    try {
      return channel).socket.isReuseAddress();
    } catch (IOException e) {
      throw new ChannelException(e);
    }
  }
  
  public EpollServerChannelConfig setReuseAddress(boolean reuseAddress)
  {
    try {
      channel).socket.setReuseAddress(reuseAddress);
      return this;
    } catch (IOException e) {
      throw new ChannelException(e);
    }
  }
  
  public int getReceiveBufferSize()
  {
    try {
      return channel).socket.getReceiveBufferSize();
    } catch (IOException e) {
      throw new ChannelException(e);
    }
  }
  
  public EpollServerChannelConfig setReceiveBufferSize(int receiveBufferSize)
  {
    try {
      channel).socket.setReceiveBufferSize(receiveBufferSize);
      return this;
    } catch (IOException e) {
      throw new ChannelException(e);
    }
  }
  
  public int getBacklog()
  {
    return backlog;
  }
  
  public EpollServerChannelConfig setBacklog(int backlog)
  {
    ObjectUtil.checkPositiveOrZero(backlog, "backlog");
    this.backlog = backlog;
    return this;
  }
  




  public int getTcpFastopen()
  {
    return pendingFastOpenRequestsThreshold;
  }
  








  public EpollServerChannelConfig setTcpFastopen(int pendingFastOpenRequestsThreshold)
  {
    ObjectUtil.checkPositiveOrZero(this.pendingFastOpenRequestsThreshold, "pendingFastOpenRequestsThreshold");
    this.pendingFastOpenRequestsThreshold = pendingFastOpenRequestsThreshold;
    return this;
  }
  
  public EpollServerChannelConfig setPerformancePreferences(int connectionTime, int latency, int bandwidth)
  {
    return this;
  }
  
  public EpollServerChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis)
  {
    super.setConnectTimeoutMillis(connectTimeoutMillis);
    return this;
  }
  
  @Deprecated
  public EpollServerChannelConfig setMaxMessagesPerRead(int maxMessagesPerRead)
  {
    super.setMaxMessagesPerRead(maxMessagesPerRead);
    return this;
  }
  
  public EpollServerChannelConfig setWriteSpinCount(int writeSpinCount)
  {
    super.setWriteSpinCount(writeSpinCount);
    return this;
  }
  
  public EpollServerChannelConfig setAllocator(ByteBufAllocator allocator)
  {
    super.setAllocator(allocator);
    return this;
  }
  
  public EpollServerChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator allocator)
  {
    super.setRecvByteBufAllocator(allocator);
    return this;
  }
  
  public EpollServerChannelConfig setAutoRead(boolean autoRead)
  {
    super.setAutoRead(autoRead);
    return this;
  }
  
  @Deprecated
  public EpollServerChannelConfig setWriteBufferHighWaterMark(int writeBufferHighWaterMark)
  {
    super.setWriteBufferHighWaterMark(writeBufferHighWaterMark);
    return this;
  }
  
  @Deprecated
  public EpollServerChannelConfig setWriteBufferLowWaterMark(int writeBufferLowWaterMark)
  {
    super.setWriteBufferLowWaterMark(writeBufferLowWaterMark);
    return this;
  }
  
  public EpollServerChannelConfig setWriteBufferWaterMark(WriteBufferWaterMark writeBufferWaterMark)
  {
    super.setWriteBufferWaterMark(writeBufferWaterMark);
    return this;
  }
  
  public EpollServerChannelConfig setMessageSizeEstimator(MessageSizeEstimator estimator)
  {
    super.setMessageSizeEstimator(estimator);
    return this;
  }
  
  public EpollServerChannelConfig setEpollMode(EpollMode mode)
  {
    super.setEpollMode(mode);
    return this;
  }
}
