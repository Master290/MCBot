package io.netty.channel.sctp;

import com.sun.nio.sctp.SctpStandardSocketOptions.InitMaxStreams;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelConfig;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.WriteBufferWaterMark;

public abstract interface SctpChannelConfig
  extends ChannelConfig
{
  public abstract boolean isSctpNoDelay();
  
  public abstract SctpChannelConfig setSctpNoDelay(boolean paramBoolean);
  
  public abstract int getSendBufferSize();
  
  public abstract SctpChannelConfig setSendBufferSize(int paramInt);
  
  public abstract int getReceiveBufferSize();
  
  public abstract SctpChannelConfig setReceiveBufferSize(int paramInt);
  
  public abstract SctpStandardSocketOptions.InitMaxStreams getInitMaxStreams();
  
  public abstract SctpChannelConfig setInitMaxStreams(SctpStandardSocketOptions.InitMaxStreams paramInitMaxStreams);
  
  public abstract SctpChannelConfig setConnectTimeoutMillis(int paramInt);
  
  @Deprecated
  public abstract SctpChannelConfig setMaxMessagesPerRead(int paramInt);
  
  public abstract SctpChannelConfig setWriteSpinCount(int paramInt);
  
  public abstract SctpChannelConfig setAllocator(ByteBufAllocator paramByteBufAllocator);
  
  public abstract SctpChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator paramRecvByteBufAllocator);
  
  public abstract SctpChannelConfig setAutoRead(boolean paramBoolean);
  
  public abstract SctpChannelConfig setAutoClose(boolean paramBoolean);
  
  public abstract SctpChannelConfig setWriteBufferHighWaterMark(int paramInt);
  
  public abstract SctpChannelConfig setWriteBufferLowWaterMark(int paramInt);
  
  public abstract SctpChannelConfig setWriteBufferWaterMark(WriteBufferWaterMark paramWriteBufferWaterMark);
  
  public abstract SctpChannelConfig setMessageSizeEstimator(MessageSizeEstimator paramMessageSizeEstimator);
}
