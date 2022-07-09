package io.netty.handler.codec.http2;

public abstract interface Http2PingFrame
  extends Http2Frame
{
  public abstract boolean ack();
  
  public abstract long content();
}
