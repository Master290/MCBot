package io.netty.handler.codec.spdy;

public abstract interface SpdySynReplyFrame
  extends SpdyHeadersFrame
{
  public abstract SpdySynReplyFrame setStreamId(int paramInt);
  
  public abstract SpdySynReplyFrame setLast(boolean paramBoolean);
  
  public abstract SpdySynReplyFrame setInvalid();
}
