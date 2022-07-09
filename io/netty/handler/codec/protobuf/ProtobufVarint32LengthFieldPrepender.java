package io.netty.handler.codec.protobuf;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;






























@ChannelHandler.Sharable
public class ProtobufVarint32LengthFieldPrepender
  extends MessageToByteEncoder<ByteBuf>
{
  public ProtobufVarint32LengthFieldPrepender() {}
  
  protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out)
    throws Exception
  {
    int bodyLen = msg.readableBytes();
    int headerLen = computeRawVarint32Size(bodyLen);
    out.ensureWritable(headerLen + bodyLen);
    writeRawVarint32(out, bodyLen);
    out.writeBytes(msg, msg.readerIndex(), bodyLen);
  }
  



  static void writeRawVarint32(ByteBuf out, int value)
  {
    for (;;)
    {
      if ((value & 0xFFFFFF80) == 0) {
        out.writeByte(value);
        return;
      }
      out.writeByte(value & 0x7F | 0x80);
      value >>>= 7;
    }
  }
  





  static int computeRawVarint32Size(int value)
  {
    if ((value & 0xFFFFFF80) == 0) {
      return 1;
    }
    if ((value & 0xC000) == 0) {
      return 2;
    }
    if ((value & 0xFFE00000) == 0) {
      return 3;
    }
    if ((value & 0xF0000000) == 0) {
      return 4;
    }
    return 5;
  }
}
