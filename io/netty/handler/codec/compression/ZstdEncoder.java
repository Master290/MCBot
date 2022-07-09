package io.netty.handler.codec.compression;

import com.github.luben.zstd.Zstd;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.internal.ObjectUtil;
import java.nio.ByteBuffer;





























public final class ZstdEncoder
  extends MessageToByteEncoder<ByteBuf>
{
  private final int blockSize;
  private final int compressionLevel;
  private final int maxEncodeSize;
  private ByteBuf buffer;
  
  public ZstdEncoder()
  {
    this(3, 65536, 33554432);
  }
  




  public ZstdEncoder(int compressionLevel)
  {
    this(compressionLevel, 65536, 33554432);
  }
  






  public ZstdEncoder(int blockSize, int maxEncodeSize)
  {
    this(3, blockSize, maxEncodeSize);
  }
  







  public ZstdEncoder(int compressionLevel, int blockSize, int maxEncodeSize)
  {
    super(true);
    this.compressionLevel = ObjectUtil.checkInRange(compressionLevel, 0, 22, "compressionLevel");
    this.blockSize = ObjectUtil.checkPositive(blockSize, "blockSize");
    this.maxEncodeSize = ObjectUtil.checkPositive(maxEncodeSize, "maxEncodeSize");
  }
  
  protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, ByteBuf msg, boolean preferDirect)
  {
    if (buffer == null) {
      throw new IllegalStateException("not added to a pipeline,or has been removed,buffer is null");
    }
    

    int remaining = msg.readableBytes() + buffer.readableBytes();
    

    if (remaining < 0) {
      throw new EncoderException("too much data to allocate a buffer for compression");
    }
    
    long bufferSize = 0L;
    while (remaining > 0) {
      int curSize = Math.min(blockSize, remaining);
      remaining -= curSize;
      bufferSize += Zstd.compressBound(curSize);
    }
    
    if ((bufferSize > maxEncodeSize) || (0L > bufferSize)) {
      throw new EncoderException("requested encode buffer size (" + bufferSize + " bytes) exceeds the maximum allowable size (" + maxEncodeSize + " bytes)");
    }
    

    return ctx.alloc().directBuffer((int)bufferSize);
  }
  
  protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out)
  {
    if (this.buffer == null) {
      throw new IllegalStateException("not added to a pipeline,or has been removed,buffer is null");
    }
    

    ByteBuf buffer = this.buffer;
    int length;
    while ((length = in.readableBytes()) > 0) {
      int nextChunkSize = Math.min(length, buffer.writableBytes());
      in.readBytes(buffer, nextChunkSize);
      
      if (!buffer.isWritable()) {
        flushBufferedData(out);
      }
    }
  }
  
  private void flushBufferedData(ByteBuf out) {
    int flushableBytes = buffer.readableBytes();
    if (flushableBytes == 0) {
      return;
    }
    
    int bufSize = (int)Zstd.compressBound(flushableBytes);
    out.ensureWritable(bufSize);
    int idx = out.writerIndex();
    try
    {
      ByteBuffer outNioBuffer = out.internalNioBuffer(idx, out.writableBytes());
      compressedLength = Zstd.compress(outNioBuffer, buffer
      
        .internalNioBuffer(buffer.readerIndex(), flushableBytes), compressionLevel);
    } catch (Exception e) {
      int compressedLength;
      throw new CompressionException(e);
    }
    int compressedLength;
    out.writerIndex(idx + compressedLength);
    buffer.clear();
  }
  
  public void flush(ChannelHandlerContext ctx)
  {
    if ((buffer != null) && (buffer.isReadable())) {
      ByteBuf buf = allocateBuffer(ctx, Unpooled.EMPTY_BUFFER, isPreferDirect());
      flushBufferedData(buf);
      ctx.write(buf);
    }
    ctx.flush();
  }
  
  public void handlerAdded(ChannelHandlerContext ctx)
  {
    buffer = ctx.alloc().directBuffer(blockSize);
    buffer.clear();
  }
  
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception
  {
    super.handlerRemoved(ctx);
    if (buffer != null) {
      buffer.release();
      buffer = null;
    }
  }
}
