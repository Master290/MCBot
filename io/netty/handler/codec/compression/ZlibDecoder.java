package io.netty.handler.codec.compression;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.internal.ObjectUtil;
























public abstract class ZlibDecoder
  extends ByteToMessageDecoder
{
  protected final int maxAllocation;
  
  public ZlibDecoder()
  {
    this(0);
  }
  





  public ZlibDecoder(int maxAllocation)
  {
    this.maxAllocation = ObjectUtil.checkPositiveOrZero(maxAllocation, "maxAllocation");
  }
  




  public abstract boolean isClosed();
  



  protected ByteBuf prepareDecompressBuffer(ChannelHandlerContext ctx, ByteBuf buffer, int preferredSize)
  {
    if (buffer == null) {
      if (maxAllocation == 0) {
        return ctx.alloc().heapBuffer(preferredSize);
      }
      
      return ctx.alloc().heapBuffer(Math.min(preferredSize, maxAllocation), maxAllocation);
    }
    



    if (buffer.ensureWritable(preferredSize, true) == 1)
    {


      decompressionBufferExhausted(buffer.duplicate());
      buffer.skipBytes(buffer.readableBytes());
      throw new DecompressionException("Decompression buffer has reached maximum size: " + buffer.maxCapacity());
    }
    
    return buffer;
  }
  
  protected void decompressionBufferExhausted(ByteBuf buffer) {}
}
