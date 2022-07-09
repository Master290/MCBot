package io.netty.channel.pool;

public abstract interface ChannelPoolMap<K, P extends ChannelPool>
{
  public abstract P get(K paramK);
  
  public abstract boolean contains(K paramK);
}
