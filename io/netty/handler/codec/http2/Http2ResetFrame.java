package io.netty.handler.codec.http2;

public abstract interface Http2ResetFrame
  extends Http2StreamFrame
{
  public abstract long errorCode();
}
