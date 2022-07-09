package io.netty.handler.codec.http.websocketx;

import io.netty.util.internal.ObjectUtil;



















public final class WebSocketDecoderConfig
{
  static final WebSocketDecoderConfig DEFAULT = new WebSocketDecoderConfig(65536, true, false, false, true, true);
  



  private final int maxFramePayloadLength;
  



  private final boolean expectMaskedFrames;
  



  private final boolean allowMaskMismatch;
  


  private final boolean allowExtensions;
  


  private final boolean closeOnProtocolViolation;
  


  private final boolean withUTF8Validator;
  



  private WebSocketDecoderConfig(int maxFramePayloadLength, boolean expectMaskedFrames, boolean allowMaskMismatch, boolean allowExtensions, boolean closeOnProtocolViolation, boolean withUTF8Validator)
  {
    this.maxFramePayloadLength = maxFramePayloadLength;
    this.expectMaskedFrames = expectMaskedFrames;
    this.allowMaskMismatch = allowMaskMismatch;
    this.allowExtensions = allowExtensions;
    this.closeOnProtocolViolation = closeOnProtocolViolation;
    this.withUTF8Validator = withUTF8Validator;
  }
  
  public int maxFramePayloadLength() {
    return maxFramePayloadLength;
  }
  
  public boolean expectMaskedFrames() {
    return expectMaskedFrames;
  }
  
  public boolean allowMaskMismatch() {
    return allowMaskMismatch;
  }
  
  public boolean allowExtensions() {
    return allowExtensions;
  }
  
  public boolean closeOnProtocolViolation() {
    return closeOnProtocolViolation;
  }
  
  public boolean withUTF8Validator() {
    return withUTF8Validator;
  }
  
  public String toString()
  {
    return "WebSocketDecoderConfig [maxFramePayloadLength=" + maxFramePayloadLength + ", expectMaskedFrames=" + expectMaskedFrames + ", allowMaskMismatch=" + allowMaskMismatch + ", allowExtensions=" + allowExtensions + ", closeOnProtocolViolation=" + closeOnProtocolViolation + ", withUTF8Validator=" + withUTF8Validator + "]";
  }
  






  public Builder toBuilder()
  {
    return new Builder(this, null);
  }
  
  public static Builder newBuilder() {
    return new Builder(DEFAULT, null);
  }
  
  public static final class Builder {
    private int maxFramePayloadLength;
    private boolean expectMaskedFrames;
    private boolean allowMaskMismatch;
    private boolean allowExtensions;
    private boolean closeOnProtocolViolation;
    private boolean withUTF8Validator;
    
    private Builder(WebSocketDecoderConfig decoderConfig) {
      ObjectUtil.checkNotNull(decoderConfig, "decoderConfig");
      maxFramePayloadLength = decoderConfig.maxFramePayloadLength();
      expectMaskedFrames = decoderConfig.expectMaskedFrames();
      allowMaskMismatch = decoderConfig.allowMaskMismatch();
      allowExtensions = decoderConfig.allowExtensions();
      closeOnProtocolViolation = decoderConfig.closeOnProtocolViolation();
      withUTF8Validator = decoderConfig.withUTF8Validator();
    }
    
    public Builder maxFramePayloadLength(int maxFramePayloadLength) {
      this.maxFramePayloadLength = maxFramePayloadLength;
      return this;
    }
    
    public Builder expectMaskedFrames(boolean expectMaskedFrames) {
      this.expectMaskedFrames = expectMaskedFrames;
      return this;
    }
    
    public Builder allowMaskMismatch(boolean allowMaskMismatch) {
      this.allowMaskMismatch = allowMaskMismatch;
      return this;
    }
    
    public Builder allowExtensions(boolean allowExtensions) {
      this.allowExtensions = allowExtensions;
      return this;
    }
    
    public Builder closeOnProtocolViolation(boolean closeOnProtocolViolation) {
      this.closeOnProtocolViolation = closeOnProtocolViolation;
      return this;
    }
    
    public Builder withUTF8Validator(boolean withUTF8Validator) {
      this.withUTF8Validator = withUTF8Validator;
      return this;
    }
    
    public WebSocketDecoderConfig build() {
      return new WebSocketDecoderConfig(maxFramePayloadLength, expectMaskedFrames, allowMaskMismatch, allowExtensions, closeOnProtocolViolation, withUTF8Validator, null);
    }
  }
}
