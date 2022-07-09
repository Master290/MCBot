package io.netty.channel;

import java.net.SocketAddress;



























public class ChannelDuplexHandler
  extends ChannelInboundHandlerAdapter
  implements ChannelOutboundHandler
{
  public ChannelDuplexHandler() {}
  
  @ChannelHandlerMask.Skip
  public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise)
    throws Exception
  {
    ctx.bind(localAddress, promise);
  }
  






  @ChannelHandlerMask.Skip
  public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise)
    throws Exception
  {
    ctx.connect(remoteAddress, localAddress, promise);
  }
  






  @ChannelHandlerMask.Skip
  public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise)
    throws Exception
  {
    ctx.disconnect(promise);
  }
  





  @ChannelHandlerMask.Skip
  public void close(ChannelHandlerContext ctx, ChannelPromise promise)
    throws Exception
  {
    ctx.close(promise);
  }
  





  @ChannelHandlerMask.Skip
  public void deregister(ChannelHandlerContext ctx, ChannelPromise promise)
    throws Exception
  {
    ctx.deregister(promise);
  }
  





  @ChannelHandlerMask.Skip
  public void read(ChannelHandlerContext ctx)
    throws Exception
  {
    ctx.read();
  }
  





  @ChannelHandlerMask.Skip
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
    throws Exception
  {
    ctx.write(msg, promise);
  }
  





  @ChannelHandlerMask.Skip
  public void flush(ChannelHandlerContext ctx)
    throws Exception
  {
    ctx.flush();
  }
}
