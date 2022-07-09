package io.netty.handler.codec.http.websocketx.extensions;

public abstract interface WebSocketClientExtensionHandshaker
{
  public abstract WebSocketExtensionData newRequestData();
  
  public abstract WebSocketClientExtension handshakeExtension(WebSocketExtensionData paramWebSocketExtensionData);
}
