package io.netty.util;

public abstract interface ResourceLeakTracker<T>
{
  public abstract void record();
  
  public abstract void record(Object paramObject);
  
  public abstract boolean close(T paramT);
}
