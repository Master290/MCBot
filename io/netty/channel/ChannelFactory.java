package io.netty.channel;

public abstract interface ChannelFactory<T extends Channel>
  extends io.netty.bootstrap.ChannelFactory<T>
{
  public abstract T newChannel();
}
