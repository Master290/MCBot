package io.netty.handler.stream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.ObjectUtil;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
























public class ChunkedFile
  implements ChunkedInput<ByteBuf>
{
  private final RandomAccessFile file;
  private final long startOffset;
  private final long endOffset;
  private final int chunkSize;
  private long offset;
  
  public ChunkedFile(File file)
    throws IOException
  {
    this(file, 8192);
  }
  




  public ChunkedFile(File file, int chunkSize)
    throws IOException
  {
    this(new RandomAccessFile(file, "r"), chunkSize);
  }
  

  public ChunkedFile(RandomAccessFile file)
    throws IOException
  {
    this(file, 8192);
  }
  




  public ChunkedFile(RandomAccessFile file, int chunkSize)
    throws IOException
  {
    this(file, 0L, file.length(), chunkSize);
  }
  






  public ChunkedFile(RandomAccessFile file, long offset, long length, int chunkSize)
    throws IOException
  {
    ObjectUtil.checkNotNull(file, "file");
    ObjectUtil.checkPositiveOrZero(offset, "offset");
    ObjectUtil.checkPositiveOrZero(length, "length");
    ObjectUtil.checkPositive(chunkSize, "chunkSize");
    
    this.file = file;
    this.offset = (this.startOffset = offset);
    endOffset = (offset + length);
    this.chunkSize = chunkSize;
    
    file.seek(offset);
  }
  


  public long startOffset()
  {
    return startOffset;
  }
  


  public long endOffset()
  {
    return endOffset;
  }
  


  public long currentOffset()
  {
    return offset;
  }
  
  public boolean isEndOfInput() throws Exception
  {
    return (offset >= endOffset) || (!file.getChannel().isOpen());
  }
  
  public void close() throws Exception
  {
    file.close();
  }
  
  @Deprecated
  public ByteBuf readChunk(ChannelHandlerContext ctx) throws Exception
  {
    return readChunk(ctx.alloc());
  }
  
  public ByteBuf readChunk(ByteBufAllocator allocator) throws Exception
  {
    long offset = this.offset;
    if (offset >= endOffset) {
      return null;
    }
    
    int chunkSize = (int)Math.min(this.chunkSize, endOffset - offset);
    

    ByteBuf buf = allocator.heapBuffer(chunkSize);
    boolean release = true;
    try {
      file.readFully(buf.array(), buf.arrayOffset(), chunkSize);
      buf.writerIndex(chunkSize);
      this.offset = (offset + chunkSize);
      release = false;
      return buf;
    } finally {
      if (release) {
        buf.release();
      }
    }
  }
  
  public long length()
  {
    return endOffset - startOffset;
  }
  
  public long progress()
  {
    return offset - startOffset;
  }
}
