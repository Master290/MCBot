package io.netty.handler.codec.http2;

public abstract interface Http2StreamFrame
  extends Http2Frame
{
  public abstract Http2StreamFrame stream(Http2FrameStream paramHttp2FrameStream);
  
  public abstract Http2FrameStream stream();
}
