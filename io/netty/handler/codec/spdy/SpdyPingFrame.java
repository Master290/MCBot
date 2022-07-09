package io.netty.handler.codec.spdy;

public abstract interface SpdyPingFrame
  extends SpdyFrame
{
  public abstract int id();
  
  public abstract SpdyPingFrame setId(int paramInt);
}
