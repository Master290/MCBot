package io.netty.handler.codec.spdy;

public abstract interface SpdySynStreamFrame
  extends SpdyHeadersFrame
{
  public abstract int associatedStreamId();
  
  public abstract SpdySynStreamFrame setAssociatedStreamId(int paramInt);
  
  public abstract byte priority();
  
  public abstract SpdySynStreamFrame setPriority(byte paramByte);
  
  public abstract boolean isUnidirectional();
  
  public abstract SpdySynStreamFrame setUnidirectional(boolean paramBoolean);
  
  public abstract SpdySynStreamFrame setStreamId(int paramInt);
  
  public abstract SpdySynStreamFrame setLast(boolean paramBoolean);
  
  public abstract SpdySynStreamFrame setInvalid();
}
