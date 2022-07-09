package io.netty.handler.codec.spdy;

public abstract interface SpdyStreamFrame
  extends SpdyFrame
{
  public abstract int streamId();
  
  public abstract SpdyStreamFrame setStreamId(int paramInt);
  
  public abstract boolean isLast();
  
  public abstract SpdyStreamFrame setLast(boolean paramBoolean);
}
