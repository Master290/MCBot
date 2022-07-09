package io.netty.handler.codec.spdy;

public abstract interface SpdyRstStreamFrame
  extends SpdyStreamFrame
{
  public abstract SpdyStreamStatus status();
  
  public abstract SpdyRstStreamFrame setStatus(SpdyStreamStatus paramSpdyStreamStatus);
  
  public abstract SpdyRstStreamFrame setStreamId(int paramInt);
  
  public abstract SpdyRstStreamFrame setLast(boolean paramBoolean);
}
