package io.netty.channel;

import io.netty.buffer.ByteBufAllocator;
import java.util.Map;

public abstract interface ChannelConfig
{
  public abstract Map<ChannelOption<?>, Object> getOptions();
  
  public abstract boolean setOptions(Map<ChannelOption<?>, ?> paramMap);
  
  public abstract <T> T getOption(ChannelOption<T> paramChannelOption);
  
  public abstract <T> boolean setOption(ChannelOption<T> paramChannelOption, T paramT);
  
  public abstract int getConnectTimeoutMillis();
  
  public abstract ChannelConfig setConnectTimeoutMillis(int paramInt);
  
  @Deprecated
  public abstract int getMaxMessagesPerRead();
  
  @Deprecated
  public abstract ChannelConfig setMaxMessagesPerRead(int paramInt);
  
  public abstract int getWriteSpinCount();
  
  public abstract ChannelConfig setWriteSpinCount(int paramInt);
  
  public abstract ByteBufAllocator getAllocator();
  
  public abstract ChannelConfig setAllocator(ByteBufAllocator paramByteBufAllocator);
  
  public abstract <T extends RecvByteBufAllocator> T getRecvByteBufAllocator();
  
  public abstract ChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator paramRecvByteBufAllocator);
  
  public abstract boolean isAutoRead();
  
  public abstract ChannelConfig setAutoRead(boolean paramBoolean);
  
  public abstract boolean isAutoClose();
  
  public abstract ChannelConfig setAutoClose(boolean paramBoolean);
  
  public abstract int getWriteBufferHighWaterMark();
  
  public abstract ChannelConfig setWriteBufferHighWaterMark(int paramInt);
  
  public abstract int getWriteBufferLowWaterMark();
  
  public abstract ChannelConfig setWriteBufferLowWaterMark(int paramInt);
  
  public abstract MessageSizeEstimator getMessageSizeEstimator();
  
  public abstract ChannelConfig setMessageSizeEstimator(MessageSizeEstimator paramMessageSizeEstimator);
  
  public abstract WriteBufferWaterMark getWriteBufferWaterMark();
  
  public abstract ChannelConfig setWriteBufferWaterMark(WriteBufferWaterMark paramWriteBufferWaterMark);
}
