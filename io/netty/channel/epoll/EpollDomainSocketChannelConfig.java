package io.netty.channel.epoll;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.socket.DuplexChannelConfig;
import io.netty.channel.unix.DomainSocketChannelConfig;
import io.netty.channel.unix.DomainSocketReadMode;
import io.netty.channel.unix.UnixChannelOption;
import io.netty.util.internal.ObjectUtil;
import java.io.IOException;
import java.util.Map;



















public final class EpollDomainSocketChannelConfig
  extends EpollChannelConfig
  implements DomainSocketChannelConfig, DuplexChannelConfig
{
  private volatile DomainSocketReadMode mode = DomainSocketReadMode.BYTES;
  private volatile boolean allowHalfClosure;
  
  EpollDomainSocketChannelConfig(AbstractEpollChannel channel) {
    super(channel);
  }
  
  public Map<ChannelOption<?>, Object> getOptions()
  {
    return getOptions(super.getOptions(), new ChannelOption[] { UnixChannelOption.DOMAIN_SOCKET_READ_MODE, ChannelOption.ALLOW_HALF_CLOSURE, ChannelOption.SO_SNDBUF, ChannelOption.SO_RCVBUF });
  }
  

  public <T> T getOption(ChannelOption<T> option)
  {
    if (option == UnixChannelOption.DOMAIN_SOCKET_READ_MODE) {
      return getReadMode();
    }
    if (option == ChannelOption.ALLOW_HALF_CLOSURE) {
      return Boolean.valueOf(isAllowHalfClosure());
    }
    if (option == ChannelOption.SO_SNDBUF) {
      return Integer.valueOf(getSendBufferSize());
    }
    if (option == ChannelOption.SO_RCVBUF) {
      return Integer.valueOf(getReceiveBufferSize());
    }
    return super.getOption(option);
  }
  
  public <T> boolean setOption(ChannelOption<T> option, T value)
  {
    validate(option, value);
    
    if (option == UnixChannelOption.DOMAIN_SOCKET_READ_MODE) {
      setReadMode((DomainSocketReadMode)value);
    } else if (option == ChannelOption.ALLOW_HALF_CLOSURE) {
      setAllowHalfClosure(((Boolean)value).booleanValue());
    } else if (option == ChannelOption.SO_SNDBUF) {
      setSendBufferSize(((Integer)value).intValue());
    } else if (option == ChannelOption.SO_RCVBUF) {
      setReceiveBufferSize(((Integer)value).intValue());
    } else {
      return super.setOption(option, value);
    }
    
    return true;
  }
  
  @Deprecated
  public EpollDomainSocketChannelConfig setMaxMessagesPerRead(int maxMessagesPerRead)
  {
    super.setMaxMessagesPerRead(maxMessagesPerRead);
    return this;
  }
  
  public EpollDomainSocketChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis)
  {
    super.setConnectTimeoutMillis(connectTimeoutMillis);
    return this;
  }
  
  public EpollDomainSocketChannelConfig setWriteSpinCount(int writeSpinCount)
  {
    super.setWriteSpinCount(writeSpinCount);
    return this;
  }
  
  public EpollDomainSocketChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator allocator)
  {
    super.setRecvByteBufAllocator(allocator);
    return this;
  }
  
  public EpollDomainSocketChannelConfig setAllocator(ByteBufAllocator allocator)
  {
    super.setAllocator(allocator);
    return this;
  }
  
  public EpollDomainSocketChannelConfig setAutoClose(boolean autoClose)
  {
    super.setAutoClose(autoClose);
    return this;
  }
  
  public EpollDomainSocketChannelConfig setMessageSizeEstimator(MessageSizeEstimator estimator)
  {
    super.setMessageSizeEstimator(estimator);
    return this;
  }
  
  @Deprecated
  public EpollDomainSocketChannelConfig setWriteBufferLowWaterMark(int writeBufferLowWaterMark)
  {
    super.setWriteBufferLowWaterMark(writeBufferLowWaterMark);
    return this;
  }
  
  @Deprecated
  public EpollDomainSocketChannelConfig setWriteBufferHighWaterMark(int writeBufferHighWaterMark)
  {
    super.setWriteBufferHighWaterMark(writeBufferHighWaterMark);
    return this;
  }
  
  public EpollDomainSocketChannelConfig setWriteBufferWaterMark(WriteBufferWaterMark writeBufferWaterMark)
  {
    super.setWriteBufferWaterMark(writeBufferWaterMark);
    return this;
  }
  
  public EpollDomainSocketChannelConfig setAutoRead(boolean autoRead)
  {
    super.setAutoRead(autoRead);
    return this;
  }
  
  public EpollDomainSocketChannelConfig setEpollMode(EpollMode mode)
  {
    super.setEpollMode(mode);
    return this;
  }
  
  public EpollDomainSocketChannelConfig setReadMode(DomainSocketReadMode mode)
  {
    this.mode = ((DomainSocketReadMode)ObjectUtil.checkNotNull(mode, "mode"));
    return this;
  }
  
  public DomainSocketReadMode getReadMode()
  {
    return mode;
  }
  
  public boolean isAllowHalfClosure()
  {
    return allowHalfClosure;
  }
  
  public EpollDomainSocketChannelConfig setAllowHalfClosure(boolean allowHalfClosure)
  {
    this.allowHalfClosure = allowHalfClosure;
    return this;
  }
  
  public int getSendBufferSize() {
    try {
      return channel).socket.getSendBufferSize();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  public EpollDomainSocketChannelConfig setSendBufferSize(int sendBufferSize) {
    try {
      channel).socket.setSendBufferSize(sendBufferSize);
      return this;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  public int getReceiveBufferSize() {
    try {
      return channel).socket.getReceiveBufferSize();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  public EpollDomainSocketChannelConfig setReceiveBufferSize(int receiveBufferSize) {
    try {
      channel).socket.setReceiveBufferSize(receiveBufferSize);
      return this;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
