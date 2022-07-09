package io.netty.channel.unix;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelConfig;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.WriteBufferWaterMark;

public abstract interface DomainSocketChannelConfig
  extends ChannelConfig
{
  @Deprecated
  public abstract DomainSocketChannelConfig setMaxMessagesPerRead(int paramInt);
  
  public abstract DomainSocketChannelConfig setConnectTimeoutMillis(int paramInt);
  
  public abstract DomainSocketChannelConfig setWriteSpinCount(int paramInt);
  
  public abstract DomainSocketChannelConfig setAllocator(ByteBufAllocator paramByteBufAllocator);
  
  public abstract DomainSocketChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator paramRecvByteBufAllocator);
  
  public abstract DomainSocketChannelConfig setAutoRead(boolean paramBoolean);
  
  public abstract DomainSocketChannelConfig setAutoClose(boolean paramBoolean);
  
  @Deprecated
  public abstract DomainSocketChannelConfig setWriteBufferHighWaterMark(int paramInt);
  
  @Deprecated
  public abstract DomainSocketChannelConfig setWriteBufferLowWaterMark(int paramInt);
  
  public abstract DomainSocketChannelConfig setWriteBufferWaterMark(WriteBufferWaterMark paramWriteBufferWaterMark);
  
  public abstract DomainSocketChannelConfig setMessageSizeEstimator(MessageSizeEstimator paramMessageSizeEstimator);
  
  public abstract DomainSocketChannelConfig setReadMode(DomainSocketReadMode paramDomainSocketReadMode);
  
  public abstract DomainSocketReadMode getReadMode();
}
