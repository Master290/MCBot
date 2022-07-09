package io.netty.channel.pool;

import io.netty.channel.Channel;

public abstract class AbstractChannelPoolHandler
  implements ChannelPoolHandler
{
  public AbstractChannelPoolHandler() {}
  
  public void channelAcquired(Channel ch)
    throws Exception
  {}
  
  public void channelReleased(Channel ch)
    throws Exception
  {}
}
