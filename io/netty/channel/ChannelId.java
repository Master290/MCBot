package io.netty.channel;

import java.io.Serializable;

public abstract interface ChannelId
  extends Serializable, Comparable<ChannelId>
{
  public abstract String asShortText();
  
  public abstract String asLongText();
}
