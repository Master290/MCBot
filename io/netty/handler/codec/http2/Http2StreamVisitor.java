package io.netty.handler.codec.http2;

public abstract interface Http2StreamVisitor
{
  public abstract boolean visit(Http2Stream paramHttp2Stream)
    throws Http2Exception;
}
