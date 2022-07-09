package io.netty.handler.codec.http.websocketx.extensions;

public abstract interface WebSocketExtension
{
  public static final int RSV1 = 4;
  public static final int RSV2 = 2;
  public static final int RSV3 = 1;
  
  public abstract int rsv();
  
  public abstract WebSocketExtensionEncoder newExtensionEncoder();
  
  public abstract WebSocketExtensionDecoder newExtensionDecoder();
}
