package io.netty.handler.codec.http2;

public abstract interface Http2FrameStreamVisitor
{
  public abstract boolean visit(Http2FrameStream paramHttp2FrameStream);
}
