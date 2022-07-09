package io.netty.handler.codec.string;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.ObjectUtil;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.List;









































@ChannelHandler.Sharable
public class LineEncoder
  extends MessageToMessageEncoder<CharSequence>
{
  private final Charset charset;
  private final byte[] lineSeparator;
  
  public LineEncoder()
  {
    this(LineSeparator.DEFAULT, CharsetUtil.UTF_8);
  }
  


  public LineEncoder(LineSeparator lineSeparator)
  {
    this(lineSeparator, CharsetUtil.UTF_8);
  }
  


  public LineEncoder(Charset charset)
  {
    this(LineSeparator.DEFAULT, charset);
  }
  


  public LineEncoder(LineSeparator lineSeparator, Charset charset)
  {
    this.charset = ((Charset)ObjectUtil.checkNotNull(charset, "charset"));
    this.lineSeparator = ((LineSeparator)ObjectUtil.checkNotNull(lineSeparator, "lineSeparator")).value().getBytes(charset);
  }
  
  protected void encode(ChannelHandlerContext ctx, CharSequence msg, List<Object> out) throws Exception
  {
    ByteBuf buffer = ByteBufUtil.encodeString(ctx.alloc(), CharBuffer.wrap(msg), charset, lineSeparator.length);
    buffer.writeBytes(lineSeparator);
    out.add(buffer);
  }
}
