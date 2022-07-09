package io.netty.channel;

import java.net.SocketAddress;

public abstract interface ChannelOutboundHandler
  extends ChannelHandler
{
  public abstract void bind(ChannelHandlerContext paramChannelHandlerContext, SocketAddress paramSocketAddress, ChannelPromise paramChannelPromise)
    throws Exception;
  
  public abstract void connect(ChannelHandlerContext paramChannelHandlerContext, SocketAddress paramSocketAddress1, SocketAddress paramSocketAddress2, ChannelPromise paramChannelPromise)
    throws Exception;
  
  public abstract void disconnect(ChannelHandlerContext paramChannelHandlerContext, ChannelPromise paramChannelPromise)
    throws Exception;
  
  public abstract void close(ChannelHandlerContext paramChannelHandlerContext, ChannelPromise paramChannelPromise)
    throws Exception;
  
  public abstract void deregister(ChannelHandlerContext paramChannelHandlerContext, ChannelPromise paramChannelPromise)
    throws Exception;
  
  public abstract void read(ChannelHandlerContext paramChannelHandlerContext)
    throws Exception;
  
  public abstract void write(ChannelHandlerContext paramChannelHandlerContext, Object paramObject, ChannelPromise paramChannelPromise)
    throws Exception;
  
  public abstract void flush(ChannelHandlerContext paramChannelHandlerContext)
    throws Exception;
}
