package io.netty.handler.codec.base64;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.internal.ObjectUtil;
import java.util.List;
































@ChannelHandler.Sharable
public class Base64Encoder
  extends MessageToMessageEncoder<ByteBuf>
{
  private final boolean breakLines;
  private final Base64Dialect dialect;
  
  public Base64Encoder()
  {
    this(true);
  }
  
  public Base64Encoder(boolean breakLines) {
    this(breakLines, Base64Dialect.STANDARD);
  }
  
  public Base64Encoder(boolean breakLines, Base64Dialect dialect) {
    this.dialect = ((Base64Dialect)ObjectUtil.checkNotNull(dialect, "dialect"));
    this.breakLines = breakLines;
  }
  
  protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception
  {
    out.add(Base64.encode(msg, msg.readerIndex(), msg.readableBytes(), breakLines, dialect));
  }
}
