package io.netty.handler.codec.compression;

import com.aayushatharva.brotli4j.encoder.Encoder;
import com.aayushatharva.brotli4j.encoder.Encoder.Parameters;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.ObjectUtil;






















@ChannelHandler.Sharable
public final class BrotliEncoder
  extends MessageToByteEncoder<ByteBuf>
{
  private final Encoder.Parameters parameters;
  
  public BrotliEncoder()
  {
    this(BrotliOptions.DEFAULT);
  }
  




  public BrotliEncoder(Encoder.Parameters parameters)
  {
    this.parameters = ((Encoder.Parameters)ObjectUtil.checkNotNull(parameters, "Parameters"));
  }
  




  public BrotliEncoder(BrotliOptions brotliOptions)
  {
    this(brotliOptions.parameters());
  }
  

  protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out)
    throws Exception
  {}
  
  protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, ByteBuf msg, boolean preferDirect)
    throws Exception
  {
    if (!msg.isReadable()) {
      return Unpooled.EMPTY_BUFFER;
    }
    try
    {
      byte[] uncompressed = ByteBufUtil.getBytes(msg, msg.readerIndex(), msg.readableBytes(), false);
      byte[] compressed = Encoder.compress(uncompressed, parameters);
      if (preferDirect) {
        ByteBuf out = ctx.alloc().ioBuffer(compressed.length);
        out.writeBytes(compressed);
        return out;
      }
      return Unpooled.wrappedBuffer(compressed);
    }
    catch (Exception e) {
      ReferenceCountUtil.release(msg);
      throw e;
    }
  }
}
