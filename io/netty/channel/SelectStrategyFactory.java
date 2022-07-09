package io.netty.channel;

public abstract interface SelectStrategyFactory
{
  public abstract SelectStrategy newSelectStrategy();
}
