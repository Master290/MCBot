package io.netty.handler.codec.http2;

import io.netty.handler.codec.http.HttpScheme;





















public final class HttpToHttp2ConnectionHandlerBuilder
  extends AbstractHttp2ConnectionHandlerBuilder<HttpToHttp2ConnectionHandler, HttpToHttp2ConnectionHandlerBuilder>
{
  private HttpScheme httpScheme;
  
  public HttpToHttp2ConnectionHandlerBuilder() {}
  
  public HttpToHttp2ConnectionHandlerBuilder validateHeaders(boolean validateHeaders)
  {
    return (HttpToHttp2ConnectionHandlerBuilder)super.validateHeaders(validateHeaders);
  }
  
  public HttpToHttp2ConnectionHandlerBuilder initialSettings(Http2Settings settings)
  {
    return (HttpToHttp2ConnectionHandlerBuilder)super.initialSettings(settings);
  }
  
  public HttpToHttp2ConnectionHandlerBuilder frameListener(Http2FrameListener frameListener)
  {
    return (HttpToHttp2ConnectionHandlerBuilder)super.frameListener(frameListener);
  }
  
  public HttpToHttp2ConnectionHandlerBuilder gracefulShutdownTimeoutMillis(long gracefulShutdownTimeoutMillis)
  {
    return (HttpToHttp2ConnectionHandlerBuilder)super.gracefulShutdownTimeoutMillis(gracefulShutdownTimeoutMillis);
  }
  
  public HttpToHttp2ConnectionHandlerBuilder server(boolean isServer)
  {
    return (HttpToHttp2ConnectionHandlerBuilder)super.server(isServer);
  }
  
  public HttpToHttp2ConnectionHandlerBuilder connection(Http2Connection connection)
  {
    return (HttpToHttp2ConnectionHandlerBuilder)super.connection(connection);
  }
  

  public HttpToHttp2ConnectionHandlerBuilder codec(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder)
  {
    return (HttpToHttp2ConnectionHandlerBuilder)super.codec(decoder, encoder);
  }
  
  public HttpToHttp2ConnectionHandlerBuilder frameLogger(Http2FrameLogger frameLogger)
  {
    return (HttpToHttp2ConnectionHandlerBuilder)super.frameLogger(frameLogger);
  }
  

  public HttpToHttp2ConnectionHandlerBuilder encoderEnforceMaxConcurrentStreams(boolean encoderEnforceMaxConcurrentStreams)
  {
    return (HttpToHttp2ConnectionHandlerBuilder)super.encoderEnforceMaxConcurrentStreams(encoderEnforceMaxConcurrentStreams);
  }
  

  public HttpToHttp2ConnectionHandlerBuilder headerSensitivityDetector(Http2HeadersEncoder.SensitivityDetector headerSensitivityDetector)
  {
    return (HttpToHttp2ConnectionHandlerBuilder)super.headerSensitivityDetector(headerSensitivityDetector);
  }
  
  @Deprecated
  public HttpToHttp2ConnectionHandlerBuilder initialHuffmanDecodeCapacity(int initialHuffmanDecodeCapacity)
  {
    return (HttpToHttp2ConnectionHandlerBuilder)super.initialHuffmanDecodeCapacity(initialHuffmanDecodeCapacity);
  }
  
  public HttpToHttp2ConnectionHandlerBuilder decoupleCloseAndGoAway(boolean decoupleCloseAndGoAway)
  {
    return (HttpToHttp2ConnectionHandlerBuilder)super.decoupleCloseAndGoAway(decoupleCloseAndGoAway);
  }
  





  public HttpToHttp2ConnectionHandlerBuilder httpScheme(HttpScheme httpScheme)
  {
    this.httpScheme = httpScheme;
    return (HttpToHttp2ConnectionHandlerBuilder)self();
  }
  
  public HttpToHttp2ConnectionHandler build()
  {
    return (HttpToHttp2ConnectionHandler)super.build();
  }
  

  protected HttpToHttp2ConnectionHandler build(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder, Http2Settings initialSettings)
  {
    return new HttpToHttp2ConnectionHandler(decoder, encoder, initialSettings, isValidateHeaders(), 
      decoupleCloseAndGoAway(), httpScheme);
  }
}
