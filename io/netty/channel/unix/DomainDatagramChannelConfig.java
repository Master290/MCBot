package io.netty.channel.unix;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelConfig;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.WriteBufferWaterMark;

public abstract interface DomainDatagramChannelConfig
  extends ChannelConfig
{
  public abstract DomainDatagramChannelConfig setAllocator(ByteBufAllocator paramByteBufAllocator);
  
  public abstract DomainDatagramChannelConfig setAutoClose(boolean paramBoolean);
  
  public abstract DomainDatagramChannelConfig setAutoRead(boolean paramBoolean);
  
  public abstract DomainDatagramChannelConfig setConnectTimeoutMillis(int paramInt);
  
  @Deprecated
  public abstract DomainDatagramChannelConfig setMaxMessagesPerRead(int paramInt);
  
  public abstract DomainDatagramChannelConfig setMessageSizeEstimator(MessageSizeEstimator paramMessageSizeEstimator);
  
  public abstract DomainDatagramChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator paramRecvByteBufAllocator);
  
  public abstract DomainDatagramChannelConfig setSendBufferSize(int paramInt);
  
  public abstract int getSendBufferSize();
  
  public abstract DomainDatagramChannelConfig setWriteBufferWaterMark(WriteBufferWaterMark paramWriteBufferWaterMark);
  
  public abstract DomainDatagramChannelConfig setWriteSpinCount(int paramInt);
}
