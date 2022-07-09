package io.netty.handler.codec.http.websocketx.extensions;

public abstract interface WebSocketServerExtension
  extends WebSocketExtension
{
  public abstract WebSocketExtensionData newReponseData();
}
