package io.netty.handler.codec.http.websocketx.extensions.compression;

import io.netty.handler.codec.http.websocketx.extensions.WebSocketClientExtension;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketClientExtensionHandshaker;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketExtensionData;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketExtensionDecoder;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketExtensionEncoder;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketExtensionFilterProvider;
import io.netty.util.internal.ObjectUtil;
import java.util.Collections;
import java.util.Map;























public final class DeflateFrameClientExtensionHandshaker
  implements WebSocketClientExtensionHandshaker
{
  private final int compressionLevel;
  private final boolean useWebkitExtensionName;
  private final WebSocketExtensionFilterProvider extensionFilterProvider;
  
  public DeflateFrameClientExtensionHandshaker(boolean useWebkitExtensionName)
  {
    this(6, useWebkitExtensionName);
  }
  





  public DeflateFrameClientExtensionHandshaker(int compressionLevel, boolean useWebkitExtensionName)
  {
    this(compressionLevel, useWebkitExtensionName, WebSocketExtensionFilterProvider.DEFAULT);
  }
  








  public DeflateFrameClientExtensionHandshaker(int compressionLevel, boolean useWebkitExtensionName, WebSocketExtensionFilterProvider extensionFilterProvider)
  {
    if ((compressionLevel < 0) || (compressionLevel > 9)) {
      throw new IllegalArgumentException("compressionLevel: " + compressionLevel + " (expected: 0-9)");
    }
    
    this.compressionLevel = compressionLevel;
    this.useWebkitExtensionName = useWebkitExtensionName;
    this.extensionFilterProvider = ((WebSocketExtensionFilterProvider)ObjectUtil.checkNotNull(extensionFilterProvider, "extensionFilterProvider"));
  }
  
  public WebSocketExtensionData newRequestData()
  {
    return new WebSocketExtensionData(useWebkitExtensionName ? "x-webkit-deflate-frame" : "deflate-frame", 
    
      Collections.emptyMap());
  }
  
  public WebSocketClientExtension handshakeExtension(WebSocketExtensionData extensionData)
  {
    if ((!"x-webkit-deflate-frame".equals(extensionData.name())) && 
      (!"deflate-frame".equals(extensionData.name()))) {
      return null;
    }
    
    if (extensionData.parameters().isEmpty()) {
      return new DeflateFrameClientExtension(compressionLevel, extensionFilterProvider);
    }
    return null;
  }
  
  private static class DeflateFrameClientExtension implements WebSocketClientExtension
  {
    private final int compressionLevel;
    private final WebSocketExtensionFilterProvider extensionFilterProvider;
    
    DeflateFrameClientExtension(int compressionLevel, WebSocketExtensionFilterProvider extensionFilterProvider)
    {
      this.compressionLevel = compressionLevel;
      this.extensionFilterProvider = extensionFilterProvider;
    }
    
    public int rsv()
    {
      return 4;
    }
    
    public WebSocketExtensionEncoder newExtensionEncoder()
    {
      return new PerFrameDeflateEncoder(compressionLevel, 15, false, extensionFilterProvider
        .encoderFilter());
    }
    
    public WebSocketExtensionDecoder newExtensionDecoder()
    {
      return new PerFrameDeflateDecoder(false, extensionFilterProvider.decoderFilter());
    }
  }
}
