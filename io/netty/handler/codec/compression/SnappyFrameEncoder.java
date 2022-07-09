package io.netty.handler.codec.compression;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;































public class SnappyFrameEncoder
  extends MessageToByteEncoder<ByteBuf>
{
  private static final int MIN_COMPRESSIBLE_LENGTH = 18;
  private static final byte[] STREAM_START = { -1, 6, 0, 0, 115, 78, 97, 80, 112, 89 };
  


  private final Snappy snappy = new Snappy();
  private boolean started;
  
  public SnappyFrameEncoder() {}
  
  protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws Exception { if (!in.isReadable()) {
      return;
    }
    
    if (!started) {
      started = true;
      out.writeBytes(STREAM_START);
    }
    
    int dataLength = in.readableBytes();
    if (dataLength > 18) {
      for (;;) {
        int lengthIdx = out.writerIndex() + 1;
        if (dataLength < 18) {
          ByteBuf slice = in.readSlice(dataLength);
          writeUnencodedChunk(slice, out, dataLength);
          break;
        }
        
        out.writeInt(0);
        if (dataLength > 32767) {
          ByteBuf slice = in.readSlice(32767);
          calculateAndWriteChecksum(slice, out);
          snappy.encode(slice, out, 32767);
          setChunkLength(out, lengthIdx);
          dataLength -= 32767;
        } else {
          ByteBuf slice = in.readSlice(dataLength);
          calculateAndWriteChecksum(slice, out);
          snappy.encode(slice, out, dataLength);
          setChunkLength(out, lengthIdx);
          break;
        }
      }
    }
    writeUnencodedChunk(in, out, dataLength);
  }
  
  private static void writeUnencodedChunk(ByteBuf in, ByteBuf out, int dataLength)
  {
    out.writeByte(1);
    writeChunkLength(out, dataLength + 4);
    calculateAndWriteChecksum(in, out);
    out.writeBytes(in, dataLength);
  }
  
  private static void setChunkLength(ByteBuf out, int lengthIdx) {
    int chunkLength = out.writerIndex() - lengthIdx - 3;
    if (chunkLength >>> 24 != 0) {
      throw new CompressionException("compressed data too large: " + chunkLength);
    }
    out.setMediumLE(lengthIdx, chunkLength);
  }
  





  private static void writeChunkLength(ByteBuf out, int chunkLength)
  {
    out.writeMediumLE(chunkLength);
  }
  





  private static void calculateAndWriteChecksum(ByteBuf slice, ByteBuf out)
  {
    out.writeIntLE(Snappy.calculateChecksum(slice));
  }
}
