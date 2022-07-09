package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.util.internal.ObjectUtil;



























public class DefaultHttp2HeadersDecoder
  implements Http2HeadersDecoder, Http2HeadersDecoder.Configuration
{
  private static final float HEADERS_COUNT_WEIGHT_NEW = 0.2F;
  private static final float HEADERS_COUNT_WEIGHT_HISTORICAL = 0.8F;
  private final HpackDecoder hpackDecoder;
  private final boolean validateHeaders;
  private long maxHeaderListSizeGoAway;
  private float headerArraySizeAccumulator = 8.0F;
  
  public DefaultHttp2HeadersDecoder() {
    this(true);
  }
  
  public DefaultHttp2HeadersDecoder(boolean validateHeaders) {
    this(validateHeaders, 8192L);
  }
  







  public DefaultHttp2HeadersDecoder(boolean validateHeaders, long maxHeaderListSize)
  {
    this(validateHeaders, maxHeaderListSize, -1);
  }
  









  public DefaultHttp2HeadersDecoder(boolean validateHeaders, long maxHeaderListSize, @Deprecated int initialHuffmanDecodeCapacity)
  {
    this(validateHeaders, new HpackDecoder(maxHeaderListSize));
  }
  



  DefaultHttp2HeadersDecoder(boolean validateHeaders, HpackDecoder hpackDecoder)
  {
    this.hpackDecoder = ((HpackDecoder)ObjectUtil.checkNotNull(hpackDecoder, "hpackDecoder"));
    this.validateHeaders = validateHeaders;
    
    maxHeaderListSizeGoAway = Http2CodecUtil.calculateMaxHeaderListSizeGoAway(hpackDecoder.getMaxHeaderListSize());
  }
  
  public void maxHeaderTableSize(long max) throws Http2Exception
  {
    hpackDecoder.setMaxHeaderTableSize(max);
  }
  
  public long maxHeaderTableSize()
  {
    return hpackDecoder.getMaxHeaderTableSize();
  }
  
  public void maxHeaderListSize(long max, long goAwayMax) throws Http2Exception
  {
    if ((goAwayMax < max) || (goAwayMax < 0L)) {
      throw Http2Exception.connectionError(Http2Error.INTERNAL_ERROR, "Header List Size GO_AWAY %d must be non-negative and >= %d", new Object[] {
        Long.valueOf(goAwayMax), Long.valueOf(max) });
    }
    hpackDecoder.setMaxHeaderListSize(max);
    maxHeaderListSizeGoAway = goAwayMax;
  }
  
  public long maxHeaderListSize()
  {
    return hpackDecoder.getMaxHeaderListSize();
  }
  
  public long maxHeaderListSizeGoAway()
  {
    return maxHeaderListSizeGoAway;
  }
  
  public Http2HeadersDecoder.Configuration configuration()
  {
    return this;
  }
  
  public Http2Headers decodeHeaders(int streamId, ByteBuf headerBlock) throws Http2Exception
  {
    try {
      Http2Headers headers = newHeaders();
      hpackDecoder.decode(streamId, headerBlock, headers, validateHeaders);
      headerArraySizeAccumulator = (0.2F * headers.size() + 0.8F * headerArraySizeAccumulator);
      
      return headers;
    } catch (Http2Exception e) {
      throw e;

    }
    catch (Throwable e)
    {
      throw Http2Exception.connectionError(Http2Error.COMPRESSION_ERROR, e, e.getMessage(), new Object[0]);
    }
  }
  



  protected final int numberOfHeadersGuess()
  {
    return (int)headerArraySizeAccumulator;
  }
  



  protected final boolean validateHeaders()
  {
    return validateHeaders;
  }
  



  protected Http2Headers newHeaders()
  {
    return new DefaultHttp2Headers(validateHeaders, (int)headerArraySizeAccumulator);
  }
}
