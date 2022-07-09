package io.netty.buffer;

public abstract interface SizeClassesMetric
{
  public abstract int sizeIdx2size(int paramInt);
  
  public abstract int sizeIdx2sizeCompute(int paramInt);
  
  public abstract long pageIdx2size(int paramInt);
  
  public abstract long pageIdx2sizeCompute(int paramInt);
  
  public abstract int size2SizeIdx(int paramInt);
  
  public abstract int pages2pageIdx(int paramInt);
  
  public abstract int pages2pageIdxFloor(int paramInt);
  
  public abstract int normalizeSize(int paramInt);
}
