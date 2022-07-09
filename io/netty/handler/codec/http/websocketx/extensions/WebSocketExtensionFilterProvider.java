package io.netty.handler.codec.http.websocketx.extensions;



















public abstract interface WebSocketExtensionFilterProvider
{
  public static final WebSocketExtensionFilterProvider DEFAULT = new WebSocketExtensionFilterProvider()
  {
    public WebSocketExtensionFilter encoderFilter() {
      return WebSocketExtensionFilter.NEVER_SKIP;
    }
    
    public WebSocketExtensionFilter decoderFilter()
    {
      return WebSocketExtensionFilter.NEVER_SKIP;
    }
  };
  
  public abstract WebSocketExtensionFilter encoderFilter();
  
  public abstract WebSocketExtensionFilter decoderFilter();
}
