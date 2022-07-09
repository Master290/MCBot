package io.netty.util;

public abstract interface TimerTask
{
  public abstract void run(Timeout paramTimeout)
    throws Exception;
}
