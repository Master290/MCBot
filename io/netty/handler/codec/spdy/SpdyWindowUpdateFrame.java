package io.netty.handler.codec.spdy;

public abstract interface SpdyWindowUpdateFrame
  extends SpdyFrame
{
  public abstract int streamId();
  
  public abstract SpdyWindowUpdateFrame setStreamId(int paramInt);
  
  public abstract int deltaWindowSize();
  
  public abstract SpdyWindowUpdateFrame setDeltaWindowSize(int paramInt);
}
