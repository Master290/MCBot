package io.netty.buffer;

import io.netty.util.internal.StringUtil;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;



















final class PoolChunkList<T>
  implements PoolChunkListMetric
{
  private static final Iterator<PoolChunkMetric> EMPTY_METRICS = Collections.emptyList().iterator();
  
  private final PoolArena<T> arena;
  
  private final PoolChunkList<T> nextList;
  
  private final int minUsage;
  
  private final int maxUsage;
  private final int maxCapacity;
  private PoolChunk<T> head;
  private final int freeMinThreshold;
  private final int freeMaxThreshold;
  private PoolChunkList<T> prevList;
  
  PoolChunkList(PoolArena<T> arena, PoolChunkList<T> nextList, int minUsage, int maxUsage, int chunkSize)
  {
    assert (minUsage <= maxUsage);
    this.arena = arena;
    this.nextList = nextList;
    this.minUsage = minUsage;
    this.maxUsage = maxUsage;
    maxCapacity = calculateMaxCapacity(minUsage, chunkSize);
    















    freeMinThreshold = (maxUsage == 100 ? 0 : (int)(chunkSize * (100.0D - maxUsage + 0.99999999D) / 100.0D));
    freeMaxThreshold = (minUsage == 100 ? 0 : (int)(chunkSize * (100.0D - minUsage + 0.99999999D) / 100.0D));
  }
  



  private static int calculateMaxCapacity(int minUsage, int chunkSize)
  {
    minUsage = minUsage0(minUsage);
    
    if (minUsage == 100)
    {
      return 0;
    }
    





    return (int)(chunkSize * (100L - minUsage) / 100L);
  }
  
  void prevList(PoolChunkList<T> prevList) {
    assert (this.prevList == null);
    this.prevList = prevList;
  }
  
  boolean allocate(PooledByteBuf<T> buf, int reqCapacity, int sizeIdx, PoolThreadCache threadCache) {
    int normCapacity = arena.sizeIdx2size(sizeIdx);
    if (normCapacity > maxCapacity)
    {

      return false;
    }
    
    for (PoolChunk<T> cur = head; cur != null; cur = next) {
      if (cur.allocate(buf, reqCapacity, sizeIdx, threadCache)) {
        if (freeBytes <= freeMinThreshold) {
          remove(cur);
          nextList.add(cur);
        }
        return true;
      }
    }
    return false;
  }
  
  boolean free(PoolChunk<T> chunk, long handle, int normCapacity, ByteBuffer nioBuffer) {
    chunk.free(handle, normCapacity, nioBuffer);
    if (freeBytes > freeMaxThreshold) {
      remove(chunk);
      
      return move0(chunk);
    }
    return true;
  }
  
  private boolean move(PoolChunk<T> chunk) {
    assert (chunk.usage() < maxUsage);
    
    if (freeBytes > freeMaxThreshold)
    {
      return move0(chunk);
    }
    

    add0(chunk);
    return true;
  }
  



  private boolean move0(PoolChunk<T> chunk)
  {
    if (prevList == null)
    {

      assert (chunk.usage() == 0);
      return false;
    }
    return prevList.move(chunk);
  }
  
  void add(PoolChunk<T> chunk) {
    if (freeBytes <= freeMinThreshold) {
      nextList.add(chunk);
      return;
    }
    add0(chunk);
  }
  


  void add0(PoolChunk<T> chunk)
  {
    parent = this;
    if (head == null) {
      head = chunk;
      prev = null;
      next = null;
    } else {
      prev = null;
      next = head;
      head.prev = chunk;
      head = chunk;
    }
  }
  
  private void remove(PoolChunk<T> cur) {
    if (cur == head) {
      head = next;
      if (head != null) {
        head.prev = null;
      }
    } else {
      PoolChunk<T> next = next;
      prev.next = next;
      if (next != null) {
        prev = prev;
      }
    }
  }
  
  public int minUsage()
  {
    return minUsage0(minUsage);
  }
  
  public int maxUsage()
  {
    return Math.min(maxUsage, 100);
  }
  
  private static int minUsage0(int value) {
    return Math.max(1, value);
  }
  
  public Iterator<PoolChunkMetric> iterator()
  {
    synchronized (arena) {
      if (head == null) {
        return EMPTY_METRICS;
      }
      List<PoolChunkMetric> metrics = new ArrayList();
      PoolChunk<T> cur = head;
      for (;;) { metrics.add(cur);
        cur = next;
        if (cur == null) {
          break;
        }
      }
      return metrics.iterator();
    }
  }
  
  public String toString()
  {
    StringBuilder buf = new StringBuilder();
    synchronized (arena) {
      if (head == null) {
        return "none";
      }
      
      PoolChunk<T> cur = head;
      for (;;) { buf.append(cur);
        cur = next;
        if (cur == null) {
          break;
        }
        buf.append(StringUtil.NEWLINE);
      }
    }
    return buf.toString();
  }
  
  void destroy(PoolArena<T> arena) {
    PoolChunk<T> chunk = head;
    while (chunk != null) {
      arena.destroyChunk(chunk);
      chunk = next;
    }
    head = null;
  }
}
