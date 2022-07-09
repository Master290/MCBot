package io.netty.channel;

















public class ChannelInboundHandlerAdapter
  extends ChannelHandlerAdapter
  implements ChannelInboundHandler
{
  public ChannelInboundHandlerAdapter() {}
  
















  @ChannelHandlerMask.Skip
  public void channelRegistered(ChannelHandlerContext ctx)
    throws Exception
  {
    ctx.fireChannelRegistered();
  }
  





  @ChannelHandlerMask.Skip
  public void channelUnregistered(ChannelHandlerContext ctx)
    throws Exception
  {
    ctx.fireChannelUnregistered();
  }
  





  @ChannelHandlerMask.Skip
  public void channelActive(ChannelHandlerContext ctx)
    throws Exception
  {
    ctx.fireChannelActive();
  }
  





  @ChannelHandlerMask.Skip
  public void channelInactive(ChannelHandlerContext ctx)
    throws Exception
  {
    ctx.fireChannelInactive();
  }
  





  @ChannelHandlerMask.Skip
  public void channelRead(ChannelHandlerContext ctx, Object msg)
    throws Exception
  {
    ctx.fireChannelRead(msg);
  }
  





  @ChannelHandlerMask.Skip
  public void channelReadComplete(ChannelHandlerContext ctx)
    throws Exception
  {
    ctx.fireChannelReadComplete();
  }
  





  @ChannelHandlerMask.Skip
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
    throws Exception
  {
    ctx.fireUserEventTriggered(evt);
  }
  





  @ChannelHandlerMask.Skip
  public void channelWritabilityChanged(ChannelHandlerContext ctx)
    throws Exception
  {
    ctx.fireChannelWritabilityChanged();
  }
  







  @ChannelHandlerMask.Skip
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    throws Exception
  {
    ctx.fireExceptionCaught(cause);
  }
}
