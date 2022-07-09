package io.netty.buffer;



final class PoolSubpage<T>
  implements PoolSubpageMetric
{
  final PoolChunk<T> chunk;
  

  private final int pageShifts;
  

  private final int runOffset;
  

  private final int runSize;
  

  private final long[] bitmap;
  

  PoolSubpage<T> prev;
  

  PoolSubpage<T> next;
  

  boolean doNotDestroy;
  

  int elemSize;
  

  private int maxNumElems;
  

  private int bitmapLength;
  

  private int nextAvail;
  
  private int numAvail;
  

  PoolSubpage()
  {
    chunk = null;
    pageShifts = -1;
    runOffset = -1;
    elemSize = -1;
    runSize = -1;
    bitmap = null;
  }
  
  PoolSubpage(PoolSubpage<T> head, PoolChunk<T> chunk, int pageShifts, int runOffset, int runSize, int elemSize) {
    this.chunk = chunk;
    this.pageShifts = pageShifts;
    this.runOffset = runOffset;
    this.runSize = runSize;
    this.elemSize = elemSize;
    bitmap = new long[runSize >>> 10];
    
    doNotDestroy = true;
    if (elemSize != 0) {
      maxNumElems = (this.numAvail = runSize / elemSize);
      nextAvail = 0;
      bitmapLength = (maxNumElems >>> 6);
      if ((maxNumElems & 0x3F) != 0) {
        bitmapLength += 1;
      }
      
      for (int i = 0; i < bitmapLength; i++) {
        bitmap[i] = 0L;
      }
    }
    addToPool(head);
  }
  


  long allocate()
  {
    if ((numAvail == 0) || (!doNotDestroy)) {
      return -1L;
    }
    
    int bitmapIdx = getNextAvail();
    int q = bitmapIdx >>> 6;
    int r = bitmapIdx & 0x3F;
    assert ((bitmap[q] >>> r & 1L) == 0L);
    bitmap[q] |= 1L << r;
    
    if (--numAvail == 0) {
      removeFromPool();
    }
    
    return toHandle(bitmapIdx);
  }
  



  boolean free(PoolSubpage<T> head, int bitmapIdx)
  {
    if (elemSize == 0) {
      return true;
    }
    int q = bitmapIdx >>> 6;
    int r = bitmapIdx & 0x3F;
    assert ((bitmap[q] >>> r & 1L) != 0L);
    bitmap[q] ^= 1L << r;
    
    setNextAvail(bitmapIdx);
    
    if (numAvail++ == 0) {
      addToPool(head);
      



      if (maxNumElems > 1) {
        return true;
      }
    }
    
    if (numAvail != maxNumElems) {
      return true;
    }
    
    if (prev == next)
    {
      return true;
    }
    

    doNotDestroy = false;
    removeFromPool();
    return false;
  }
  
  private void addToPool(PoolSubpage<T> head)
  {
    assert ((prev == null) && (next == null));
    prev = head;
    next = next;
    next.prev = this;
    next = this;
  }
  
  private void removeFromPool() {
    assert ((prev != null) && (next != null));
    prev.next = next;
    next.prev = prev;
    next = null;
    prev = null;
  }
  
  private void setNextAvail(int bitmapIdx) {
    nextAvail = bitmapIdx;
  }
  
  private int getNextAvail() {
    int nextAvail = this.nextAvail;
    if (nextAvail >= 0) {
      this.nextAvail = -1;
      return nextAvail;
    }
    return findNextAvail();
  }
  
  private int findNextAvail() {
    long[] bitmap = this.bitmap;
    int bitmapLength = this.bitmapLength;
    for (int i = 0; i < bitmapLength; i++) {
      long bits = bitmap[i];
      if ((bits ^ 0xFFFFFFFFFFFFFFFF) != 0L) {
        return findNextAvail0(i, bits);
      }
    }
    return -1;
  }
  
  private int findNextAvail0(int i, long bits) {
    int maxNumElems = this.maxNumElems;
    int baseVal = i << 6;
    
    for (int j = 0; j < 64; j++) {
      if ((bits & 1L) == 0L) {
        int val = baseVal | j;
        if (val >= maxNumElems) break;
        return val;
      }
      


      bits >>>= 1;
    }
    return -1;
  }
  
  private long toHandle(int bitmapIdx) {
    int pages = runSize >> pageShifts;
    return runOffset << 49 | pages << 34 | 0x200000000 | 0x100000000 | bitmapIdx;
  }
  




  public String toString()
  {
    int elemSize;
    


    if (chunk == null)
    {
      boolean doNotDestroy = true;
      int maxNumElems = 0;
      int numAvail = 0;
      elemSize = -1;
    } else { int elemSize;
      synchronized (chunk.arena) { int maxNumElems;
        if (!this.doNotDestroy) {
          boolean doNotDestroy = false;
          int elemSize;
          int numAvail; maxNumElems = numAvail = elemSize = -1;
        } else {
          boolean doNotDestroy = true;
          int maxNumElems = this.maxNumElems;
          int numAvail = this.numAvail;
          elemSize = this.elemSize; } } }
    int elemSize;
    int numAvail;
    int maxNumElems;
    boolean doNotDestroy;
    if (!doNotDestroy) {
      return "(" + runOffset + ": not in use)";
    }
    
    return "(" + runOffset + ": " + (maxNumElems - numAvail) + '/' + maxNumElems + ", offset: " + runOffset + ", length: " + runSize + ", elemSize: " + elemSize + ')';
  }
  

  public int maxNumElements()
  {
    if (chunk == null)
    {
      return 0;
    }
    
    synchronized (chunk.arena) {
      return maxNumElems;
    }
  }
  
  public int numAvailable()
  {
    if (chunk == null)
    {
      return 0;
    }
    
    synchronized (chunk.arena) {
      return numAvail;
    }
  }
  
  public int elementSize()
  {
    if (chunk == null)
    {
      return -1;
    }
    
    synchronized (chunk.arena) {
      return elemSize;
    }
  }
  
  public int pageSize()
  {
    return 1 << pageShifts;
  }
  
  void destroy() {
    if (chunk != null) {
      chunk.destroy();
    }
  }
}
