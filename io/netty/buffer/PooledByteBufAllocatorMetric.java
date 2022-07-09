package io.netty.buffer;

import io.netty.util.internal.StringUtil;
import java.util.List;



















public final class PooledByteBufAllocatorMetric
  implements ByteBufAllocatorMetric
{
  private final PooledByteBufAllocator allocator;
  
  PooledByteBufAllocatorMetric(PooledByteBufAllocator allocator)
  {
    this.allocator = allocator;
  }
  


  public int numHeapArenas()
  {
    return allocator.numHeapArenas();
  }
  


  public int numDirectArenas()
  {
    return allocator.numDirectArenas();
  }
  


  public List<PoolArenaMetric> heapArenas()
  {
    return allocator.heapArenas();
  }
  


  public List<PoolArenaMetric> directArenas()
  {
    return allocator.directArenas();
  }
  


  public int numThreadLocalCaches()
  {
    return allocator.numThreadLocalCaches();
  }
  




  @Deprecated
  public int tinyCacheSize()
  {
    return allocator.tinyCacheSize();
  }
  


  public int smallCacheSize()
  {
    return allocator.smallCacheSize();
  }
  


  public int normalCacheSize()
  {
    return allocator.normalCacheSize();
  }
  


  public int chunkSize()
  {
    return allocator.chunkSize();
  }
  
  public long usedHeapMemory()
  {
    return allocator.usedHeapMemory();
  }
  
  public long usedDirectMemory()
  {
    return allocator.usedDirectMemory();
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder(256);
    sb.append(StringUtil.simpleClassName(this))
      .append("(usedHeapMemory: ").append(usedHeapMemory())
      .append("; usedDirectMemory: ").append(usedDirectMemory())
      .append("; numHeapArenas: ").append(numHeapArenas())
      .append("; numDirectArenas: ").append(numDirectArenas())
      .append("; smallCacheSize: ").append(smallCacheSize())
      .append("; normalCacheSize: ").append(normalCacheSize())
      .append("; numThreadLocalCaches: ").append(numThreadLocalCaches())
      .append("; chunkSize: ").append(chunkSize()).append(')');
    return sb.toString();
  }
}
