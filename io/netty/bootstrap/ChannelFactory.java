package io.netty.bootstrap;

import io.netty.channel.Channel;

@Deprecated
public abstract interface ChannelFactory<T extends Channel>
{
  public abstract T newChannel();
}
