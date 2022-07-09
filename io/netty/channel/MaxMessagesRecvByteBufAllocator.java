package io.netty.channel;

public abstract interface MaxMessagesRecvByteBufAllocator
  extends RecvByteBufAllocator
{
  public abstract int maxMessagesPerRead();
  
  public abstract MaxMessagesRecvByteBufAllocator maxMessagesPerRead(int paramInt);
}
