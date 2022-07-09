package io.netty.channel.socket;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelConfig;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.WriteBufferWaterMark;
import java.net.InetAddress;
import java.net.NetworkInterface;

public abstract interface DatagramChannelConfig
  extends ChannelConfig
{
  public abstract int getSendBufferSize();
  
  public abstract DatagramChannelConfig setSendBufferSize(int paramInt);
  
  public abstract int getReceiveBufferSize();
  
  public abstract DatagramChannelConfig setReceiveBufferSize(int paramInt);
  
  public abstract int getTrafficClass();
  
  public abstract DatagramChannelConfig setTrafficClass(int paramInt);
  
  public abstract boolean isReuseAddress();
  
  public abstract DatagramChannelConfig setReuseAddress(boolean paramBoolean);
  
  public abstract boolean isBroadcast();
  
  public abstract DatagramChannelConfig setBroadcast(boolean paramBoolean);
  
  public abstract boolean isLoopbackModeDisabled();
  
  public abstract DatagramChannelConfig setLoopbackModeDisabled(boolean paramBoolean);
  
  public abstract int getTimeToLive();
  
  public abstract DatagramChannelConfig setTimeToLive(int paramInt);
  
  public abstract InetAddress getInterface();
  
  public abstract DatagramChannelConfig setInterface(InetAddress paramInetAddress);
  
  public abstract NetworkInterface getNetworkInterface();
  
  public abstract DatagramChannelConfig setNetworkInterface(NetworkInterface paramNetworkInterface);
  
  @Deprecated
  public abstract DatagramChannelConfig setMaxMessagesPerRead(int paramInt);
  
  public abstract DatagramChannelConfig setWriteSpinCount(int paramInt);
  
  public abstract DatagramChannelConfig setConnectTimeoutMillis(int paramInt);
  
  public abstract DatagramChannelConfig setAllocator(ByteBufAllocator paramByteBufAllocator);
  
  public abstract DatagramChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator paramRecvByteBufAllocator);
  
  public abstract DatagramChannelConfig setAutoRead(boolean paramBoolean);
  
  public abstract DatagramChannelConfig setAutoClose(boolean paramBoolean);
  
  public abstract DatagramChannelConfig setMessageSizeEstimator(MessageSizeEstimator paramMessageSizeEstimator);
  
  public abstract DatagramChannelConfig setWriteBufferWaterMark(WriteBufferWaterMark paramWriteBufferWaterMark);
}
