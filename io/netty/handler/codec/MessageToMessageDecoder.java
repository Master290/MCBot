package io.netty.handler.codec;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.TypeParameterMatcher;
import java.util.List;













































public abstract class MessageToMessageDecoder<I>
  extends ChannelInboundHandlerAdapter
{
  private final TypeParameterMatcher matcher;
  
  protected MessageToMessageDecoder()
  {
    matcher = TypeParameterMatcher.find(this, MessageToMessageDecoder.class, "I");
  }
  




  protected MessageToMessageDecoder(Class<? extends I> inboundMessageType)
  {
    matcher = TypeParameterMatcher.get(inboundMessageType);
  }
  


  public boolean acceptInboundMessage(Object msg)
    throws Exception
  {
    return matcher.match(msg);
  }
  
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
  {
    CodecOutputList out = CodecOutputList.newInstance();
    try {
      if (acceptInboundMessage(msg))
      {
        I cast = msg;
        try {
          decode(ctx, cast, out);
        } finally {
          ReferenceCountUtil.release(cast);
        }
      } else {
        out.add(msg);
      } } catch (DecoderException e) { int size;
      int i;
      throw e;
    } catch (Exception e) {
      throw new DecoderException(e);
    } finally {
      try {
        int size = out.size();
        for (int i = 0; i < size; i++) {
          ctx.fireChannelRead(out.getUnsafe(i));
        }
      } finally {
        out.recycle();
      }
    }
  }
  
  protected abstract void decode(ChannelHandlerContext paramChannelHandlerContext, I paramI, List<Object> paramList)
    throws Exception;
}
