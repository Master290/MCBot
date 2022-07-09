package io.netty.channel.epoll;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelOption;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.socket.DatagramChannelConfig;
import io.netty.util.internal.ObjectUtil;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Map;














public final class EpollDatagramChannelConfig
  extends EpollChannelConfig
  implements DatagramChannelConfig
{
  private static final RecvByteBufAllocator DEFAULT_RCVBUF_ALLOCATOR = new FixedRecvByteBufAllocator(2048);
  private boolean activeOnOpen;
  private volatile int maxDatagramSize;
  private volatile boolean gro;
  
  EpollDatagramChannelConfig(EpollDatagramChannel channel) { super(channel);
    setRecvByteBufAllocator(DEFAULT_RCVBUF_ALLOCATOR);
  }
  

  public Map<ChannelOption<?>, Object> getOptions()
  {
    return getOptions(
      super.getOptions(), new ChannelOption[] { ChannelOption.SO_BROADCAST, ChannelOption.SO_RCVBUF, ChannelOption.SO_SNDBUF, ChannelOption.SO_REUSEADDR, ChannelOption.IP_MULTICAST_LOOP_DISABLED, ChannelOption.IP_MULTICAST_ADDR, ChannelOption.IP_MULTICAST_IF, ChannelOption.IP_MULTICAST_TTL, ChannelOption.IP_TOS, ChannelOption.DATAGRAM_CHANNEL_ACTIVE_ON_REGISTRATION, EpollChannelOption.SO_REUSEPORT, EpollChannelOption.IP_FREEBIND, EpollChannelOption.IP_TRANSPARENT, EpollChannelOption.IP_RECVORIGDSTADDR, EpollChannelOption.MAX_DATAGRAM_PAYLOAD_SIZE, EpollChannelOption.UDP_GRO });
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
    if (option == EpollChannelOption.SO_REUSEPORT) {
      return Boolean.valueOf(isReusePort());
    }
    if (option == EpollChannelOption.IP_TRANSPARENT) {
      return Boolean.valueOf(isIpTransparent());
    }
    if (option == EpollChannelOption.IP_FREEBIND) {
      return Boolean.valueOf(isFreeBind());
    }
    if (option == EpollChannelOption.IP_RECVORIGDSTADDR) {
      return Boolean.valueOf(isIpRecvOrigDestAddr());
    }
    if (option == EpollChannelOption.MAX_DATAGRAM_PAYLOAD_SIZE) {
      return Integer.valueOf(getMaxDatagramPayloadSize());
    }
    if (option == EpollChannelOption.UDP_GRO) {
      return Boolean.valueOf(isUdpGro());
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
    } else if (option == EpollChannelOption.SO_REUSEPORT) {
      setReusePort(((Boolean)value).booleanValue());
    } else if (option == EpollChannelOption.IP_FREEBIND) {
      setFreeBind(((Boolean)value).booleanValue());
    } else if (option == EpollChannelOption.IP_TRANSPARENT) {
      setIpTransparent(((Boolean)value).booleanValue());
    } else if (option == EpollChannelOption.IP_RECVORIGDSTADDR) {
      setIpRecvOrigDestAddr(((Boolean)value).booleanValue());
    } else if (option == EpollChannelOption.MAX_DATAGRAM_PAYLOAD_SIZE) {
      setMaxDatagramPayloadSize(((Integer)value).intValue());
    } else if (option == EpollChannelOption.UDP_GRO) {
      setUdpGro(((Boolean)value).booleanValue());
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
  
  public EpollDatagramChannelConfig setMessageSizeEstimator(MessageSizeEstimator estimator)
  {
    super.setMessageSizeEstimator(estimator);
    return this;
  }
  
  @Deprecated
  public EpollDatagramChannelConfig setWriteBufferLowWaterMark(int writeBufferLowWaterMark)
  {
    super.setWriteBufferLowWaterMark(writeBufferLowWaterMark);
    return this;
  }
  
  @Deprecated
  public EpollDatagramChannelConfig setWriteBufferHighWaterMark(int writeBufferHighWaterMark)
  {
    super.setWriteBufferHighWaterMark(writeBufferHighWaterMark);
    return this;
  }
  
  public EpollDatagramChannelConfig setWriteBufferWaterMark(WriteBufferWaterMark writeBufferWaterMark)
  {
    super.setWriteBufferWaterMark(writeBufferWaterMark);
    return this;
  }
  
  public EpollDatagramChannelConfig setAutoClose(boolean autoClose)
  {
    super.setAutoClose(autoClose);
    return this;
  }
  
  public EpollDatagramChannelConfig setAutoRead(boolean autoRead)
  {
    super.setAutoRead(autoRead);
    return this;
  }
  
  public EpollDatagramChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator allocator)
  {
    super.setRecvByteBufAllocator(allocator);
    return this;
  }
  
  public EpollDatagramChannelConfig setWriteSpinCount(int writeSpinCount)
  {
    super.setWriteSpinCount(writeSpinCount);
    return this;
  }
  
  public EpollDatagramChannelConfig setAllocator(ByteBufAllocator allocator)
  {
    super.setAllocator(allocator);
    return this;
  }
  
  public EpollDatagramChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis)
  {
    super.setConnectTimeoutMillis(connectTimeoutMillis);
    return this;
  }
  
  @Deprecated
  public EpollDatagramChannelConfig setMaxMessagesPerRead(int maxMessagesPerRead)
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
  
  public EpollDatagramChannelConfig setSendBufferSize(int sendBufferSize)
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
  
  public EpollDatagramChannelConfig setReceiveBufferSize(int receiveBufferSize)
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
  
  public EpollDatagramChannelConfig setTrafficClass(int trafficClass)
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
  
  public EpollDatagramChannelConfig setReuseAddress(boolean reuseAddress)
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
  
  public EpollDatagramChannelConfig setBroadcast(boolean broadcast)
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
    try {
      return channel).socket.isLoopbackModeDisabled();
    } catch (IOException e) {
      throw new ChannelException(e);
    }
  }
  
  public DatagramChannelConfig setLoopbackModeDisabled(boolean loopbackModeDisabled)
  {
    try {
      channel).socket.setLoopbackModeDisabled(loopbackModeDisabled);
      return this;
    } catch (IOException e) {
      throw new ChannelException(e);
    }
  }
  
  public int getTimeToLive()
  {
    try {
      return channel).socket.getTimeToLive();
    } catch (IOException e) {
      throw new ChannelException(e);
    }
  }
  
  public EpollDatagramChannelConfig setTimeToLive(int ttl)
  {
    try {
      channel).socket.setTimeToLive(ttl);
      return this;
    } catch (IOException e) {
      throw new ChannelException(e);
    }
  }
  
  public InetAddress getInterface()
  {
    try {
      return channel).socket.getInterface();
    } catch (IOException e) {
      throw new ChannelException(e);
    }
  }
  
  public EpollDatagramChannelConfig setInterface(InetAddress interfaceAddress)
  {
    try {
      channel).socket.setInterface(interfaceAddress);
      return this;
    } catch (IOException e) {
      throw new ChannelException(e);
    }
  }
  
  public NetworkInterface getNetworkInterface()
  {
    try {
      return channel).socket.getNetworkInterface();
    } catch (IOException e) {
      throw new ChannelException(e);
    }
  }
  
  public EpollDatagramChannelConfig setNetworkInterface(NetworkInterface networkInterface)
  {
    try {
      EpollDatagramChannel datagramChannel = (EpollDatagramChannel)channel;
      socket.setNetworkInterface(networkInterface);
      return this;
    } catch (IOException e) {
      throw new ChannelException(e);
    }
  }
  
  public EpollDatagramChannelConfig setEpollMode(EpollMode mode)
  {
    super.setEpollMode(mode);
    return this;
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
  





  public EpollDatagramChannelConfig setReusePort(boolean reusePort)
  {
    try
    {
      channel).socket.setReusePort(reusePort);
      return this;
    } catch (IOException e) {
      throw new ChannelException(e);
    }
  }
  


  public boolean isIpTransparent()
  {
    try
    {
      return channel).socket.isIpTransparent();
    } catch (IOException e) {
      throw new ChannelException(e);
    }
  }
  


  public EpollDatagramChannelConfig setIpTransparent(boolean ipTransparent)
  {
    try
    {
      channel).socket.setIpTransparent(ipTransparent);
      return this;
    } catch (IOException e) {
      throw new ChannelException(e);
    }
  }
  


  public boolean isFreeBind()
  {
    try
    {
      return channel).socket.isIpFreeBind();
    } catch (IOException e) {
      throw new ChannelException(e);
    }
  }
  


  public EpollDatagramChannelConfig setFreeBind(boolean freeBind)
  {
    try
    {
      channel).socket.setIpFreeBind(freeBind);
      return this;
    } catch (IOException e) {
      throw new ChannelException(e);
    }
  }
  


  public boolean isIpRecvOrigDestAddr()
  {
    try
    {
      return channel).socket.isIpRecvOrigDestAddr();
    } catch (IOException e) {
      throw new ChannelException(e);
    }
  }
  


  public EpollDatagramChannelConfig setIpRecvOrigDestAddr(boolean ipTransparent)
  {
    try
    {
      channel).socket.setIpRecvOrigDestAddr(ipTransparent);
      return this;
    } catch (IOException e) {
      throw new ChannelException(e);
    }
  }
  







  public EpollDatagramChannelConfig setMaxDatagramPayloadSize(int maxDatagramSize)
  {
    this.maxDatagramSize = ObjectUtil.checkPositiveOrZero(maxDatagramSize, "maxDatagramSize");
    return this;
  }
  


  public int getMaxDatagramPayloadSize()
  {
    return maxDatagramSize;
  }
  





  public EpollDatagramChannelConfig setUdpGro(boolean gro)
  {
    try
    {
      channel).socket.setUdpGro(gro);
    } catch (IOException e) {
      throw new ChannelException(e);
    }
    this.gro = gro;
    return this;
  }
  





  public boolean isUdpGro()
  {
    return gro;
  }
  
  public EpollDatagramChannelConfig setMaxMessagesPerWrite(int maxMessagesPerWrite)
  {
    super.setMaxMessagesPerWrite(maxMessagesPerWrite);
    return this;
  }
}
