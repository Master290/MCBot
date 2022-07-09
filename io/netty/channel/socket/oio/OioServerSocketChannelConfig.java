package io.netty.channel.socket.oio;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.socket.ServerSocketChannelConfig;

@Deprecated
public abstract interface OioServerSocketChannelConfig
  extends ServerSocketChannelConfig
{
  public abstract OioServerSocketChannelConfig setSoTimeout(int paramInt);
  
  public abstract int getSoTimeout();
  
  public abstract OioServerSocketChannelConfig setBacklog(int paramInt);
  
  public abstract OioServerSocketChannelConfig setReuseAddress(boolean paramBoolean);
  
  public abstract OioServerSocketChannelConfig setReceiveBufferSize(int paramInt);
  
  public abstract OioServerSocketChannelConfig setPerformancePreferences(int paramInt1, int paramInt2, int paramInt3);
  
  public abstract OioServerSocketChannelConfig setConnectTimeoutMillis(int paramInt);
  
  @Deprecated
  public abstract OioServerSocketChannelConfig setMaxMessagesPerRead(int paramInt);
  
  public abstract OioServerSocketChannelConfig setWriteSpinCount(int paramInt);
  
  public abstract OioServerSocketChannelConfig setAllocator(ByteBufAllocator paramByteBufAllocator);
  
  public abstract OioServerSocketChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator paramRecvByteBufAllocator);
  
  public abstract OioServerSocketChannelConfig setAutoRead(boolean paramBoolean);
  
  public abstract OioServerSocketChannelConfig setAutoClose(boolean paramBoolean);
  
  public abstract OioServerSocketChannelConfig setWriteBufferHighWaterMark(int paramInt);
  
  public abstract OioServerSocketChannelConfig setWriteBufferLowWaterMark(int paramInt);
  
  public abstract OioServerSocketChannelConfig setWriteBufferWaterMark(WriteBufferWaterMark paramWriteBufferWaterMark);
  
  public abstract OioServerSocketChannelConfig setMessageSizeEstimator(MessageSizeEstimator paramMessageSizeEstimator);
}
