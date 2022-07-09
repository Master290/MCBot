package io.netty.buffer;

public abstract interface PoolChunkMetric
{
  public abstract int usage();
  
  public abstract int chunkSize();
  
  public abstract int freeBytes();
}
