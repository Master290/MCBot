package io.netty.handler.codec.http.websocketx.extensions.compression;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketExtensionFilter;
























class PerFrameDeflateEncoder
  extends DeflateEncoder
{
  PerFrameDeflateEncoder(int compressionLevel, int windowSize, boolean noContext)
  {
    super(compressionLevel, windowSize, noContext, WebSocketExtensionFilter.NEVER_SKIP);
  }
  








  PerFrameDeflateEncoder(int compressionLevel, int windowSize, boolean noContext, WebSocketExtensionFilter extensionEncoderFilter)
  {
    super(compressionLevel, windowSize, noContext, extensionEncoderFilter);
  }
  
  public boolean acceptOutboundMessage(Object msg) throws Exception
  {
    if (!super.acceptOutboundMessage(msg)) {
      return false;
    }
    
    WebSocketFrame wsFrame = (WebSocketFrame)msg;
    if (extensionEncoderFilter().mustSkip(wsFrame)) {
      return false;
    }
    
    if (((msg instanceof TextWebSocketFrame)) || ((msg instanceof BinaryWebSocketFrame)) || ((msg instanceof ContinuationWebSocketFrame))) {} return 
    
      (wsFrame.content().readableBytes() > 0) && 
      ((wsFrame.rsv() & 0x4) == 0);
  }
  
  protected int rsv(WebSocketFrame msg)
  {
    return msg.rsv() | 0x4;
  }
  
  protected boolean removeFrameTail(WebSocketFrame msg)
  {
    return true;
  }
}
