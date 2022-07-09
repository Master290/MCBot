package io.netty.handler.ipfilter;

import java.net.InetSocketAddress;

public abstract interface IpFilterRule
{
  public abstract boolean matches(InetSocketAddress paramInetSocketAddress);
  
  public abstract IpFilterRuleType ruleType();
}
