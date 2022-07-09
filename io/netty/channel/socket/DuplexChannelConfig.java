package io.netty.channel.socket;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelConfig;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.WriteBufferWaterMark;

public abstract interface DuplexChannelConfig
  extends ChannelConfig
{
  public abstract boolean isAllowHalfClosure();
  
  public abstract DuplexChannelConfig setAllowHalfClosure(boolean paramBoolean);
  
  @Deprecated
  public abstract DuplexChannelConfig setMaxMessagesPerRead(int paramInt);
  
  public abstract DuplexChannelConfig setWriteSpinCount(int paramInt);
  
  public abstract DuplexChannelConfig setAllocator(ByteBufAllocator paramByteBufAllocator);
  
  public abstract DuplexChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator paramRecvByteBufAllocator);
  
  public abstract DuplexChannelConfig setAutoRead(boolean paramBoolean);
  
  public abstract DuplexChannelConfig setAutoClose(boolean paramBoolean);
  
  public abstract DuplexChannelConfig setMessageSizeEstimator(MessageSizeEstimator paramMessageSizeEstimator);
  
  public abstract DuplexChannelConfig setWriteBufferWaterMark(WriteBufferWaterMark paramWriteBufferWaterMark);
}
