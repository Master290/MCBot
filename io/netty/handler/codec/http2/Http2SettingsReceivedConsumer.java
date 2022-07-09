package io.netty.handler.codec.http2;

public abstract interface Http2SettingsReceivedConsumer
{
  public abstract void consumeReceivedSettings(Http2Settings paramHttp2Settings);
}
