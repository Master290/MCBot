package io.netty.channel.pool;

import io.netty.channel.Channel;

public abstract interface ChannelPoolHandler
{
  public abstract void channelReleased(Channel paramChannel)
    throws Exception;
  
  public abstract void channelAcquired(Channel paramChannel)
    throws Exception;
  
  public abstract void channelCreated(Channel paramChannel)
    throws Exception;
}
