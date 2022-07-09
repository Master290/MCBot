package io.netty.handler.codec.spdy;

public abstract interface SpdyHeadersFrame
  extends SpdyStreamFrame
{
  public abstract boolean isInvalid();
  
  public abstract SpdyHeadersFrame setInvalid();
  
  public abstract boolean isTruncated();
  
  public abstract SpdyHeadersFrame setTruncated();
  
  public abstract SpdyHeaders headers();
  
  public abstract SpdyHeadersFrame setStreamId(int paramInt);
  
  public abstract SpdyHeadersFrame setLast(boolean paramBoolean);
}
