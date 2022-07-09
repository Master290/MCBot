package io.netty.util;

public abstract interface Timeout
{
  public abstract Timer timer();
  
  public abstract TimerTask task();
  
  public abstract boolean isExpired();
  
  public abstract boolean isCancelled();
  
  public abstract boolean cancel();
}
