package io.netty.buffer;

import java.util.List;

public abstract interface PoolArenaMetric
  extends SizeClassesMetric
{
  public abstract int numThreadCaches();
  
  @Deprecated
  public abstract int numTinySubpages();
  
  public abstract int numSmallSubpages();
  
  public abstract int numChunkLists();
  
  @Deprecated
  public abstract List<PoolSubpageMetric> tinySubpages();
  
  public abstract List<PoolSubpageMetric> smallSubpages();
  
  public abstract List<PoolChunkListMetric> chunkLists();
  
  public abstract long numAllocations();
  
  @Deprecated
  public abstract long numTinyAllocations();
  
  public abstract long numSmallAllocations();
  
  public abstract long numNormalAllocations();
  
  public abstract long numHugeAllocations();
  
  public abstract long numDeallocations();
  
  @Deprecated
  public abstract long numTinyDeallocations();
  
  public abstract long numSmallDeallocations();
  
  public abstract long numNormalDeallocations();
  
  public abstract long numHugeDeallocations();
  
  public abstract long numActiveAllocations();
  
  @Deprecated
  public abstract long numActiveTinyAllocations();
  
  public abstract long numActiveSmallAllocations();
  
  public abstract long numActiveNormalAllocations();
  
  public abstract long numActiveHugeAllocations();
  
  public abstract long numActiveBytes();
}
