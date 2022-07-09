package io.netty.channel;

public abstract interface ChannelInboundInvoker
{
  public abstract ChannelInboundInvoker fireChannelRegistered();
  
  public abstract ChannelInboundInvoker fireChannelUnregistered();
  
  public abstract ChannelInboundInvoker fireChannelActive();
  
  public abstract ChannelInboundInvoker fireChannelInactive();
  
  public abstract ChannelInboundInvoker fireExceptionCaught(Throwable paramThrowable);
  
  public abstract ChannelInboundInvoker fireUserEventTriggered(Object paramObject);
  
  public abstract ChannelInboundInvoker fireChannelRead(Object paramObject);
  
  public abstract ChannelInboundInvoker fireChannelReadComplete();
  
  public abstract ChannelInboundInvoker fireChannelWritabilityChanged();
}
