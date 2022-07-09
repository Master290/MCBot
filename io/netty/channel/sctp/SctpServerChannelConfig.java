package io.netty.channel.sctp;

import com.sun.nio.sctp.SctpStandardSocketOptions.InitMaxStreams;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelConfig;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.WriteBufferWaterMark;

public abstract interface SctpServerChannelConfig
  extends ChannelConfig
{
  public abstract int getBacklog();
  
  public abstract SctpServerChannelConfig setBacklog(int paramInt);
  
  public abstract int getSendBufferSize();
  
  public abstract SctpServerChannelConfig setSendBufferSize(int paramInt);
  
  public abstract int getReceiveBufferSize();
  
  public abstract SctpServerChannelConfig setReceiveBufferSize(int paramInt);
  
  public abstract SctpStandardSocketOptions.InitMaxStreams getInitMaxStreams();
  
  public abstract SctpServerChannelConfig setInitMaxStreams(SctpStandardSocketOptions.InitMaxStreams paramInitMaxStreams);
  
  @Deprecated
  public abstract SctpServerChannelConfig setMaxMessagesPerRead(int paramInt);
  
  public abstract SctpServerChannelConfig setWriteSpinCount(int paramInt);
  
  public abstract SctpServerChannelConfig setConnectTimeoutMillis(int paramInt);
  
  public abstract SctpServerChannelConfig setAllocator(ByteBufAllocator paramByteBufAllocator);
  
  public abstract SctpServerChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator paramRecvByteBufAllocator);
  
  public abstract SctpServerChannelConfig setAutoRead(boolean paramBoolean);
  
  public abstract SctpServerChannelConfig setAutoClose(boolean paramBoolean);
  
  public abstract SctpServerChannelConfig setWriteBufferHighWaterMark(int paramInt);
  
  public abstract SctpServerChannelConfig setWriteBufferLowWaterMark(int paramInt);
  
  public abstract SctpServerChannelConfig setWriteBufferWaterMark(WriteBufferWaterMark paramWriteBufferWaterMark);
  
  public abstract SctpServerChannelConfig setMessageSizeEstimator(MessageSizeEstimator paramMessageSizeEstimator);
}
