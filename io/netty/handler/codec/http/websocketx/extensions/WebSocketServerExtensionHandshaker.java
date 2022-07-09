package io.netty.handler.codec.http.websocketx.extensions;

public abstract interface WebSocketServerExtensionHandshaker
{
  public abstract WebSocketServerExtension handshakeExtension(WebSocketExtensionData paramWebSocketExtensionData);
}
