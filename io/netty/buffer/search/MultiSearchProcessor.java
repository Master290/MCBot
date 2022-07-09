package io.netty.buffer.search;

public abstract interface MultiSearchProcessor
  extends SearchProcessor
{
  public abstract int getFoundNeedleId();
}
