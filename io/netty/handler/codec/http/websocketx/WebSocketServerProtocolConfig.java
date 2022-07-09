package io.netty.handler.codec.http.websocketx;

import io.netty.util.internal.ObjectUtil;
































public final class WebSocketServerProtocolConfig
{
  static final long DEFAULT_HANDSHAKE_TIMEOUT_MILLIS = 10000L;
  private final String websocketPath;
  private final String subprotocols;
  private final boolean checkStartsWith;
  private final long handshakeTimeoutMillis;
  private final long forceCloseTimeoutMillis;
  private final boolean handleCloseFrames;
  private final WebSocketCloseStatus sendCloseFrame;
  private final boolean dropPongFrames;
  private final WebSocketDecoderConfig decoderConfig;
  
  private WebSocketServerProtocolConfig(String websocketPath, String subprotocols, boolean checkStartsWith, long handshakeTimeoutMillis, long forceCloseTimeoutMillis, boolean handleCloseFrames, WebSocketCloseStatus sendCloseFrame, boolean dropPongFrames, WebSocketDecoderConfig decoderConfig)
  {
    this.websocketPath = websocketPath;
    this.subprotocols = subprotocols;
    this.checkStartsWith = checkStartsWith;
    this.handshakeTimeoutMillis = ObjectUtil.checkPositive(handshakeTimeoutMillis, "handshakeTimeoutMillis");
    this.forceCloseTimeoutMillis = forceCloseTimeoutMillis;
    this.handleCloseFrames = handleCloseFrames;
    this.sendCloseFrame = sendCloseFrame;
    this.dropPongFrames = dropPongFrames;
    this.decoderConfig = (decoderConfig == null ? WebSocketDecoderConfig.DEFAULT : decoderConfig);
  }
  
  public String websocketPath() {
    return websocketPath;
  }
  
  public String subprotocols() {
    return subprotocols;
  }
  
  public boolean checkStartsWith() {
    return checkStartsWith;
  }
  
  public long handshakeTimeoutMillis() {
    return handshakeTimeoutMillis;
  }
  
  public long forceCloseTimeoutMillis() {
    return forceCloseTimeoutMillis;
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
  
  public WebSocketDecoderConfig decoderConfig() {
    return decoderConfig;
  }
  
  public String toString()
  {
    return "WebSocketServerProtocolConfig {websocketPath=" + websocketPath + ", subprotocols=" + subprotocols + ", checkStartsWith=" + checkStartsWith + ", handshakeTimeoutMillis=" + handshakeTimeoutMillis + ", forceCloseTimeoutMillis=" + forceCloseTimeoutMillis + ", handleCloseFrames=" + handleCloseFrames + ", sendCloseFrame=" + sendCloseFrame + ", dropPongFrames=" + dropPongFrames + ", decoderConfig=" + decoderConfig + "}";
  }
  









  public Builder toBuilder()
  {
    return new Builder(this, null);
  }
  
  public static Builder newBuilder() {
    return new Builder("/", null, false, 10000L, 0L, true, WebSocketCloseStatus.NORMAL_CLOSURE, true, WebSocketDecoderConfig.DEFAULT, null);
  }
  
  public static final class Builder
  {
    private String websocketPath;
    private String subprotocols;
    private boolean checkStartsWith;
    private long handshakeTimeoutMillis;
    private long forceCloseTimeoutMillis;
    private boolean handleCloseFrames;
    private WebSocketCloseStatus sendCloseFrame;
    private boolean dropPongFrames;
    private WebSocketDecoderConfig decoderConfig;
    private WebSocketDecoderConfig.Builder decoderConfigBuilder;
    
    private Builder(WebSocketServerProtocolConfig serverConfig) {
      this(((WebSocketServerProtocolConfig)ObjectUtil.checkNotNull(serverConfig, "serverConfig")).websocketPath(), serverConfig
        .subprotocols(), serverConfig
        .checkStartsWith(), serverConfig
        .handshakeTimeoutMillis(), serverConfig
        .forceCloseTimeoutMillis(), serverConfig
        .handleCloseFrames(), serverConfig
        .sendCloseFrame(), serverConfig
        .dropPongFrames(), serverConfig
        .decoderConfig());
    }
    








    private Builder(String websocketPath, String subprotocols, boolean checkStartsWith, long handshakeTimeoutMillis, long forceCloseTimeoutMillis, boolean handleCloseFrames, WebSocketCloseStatus sendCloseFrame, boolean dropPongFrames, WebSocketDecoderConfig decoderConfig)
    {
      this.websocketPath = websocketPath;
      this.subprotocols = subprotocols;
      this.checkStartsWith = checkStartsWith;
      this.handshakeTimeoutMillis = handshakeTimeoutMillis;
      this.forceCloseTimeoutMillis = forceCloseTimeoutMillis;
      this.handleCloseFrames = handleCloseFrames;
      this.sendCloseFrame = sendCloseFrame;
      this.dropPongFrames = dropPongFrames;
      this.decoderConfig = decoderConfig;
    }
    


    public Builder websocketPath(String websocketPath)
    {
      this.websocketPath = websocketPath;
      return this;
    }
    


    public Builder subprotocols(String subprotocols)
    {
      this.subprotocols = subprotocols;
      return this;
    }
    



    public Builder checkStartsWith(boolean checkStartsWith)
    {
      this.checkStartsWith = checkStartsWith;
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
    


    public Builder decoderConfig(WebSocketDecoderConfig decoderConfig)
    {
      this.decoderConfig = (decoderConfig == null ? WebSocketDecoderConfig.DEFAULT : decoderConfig);
      decoderConfigBuilder = null;
      return this;
    }
    
    private WebSocketDecoderConfig.Builder decoderConfigBuilder() {
      if (decoderConfigBuilder == null) {
        decoderConfigBuilder = decoderConfig.toBuilder();
      }
      return decoderConfigBuilder;
    }
    
    public Builder maxFramePayloadLength(int maxFramePayloadLength) {
      decoderConfigBuilder().maxFramePayloadLength(maxFramePayloadLength);
      return this;
    }
    
    public Builder expectMaskedFrames(boolean expectMaskedFrames) {
      decoderConfigBuilder().expectMaskedFrames(expectMaskedFrames);
      return this;
    }
    
    public Builder allowMaskMismatch(boolean allowMaskMismatch) {
      decoderConfigBuilder().allowMaskMismatch(allowMaskMismatch);
      return this;
    }
    
    public Builder allowExtensions(boolean allowExtensions) {
      decoderConfigBuilder().allowExtensions(allowExtensions);
      return this;
    }
    
    public Builder closeOnProtocolViolation(boolean closeOnProtocolViolation) {
      decoderConfigBuilder().closeOnProtocolViolation(closeOnProtocolViolation);
      return this;
    }
    
    public Builder withUTF8Validator(boolean withUTF8Validator) {
      decoderConfigBuilder().withUTF8Validator(withUTF8Validator);
      return this;
    }
    


    public WebSocketServerProtocolConfig build()
    {
      return new WebSocketServerProtocolConfig(websocketPath, subprotocols, checkStartsWith, handshakeTimeoutMillis, forceCloseTimeoutMillis, handleCloseFrames, sendCloseFrame, dropPongFrames, decoderConfigBuilder == null ? decoderConfig : decoderConfigBuilder
      







        .build(), null);
    }
  }
}
