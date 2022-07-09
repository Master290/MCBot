package io.netty.handler.codec.compression;

import com.aayushatharva.brotli4j.decoder.DecoderJNI.Wrapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.internal.ObjectUtil;
import java.nio.ByteBuffer;
import java.util.List;

















public final class BrotliDecoder
  extends ByteToMessageDecoder
{
  private final int inputBufferSize;
  private DecoderJNI.Wrapper decoder;
  private boolean destroyed;
  
  private static enum State
  {
    DONE,  NEEDS_MORE_INPUT,  ERROR;
    
    private State() {}
  }
  
  static {
    try { 
    } catch (Throwable throwable) { throw new ExceptionInInitializerError(throwable);
    }
  }
  






  public BrotliDecoder()
  {
    this(8192);
  }
  



  public BrotliDecoder(int inputBufferSize)
  {
    this.inputBufferSize = ObjectUtil.checkPositive(inputBufferSize, "inputBufferSize");
  }
  
  private ByteBuf pull(ByteBufAllocator alloc) {
    ByteBuffer nativeBuffer = decoder.pull();
    
    ByteBuf copy = alloc.buffer(nativeBuffer.remaining());
    copy.writeBytes(nativeBuffer);
    return copy;
  }
  
  private State decompress(ByteBuf input, List<Object> output, ByteBufAllocator alloc) {
    for (;;) {
      switch (1.$SwitchMap$com$aayushatharva$brotli4j$decoder$DecoderJNI$Status[decoder.getStatus().ordinal()]) {
      case 1: 
        return State.DONE;
      
      case 2: 
        decoder.push(0);
        break;
      
      case 3: 
        if (decoder.hasOutput()) {
          output.add(pull(alloc));
        }
        
        if (!input.isReadable()) {
          return State.NEEDS_MORE_INPUT;
        }
        
        ByteBuffer decoderInputBuffer = decoder.getInputBuffer();
        decoderInputBuffer.clear();
        int readBytes = readBytes(input, decoderInputBuffer);
        decoder.push(readBytes);
        break;
      
      case 4: 
        output.add(pull(alloc));
      }
      
    }
    return State.ERROR;
  }
  

  private static int readBytes(ByteBuf in, ByteBuffer dest)
  {
    int limit = Math.min(in.readableBytes(), dest.remaining());
    ByteBuffer slice = dest.slice();
    slice.limit(limit);
    in.readBytes(slice);
    dest.position(dest.position() + limit);
    return limit;
  }
  
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception
  {
    decoder = new DecoderJNI.Wrapper(inputBufferSize);
  }
  
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
  {
    if (destroyed)
    {
      in.skipBytes(in.readableBytes());
      return;
    }
    
    if (!in.isReadable()) {
      return;
    }
    try
    {
      State state = decompress(in, out, ctx.alloc());
      if (state == State.DONE) {
        destroy();
      } else if (state == State.ERROR) {
        throw new DecompressionException("Brotli stream corrupted");
      }
    } catch (Exception e) {
      destroy();
      throw e;
    }
  }
  
  private void destroy() {
    if (!destroyed) {
      destroyed = true;
      decoder.destroy();
    }
  }
  
  protected void handlerRemoved0(ChannelHandlerContext ctx) throws Exception
  {
    try {
      destroy();
      
      super.handlerRemoved0(ctx); } finally { super.handlerRemoved0(ctx);
    }
  }
  
  public void channelInactive(ChannelHandlerContext ctx) throws Exception
  {
    try {
      destroy();
      
      super.channelInactive(ctx); } finally { super.channelInactive(ctx);
    }
  }
}
