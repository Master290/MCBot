package io.netty.util.internal;

public abstract interface LongCounter
{
  public abstract void add(long paramLong);
  
  public abstract void increment();
  
  public abstract void decrement();
  
  public abstract long value();
}
