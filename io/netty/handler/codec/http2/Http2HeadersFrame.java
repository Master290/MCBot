package io.netty.handler.codec.http2;

public abstract interface Http2HeadersFrame
  extends Http2StreamFrame
{
  public abstract Http2Headers headers();
  
  public abstract int padding();
  
  public abstract boolean isEndStream();
}
