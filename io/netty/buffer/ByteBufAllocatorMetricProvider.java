package io.netty.buffer;

public abstract interface ByteBufAllocatorMetricProvider
{
  public abstract ByteBufAllocatorMetric metric();
}
