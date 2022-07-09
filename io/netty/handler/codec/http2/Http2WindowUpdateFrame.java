package io.netty.handler.codec.http2;

public abstract interface Http2WindowUpdateFrame
  extends Http2StreamFrame
{
  public abstract int windowSizeIncrement();
}
