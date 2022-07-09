package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.internal.ObjectUtil;
import java.util.List;































public class DatagramPacketDecoder
  extends MessageToMessageDecoder<DatagramPacket>
{
  private final MessageToMessageDecoder<ByteBuf> decoder;
  
  public DatagramPacketDecoder(MessageToMessageDecoder<ByteBuf> decoder)
  {
    this.decoder = ((MessageToMessageDecoder)ObjectUtil.checkNotNull(decoder, "decoder"));
  }
  
  public boolean acceptInboundMessage(Object msg) throws Exception
  {
    if ((msg instanceof DatagramPacket)) {
      return decoder.acceptInboundMessage(((DatagramPacket)msg).content());
    }
    return false;
  }
  
  protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception
  {
    decoder.decode(ctx, msg.content(), out);
  }
  
  public void channelRegistered(ChannelHandlerContext ctx) throws Exception
  {
    decoder.channelRegistered(ctx);
  }
  
  public void channelUnregistered(ChannelHandlerContext ctx) throws Exception
  {
    decoder.channelUnregistered(ctx);
  }
  
  public void channelActive(ChannelHandlerContext ctx) throws Exception
  {
    decoder.channelActive(ctx);
  }
  
  public void channelInactive(ChannelHandlerContext ctx) throws Exception
  {
    decoder.channelInactive(ctx);
  }
  
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception
  {
    decoder.channelReadComplete(ctx);
  }
  
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception
  {
    decoder.userEventTriggered(ctx, evt);
  }
  
  public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception
  {
    decoder.channelWritabilityChanged(ctx);
  }
  
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
  {
    decoder.exceptionCaught(ctx, cause);
  }
  
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception
  {
    decoder.handlerAdded(ctx);
  }
  
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception
  {
    decoder.handlerRemoved(ctx);
  }
  
  public boolean isSharable()
  {
    return decoder.isSharable();
  }
}
