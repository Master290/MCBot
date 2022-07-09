package io.netty.handler.codec.http2;

public abstract interface Http2FrameSizePolicy
{
  public abstract void maxFrameSize(int paramInt)
    throws Http2Exception;
  
  public abstract int maxFrameSize();
}
