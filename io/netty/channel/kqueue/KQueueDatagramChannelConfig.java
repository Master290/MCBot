package io.netty.channel.kqueue;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelOption;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.socket.DatagramChannelConfig;
import io.netty.channel.unix.UnixChannelOption;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Map;


























public final class KQueueDatagramChannelConfig
  extends KQueueChannelConfig
  implements DatagramChannelConfig
{
  private static final RecvByteBufAllocator DEFAULT_RCVBUF_ALLOCATOR = new FixedRecvByteBufAllocator(2048);
  private boolean activeOnOpen;
  
  KQueueDatagramChannelConfig(KQueueDatagramChannel channel) {
    super(channel);
    setRecvByteBufAllocator(DEFAULT_RCVBUF_ALLOCATOR);
  }
  

  public Map<ChannelOption<?>, Object> getOptions()
  {
    return getOptions(
      super.getOptions(), new ChannelOption[] { ChannelOption.SO_BROADCAST, ChannelOption.SO_RCVBUF, ChannelOption.SO_SNDBUF, ChannelOption.SO_REUSEADDR, ChannelOption.IP_MULTICAST_LOOP_DISABLED, ChannelOption.IP_MULTICAST_ADDR, ChannelOption.IP_MULTICAST_IF, ChannelOption.IP_MULTICAST_TTL, ChannelOption.IP_TOS, ChannelOption.DATAGRAM_CHANNEL_ACTIVE_ON_REGISTRATION, UnixChannelOption.SO_REUSEPORT });
  }
  




  public <T> T getOption(ChannelOption<T> option)
  {
    if (option == ChannelOption.SO_BROADCAST) {
      return Boolean.valueOf(isBroadcast());
    }
    if (option == ChannelOption.SO_RCVBUF) {
      return Integer.valueOf(getReceiveBufferSize());
    }
    if (option == ChannelOption.SO_SNDBUF) {
      return Integer.valueOf(getSendBufferSize());
    }
    if (option == ChannelOption.SO_REUSEADDR) {
      return Boolean.valueOf(isReuseAddress());
    }
    if (option == ChannelOption.IP_MULTICAST_LOOP_DISABLED) {
      return Boolean.valueOf(isLoopbackModeDisabled());
    }
    if (option == ChannelOption.IP_MULTICAST_ADDR) {
      return getInterface();
    }
    if (option == ChannelOption.IP_MULTICAST_IF) {
      return getNetworkInterface();
    }
    if (option == ChannelOption.IP_MULTICAST_TTL) {
      return Integer.valueOf(getTimeToLive());
    }
    if (option == ChannelOption.IP_TOS) {
      return Integer.valueOf(getTrafficClass());
    }
    if (option == ChannelOption.DATAGRAM_CHANNEL_ACTIVE_ON_REGISTRATION) {
      return Boolean.valueOf(activeOnOpen);
    }
    if (option == UnixChannelOption.SO_REUSEPORT) {
      return Boolean.valueOf(isReusePort());
    }
    return super.getOption(option);
  }
  

  public <T> boolean setOption(ChannelOption<T> option, T value)
  {
    validate(option, value);
    
    if (option == ChannelOption.SO_BROADCAST) {
      setBroadcast(((Boolean)value).booleanValue());
    } else if (option == ChannelOption.SO_RCVBUF) {
      setReceiveBufferSize(((Integer)value).intValue());
    } else if (option == ChannelOption.SO_SNDBUF) {
      setSendBufferSize(((Integer)value).intValue());
    } else if (option == ChannelOption.SO_REUSEADDR) {
      setReuseAddress(((Boolean)value).booleanValue());
    } else if (option == ChannelOption.IP_MULTICAST_LOOP_DISABLED) {
      setLoopbackModeDisabled(((Boolean)value).booleanValue());
    } else if (option == ChannelOption.IP_MULTICAST_ADDR) {
      setInterface((InetAddress)value);
    } else if (option == ChannelOption.IP_MULTICAST_IF) {
      setNetworkInterface((NetworkInterface)value);
    } else if (option == ChannelOption.IP_MULTICAST_TTL) {
      setTimeToLive(((Integer)value).intValue());
    } else if (option == ChannelOption.IP_TOS) {
      setTrafficClass(((Integer)value).intValue());
    } else if (option == ChannelOption.DATAGRAM_CHANNEL_ACTIVE_ON_REGISTRATION) {
      setActiveOnOpen(((Boolean)value).booleanValue());
    } else if (option == UnixChannelOption.SO_REUSEPORT) {
      setReusePort(((Boolean)value).booleanValue());
    } else {
      return super.setOption(option, value);
    }
    
    return true;
  }
  
  private void setActiveOnOpen(boolean activeOnOpen) {
    if (channel.isRegistered()) {
      throw new IllegalStateException("Can only changed before channel was registered");
    }
    this.activeOnOpen = activeOnOpen;
  }
  
  boolean getActiveOnOpen() {
    return activeOnOpen;
  }
  

  public boolean isReusePort()
  {
    try
    {
      return channel).socket.isReusePort();
    } catch (IOException e) {
      throw new ChannelException(e);
    }
  }
  





  public KQueueDatagramChannelConfig setReusePort(boolean reusePort)
  {
    try
    {
      channel).socket.setReusePort(reusePort);
      return this;
    } catch (IOException e) {
      throw new ChannelException(e);
    }
  }
  
  public KQueueDatagramChannelConfig setRcvAllocTransportProvidesGuess(boolean transportProvidesGuess)
  {
    super.setRcvAllocTransportProvidesGuess(transportProvidesGuess);
    return this;
  }
  
  public KQueueDatagramChannelConfig setMessageSizeEstimator(MessageSizeEstimator estimator)
  {
    super.setMessageSizeEstimator(estimator);
    return this;
  }
  
  @Deprecated
  public KQueueDatagramChannelConfig setWriteBufferLowWaterMark(int writeBufferLowWaterMark)
  {
    super.setWriteBufferLowWaterMark(writeBufferLowWaterMark);
    return this;
  }
  
  @Deprecated
  public KQueueDatagramChannelConfig setWriteBufferHighWaterMark(int writeBufferHighWaterMark)
  {
    super.setWriteBufferHighWaterMark(writeBufferHighWaterMark);
    return this;
  }
  
  public KQueueDatagramChannelConfig setWriteBufferWaterMark(WriteBufferWaterMark writeBufferWaterMark)
  {
    super.setWriteBufferWaterMark(writeBufferWaterMark);
    return this;
  }
  
  public KQueueDatagramChannelConfig setAutoClose(boolean autoClose)
  {
    super.setAutoClose(autoClose);
    return this;
  }
  
  public KQueueDatagramChannelConfig setAutoRead(boolean autoRead)
  {
    super.setAutoRead(autoRead);
    return this;
  }
  
  public KQueueDatagramChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator allocator)
  {
    super.setRecvByteBufAllocator(allocator);
    return this;
  }
  
  public KQueueDatagramChannelConfig setWriteSpinCount(int writeSpinCount)
  {
    super.setWriteSpinCount(writeSpinCount);
    return this;
  }
  
  public KQueueDatagramChannelConfig setAllocator(ByteBufAllocator allocator)
  {
    super.setAllocator(allocator);
    return this;
  }
  
  public KQueueDatagramChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis)
  {
    super.setConnectTimeoutMillis(connectTimeoutMillis);
    return this;
  }
  
  @Deprecated
  public KQueueDatagramChannelConfig setMaxMessagesPerRead(int maxMessagesPerRead)
  {
    super.setMaxMessagesPerRead(maxMessagesPerRead);
    return this;
  }
  
  public int getSendBufferSize()
  {
    try {
      return channel).socket.getSendBufferSize();
    } catch (IOException e) {
      throw new ChannelException(e);
    }
  }
  
  public KQueueDatagramChannelConfig setSendBufferSize(int sendBufferSize)
  {
    try {
      channel).socket.setSendBufferSize(sendBufferSize);
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
  
  public KQueueDatagramChannelConfig setReceiveBufferSize(int receiveBufferSize)
  {
    try {
      channel).socket.setReceiveBufferSize(receiveBufferSize);
      return this;
    } catch (IOException e) {
      throw new ChannelException(e);
    }
  }
  
  public int getTrafficClass()
  {
    try {
      return channel).socket.getTrafficClass();
    } catch (IOException e) {
      throw new ChannelException(e);
    }
  }
  
  public KQueueDatagramChannelConfig setTrafficClass(int trafficClass)
  {
    try {
      channel).socket.setTrafficClass(trafficClass);
      return this;
    } catch (IOException e) {
      throw new ChannelException(e);
    }
  }
  
  public boolean isReuseAddress()
  {
    try {
      return channel).socket.isReuseAddress();
    } catch (IOException e) {
      throw new ChannelException(e);
    }
  }
  
  public KQueueDatagramChannelConfig setReuseAddress(boolean reuseAddress)
  {
    try {
      channel).socket.setReuseAddress(reuseAddress);
      return this;
    } catch (IOException e) {
      throw new ChannelException(e);
    }
  }
  
  public boolean isBroadcast()
  {
    try {
      return channel).socket.isBroadcast();
    } catch (IOException e) {
      throw new ChannelException(e);
    }
  }
  
  public KQueueDatagramChannelConfig setBroadcast(boolean broadcast)
  {
    try {
      channel).socket.setBroadcast(broadcast);
      return this;
    } catch (IOException e) {
      throw new ChannelException(e);
    }
  }
  
  public boolean isLoopbackModeDisabled()
  {
    return false;
  }
  
  public DatagramChannelConfig setLoopbackModeDisabled(boolean loopbackModeDisabled)
  {
    throw new UnsupportedOperationException("Multicast not supported");
  }
  
  public int getTimeToLive()
  {
    return -1;
  }
  
  public KQueueDatagramChannelConfig setTimeToLive(int ttl)
  {
    throw new UnsupportedOperationException("Multicast not supported");
  }
  
  public InetAddress getInterface()
  {
    return null;
  }
  
  public KQueueDatagramChannelConfig setInterface(InetAddress interfaceAddress)
  {
    throw new UnsupportedOperationException("Multicast not supported");
  }
  
  public NetworkInterface getNetworkInterface()
  {
    return null;
  }
  
  public KQueueDatagramChannelConfig setNetworkInterface(NetworkInterface networkInterface)
  {
    throw new UnsupportedOperationException("Multicast not supported");
  }
  
  public KQueueDatagramChannelConfig setMaxMessagesPerWrite(int maxMessagesPerWrite)
  {
    super.setMaxMessagesPerWrite(maxMessagesPerWrite);
    return this;
  }
}
