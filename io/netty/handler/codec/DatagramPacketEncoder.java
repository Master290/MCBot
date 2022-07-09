package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.AddressedEnvelope;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;





































public class DatagramPacketEncoder<M>
  extends MessageToMessageEncoder<AddressedEnvelope<M, InetSocketAddress>>
{
  private final MessageToMessageEncoder<? super M> encoder;
  
  public DatagramPacketEncoder(MessageToMessageEncoder<? super M> encoder)
  {
    this.encoder = ((MessageToMessageEncoder)ObjectUtil.checkNotNull(encoder, "encoder"));
  }
  
  public boolean acceptOutboundMessage(Object msg) throws Exception
  {
    if (super.acceptOutboundMessage(msg))
    {
      AddressedEnvelope envelope = (AddressedEnvelope)msg;
      return (encoder.acceptOutboundMessage(envelope.content())) && 
        (((envelope.sender() instanceof InetSocketAddress)) || (envelope.sender() == null)) && 
        ((envelope.recipient() instanceof InetSocketAddress));
    }
    return false;
  }
  
  protected void encode(ChannelHandlerContext ctx, AddressedEnvelope<M, InetSocketAddress> msg, List<Object> out)
    throws Exception
  {
    assert (out.isEmpty());
    
    encoder.encode(ctx, msg.content(), out);
    if (out.size() != 1)
    {
      throw new EncoderException(StringUtil.simpleClassName(encoder) + " must produce only one message.");
    }
    Object content = out.get(0);
    if ((content instanceof ByteBuf))
    {
      out.set(0, new DatagramPacket((ByteBuf)content, (InetSocketAddress)msg.recipient(), (InetSocketAddress)msg.sender()));
    }
    else {
      throw new EncoderException(StringUtil.simpleClassName(encoder) + " must produce only ByteBuf.");
    }
  }
  
  public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception
  {
    encoder.bind(ctx, localAddress, promise);
  }
  

  public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise)
    throws Exception
  {
    encoder.connect(ctx, remoteAddress, localAddress, promise);
  }
  
  public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception
  {
    encoder.disconnect(ctx, promise);
  }
  
  public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception
  {
    encoder.close(ctx, promise);
  }
  
  public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception
  {
    encoder.deregister(ctx, promise);
  }
  
  public void read(ChannelHandlerContext ctx) throws Exception
  {
    encoder.read(ctx);
  }
  
  public void flush(ChannelHandlerContext ctx) throws Exception
  {
    encoder.flush(ctx);
  }
  
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception
  {
    encoder.handlerAdded(ctx);
  }
  
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception
  {
    encoder.handlerRemoved(ctx);
  }
  
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
  {
    encoder.exceptionCaught(ctx, cause);
  }
  
  public boolean isSharable()
  {
    return encoder.isSharable();
  }
}
