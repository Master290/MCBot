package io.netty.handler.codec.http;

import io.netty.handler.codec.DecoderResult;





















public final class HttpMessageDecoderResult
  extends DecoderResult
{
  private final int initialLineLength;
  private final int headerSize;
  
  HttpMessageDecoderResult(int initialLineLength, int headerSize)
  {
    super(SIGNAL_SUCCESS);
    this.initialLineLength = initialLineLength;
    this.headerSize = headerSize;
  }
  


  public int initialLineLength()
  {
    return initialLineLength;
  }
  


  public int headerSize()
  {
    return headerSize;
  }
  


  public int totalSize()
  {
    return initialLineLength + headerSize;
  }
}
