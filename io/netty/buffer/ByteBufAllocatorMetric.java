package io.netty.buffer;

public abstract interface ByteBufAllocatorMetric
{
  public abstract long usedHeapMemory();
  
  public abstract long usedDirectMemory();
}
