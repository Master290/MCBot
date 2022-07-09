package io.netty.handler.codec.http.websocketx;

import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.internal.ObjectUtil;
import java.net.URI;







































public final class WebSocketClientProtocolConfig
{
  static final boolean DEFAULT_PERFORM_MASKING = true;
  static final boolean DEFAULT_ALLOW_MASK_MISMATCH = false;
  static final boolean DEFAULT_HANDLE_CLOSE_FRAMES = true;
  static final boolean DEFAULT_DROP_PONG_FRAMES = true;
  private final URI webSocketUri;
  private final String subprotocol;
  private final WebSocketVersion version;
  private final boolean allowExtensions;
  private final HttpHeaders customHeaders;
  private final int maxFramePayloadLength;
  private final boolean performMasking;
  private final boolean allowMaskMismatch;
  private final boolean handleCloseFrames;
  private final WebSocketCloseStatus sendCloseFrame;
  private final boolean dropPongFrames;
  private final long handshakeTimeoutMillis;
  private final long forceCloseTimeoutMillis;
  private final boolean absoluteUpgradeUrl;
  
  private WebSocketClientProtocolConfig(URI webSocketUri, String subprotocol, WebSocketVersion version, boolean allowExtensions, HttpHeaders customHeaders, int maxFramePayloadLength, boolean performMasking, boolean allowMaskMismatch, boolean handleCloseFrames, WebSocketCloseStatus sendCloseFrame, boolean dropPongFrames, long handshakeTimeoutMillis, long forceCloseTimeoutMillis, boolean absoluteUpgradeUrl)
  {
    this.webSocketUri = webSocketUri;
    this.subprotocol = subprotocol;
    this.version = version;
    this.allowExtensions = allowExtensions;
    this.customHeaders = customHeaders;
    this.maxFramePayloadLength = maxFramePayloadLength;
    this.performMasking = performMasking;
    this.allowMaskMismatch = allowMaskMismatch;
    this.forceCloseTimeoutMillis = forceCloseTimeoutMillis;
    this.handleCloseFrames = handleCloseFrames;
    this.sendCloseFrame = sendCloseFrame;
    this.dropPongFrames = dropPongFrames;
    this.handshakeTimeoutMillis = ObjectUtil.checkPositive(handshakeTimeoutMillis, "handshakeTimeoutMillis");
    this.absoluteUpgradeUrl = absoluteUpgradeUrl;
  }
  
  public URI webSocketUri() {
    return webSocketUri;
  }
  
  public String subprotocol() {
    return subprotocol;
  }
  
  public WebSocketVersion version() {
    return version;
  }
  
  public boolean allowExtensions() {
    return allowExtensions;
  }
  
  public HttpHeaders customHeaders() {
    return customHeaders;
  }
  
  public int maxFramePayloadLength() {
    return maxFramePayloadLength;
  }
  
  public boolean performMasking() {
    return performMasking;
  }
  
  public boolean allowMaskMismatch() {
    return allowMaskMismatch;
  }
  
  public boolean handleCloseFrames() {
    return handleCloseFrames;
  }
  
  public WebSocketCloseStatus sendCloseFrame() {
    return sendCloseFrame;
  }
  
  public boolean dropPongFrames() {
    return dropPongFrames;
  }
  
  public long handshakeTimeoutMillis() {
    return handshakeTimeoutMillis;
  }
  
  public long forceCloseTimeoutMillis() {
    return forceCloseTimeoutMillis;
  }
  
  public boolean absoluteUpgradeUrl() {
    return absoluteUpgradeUrl;
  }
  
  public String toString()
  {
    return "WebSocketClientProtocolConfig {webSocketUri=" + webSocketUri + ", subprotocol=" + subprotocol + ", version=" + version + ", allowExtensions=" + allowExtensions + ", customHeaders=" + customHeaders + ", maxFramePayloadLength=" + maxFramePayloadLength + ", performMasking=" + performMasking + ", allowMaskMismatch=" + allowMaskMismatch + ", handleCloseFrames=" + handleCloseFrames + ", sendCloseFrame=" + sendCloseFrame + ", dropPongFrames=" + dropPongFrames + ", handshakeTimeoutMillis=" + handshakeTimeoutMillis + ", forceCloseTimeoutMillis=" + forceCloseTimeoutMillis + ", absoluteUpgradeUrl=" + absoluteUpgradeUrl + "}";
  }
  














  public Builder toBuilder()
  {
    return new Builder(this, null);
  }
  
  public static Builder newBuilder() {
    return new Builder(
      URI.create("https://localhost/"), null, WebSocketVersion.V13, false, EmptyHttpHeaders.INSTANCE, 65536, true, false, true, WebSocketCloseStatus.NORMAL_CLOSURE, true, 10000L, -1L, false, null);
  }
  

  public static final class Builder
  {
    private URI webSocketUri;
    
    private String subprotocol;
    
    private WebSocketVersion version;
    
    private boolean allowExtensions;
    
    private HttpHeaders customHeaders;
    
    private int maxFramePayloadLength;
    
    private boolean performMasking;
    
    private boolean allowMaskMismatch;
    
    private boolean handleCloseFrames;
    
    private WebSocketCloseStatus sendCloseFrame;
    
    private boolean dropPongFrames;
    private long handshakeTimeoutMillis;
    private long forceCloseTimeoutMillis;
    private boolean absoluteUpgradeUrl;
    
    private Builder(WebSocketClientProtocolConfig clientConfig)
    {
      this(((WebSocketClientProtocolConfig)ObjectUtil.checkNotNull(clientConfig, "clientConfig")).webSocketUri(), clientConfig
        .subprotocol(), clientConfig
        .version(), clientConfig
        .allowExtensions(), clientConfig
        .customHeaders(), clientConfig
        .maxFramePayloadLength(), clientConfig
        .performMasking(), clientConfig
        .allowMaskMismatch(), clientConfig
        .handleCloseFrames(), clientConfig
        .sendCloseFrame(), clientConfig
        .dropPongFrames(), clientConfig
        .handshakeTimeoutMillis(), clientConfig
        .forceCloseTimeoutMillis(), clientConfig
        .absoluteUpgradeUrl());
    }
    












    private Builder(URI webSocketUri, String subprotocol, WebSocketVersion version, boolean allowExtensions, HttpHeaders customHeaders, int maxFramePayloadLength, boolean performMasking, boolean allowMaskMismatch, boolean handleCloseFrames, WebSocketCloseStatus sendCloseFrame, boolean dropPongFrames, long handshakeTimeoutMillis, long forceCloseTimeoutMillis, boolean absoluteUpgradeUrl)
    {
      this.webSocketUri = webSocketUri;
      this.subprotocol = subprotocol;
      this.version = version;
      this.allowExtensions = allowExtensions;
      this.customHeaders = customHeaders;
      this.maxFramePayloadLength = maxFramePayloadLength;
      this.performMasking = performMasking;
      this.allowMaskMismatch = allowMaskMismatch;
      this.handleCloseFrames = handleCloseFrames;
      this.sendCloseFrame = sendCloseFrame;
      this.dropPongFrames = dropPongFrames;
      this.handshakeTimeoutMillis = handshakeTimeoutMillis;
      this.forceCloseTimeoutMillis = forceCloseTimeoutMillis;
      this.absoluteUpgradeUrl = absoluteUpgradeUrl;
    }
    



    public Builder webSocketUri(String webSocketUri)
    {
      return webSocketUri(URI.create(webSocketUri));
    }
    



    public Builder webSocketUri(URI webSocketUri)
    {
      this.webSocketUri = webSocketUri;
      return this;
    }
    


    public Builder subprotocol(String subprotocol)
    {
      this.subprotocol = subprotocol;
      return this;
    }
    


    public Builder version(WebSocketVersion version)
    {
      this.version = version;
      return this;
    }
    


    public Builder allowExtensions(boolean allowExtensions)
    {
      this.allowExtensions = allowExtensions;
      return this;
    }
    


    public Builder customHeaders(HttpHeaders customHeaders)
    {
      this.customHeaders = customHeaders;
      return this;
    }
    


    public Builder maxFramePayloadLength(int maxFramePayloadLength)
    {
      this.maxFramePayloadLength = maxFramePayloadLength;
      return this;
    }
    




    public Builder performMasking(boolean performMasking)
    {
      this.performMasking = performMasking;
      return this;
    }
    


    public Builder allowMaskMismatch(boolean allowMaskMismatch)
    {
      this.allowMaskMismatch = allowMaskMismatch;
      return this;
    }
    


    public Builder handleCloseFrames(boolean handleCloseFrames)
    {
      this.handleCloseFrames = handleCloseFrames;
      return this;
    }
    


    public Builder sendCloseFrame(WebSocketCloseStatus sendCloseFrame)
    {
      this.sendCloseFrame = sendCloseFrame;
      return this;
    }
    


    public Builder dropPongFrames(boolean dropPongFrames)
    {
      this.dropPongFrames = dropPongFrames;
      return this;
    }
    



    public Builder handshakeTimeoutMillis(long handshakeTimeoutMillis)
    {
      this.handshakeTimeoutMillis = handshakeTimeoutMillis;
      return this;
    }
    


    public Builder forceCloseTimeoutMillis(long forceCloseTimeoutMillis)
    {
      this.forceCloseTimeoutMillis = forceCloseTimeoutMillis;
      return this;
    }
    


    public Builder absoluteUpgradeUrl(boolean absoluteUpgradeUrl)
    {
      this.absoluteUpgradeUrl = absoluteUpgradeUrl;
      return this;
    }
    


    public WebSocketClientProtocolConfig build()
    {
      return new WebSocketClientProtocolConfig(webSocketUri, subprotocol, version, allowExtensions, customHeaders, maxFramePayloadLength, performMasking, allowMaskMismatch, handleCloseFrames, sendCloseFrame, dropPongFrames, handshakeTimeoutMillis, forceCloseTimeoutMillis, absoluteUpgradeUrl, null);
    }
  }
}
