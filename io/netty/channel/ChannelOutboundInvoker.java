package io.netty.channel;

import java.net.SocketAddress;

public abstract interface ChannelOutboundInvoker
{
  public abstract ChannelFuture bind(SocketAddress paramSocketAddress);
  
  public abstract ChannelFuture connect(SocketAddress paramSocketAddress);
  
  public abstract ChannelFuture connect(SocketAddress paramSocketAddress1, SocketAddress paramSocketAddress2);
  
  public abstract ChannelFuture disconnect();
  
  public abstract ChannelFuture close();
  
  public abstract ChannelFuture deregister();
  
  public abstract ChannelFuture bind(SocketAddress paramSocketAddress, ChannelPromise paramChannelPromise);
  
  public abstract ChannelFuture connect(SocketAddress paramSocketAddress, ChannelPromise paramChannelPromise);
  
  public abstract ChannelFuture connect(SocketAddress paramSocketAddress1, SocketAddress paramSocketAddress2, ChannelPromise paramChannelPromise);
  
  public abstract ChannelFuture disconnect(ChannelPromise paramChannelPromise);
  
  public abstract ChannelFuture close(ChannelPromise paramChannelPromise);
  
  public abstract ChannelFuture deregister(ChannelPromise paramChannelPromise);
  
  public abstract ChannelOutboundInvoker read();
  
  public abstract ChannelFuture write(Object paramObject);
  
  public abstract ChannelFuture write(Object paramObject, ChannelPromise paramChannelPromise);
  
  public abstract ChannelOutboundInvoker flush();
  
  public abstract ChannelFuture writeAndFlush(Object paramObject, ChannelPromise paramChannelPromise);
  
  public abstract ChannelFuture writeAndFlush(Object paramObject);
  
  public abstract ChannelPromise newPromise();
  
  public abstract ChannelProgressivePromise newProgressivePromise();
  
  public abstract ChannelFuture newSucceededFuture();
  
  public abstract ChannelFuture newFailedFuture(Throwable paramThrowable);
  
  public abstract ChannelPromise voidPromise();
}
