package io.netty.buffer;

import io.netty.util.internal.MathUtil;
import io.netty.util.internal.ObjectPool;
import io.netty.util.internal.ObjectPool.Handle;
import io.netty.util.internal.ObjectPool.ObjectCreator;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;



























final class PoolThreadCache
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(PoolThreadCache.class);
  
  private static final int INTEGER_SIZE_MINUS_ONE = 31;
  
  final PoolArena<byte[]> heapArena;
  
  final PoolArena<ByteBuffer> directArena;
  
  private final MemoryRegionCache<byte[]>[] smallSubPageHeapCaches;
  private final MemoryRegionCache<ByteBuffer>[] smallSubPageDirectCaches;
  private final MemoryRegionCache<byte[]>[] normalHeapCaches;
  private final MemoryRegionCache<ByteBuffer>[] normalDirectCaches;
  private final int freeSweepAllocationThreshold;
  private final AtomicBoolean freed = new AtomicBoolean();
  


  private int allocations;
  


  PoolThreadCache(PoolArena<byte[]> heapArena, PoolArena<ByteBuffer> directArena, int smallCacheSize, int normalCacheSize, int maxCachedBufferCapacity, int freeSweepAllocationThreshold)
  {
    ObjectUtil.checkPositiveOrZero(maxCachedBufferCapacity, "maxCachedBufferCapacity");
    this.freeSweepAllocationThreshold = freeSweepAllocationThreshold;
    this.heapArena = heapArena;
    this.directArena = directArena;
    if (directArena != null) {
      smallSubPageDirectCaches = createSubPageCaches(smallCacheSize, numSmallSubpagePools);
      

      normalDirectCaches = createNormalCaches(normalCacheSize, maxCachedBufferCapacity, directArena);
      

      numThreadCaches.getAndIncrement();
    }
    else {
      smallSubPageDirectCaches = null;
      normalDirectCaches = null;
    }
    if (heapArena != null)
    {
      smallSubPageHeapCaches = createSubPageCaches(smallCacheSize, numSmallSubpagePools);
      

      normalHeapCaches = createNormalCaches(normalCacheSize, maxCachedBufferCapacity, heapArena);
      

      numThreadCaches.getAndIncrement();
    }
    else {
      smallSubPageHeapCaches = null;
      normalHeapCaches = null;
    }
    

    if (((smallSubPageDirectCaches != null) || (normalDirectCaches != null) || (smallSubPageHeapCaches != null) || (normalHeapCaches != null)) && (freeSweepAllocationThreshold < 1))
    {

      throw new IllegalArgumentException("freeSweepAllocationThreshold: " + freeSweepAllocationThreshold + " (expected: > 0)");
    }
  }
  

  private static <T> MemoryRegionCache<T>[] createSubPageCaches(int cacheSize, int numCaches)
  {
    if ((cacheSize > 0) && (numCaches > 0))
    {
      MemoryRegionCache<T>[] cache = new MemoryRegionCache[numCaches];
      for (int i = 0; i < cache.length; i++)
      {
        cache[i] = new SubPageMemoryRegionCache(cacheSize);
      }
      return cache;
    }
    return null;
  }
  


  private static <T> MemoryRegionCache<T>[] createNormalCaches(int cacheSize, int maxCachedBufferCapacity, PoolArena<T> area)
  {
    if ((cacheSize > 0) && (maxCachedBufferCapacity > 0)) {
      int max = Math.min(chunkSize, maxCachedBufferCapacity);
      

      List<MemoryRegionCache<T>> cache = new ArrayList();
      for (int idx = numSmallSubpagePools; (idx < nSizes) && (area.sizeIdx2size(idx) <= max); idx++) {
        cache.add(new NormalMemoryRegionCache(cacheSize));
      }
      return (MemoryRegionCache[])cache.toArray(new MemoryRegionCache[0]);
    }
    return null;
  }
  

  static int log2(int val)
  {
    return 31 - Integer.numberOfLeadingZeros(val);
  }
  


  boolean allocateSmall(PoolArena<?> area, PooledByteBuf<?> buf, int reqCapacity, int sizeIdx)
  {
    return allocate(cacheForSmall(area, sizeIdx), buf, reqCapacity);
  }
  


  boolean allocateNormal(PoolArena<?> area, PooledByteBuf<?> buf, int reqCapacity, int sizeIdx)
  {
    return allocate(cacheForNormal(area, sizeIdx), buf, reqCapacity);
  }
  
  private boolean allocate(MemoryRegionCache<?> cache, PooledByteBuf buf, int reqCapacity)
  {
    if (cache == null)
    {
      return false;
    }
    boolean allocated = cache.allocate(buf, reqCapacity, this);
    if (++allocations >= freeSweepAllocationThreshold) {
      allocations = 0;
      trim();
    }
    return allocated;
  }
  





  boolean add(PoolArena<?> area, PoolChunk chunk, ByteBuffer nioBuffer, long handle, int normCapacity, PoolArena.SizeClass sizeClass)
  {
    int sizeIdx = area.size2SizeIdx(normCapacity);
    MemoryRegionCache<?> cache = cache(area, sizeIdx, sizeClass);
    if (cache == null) {
      return false;
    }
    return cache.add(chunk, nioBuffer, handle, normCapacity);
  }
  
  private MemoryRegionCache<?> cache(PoolArena<?> area, int sizeIdx, PoolArena.SizeClass sizeClass) {
    switch (1.$SwitchMap$io$netty$buffer$PoolArena$SizeClass[sizeClass.ordinal()]) {
    case 1: 
      return cacheForNormal(area, sizeIdx);
    case 2: 
      return cacheForSmall(area, sizeIdx);
    }
    throw new Error();
  }
  
  protected void finalize()
    throws Throwable
  {
    try
    {
      super.finalize();
      
      free(true); } finally { free(true);
    }
  }
  




  void free(boolean finalizer)
  {
    if (freed.compareAndSet(false, true))
    {


      int numFreed = free(smallSubPageDirectCaches, finalizer) + free(normalDirectCaches, finalizer) + free(smallSubPageHeapCaches, finalizer) + free(normalHeapCaches, finalizer);
      
      if ((numFreed > 0) && (logger.isDebugEnabled())) {
        logger.debug("Freed {} thread-local buffer(s) from thread: {}", Integer.valueOf(numFreed), 
          Thread.currentThread().getName());
      }
      
      if (directArena != null) {
        directArena.numThreadCaches.getAndDecrement();
      }
      
      if (heapArena != null) {
        heapArena.numThreadCaches.getAndDecrement();
      }
    }
  }
  
  private static int free(MemoryRegionCache<?>[] caches, boolean finalizer) {
    if (caches == null) {
      return 0;
    }
    
    int numFreed = 0;
    for (MemoryRegionCache<?> c : caches) {
      numFreed += free(c, finalizer);
    }
    return numFreed;
  }
  
  private static int free(MemoryRegionCache<?> cache, boolean finalizer) {
    if (cache == null) {
      return 0;
    }
    return cache.free(finalizer);
  }
  
  void trim() {
    trim(smallSubPageDirectCaches);
    trim(normalDirectCaches);
    trim(smallSubPageHeapCaches);
    trim(normalHeapCaches);
  }
  
  private static void trim(MemoryRegionCache<?>[] caches) {
    if (caches == null) {
      return;
    }
    for (MemoryRegionCache<?> c : caches) {
      trim(c);
    }
  }
  
  private static void trim(MemoryRegionCache<?> cache) {
    if (cache == null) {
      return;
    }
    cache.trim();
  }
  
  private MemoryRegionCache<?> cacheForSmall(PoolArena<?> area, int sizeIdx) {
    if (area.isDirect()) {
      return cache(smallSubPageDirectCaches, sizeIdx);
    }
    return cache(smallSubPageHeapCaches, sizeIdx);
  }
  
  private MemoryRegionCache<?> cacheForNormal(PoolArena<?> area, int sizeIdx)
  {
    int idx = sizeIdx - numSmallSubpagePools;
    if (area.isDirect()) {
      return cache(normalDirectCaches, idx);
    }
    return cache(normalHeapCaches, idx);
  }
  
  private static <T> MemoryRegionCache<T> cache(MemoryRegionCache<T>[] cache, int sizeIdx) {
    if ((cache == null) || (sizeIdx > cache.length - 1)) {
      return null;
    }
    return cache[sizeIdx];
  }
  
  private static final class SubPageMemoryRegionCache<T>
    extends PoolThreadCache.MemoryRegionCache<T>
  {
    SubPageMemoryRegionCache(int size)
    {
      super(PoolArena.SizeClass.Small);
    }
    


    protected void initBuf(PoolChunk<T> chunk, ByteBuffer nioBuffer, long handle, PooledByteBuf<T> buf, int reqCapacity, PoolThreadCache threadCache)
    {
      chunk.initBufWithSubpage(buf, nioBuffer, handle, reqCapacity, threadCache);
    }
  }
  
  private static final class NormalMemoryRegionCache<T>
    extends PoolThreadCache.MemoryRegionCache<T>
  {
    NormalMemoryRegionCache(int size)
    {
      super(PoolArena.SizeClass.Normal);
    }
    


    protected void initBuf(PoolChunk<T> chunk, ByteBuffer nioBuffer, long handle, PooledByteBuf<T> buf, int reqCapacity, PoolThreadCache threadCache)
    {
      chunk.initBuf(buf, nioBuffer, handle, reqCapacity, threadCache);
    }
  }
  
  private static abstract class MemoryRegionCache<T> {
    private final int size;
    private final Queue<Entry<T>> queue;
    private final PoolArena.SizeClass sizeClass;
    private int allocations;
    
    MemoryRegionCache(int size, PoolArena.SizeClass sizeClass) {
      this.size = MathUtil.safeFindNextPositivePowerOfTwo(size);
      queue = PlatformDependent.newFixedMpscQueue(this.size);
      this.sizeClass = sizeClass;
    }
    




    protected abstract void initBuf(PoolChunk<T> paramPoolChunk, ByteBuffer paramByteBuffer, long paramLong, PooledByteBuf<T> paramPooledByteBuf, int paramInt, PoolThreadCache paramPoolThreadCache);
    



    public final boolean add(PoolChunk<T> chunk, ByteBuffer nioBuffer, long handle, int normCapacity)
    {
      Entry<T> entry = newEntry(chunk, nioBuffer, handle, normCapacity);
      boolean queued = queue.offer(entry);
      if (!queued)
      {
        entry.recycle();
      }
      
      return queued;
    }
    


    public final boolean allocate(PooledByteBuf<T> buf, int reqCapacity, PoolThreadCache threadCache)
    {
      Entry<T> entry = (Entry)queue.poll();
      if (entry == null) {
        return false;
      }
      initBuf(chunk, nioBuffer, handle, buf, reqCapacity, threadCache);
      entry.recycle();
      

      allocations += 1;
      return true;
    }
    


    public final int free(boolean finalizer)
    {
      return free(Integer.MAX_VALUE, finalizer);
    }
    
    private int free(int max, boolean finalizer) {
      for (int numFreed = 0; 
          numFreed < max; numFreed++) {
        Entry<T> entry = (Entry)queue.poll();
        if (entry != null) {
          freeEntry(entry, finalizer);
        }
        else {
          return numFreed;
        }
      }
      return numFreed;
    }
    


    public final void trim()
    {
      int free = size - allocations;
      allocations = 0;
      

      if (free > 0) {
        free(free, false);
      }
    }
    
    private void freeEntry(Entry entry, boolean finalizer)
    {
      PoolChunk chunk = chunk;
      long handle = handle;
      ByteBuffer nioBuffer = nioBuffer;
      
      if (!finalizer)
      {

        entry.recycle();
      }
      
      arena.freeChunk(chunk, handle, normCapacity, sizeClass, nioBuffer, finalizer);
    }
    
    static final class Entry<T> {
      final ObjectPool.Handle<Entry<?>> recyclerHandle;
      PoolChunk<T> chunk;
      ByteBuffer nioBuffer;
      long handle = -1L;
      int normCapacity;
      
      Entry(ObjectPool.Handle<Entry<?>> recyclerHandle) {
        this.recyclerHandle = recyclerHandle;
      }
      
      void recycle() {
        chunk = null;
        nioBuffer = null;
        handle = -1L;
        recyclerHandle.recycle(this);
      }
    }
    
    private static Entry newEntry(PoolChunk<?> chunk, ByteBuffer nioBuffer, long handle, int normCapacity)
    {
      Entry entry = (Entry)RECYCLER.get();
      chunk = chunk;
      nioBuffer = nioBuffer;
      handle = handle;
      normCapacity = normCapacity;
      return entry;
    }
    

    private static final ObjectPool<Entry> RECYCLER = ObjectPool.newPool(new ObjectPool.ObjectCreator()
    {
      public PoolThreadCache.MemoryRegionCache.Entry newObject(ObjectPool.Handle<PoolThreadCache.MemoryRegionCache.Entry> handle)
      {
        return new PoolThreadCache.MemoryRegionCache.Entry(handle);
      }
    });
  }
}
