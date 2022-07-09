package io.netty.handler.codec.protobuf;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import java.util.List;



































public class ProtobufVarint32FrameDecoder
  extends ByteToMessageDecoder
{
  public ProtobufVarint32FrameDecoder() {}
  
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
    throws Exception
  {
    in.markReaderIndex();
    int preIndex = in.readerIndex();
    int length = readRawVarint32(in);
    if (preIndex == in.readerIndex()) {
      return;
    }
    if (length < 0) {
      throw new CorruptedFrameException("negative length: " + length);
    }
    
    if (in.readableBytes() < length) {
      in.resetReaderIndex();
    } else {
      out.add(in.readRetainedSlice(length));
    }
  }
  




  private static int readRawVarint32(ByteBuf buffer)
  {
    if (!buffer.isReadable()) {
      return 0;
    }
    buffer.markReaderIndex();
    byte tmp = buffer.readByte();
    if (tmp >= 0) {
      return tmp;
    }
    int result = tmp & 0x7F;
    if (!buffer.isReadable()) {
      buffer.resetReaderIndex();
      return 0;
    }
    if ((tmp = buffer.readByte()) >= 0) {
      result |= tmp << 7;
    } else {
      result |= (tmp & 0x7F) << 7;
      if (!buffer.isReadable()) {
        buffer.resetReaderIndex();
        return 0;
      }
      if ((tmp = buffer.readByte()) >= 0) {
        result |= tmp << 14;
      } else {
        result |= (tmp & 0x7F) << 14;
        if (!buffer.isReadable()) {
          buffer.resetReaderIndex();
          return 0;
        }
        if ((tmp = buffer.readByte()) >= 0) {
          result |= tmp << 21;
        } else {
          result |= (tmp & 0x7F) << 21;
          if (!buffer.isReadable()) {
            buffer.resetReaderIndex();
            return 0;
          }
          result |= (tmp = buffer.readByte()) << 28;
          if (tmp < 0) {
            throw new CorruptedFrameException("malformed varint.");
          }
        }
      }
    }
    return result;
  }
}
