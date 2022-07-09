package io.netty.handler.codec.http2;

public abstract interface Http2SettingsFrame
  extends Http2Frame
{
  public abstract Http2Settings settings();
  
  public abstract String name();
}
