package io.netty.util;

@Deprecated
public abstract interface ResourceLeak
{
  public abstract void record();
  
  public abstract void record(Object paramObject);
  
  public abstract boolean close();
}
