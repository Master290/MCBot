package io.netty.channel.socket.oio;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.socket.SocketChannelConfig;

@Deprecated
public abstract interface OioSocketChannelConfig
  extends SocketChannelConfig
{
  public abstract OioSocketChannelConfig setSoTimeout(int paramInt);
  
  public abstract int getSoTimeout();
  
  public abstract OioSocketChannelConfig setTcpNoDelay(boolean paramBoolean);
  
  public abstract OioSocketChannelConfig setSoLinger(int paramInt);
  
  public abstract OioSocketChannelConfig setSendBufferSize(int paramInt);
  
  public abstract OioSocketChannelConfig setReceiveBufferSize(int paramInt);
  
  public abstract OioSocketChannelConfig setKeepAlive(boolean paramBoolean);
  
  public abstract OioSocketChannelConfig setTrafficClass(int paramInt);
  
  public abstract OioSocketChannelConfig setReuseAddress(boolean paramBoolean);
  
  public abstract OioSocketChannelConfig setPerformancePreferences(int paramInt1, int paramInt2, int paramInt3);
  
  public abstract OioSocketChannelConfig setAllowHalfClosure(boolean paramBoolean);
  
  public abstract OioSocketChannelConfig setConnectTimeoutMillis(int paramInt);
  
  @Deprecated
  public abstract OioSocketChannelConfig setMaxMessagesPerRead(int paramInt);
  
  public abstract OioSocketChannelConfig setWriteSpinCount(int paramInt);
  
  public abstract OioSocketChannelConfig setAllocator(ByteBufAllocator paramByteBufAllocator);
  
  public abstract OioSocketChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator paramRecvByteBufAllocator);
  
  public abstract OioSocketChannelConfig setAutoRead(boolean paramBoolean);
  
  public abstract OioSocketChannelConfig setAutoClose(boolean paramBoolean);
  
  public abstract OioSocketChannelConfig setWriteBufferHighWaterMark(int paramInt);
  
  public abstract OioSocketChannelConfig setWriteBufferLowWaterMark(int paramInt);
  
  public abstract OioSocketChannelConfig setWriteBufferWaterMark(WriteBufferWaterMark paramWriteBufferWaterMark);
  
  public abstract OioSocketChannelConfig setMessageSizeEstimator(MessageSizeEstimator paramMessageSizeEstimator);
}
