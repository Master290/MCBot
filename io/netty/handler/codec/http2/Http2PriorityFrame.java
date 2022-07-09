package io.netty.handler.codec.http2;

public abstract interface Http2PriorityFrame
  extends Http2StreamFrame
{
  public abstract int streamDependency();
  
  public abstract short weight();
  
  public abstract boolean exclusive();
  
  public abstract Http2PriorityFrame stream(Http2FrameStream paramHttp2FrameStream);
}
