package io.netty.handler.codec.http.websocketx.extensions;

import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

public abstract class WebSocketExtensionDecoder
  extends MessageToMessageDecoder<WebSocketFrame>
{
  public WebSocketExtensionDecoder() {}
}
