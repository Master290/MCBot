package io.netty.buffer;

public abstract interface PoolSubpageMetric
{
  public abstract int maxNumElements();
  
  public abstract int numAvailable();
  
  public abstract int elementSize();
  
  public abstract int pageSize();
}
