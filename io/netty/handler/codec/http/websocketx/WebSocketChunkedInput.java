package io.netty.handler.codec.http.websocketx;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.stream.ChunkedInput;
import io.netty.util.internal.ObjectUtil;
























public final class WebSocketChunkedInput
  implements ChunkedInput<WebSocketFrame>
{
  private final ChunkedInput<ByteBuf> input;
  private final int rsv;
  
  public WebSocketChunkedInput(ChunkedInput<ByteBuf> input)
  {
    this(input, 0);
  }
  






  public WebSocketChunkedInput(ChunkedInput<ByteBuf> input, int rsv)
  {
    this.input = ((ChunkedInput)ObjectUtil.checkNotNull(input, "input"));
    this.rsv = rsv;
  }
  



  public boolean isEndOfInput()
    throws Exception
  {
    return input.isEndOfInput();
  }
  


  public void close()
    throws Exception
  {
    input.close();
  }
  









  @Deprecated
  public WebSocketFrame readChunk(ChannelHandlerContext ctx)
    throws Exception
  {
    return readChunk(ctx.alloc());
  }
  







  public WebSocketFrame readChunk(ByteBufAllocator allocator)
    throws Exception
  {
    ByteBuf buf = (ByteBuf)input.readChunk(allocator);
    if (buf == null) {
      return null;
    }
    return new ContinuationWebSocketFrame(input.isEndOfInput(), rsv, buf);
  }
  
  public long length()
  {
    return input.length();
  }
  
  public long progress()
  {
    return input.progress();
  }
}
