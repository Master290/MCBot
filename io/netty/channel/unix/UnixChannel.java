package io.netty.channel.unix;

import io.netty.channel.Channel;

public abstract interface UnixChannel
  extends Channel
{
  public abstract FileDescriptor fd();
}
