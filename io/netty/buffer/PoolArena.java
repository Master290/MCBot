package io.netty.buffer;

import io.netty.util.internal.LongCounter;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.StringUtil;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

















abstract class PoolArena<T>
  extends SizeClasses
  implements PoolArenaMetric
{
  static final boolean HAS_UNSAFE = PlatformDependent.hasUnsafe();
  final PooledByteBufAllocator parent;
  
  static enum SizeClass { Small, 
    Normal;
    

    private SizeClass() {}
  }
  

  final int numSmallSubpagePools;
  
  final int directMemoryCacheAlignment;
  
  private final PoolSubpage<T>[] smallSubpagePools;
  
  private final PoolChunkList<T> q050;
  private final PoolChunkList<T> q025;
  private final PoolChunkList<T> q000;
  private final PoolChunkList<T> qInit;
  private final PoolChunkList<T> q075;
  private final PoolChunkList<T> q100;
  private final List<PoolChunkListMetric> chunkListMetrics;
  private long allocationsNormal;
  private final LongCounter allocationsSmall = PlatformDependent.newLongCounter();
  private final LongCounter allocationsHuge = PlatformDependent.newLongCounter();
  private final LongCounter activeBytesHuge = PlatformDependent.newLongCounter();
  
  private long deallocationsSmall;
  
  private long deallocationsNormal;
  
  private final LongCounter deallocationsHuge = PlatformDependent.newLongCounter();
  

  final AtomicInteger numThreadCaches = new AtomicInteger();
  



  protected PoolArena(PooledByteBufAllocator parent, int pageSize, int pageShifts, int chunkSize, int cacheAlignment)
  {
    super(pageSize, pageShifts, chunkSize, cacheAlignment);
    this.parent = parent;
    directMemoryCacheAlignment = cacheAlignment;
    
    numSmallSubpagePools = nSubpages;
    smallSubpagePools = newSubpagePoolArray(numSmallSubpagePools);
    for (int i = 0; i < smallSubpagePools.length; i++) {
      smallSubpagePools[i] = newSubpagePoolHead();
    }
    
    q100 = new PoolChunkList(this, null, 100, Integer.MAX_VALUE, chunkSize);
    q075 = new PoolChunkList(this, q100, 75, 100, chunkSize);
    q050 = new PoolChunkList(this, q075, 50, 100, chunkSize);
    q025 = new PoolChunkList(this, q050, 25, 75, chunkSize);
    q000 = new PoolChunkList(this, q025, 1, 50, chunkSize);
    qInit = new PoolChunkList(this, q000, Integer.MIN_VALUE, 25, chunkSize);
    
    q100.prevList(q075);
    q075.prevList(q050);
    q050.prevList(q025);
    q025.prevList(q000);
    q000.prevList(null);
    qInit.prevList(qInit);
    
    List<PoolChunkListMetric> metrics = new ArrayList(6);
    metrics.add(qInit);
    metrics.add(q000);
    metrics.add(q025);
    metrics.add(q050);
    metrics.add(q075);
    metrics.add(q100);
    chunkListMetrics = Collections.unmodifiableList(metrics);
  }
  
  private PoolSubpage<T> newSubpagePoolHead() {
    PoolSubpage<T> head = new PoolSubpage();
    prev = head;
    next = head;
    return head;
  }
  
  private PoolSubpage<T>[] newSubpagePoolArray(int size)
  {
    return new PoolSubpage[size];
  }
  
  abstract boolean isDirect();
  
  PooledByteBuf<T> allocate(PoolThreadCache cache, int reqCapacity, int maxCapacity) {
    PooledByteBuf<T> buf = newByteBuf(maxCapacity);
    allocate(cache, buf, reqCapacity);
    return buf;
  }
  
  private void allocate(PoolThreadCache cache, PooledByteBuf<T> buf, int reqCapacity) {
    int sizeIdx = size2SizeIdx(reqCapacity);
    
    if (sizeIdx <= smallMaxSizeIdx) {
      tcacheAllocateSmall(cache, buf, reqCapacity, sizeIdx);
    } else if (sizeIdx < nSizes) {
      tcacheAllocateNormal(cache, buf, reqCapacity, sizeIdx);
    }
    else {
      int normCapacity = directMemoryCacheAlignment > 0 ? normalizeSize(reqCapacity) : reqCapacity;
      
      allocateHuge(buf, normCapacity);
    }
  }
  

  private void tcacheAllocateSmall(PoolThreadCache cache, PooledByteBuf<T> buf, int reqCapacity, int sizeIdx)
  {
    if (cache.allocateSmall(this, buf, reqCapacity, sizeIdx))
    {
      return;
    }
    




    PoolSubpage<T> head = smallSubpagePools[sizeIdx];
    
    synchronized (head) {
      PoolSubpage<T> s = next;
      boolean needsNormalAllocation = s == head;
      if (!needsNormalAllocation) {
        assert ((doNotDestroy) && (elemSize == sizeIdx2size(sizeIdx)));
        long handle = s.allocate();
        assert (handle >= 0L);
        chunk.initBufWithSubpage(buf, null, handle, reqCapacity, cache);
      }
    }
    boolean needsNormalAllocation;
    if (needsNormalAllocation) {
      synchronized (this) {
        allocateNormal(buf, reqCapacity, sizeIdx, cache);
      }
    }
    
    incSmallAllocation();
  }
  
  private void tcacheAllocateNormal(PoolThreadCache cache, PooledByteBuf<T> buf, int reqCapacity, int sizeIdx)
  {
    if (cache.allocateNormal(this, buf, reqCapacity, sizeIdx))
    {
      return;
    }
    synchronized (this) {
      allocateNormal(buf, reqCapacity, sizeIdx, cache);
      allocationsNormal += 1L;
    }
  }
  
  private void allocateNormal(PooledByteBuf<T> buf, int reqCapacity, int sizeIdx, PoolThreadCache threadCache)
  {
    if ((q050.allocate(buf, reqCapacity, sizeIdx, threadCache)) || 
      (q025.allocate(buf, reqCapacity, sizeIdx, threadCache)) || 
      (q000.allocate(buf, reqCapacity, sizeIdx, threadCache)) || 
      (qInit.allocate(buf, reqCapacity, sizeIdx, threadCache)) || 
      (q075.allocate(buf, reqCapacity, sizeIdx, threadCache))) {
      return;
    }
    

    PoolChunk<T> c = newChunk(pageSize, nPSizes, pageShifts, chunkSize);
    boolean success = c.allocate(buf, reqCapacity, sizeIdx, threadCache);
    assert (success);
    qInit.add(c);
  }
  
  private void incSmallAllocation() {
    allocationsSmall.increment();
  }
  
  private void allocateHuge(PooledByteBuf<T> buf, int reqCapacity) {
    PoolChunk<T> chunk = newUnpooledChunk(reqCapacity);
    activeBytesHuge.add(chunk.chunkSize());
    buf.initUnpooled(chunk, reqCapacity);
    allocationsHuge.increment();
  }
  
  void free(PoolChunk<T> chunk, ByteBuffer nioBuffer, long handle, int normCapacity, PoolThreadCache cache) {
    if (unpooled) {
      int size = chunk.chunkSize();
      destroyChunk(chunk);
      activeBytesHuge.add(-size);
      deallocationsHuge.increment();
    } else {
      SizeClass sizeClass = sizeClass(handle);
      if ((cache != null) && (cache.add(this, chunk, nioBuffer, handle, normCapacity, sizeClass)))
      {
        return;
      }
      
      freeChunk(chunk, handle, normCapacity, sizeClass, nioBuffer, false);
    }
  }
  
  private static SizeClass sizeClass(long handle) {
    return PoolChunk.isSubpage(handle) ? SizeClass.Small : SizeClass.Normal;
  }
  
  void freeChunk(PoolChunk<T> chunk, long handle, int normCapacity, SizeClass sizeClass, ByteBuffer nioBuffer, boolean finalizer)
  {
    boolean destroyChunk;
    synchronized (this)
    {

      if (!finalizer) {
        switch (1.$SwitchMap$io$netty$buffer$PoolArena$SizeClass[sizeClass.ordinal()]) {
        case 1: 
          deallocationsNormal += 1L;
          break;
        case 2: 
          deallocationsSmall += 1L;
          break;
        default: 
          throw new Error();
        }
      }
      destroyChunk = !parent.free(chunk, handle, normCapacity, nioBuffer); }
    boolean destroyChunk;
    if (destroyChunk)
    {
      destroyChunk(chunk);
    }
  }
  
  PoolSubpage<T> findSubpagePoolHead(int sizeIdx) {
    return smallSubpagePools[sizeIdx];
  }
  
  void reallocate(PooledByteBuf<T> buf, int newCapacity, boolean freeOldMemory) {
    assert ((newCapacity >= 0) && (newCapacity <= buf.maxCapacity()));
    
    int oldCapacity = length;
    if (oldCapacity == newCapacity) {
      return;
    }
    
    PoolChunk<T> oldChunk = chunk;
    ByteBuffer oldNioBuffer = tmpNioBuf;
    long oldHandle = handle;
    T oldMemory = memory;
    int oldOffset = offset;
    int oldMaxLength = maxLength;
    

    allocate(parent.threadCache(), buf, newCapacity);
    int bytesToCopy;
    int bytesToCopy; if (newCapacity > oldCapacity) {
      bytesToCopy = oldCapacity;
    } else {
      buf.trimIndicesToCapacity(newCapacity);
      bytesToCopy = newCapacity;
    }
    memoryCopy(oldMemory, oldOffset, buf, bytesToCopy);
    if (freeOldMemory) {
      free(oldChunk, oldNioBuffer, oldHandle, oldMaxLength, cache);
    }
  }
  
  public int numThreadCaches()
  {
    return numThreadCaches.get();
  }
  
  public int numTinySubpages()
  {
    return 0;
  }
  
  public int numSmallSubpages()
  {
    return smallSubpagePools.length;
  }
  
  public int numChunkLists()
  {
    return chunkListMetrics.size();
  }
  
  public List<PoolSubpageMetric> tinySubpages()
  {
    return Collections.emptyList();
  }
  
  public List<PoolSubpageMetric> smallSubpages()
  {
    return subPageMetricList(smallSubpagePools);
  }
  
  public List<PoolChunkListMetric> chunkLists()
  {
    return chunkListMetrics;
  }
  
  private static List<PoolSubpageMetric> subPageMetricList(PoolSubpage<?>[] pages) {
    List<PoolSubpageMetric> metrics = new ArrayList();
    for (PoolSubpage<?> head : pages) {
      if (next != head)
      {

        PoolSubpage<?> s = next;
        for (;;) {
          metrics.add(s);
          s = next;
          if (s == head)
            break;
        }
      }
    }
    return metrics;
  }
  
  public long numAllocations()
  {
    long allocsNormal;
    synchronized (this) {
      allocsNormal = allocationsNormal; }
    long allocsNormal;
    return allocationsSmall.value() + allocsNormal + allocationsHuge.value();
  }
  
  public long numTinyAllocations()
  {
    return 0L;
  }
  
  public long numSmallAllocations()
  {
    return allocationsSmall.value();
  }
  
  public synchronized long numNormalAllocations()
  {
    return allocationsNormal;
  }
  
  public long numDeallocations()
  {
    long deallocs;
    synchronized (this) {
      deallocs = deallocationsSmall + deallocationsNormal; }
    long deallocs;
    return deallocs + deallocationsHuge.value();
  }
  
  public long numTinyDeallocations()
  {
    return 0L;
  }
  
  public synchronized long numSmallDeallocations()
  {
    return deallocationsSmall;
  }
  
  public synchronized long numNormalDeallocations()
  {
    return deallocationsNormal;
  }
  
  public long numHugeAllocations()
  {
    return allocationsHuge.value();
  }
  
  public long numHugeDeallocations()
  {
    return deallocationsHuge.value();
  }
  

  public long numActiveAllocations()
  {
    long val = allocationsSmall.value() + allocationsHuge.value() - deallocationsHuge.value();
    synchronized (this) {
      val += allocationsNormal - (deallocationsSmall + deallocationsNormal);
    }
    return Math.max(val, 0L);
  }
  
  public long numActiveTinyAllocations()
  {
    return 0L;
  }
  
  public long numActiveSmallAllocations()
  {
    return Math.max(numSmallAllocations() - numSmallDeallocations(), 0L);
  }
  
  public long numActiveNormalAllocations()
  {
    long val;
    synchronized (this) {
      val = allocationsNormal - deallocationsNormal; }
    long val;
    return Math.max(val, 0L);
  }
  
  public long numActiveHugeAllocations()
  {
    return Math.max(numHugeAllocations() - numHugeDeallocations(), 0L);
  }
  
  public long numActiveBytes()
  {
    long val = activeBytesHuge.value();
    synchronized (this) {
      for (int i = 0; i < chunkListMetrics.size(); i++) {
        for (PoolChunkMetric m : (PoolChunkListMetric)chunkListMetrics.get(i)) {
          val += m.chunkSize();
        }
      }
    }
    return Math.max(0L, val);
  }
  




  protected abstract PoolChunk<T> newChunk(int paramInt1, int paramInt2, int paramInt3, int paramInt4);
  




  protected abstract PoolChunk<T> newUnpooledChunk(int paramInt);
  




  protected abstract PooledByteBuf<T> newByteBuf(int paramInt);
  



  protected abstract void memoryCopy(T paramT, int paramInt1, PooledByteBuf<T> paramPooledByteBuf, int paramInt2);
  



  protected abstract void destroyChunk(PoolChunk<T> paramPoolChunk);
  



  public synchronized String toString()
  {
    StringBuilder buf = new StringBuilder().append("Chunk(s) at 0~25%:").append(StringUtil.NEWLINE).append(qInit).append(StringUtil.NEWLINE).append("Chunk(s) at 0~50%:").append(StringUtil.NEWLINE).append(q000).append(StringUtil.NEWLINE).append("Chunk(s) at 25~75%:").append(StringUtil.NEWLINE).append(q025).append(StringUtil.NEWLINE).append("Chunk(s) at 50~100%:").append(StringUtil.NEWLINE).append(q050).append(StringUtil.NEWLINE).append("Chunk(s) at 75~100%:").append(StringUtil.NEWLINE).append(q075).append(StringUtil.NEWLINE).append("Chunk(s) at 100%:").append(StringUtil.NEWLINE).append(q100).append(StringUtil.NEWLINE).append("small subpages:");
    appendPoolSubPages(buf, smallSubpagePools);
    buf.append(StringUtil.NEWLINE);
    
    return buf.toString();
  }
  
  private static void appendPoolSubPages(StringBuilder buf, PoolSubpage<?>[] subpages) {
    for (int i = 0; i < subpages.length; i++) {
      PoolSubpage<?> head = subpages[i];
      if (next != head)
      {




        buf.append(StringUtil.NEWLINE).append(i).append(": ");
        PoolSubpage<?> s = next;
        for (;;) {
          buf.append(s);
          s = next;
          if (s == head) {
            break;
          }
        }
      }
    }
  }
  
  protected final void finalize() throws Throwable {
    try {
      super.finalize();
      
      destroyPoolSubPages(smallSubpagePools);
      destroyPoolChunkLists(new PoolChunkList[] { qInit, q000, q025, q050, q075, q100 });
    }
    finally
    {
      destroyPoolSubPages(smallSubpagePools);
      destroyPoolChunkLists(new PoolChunkList[] { qInit, q000, q025, q050, q075, q100 });
    }
  }
  
  private static void destroyPoolSubPages(PoolSubpage<?>[] pages) {
    for (PoolSubpage<?> page : pages) {
      page.destroy();
    }
  }
  
  private void destroyPoolChunkLists(PoolChunkList<T>... chunkLists) {
    for (PoolChunkList<T> chunkList : chunkLists) {
      chunkList.destroy(this);
    }
  }
  
  static final class HeapArena extends PoolArena<byte[]>
  {
    HeapArena(PooledByteBufAllocator parent, int pageSize, int pageShifts, int chunkSize, int directMemoryCacheAlignment)
    {
      super(pageSize, pageShifts, chunkSize, directMemoryCacheAlignment);
    }
    
    private static byte[] newByteArray(int size)
    {
      return PlatformDependent.allocateUninitializedArray(size);
    }
    
    boolean isDirect()
    {
      return false;
    }
    
    protected PoolChunk<byte[]> newChunk(int pageSize, int maxPageIdx, int pageShifts, int chunkSize)
    {
      return new PoolChunk(this, null, 
        newByteArray(chunkSize), pageSize, pageShifts, chunkSize, maxPageIdx);
    }
    
    protected PoolChunk<byte[]> newUnpooledChunk(int capacity)
    {
      return new PoolChunk(this, null, newByteArray(capacity), capacity);
    }
    


    protected void destroyChunk(PoolChunk<byte[]> chunk) {}
    

    protected PooledByteBuf<byte[]> newByteBuf(int maxCapacity)
    {
      return HAS_UNSAFE ? PooledUnsafeHeapByteBuf.newUnsafeInstance(maxCapacity) : 
        PooledHeapByteBuf.newInstance(maxCapacity);
    }
    
    protected void memoryCopy(byte[] src, int srcOffset, PooledByteBuf<byte[]> dst, int length)
    {
      if (length == 0) {
        return;
      }
      
      System.arraycopy(src, srcOffset, memory, offset, length);
    }
  }
  
  static final class DirectArena extends PoolArena<ByteBuffer>
  {
    DirectArena(PooledByteBufAllocator parent, int pageSize, int pageShifts, int chunkSize, int directMemoryCacheAlignment)
    {
      super(pageSize, pageShifts, chunkSize, directMemoryCacheAlignment);
    }
    

    boolean isDirect()
    {
      return true;
    }
    

    protected PoolChunk<ByteBuffer> newChunk(int pageSize, int maxPageIdx, int pageShifts, int chunkSize)
    {
      if (directMemoryCacheAlignment == 0) {
        ByteBuffer memory = allocateDirect(chunkSize);
        return new PoolChunk(this, memory, memory, pageSize, pageShifts, chunkSize, maxPageIdx);
      }
      

      ByteBuffer base = allocateDirect(chunkSize + directMemoryCacheAlignment);
      ByteBuffer memory = PlatformDependent.alignDirectBuffer(base, directMemoryCacheAlignment);
      return new PoolChunk(this, base, memory, pageSize, pageShifts, chunkSize, maxPageIdx);
    }
    

    protected PoolChunk<ByteBuffer> newUnpooledChunk(int capacity)
    {
      if (directMemoryCacheAlignment == 0) {
        ByteBuffer memory = allocateDirect(capacity);
        return new PoolChunk(this, memory, memory, capacity);
      }
      
      ByteBuffer base = allocateDirect(capacity + directMemoryCacheAlignment);
      ByteBuffer memory = PlatformDependent.alignDirectBuffer(base, directMemoryCacheAlignment);
      return new PoolChunk(this, base, memory, capacity);
    }
    
    private static ByteBuffer allocateDirect(int capacity) {
      return PlatformDependent.useDirectBufferNoCleaner() ? 
        PlatformDependent.allocateDirectNoCleaner(capacity) : ByteBuffer.allocateDirect(capacity);
    }
    
    protected void destroyChunk(PoolChunk<ByteBuffer> chunk)
    {
      if (PlatformDependent.useDirectBufferNoCleaner()) {
        PlatformDependent.freeDirectNoCleaner((ByteBuffer)base);
      } else {
        PlatformDependent.freeDirectBuffer((ByteBuffer)base);
      }
    }
    
    protected PooledByteBuf<ByteBuffer> newByteBuf(int maxCapacity)
    {
      if (HAS_UNSAFE) {
        return PooledUnsafeDirectByteBuf.newInstance(maxCapacity);
      }
      return PooledDirectByteBuf.newInstance(maxCapacity);
    }
    

    protected void memoryCopy(ByteBuffer src, int srcOffset, PooledByteBuf<ByteBuffer> dstBuf, int length)
    {
      if (length == 0) {
        return;
      }
      
      if (HAS_UNSAFE) {
        PlatformDependent.copyMemory(
          PlatformDependent.directBufferAddress(src) + srcOffset, 
          PlatformDependent.directBufferAddress((ByteBuffer)memory) + offset, length);
      }
      else {
        src = src.duplicate();
        ByteBuffer dst = dstBuf.internalNioBuffer();
        src.position(srcOffset).limit(srcOffset + length);
        dst.position(offset);
        dst.put(src);
      }
    }
  }
}
