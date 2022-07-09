package io.netty.handler.codec.http2;

import io.netty.util.internal.ObjectUtil;
































public class Http2FrameCodecBuilder
  extends AbstractHttp2ConnectionHandlerBuilder<Http2FrameCodec, Http2FrameCodecBuilder>
{
  private Http2FrameWriter frameWriter;
  
  protected Http2FrameCodecBuilder() {}
  
  Http2FrameCodecBuilder(boolean server)
  {
    server(server);
    
    gracefulShutdownTimeoutMillis(0L);
  }
  


  public static Http2FrameCodecBuilder forClient()
  {
    return new Http2FrameCodecBuilder(false);
  }
  


  public static Http2FrameCodecBuilder forServer()
  {
    return new Http2FrameCodecBuilder(true);
  }
  
  Http2FrameCodecBuilder frameWriter(Http2FrameWriter frameWriter)
  {
    this.frameWriter = ((Http2FrameWriter)ObjectUtil.checkNotNull(frameWriter, "frameWriter"));
    return this;
  }
  
  public Http2Settings initialSettings()
  {
    return super.initialSettings();
  }
  
  public Http2FrameCodecBuilder initialSettings(Http2Settings settings)
  {
    return (Http2FrameCodecBuilder)super.initialSettings(settings);
  }
  
  public long gracefulShutdownTimeoutMillis()
  {
    return super.gracefulShutdownTimeoutMillis();
  }
  
  public Http2FrameCodecBuilder gracefulShutdownTimeoutMillis(long gracefulShutdownTimeoutMillis)
  {
    return (Http2FrameCodecBuilder)super.gracefulShutdownTimeoutMillis(gracefulShutdownTimeoutMillis);
  }
  
  public boolean isServer()
  {
    return super.isServer();
  }
  
  public int maxReservedStreams()
  {
    return super.maxReservedStreams();
  }
  
  public Http2FrameCodecBuilder maxReservedStreams(int maxReservedStreams)
  {
    return (Http2FrameCodecBuilder)super.maxReservedStreams(maxReservedStreams);
  }
  
  public boolean isValidateHeaders()
  {
    return super.isValidateHeaders();
  }
  
  public Http2FrameCodecBuilder validateHeaders(boolean validateHeaders)
  {
    return (Http2FrameCodecBuilder)super.validateHeaders(validateHeaders);
  }
  
  public Http2FrameLogger frameLogger()
  {
    return super.frameLogger();
  }
  
  public Http2FrameCodecBuilder frameLogger(Http2FrameLogger frameLogger)
  {
    return (Http2FrameCodecBuilder)super.frameLogger(frameLogger);
  }
  
  public boolean encoderEnforceMaxConcurrentStreams()
  {
    return super.encoderEnforceMaxConcurrentStreams();
  }
  
  public Http2FrameCodecBuilder encoderEnforceMaxConcurrentStreams(boolean encoderEnforceMaxConcurrentStreams)
  {
    return (Http2FrameCodecBuilder)super.encoderEnforceMaxConcurrentStreams(encoderEnforceMaxConcurrentStreams);
  }
  
  public int encoderEnforceMaxQueuedControlFrames()
  {
    return super.encoderEnforceMaxQueuedControlFrames();
  }
  
  public Http2FrameCodecBuilder encoderEnforceMaxQueuedControlFrames(int maxQueuedControlFrames)
  {
    return (Http2FrameCodecBuilder)super.encoderEnforceMaxQueuedControlFrames(maxQueuedControlFrames);
  }
  
  public Http2HeadersEncoder.SensitivityDetector headerSensitivityDetector()
  {
    return super.headerSensitivityDetector();
  }
  

  public Http2FrameCodecBuilder headerSensitivityDetector(Http2HeadersEncoder.SensitivityDetector headerSensitivityDetector)
  {
    return (Http2FrameCodecBuilder)super.headerSensitivityDetector(headerSensitivityDetector);
  }
  
  public Http2FrameCodecBuilder encoderIgnoreMaxHeaderListSize(boolean ignoreMaxHeaderListSize)
  {
    return (Http2FrameCodecBuilder)super.encoderIgnoreMaxHeaderListSize(ignoreMaxHeaderListSize);
  }
  
  @Deprecated
  public Http2FrameCodecBuilder initialHuffmanDecodeCapacity(int initialHuffmanDecodeCapacity)
  {
    return (Http2FrameCodecBuilder)super.initialHuffmanDecodeCapacity(initialHuffmanDecodeCapacity);
  }
  
  public Http2FrameCodecBuilder autoAckSettingsFrame(boolean autoAckSettings)
  {
    return (Http2FrameCodecBuilder)super.autoAckSettingsFrame(autoAckSettings);
  }
  
  public Http2FrameCodecBuilder autoAckPingFrame(boolean autoAckPingFrame)
  {
    return (Http2FrameCodecBuilder)super.autoAckPingFrame(autoAckPingFrame);
  }
  
  public Http2FrameCodecBuilder decoupleCloseAndGoAway(boolean decoupleCloseAndGoAway)
  {
    return (Http2FrameCodecBuilder)super.decoupleCloseAndGoAway(decoupleCloseAndGoAway);
  }
  
  public int decoderEnforceMaxConsecutiveEmptyDataFrames()
  {
    return super.decoderEnforceMaxConsecutiveEmptyDataFrames();
  }
  
  public Http2FrameCodecBuilder decoderEnforceMaxConsecutiveEmptyDataFrames(int maxConsecutiveEmptyFrames)
  {
    return (Http2FrameCodecBuilder)super.decoderEnforceMaxConsecutiveEmptyDataFrames(maxConsecutiveEmptyFrames);
  }
  



  public Http2FrameCodec build()
  {
    Http2FrameWriter frameWriter = this.frameWriter;
    if (frameWriter != null)
    {

      DefaultHttp2Connection connection = new DefaultHttp2Connection(isServer(), maxReservedStreams());
      Long maxHeaderListSize = initialSettings().maxHeaderListSize();
      

      Http2FrameReader frameReader = new DefaultHttp2FrameReader(maxHeaderListSize == null ? new DefaultHttp2HeadersDecoder(isValidateHeaders()) : new DefaultHttp2HeadersDecoder(isValidateHeaders(), maxHeaderListSize.longValue()));
      
      if (frameLogger() != null) {
        frameWriter = new Http2OutboundFrameLogger(frameWriter, frameLogger());
        frameReader = new Http2InboundFrameLogger(frameReader, frameLogger());
      }
      Http2ConnectionEncoder encoder = new DefaultHttp2ConnectionEncoder(connection, frameWriter);
      if (encoderEnforceMaxConcurrentStreams()) {
        encoder = new StreamBufferingEncoder(encoder);
      }
      
      Http2ConnectionDecoder decoder = new DefaultHttp2ConnectionDecoder(connection, encoder, frameReader, promisedRequestVerifier(), isAutoAckSettingsFrame(), isAutoAckPingFrame());
      int maxConsecutiveEmptyDataFrames = decoderEnforceMaxConsecutiveEmptyDataFrames();
      if (maxConsecutiveEmptyDataFrames > 0) {
        decoder = new Http2EmptyDataFrameConnectionDecoder(decoder, maxConsecutiveEmptyDataFrames);
      }
      return build(decoder, encoder, initialSettings());
    }
    return (Http2FrameCodec)super.build();
  }
  

  protected Http2FrameCodec build(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder, Http2Settings initialSettings)
  {
    Http2FrameCodec codec = new Http2FrameCodec(encoder, decoder, initialSettings, decoupleCloseAndGoAway());
    codec.gracefulShutdownTimeoutMillis(gracefulShutdownTimeoutMillis());
    return codec;
  }
}
