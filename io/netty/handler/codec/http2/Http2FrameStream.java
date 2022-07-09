package io.netty.handler.codec.http2;

public abstract interface Http2FrameStream
{
  public abstract int id();
  
  public abstract Http2Stream.State state();
}
