package io.netty.channel.udt;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.WriteBufferWaterMark;

@Deprecated
public abstract interface UdtServerChannelConfig
  extends UdtChannelConfig
{
  public abstract int getBacklog();
  
  public abstract UdtServerChannelConfig setBacklog(int paramInt);
  
  public abstract UdtServerChannelConfig setConnectTimeoutMillis(int paramInt);
  
  @Deprecated
  public abstract UdtServerChannelConfig setMaxMessagesPerRead(int paramInt);
  
  public abstract UdtServerChannelConfig setWriteSpinCount(int paramInt);
  
  public abstract UdtServerChannelConfig setAllocator(ByteBufAllocator paramByteBufAllocator);
  
  public abstract UdtServerChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator paramRecvByteBufAllocator);
  
  public abstract UdtServerChannelConfig setAutoRead(boolean paramBoolean);
  
  public abstract UdtServerChannelConfig setAutoClose(boolean paramBoolean);
  
  public abstract UdtServerChannelConfig setProtocolReceiveBufferSize(int paramInt);
  
  public abstract UdtServerChannelConfig setProtocolSendBufferSize(int paramInt);
  
  public abstract UdtServerChannelConfig setReceiveBufferSize(int paramInt);
  
  public abstract UdtServerChannelConfig setReuseAddress(boolean paramBoolean);
  
  public abstract UdtServerChannelConfig setSendBufferSize(int paramInt);
  
  public abstract UdtServerChannelConfig setSoLinger(int paramInt);
  
  public abstract UdtServerChannelConfig setSystemReceiveBufferSize(int paramInt);
  
  public abstract UdtServerChannelConfig setSystemSendBufferSize(int paramInt);
  
  public abstract UdtServerChannelConfig setWriteBufferHighWaterMark(int paramInt);
  
  public abstract UdtServerChannelConfig setWriteBufferLowWaterMark(int paramInt);
  
  public abstract UdtServerChannelConfig setWriteBufferWaterMark(WriteBufferWaterMark paramWriteBufferWaterMark);
  
  public abstract UdtServerChannelConfig setMessageSizeEstimator(MessageSizeEstimator paramMessageSizeEstimator);
}
