package io.netty.handler.stream;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;

public abstract interface ChunkedInput<B>
{
  public abstract boolean isEndOfInput()
    throws Exception;
  
  public abstract void close()
    throws Exception;
  
  @Deprecated
  public abstract B readChunk(ChannelHandlerContext paramChannelHandlerContext)
    throws Exception;
  
  public abstract B readChunk(ByteBufAllocator paramByteBufAllocator)
    throws Exception;
  
  public abstract long length();
  
  public abstract long progress();
}
