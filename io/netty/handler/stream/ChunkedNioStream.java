package io.netty.handler.stream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.ObjectUtil;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;






























public class ChunkedNioStream
  implements ChunkedInput<ByteBuf>
{
  private final ReadableByteChannel in;
  private final int chunkSize;
  private long offset;
  private final ByteBuffer byteBuffer;
  
  public ChunkedNioStream(ReadableByteChannel in)
  {
    this(in, 8192);
  }
  





  public ChunkedNioStream(ReadableByteChannel in, int chunkSize)
  {
    this.in = ((ReadableByteChannel)ObjectUtil.checkNotNull(in, "in"));
    this.chunkSize = ObjectUtil.checkPositive(chunkSize, "chunkSize");
    byteBuffer = ByteBuffer.allocate(chunkSize);
  }
  


  public long transferredBytes()
  {
    return offset;
  }
  
  public boolean isEndOfInput() throws Exception
  {
    if (byteBuffer.position() > 0)
    {
      return false;
    }
    if (in.isOpen())
    {
      int b = in.read(byteBuffer);
      if (b < 0) {
        return true;
      }
      offset += b;
      return false;
    }
    
    return true;
  }
  
  public void close() throws Exception
  {
    in.close();
  }
  
  @Deprecated
  public ByteBuf readChunk(ChannelHandlerContext ctx) throws Exception
  {
    return readChunk(ctx.alloc());
  }
  
  public ByteBuf readChunk(ByteBufAllocator allocator) throws Exception
  {
    if (isEndOfInput()) {
      return null;
    }
    
    int readBytes = byteBuffer.position();
    for (;;) {
      int localReadBytes = in.read(byteBuffer);
      if (localReadBytes < 0) {
        break;
      }
      readBytes += localReadBytes;
      offset += localReadBytes;
      if (readBytes == chunkSize) {
        break;
      }
    }
    byteBuffer.flip();
    boolean release = true;
    ByteBuf buffer = allocator.buffer(byteBuffer.remaining());
    try {
      buffer.writeBytes(byteBuffer);
      byteBuffer.clear();
      release = false;
      return buffer;
    } finally {
      if (release) {
        buffer.release();
      }
    }
  }
  
  public long length()
  {
    return -1L;
  }
  
  public long progress()
  {
    return offset;
  }
}
