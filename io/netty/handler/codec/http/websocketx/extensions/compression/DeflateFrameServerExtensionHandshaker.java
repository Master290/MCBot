package io.netty.handler.codec.http.websocketx.extensions.compression;

import io.netty.handler.codec.http.websocketx.extensions.WebSocketExtensionData;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketExtensionDecoder;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketExtensionEncoder;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketExtensionFilterProvider;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketServerExtension;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketServerExtensionHandshaker;
import io.netty.util.internal.ObjectUtil;
import java.util.Collections;
import java.util.Map;























public final class DeflateFrameServerExtensionHandshaker
  implements WebSocketServerExtensionHandshaker
{
  static final String X_WEBKIT_DEFLATE_FRAME_EXTENSION = "x-webkit-deflate-frame";
  static final String DEFLATE_FRAME_EXTENSION = "deflate-frame";
  private final int compressionLevel;
  private final WebSocketExtensionFilterProvider extensionFilterProvider;
  
  public DeflateFrameServerExtensionHandshaker()
  {
    this(6);
  }
  





  public DeflateFrameServerExtensionHandshaker(int compressionLevel)
  {
    this(compressionLevel, WebSocketExtensionFilterProvider.DEFAULT);
  }
  








  public DeflateFrameServerExtensionHandshaker(int compressionLevel, WebSocketExtensionFilterProvider extensionFilterProvider)
  {
    if ((compressionLevel < 0) || (compressionLevel > 9)) {
      throw new IllegalArgumentException("compressionLevel: " + compressionLevel + " (expected: 0-9)");
    }
    
    this.compressionLevel = compressionLevel;
    this.extensionFilterProvider = ((WebSocketExtensionFilterProvider)ObjectUtil.checkNotNull(extensionFilterProvider, "extensionFilterProvider"));
  }
  
  public WebSocketServerExtension handshakeExtension(WebSocketExtensionData extensionData)
  {
    if ((!"x-webkit-deflate-frame".equals(extensionData.name())) && 
      (!"deflate-frame".equals(extensionData.name()))) {
      return null;
    }
    
    if (extensionData.parameters().isEmpty()) {
      return new DeflateFrameServerExtension(compressionLevel, extensionData.name(), extensionFilterProvider);
    }
    return null;
  }
  
  private static class DeflateFrameServerExtension
    implements WebSocketServerExtension
  {
    private final String extensionName;
    private final int compressionLevel;
    private final WebSocketExtensionFilterProvider extensionFilterProvider;
    
    DeflateFrameServerExtension(int compressionLevel, String extensionName, WebSocketExtensionFilterProvider extensionFilterProvider)
    {
      this.extensionName = extensionName;
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
    
    public WebSocketExtensionData newReponseData()
    {
      return new WebSocketExtensionData(extensionName, Collections.emptyMap());
    }
  }
}
