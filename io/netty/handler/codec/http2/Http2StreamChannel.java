package io.netty.handler.codec.http2;

import io.netty.channel.Channel;

public abstract interface Http2StreamChannel
  extends Channel
{
  public abstract Http2FrameStream stream();
}
