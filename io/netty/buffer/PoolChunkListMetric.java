package io.netty.buffer;

public abstract interface PoolChunkListMetric
  extends Iterable<PoolChunkMetric>
{
  public abstract int minUsage();
  
  public abstract int maxUsage();
}
