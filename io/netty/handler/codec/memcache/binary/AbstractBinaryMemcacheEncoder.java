package io.netty.handler.codec.memcache.binary;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.memcache.AbstractMemcacheObjectEncoder;






















public abstract class AbstractBinaryMemcacheEncoder<M extends BinaryMemcacheMessage>
  extends AbstractMemcacheObjectEncoder<M>
{
  private static final int MINIMUM_HEADER_SIZE = 24;
  
  public AbstractBinaryMemcacheEncoder() {}
  
  protected ByteBuf encodeMessage(ChannelHandlerContext ctx, M msg)
  {
    ByteBuf buf = ctx.alloc().buffer(24 + msg.extrasLength() + msg
      .keyLength());
    
    encodeHeader(buf, msg);
    encodeExtras(buf, msg.extras());
    encodeKey(buf, msg.key());
    
    return buf;
  }
  





  private static void encodeExtras(ByteBuf buf, ByteBuf extras)
  {
    if ((extras == null) || (!extras.isReadable())) {
      return;
    }
    
    buf.writeBytes(extras);
  }
  





  private static void encodeKey(ByteBuf buf, ByteBuf key)
  {
    if ((key == null) || (!key.isReadable())) {
      return;
    }
    
    buf.writeBytes(key);
  }
  
  protected abstract void encodeHeader(ByteBuf paramByteBuf, M paramM);
}
