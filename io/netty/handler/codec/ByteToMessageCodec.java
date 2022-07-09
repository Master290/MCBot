package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.internal.TypeParameterMatcher;
import java.util.List;

























public abstract class ByteToMessageCodec<I>
  extends ChannelDuplexHandler
{
  private final TypeParameterMatcher outboundMsgMatcher;
  private final MessageToByteEncoder<I> encoder;
  private final ByteToMessageDecoder decoder = new ByteToMessageDecoder()
  {
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
      ByteToMessageCodec.this.decode(ctx, in, out);
    }
    
    protected void decodeLast(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
    {
      ByteToMessageCodec.this.decodeLast(ctx, in, out);
    }
  };
  


  protected ByteToMessageCodec()
  {
    this(true);
  }
  


  protected ByteToMessageCodec(Class<? extends I> outboundMessageType)
  {
    this(outboundMessageType, true);
  }
  






  protected ByteToMessageCodec(boolean preferDirect)
  {
    ensureNotSharable();
    outboundMsgMatcher = TypeParameterMatcher.find(this, ByteToMessageCodec.class, "I");
    encoder = new Encoder(preferDirect);
  }
  







  protected ByteToMessageCodec(Class<? extends I> outboundMessageType, boolean preferDirect)
  {
    ensureNotSharable();
    outboundMsgMatcher = TypeParameterMatcher.get(outboundMessageType);
    encoder = new Encoder(preferDirect);
  }
  



  public boolean acceptOutboundMessage(Object msg)
    throws Exception
  {
    return outboundMsgMatcher.match(msg);
  }
  
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
  {
    decoder.channelRead(ctx, msg);
  }
  
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception
  {
    encoder.write(ctx, msg, promise);
  }
  
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception
  {
    decoder.channelReadComplete(ctx);
  }
  
  public void channelInactive(ChannelHandlerContext ctx) throws Exception
  {
    decoder.channelInactive(ctx);
  }
  
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception
  {
    try {
      decoder.handlerAdded(ctx);
      
      encoder.handlerAdded(ctx); } finally { encoder.handlerAdded(ctx);
    }
  }
  
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception
  {
    try {
      decoder.handlerRemoved(ctx);
      
      encoder.handlerRemoved(ctx); } finally { encoder.handlerRemoved(ctx);
    }
  }
  


  protected abstract void encode(ChannelHandlerContext paramChannelHandlerContext, I paramI, ByteBuf paramByteBuf)
    throws Exception;
  


  protected abstract void decode(ChannelHandlerContext paramChannelHandlerContext, ByteBuf paramByteBuf, List<Object> paramList)
    throws Exception;
  

  protected void decodeLast(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
    throws Exception
  {
    if (in.isReadable())
    {

      decode(ctx, in, out);
    }
  }
  
  private final class Encoder extends MessageToByteEncoder<I> {
    Encoder(boolean preferDirect) {
      super();
    }
    
    public boolean acceptOutboundMessage(Object msg) throws Exception
    {
      return ByteToMessageCodec.this.acceptOutboundMessage(msg);
    }
    
    protected void encode(ChannelHandlerContext ctx, I msg, ByteBuf out) throws Exception
    {
      ByteToMessageCodec.this.encode(ctx, msg, out);
    }
  }
}
