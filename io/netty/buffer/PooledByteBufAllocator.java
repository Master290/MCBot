package io.netty.buffer;

import io.netty.util.NettyRuntime;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.concurrent.FastThreadLocalThread;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.ThreadExecutorMap;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;



















public class PooledByteBufAllocator
  extends AbstractByteBufAllocator
  implements ByteBufAllocatorMetricProvider
{
  private static final InternalLogger logger;
  private static final int DEFAULT_NUM_HEAP_ARENA;
  private static final int DEFAULT_NUM_DIRECT_ARENA;
  private static final int DEFAULT_PAGE_SIZE;
  private static final int DEFAULT_MAX_ORDER;
  private static final int DEFAULT_SMALL_CACHE_SIZE;
  private static final int DEFAULT_NORMAL_CACHE_SIZE;
  static final int DEFAULT_MAX_CACHED_BUFFER_CAPACITY;
  private static final int DEFAULT_CACHE_TRIM_INTERVAL;
  private static final long DEFAULT_CACHE_TRIM_INTERVAL_MILLIS;
  private static final boolean DEFAULT_USE_CACHE_FOR_ALL_THREADS;
  private static final int DEFAULT_DIRECT_MEMORY_CACHE_ALIGNMENT;
  static final int DEFAULT_MAX_CACHED_BYTEBUFFERS_PER_CHUNK;
  private static final int MIN_PAGE_SIZE = 4096;
  private static final int MAX_CHUNK_SIZE = 1073741824;
  private final Runnable trimTask = new Runnable()
  {
    public void run() {
      trimCurrentThreadCache();
    }
  };
  
  static
  {
    logger = InternalLoggerFactory.getInstance(PooledByteBufAllocator.class);
    
























    int defaultAlignment = SystemPropertyUtil.getInt("io.netty.allocator.directMemoryCacheAlignment", 0);
    
    int defaultPageSize = SystemPropertyUtil.getInt("io.netty.allocator.pageSize", 8192);
    Throwable pageSizeFallbackCause = null;
    try {
      validateAndCalculatePageShifts(defaultPageSize, defaultAlignment);
    } catch (Throwable t) {
      pageSizeFallbackCause = t;
      defaultPageSize = 8192;
      defaultAlignment = 0;
    }
    DEFAULT_PAGE_SIZE = defaultPageSize;
    DEFAULT_DIRECT_MEMORY_CACHE_ALIGNMENT = defaultAlignment;
    
    int defaultMaxOrder = SystemPropertyUtil.getInt("io.netty.allocator.maxOrder", 11);
    Throwable maxOrderFallbackCause = null;
    try {
      validateAndCalculateChunkSize(DEFAULT_PAGE_SIZE, defaultMaxOrder);
    } catch (Throwable t) {
      maxOrderFallbackCause = t;
      defaultMaxOrder = 11;
    }
    DEFAULT_MAX_ORDER = defaultMaxOrder;
    


    Runtime runtime = Runtime.getRuntime();
    







    int defaultMinNumArena = NettyRuntime.availableProcessors() * 2;
    int defaultChunkSize = DEFAULT_PAGE_SIZE << DEFAULT_MAX_ORDER;
    DEFAULT_NUM_HEAP_ARENA = Math.max(0, 
      SystemPropertyUtil.getInt("io.netty.allocator.numHeapArenas", 
      
      (int)Math.min(defaultMinNumArena, runtime
      
      .maxMemory() / defaultChunkSize / 2L / 3L)));
    DEFAULT_NUM_DIRECT_ARENA = Math.max(0, 
      SystemPropertyUtil.getInt("io.netty.allocator.numDirectArenas", 
      
      (int)Math.min(defaultMinNumArena, 
      
      PlatformDependent.maxDirectMemory() / defaultChunkSize / 2L / 3L)));
    

    DEFAULT_SMALL_CACHE_SIZE = SystemPropertyUtil.getInt("io.netty.allocator.smallCacheSize", 256);
    DEFAULT_NORMAL_CACHE_SIZE = SystemPropertyUtil.getInt("io.netty.allocator.normalCacheSize", 64);
    


    DEFAULT_MAX_CACHED_BUFFER_CAPACITY = SystemPropertyUtil.getInt("io.netty.allocator.maxCachedBufferCapacity", 32768);
    


    DEFAULT_CACHE_TRIM_INTERVAL = SystemPropertyUtil.getInt("io.netty.allocator.cacheTrimInterval", 8192);
    

    if (SystemPropertyUtil.contains("io.netty.allocation.cacheTrimIntervalMillis")) {
      logger.warn("-Dio.netty.allocation.cacheTrimIntervalMillis is deprecated, use -Dio.netty.allocator.cacheTrimIntervalMillis");
      

      if (SystemPropertyUtil.contains("io.netty.allocator.cacheTrimIntervalMillis"))
      {
        DEFAULT_CACHE_TRIM_INTERVAL_MILLIS = SystemPropertyUtil.getLong("io.netty.allocator.cacheTrimIntervalMillis", 0L);
      }
      else {
        DEFAULT_CACHE_TRIM_INTERVAL_MILLIS = SystemPropertyUtil.getLong("io.netty.allocation.cacheTrimIntervalMillis", 0L);
      }
    }
    else {
      DEFAULT_CACHE_TRIM_INTERVAL_MILLIS = SystemPropertyUtil.getLong("io.netty.allocator.cacheTrimIntervalMillis", 0L);
    }
    

    DEFAULT_USE_CACHE_FOR_ALL_THREADS = SystemPropertyUtil.getBoolean("io.netty.allocator.useCacheForAllThreads", true);
    



    DEFAULT_MAX_CACHED_BYTEBUFFERS_PER_CHUNK = SystemPropertyUtil.getInt("io.netty.allocator.maxCachedByteBuffersPerChunk", 1023);
    

    if (logger.isDebugEnabled()) {
      logger.debug("-Dio.netty.allocator.numHeapArenas: {}", Integer.valueOf(DEFAULT_NUM_HEAP_ARENA));
      logger.debug("-Dio.netty.allocator.numDirectArenas: {}", Integer.valueOf(DEFAULT_NUM_DIRECT_ARENA));
      if (pageSizeFallbackCause == null) {
        logger.debug("-Dio.netty.allocator.pageSize: {}", Integer.valueOf(DEFAULT_PAGE_SIZE));
      } else {
        logger.debug("-Dio.netty.allocator.pageSize: {}", Integer.valueOf(DEFAULT_PAGE_SIZE), pageSizeFallbackCause);
      }
      if (maxOrderFallbackCause == null) {
        logger.debug("-Dio.netty.allocator.maxOrder: {}", Integer.valueOf(DEFAULT_MAX_ORDER));
      } else {
        logger.debug("-Dio.netty.allocator.maxOrder: {}", Integer.valueOf(DEFAULT_MAX_ORDER), maxOrderFallbackCause);
      }
      logger.debug("-Dio.netty.allocator.chunkSize: {}", Integer.valueOf(DEFAULT_PAGE_SIZE << DEFAULT_MAX_ORDER));
      logger.debug("-Dio.netty.allocator.smallCacheSize: {}", Integer.valueOf(DEFAULT_SMALL_CACHE_SIZE));
      logger.debug("-Dio.netty.allocator.normalCacheSize: {}", Integer.valueOf(DEFAULT_NORMAL_CACHE_SIZE));
      logger.debug("-Dio.netty.allocator.maxCachedBufferCapacity: {}", Integer.valueOf(DEFAULT_MAX_CACHED_BUFFER_CAPACITY));
      logger.debug("-Dio.netty.allocator.cacheTrimInterval: {}", Integer.valueOf(DEFAULT_CACHE_TRIM_INTERVAL));
      logger.debug("-Dio.netty.allocator.cacheTrimIntervalMillis: {}", Long.valueOf(DEFAULT_CACHE_TRIM_INTERVAL_MILLIS));
      logger.debug("-Dio.netty.allocator.useCacheForAllThreads: {}", Boolean.valueOf(DEFAULT_USE_CACHE_FOR_ALL_THREADS));
      logger.debug("-Dio.netty.allocator.maxCachedByteBuffersPerChunk: {}", 
        Integer.valueOf(DEFAULT_MAX_CACHED_BYTEBUFFERS_PER_CHUNK));
    }
  }
  
  public static final PooledByteBufAllocator DEFAULT = new PooledByteBufAllocator(
    PlatformDependent.directBufferPreferred());
  private final PoolArena<byte[]>[] heapArenas;
  private final PoolArena<ByteBuffer>[] directArenas;
  private final int smallCacheSize;
  private final int normalCacheSize;
  private final List<PoolArenaMetric> heapArenaMetrics;
  private final List<PoolArenaMetric> directArenaMetrics;
  private final PoolThreadLocalCache threadCache;
  private final int chunkSize;
  private final PooledByteBufAllocatorMetric metric;
  
  public PooledByteBufAllocator()
  {
    this(false);
  }
  
  public PooledByteBufAllocator(boolean preferDirect)
  {
    this(preferDirect, DEFAULT_NUM_HEAP_ARENA, DEFAULT_NUM_DIRECT_ARENA, DEFAULT_PAGE_SIZE, DEFAULT_MAX_ORDER);
  }
  
  public PooledByteBufAllocator(int nHeapArena, int nDirectArena, int pageSize, int maxOrder)
  {
    this(false, nHeapArena, nDirectArena, pageSize, maxOrder);
  }
  



  @Deprecated
  public PooledByteBufAllocator(boolean preferDirect, int nHeapArena, int nDirectArena, int pageSize, int maxOrder)
  {
    this(preferDirect, nHeapArena, nDirectArena, pageSize, maxOrder, 0, DEFAULT_SMALL_CACHE_SIZE, DEFAULT_NORMAL_CACHE_SIZE);
  }
  





  @Deprecated
  public PooledByteBufAllocator(boolean preferDirect, int nHeapArena, int nDirectArena, int pageSize, int maxOrder, int tinyCacheSize, int smallCacheSize, int normalCacheSize)
  {
    this(preferDirect, nHeapArena, nDirectArena, pageSize, maxOrder, smallCacheSize, normalCacheSize, DEFAULT_USE_CACHE_FOR_ALL_THREADS, DEFAULT_DIRECT_MEMORY_CACHE_ALIGNMENT);
  }
  







  @Deprecated
  public PooledByteBufAllocator(boolean preferDirect, int nHeapArena, int nDirectArena, int pageSize, int maxOrder, int tinyCacheSize, int smallCacheSize, int normalCacheSize, boolean useCacheForAllThreads)
  {
    this(preferDirect, nHeapArena, nDirectArena, pageSize, maxOrder, smallCacheSize, normalCacheSize, useCacheForAllThreads);
  }
  




  public PooledByteBufAllocator(boolean preferDirect, int nHeapArena, int nDirectArena, int pageSize, int maxOrder, int smallCacheSize, int normalCacheSize, boolean useCacheForAllThreads)
  {
    this(preferDirect, nHeapArena, nDirectArena, pageSize, maxOrder, smallCacheSize, normalCacheSize, useCacheForAllThreads, DEFAULT_DIRECT_MEMORY_CACHE_ALIGNMENT);
  }
  







  @Deprecated
  public PooledByteBufAllocator(boolean preferDirect, int nHeapArena, int nDirectArena, int pageSize, int maxOrder, int tinyCacheSize, int smallCacheSize, int normalCacheSize, boolean useCacheForAllThreads, int directMemoryCacheAlignment)
  {
    this(preferDirect, nHeapArena, nDirectArena, pageSize, maxOrder, smallCacheSize, normalCacheSize, useCacheForAllThreads, directMemoryCacheAlignment);
  }
  



  public PooledByteBufAllocator(boolean preferDirect, int nHeapArena, int nDirectArena, int pageSize, int maxOrder, int smallCacheSize, int normalCacheSize, boolean useCacheForAllThreads, int directMemoryCacheAlignment)
  {
    super(preferDirect);
    threadCache = new PoolThreadLocalCache(useCacheForAllThreads);
    this.smallCacheSize = smallCacheSize;
    this.normalCacheSize = normalCacheSize;
    
    if (directMemoryCacheAlignment != 0) {
      if (!PlatformDependent.hasAlignDirectByteBuffer()) {
        throw new UnsupportedOperationException("Buffer alignment is not supported. Either Unsafe or ByteBuffer.alignSlice() must be available.");
      }
      


      pageSize = (int)PlatformDependent.align(pageSize, directMemoryCacheAlignment);
    }
    
    chunkSize = validateAndCalculateChunkSize(pageSize, maxOrder);
    
    ObjectUtil.checkPositiveOrZero(nHeapArena, "nHeapArena");
    ObjectUtil.checkPositiveOrZero(nDirectArena, "nDirectArena");
    
    ObjectUtil.checkPositiveOrZero(directMemoryCacheAlignment, "directMemoryCacheAlignment");
    if ((directMemoryCacheAlignment > 0) && (!isDirectMemoryCacheAlignmentSupported())) {
      throw new IllegalArgumentException("directMemoryCacheAlignment is not supported");
    }
    
    if ((directMemoryCacheAlignment & -directMemoryCacheAlignment) != directMemoryCacheAlignment) {
      throw new IllegalArgumentException("directMemoryCacheAlignment: " + directMemoryCacheAlignment + " (expected: power of two)");
    }
    

    int pageShifts = validateAndCalculatePageShifts(pageSize, directMemoryCacheAlignment);
    
    if (nHeapArena > 0) {
      heapArenas = newArenaArray(nHeapArena);
      List<PoolArenaMetric> metrics = new ArrayList(heapArenas.length);
      for (int i = 0; i < heapArenas.length; i++) {
        PoolArena.HeapArena arena = new PoolArena.HeapArena(this, pageSize, pageShifts, chunkSize, directMemoryCacheAlignment);
        

        heapArenas[i] = arena;
        metrics.add(arena);
      }
      heapArenaMetrics = Collections.unmodifiableList(metrics);
    } else {
      heapArenas = null;
      heapArenaMetrics = Collections.emptyList();
    }
    
    if (nDirectArena > 0) {
      directArenas = newArenaArray(nDirectArena);
      List<PoolArenaMetric> metrics = new ArrayList(directArenas.length);
      for (int i = 0; i < directArenas.length; i++) {
        PoolArena.DirectArena arena = new PoolArena.DirectArena(this, pageSize, pageShifts, chunkSize, directMemoryCacheAlignment);
        
        directArenas[i] = arena;
        metrics.add(arena);
      }
      directArenaMetrics = Collections.unmodifiableList(metrics);
    } else {
      directArenas = null;
      directArenaMetrics = Collections.emptyList();
    }
    metric = new PooledByteBufAllocatorMetric(this);
  }
  
  private static <T> PoolArena<T>[] newArenaArray(int size)
  {
    return new PoolArena[size];
  }
  
  private static int validateAndCalculatePageShifts(int pageSize, int alignment) {
    if (pageSize < 4096) {
      throw new IllegalArgumentException("pageSize: " + pageSize + " (expected: " + 4096 + ')');
    }
    
    if ((pageSize & pageSize - 1) != 0) {
      throw new IllegalArgumentException("pageSize: " + pageSize + " (expected: power of 2)");
    }
    
    if (pageSize < alignment) {
      throw new IllegalArgumentException("Alignment cannot be greater than page size. Alignment: " + alignment + ", page size: " + pageSize + '.');
    }
    


    return 31 - Integer.numberOfLeadingZeros(pageSize);
  }
  
  private static int validateAndCalculateChunkSize(int pageSize, int maxOrder) {
    if (maxOrder > 14) {
      throw new IllegalArgumentException("maxOrder: " + maxOrder + " (expected: 0-14)");
    }
    

    int chunkSize = pageSize;
    for (int i = maxOrder; i > 0; i--) {
      if (chunkSize > 536870912) {
        throw new IllegalArgumentException(String.format("pageSize (%d) << maxOrder (%d) must not exceed %d", new Object[] {
          Integer.valueOf(pageSize), Integer.valueOf(maxOrder), Integer.valueOf(1073741824) }));
      }
      chunkSize <<= 1;
    }
    return chunkSize;
  }
  
  protected ByteBuf newHeapBuffer(int initialCapacity, int maxCapacity)
  {
    PoolThreadCache cache = (PoolThreadCache)threadCache.get();
    PoolArena<byte[]> heapArena = heapArena;
    ByteBuf buf;
    ByteBuf buf;
    if (heapArena != null) {
      buf = heapArena.allocate(cache, initialCapacity, maxCapacity);
    } else {
      buf = PlatformDependent.hasUnsafe() ? new UnpooledUnsafeHeapByteBuf(this, initialCapacity, maxCapacity) : new UnpooledHeapByteBuf(this, initialCapacity, maxCapacity);
    }
    


    return toLeakAwareBuffer(buf);
  }
  
  protected ByteBuf newDirectBuffer(int initialCapacity, int maxCapacity)
  {
    PoolThreadCache cache = (PoolThreadCache)threadCache.get();
    PoolArena<ByteBuffer> directArena = directArena;
    ByteBuf buf;
    ByteBuf buf;
    if (directArena != null) {
      buf = directArena.allocate(cache, initialCapacity, maxCapacity);
    }
    else {
      buf = PlatformDependent.hasUnsafe() ? UnsafeByteBufUtil.newUnsafeDirectByteBuf(this, initialCapacity, maxCapacity) : new UnpooledDirectByteBuf(this, initialCapacity, maxCapacity);
    }
    

    return toLeakAwareBuffer(buf);
  }
  


  public static int defaultNumHeapArena()
  {
    return DEFAULT_NUM_HEAP_ARENA;
  }
  


  public static int defaultNumDirectArena()
  {
    return DEFAULT_NUM_DIRECT_ARENA;
  }
  


  public static int defaultPageSize()
  {
    return DEFAULT_PAGE_SIZE;
  }
  


  public static int defaultMaxOrder()
  {
    return DEFAULT_MAX_ORDER;
  }
  


  public static boolean defaultUseCacheForAllThreads()
  {
    return DEFAULT_USE_CACHE_FOR_ALL_THREADS;
  }
  


  public static boolean defaultPreferDirect()
  {
    return PlatformDependent.directBufferPreferred();
  }
  




  @Deprecated
  public static int defaultTinyCacheSize()
  {
    return 0;
  }
  


  public static int defaultSmallCacheSize()
  {
    return DEFAULT_SMALL_CACHE_SIZE;
  }
  


  public static int defaultNormalCacheSize()
  {
    return DEFAULT_NORMAL_CACHE_SIZE;
  }
  


  public static boolean isDirectMemoryCacheAlignmentSupported()
  {
    return PlatformDependent.hasUnsafe();
  }
  
  public boolean isDirectBufferPooled()
  {
    return directArenas != null;
  }
  



  @Deprecated
  public boolean hasThreadLocalCache()
  {
    return threadCache.isSet();
  }
  


  @Deprecated
  public void freeThreadLocalCache()
  {
    threadCache.remove();
  }
  
  final class PoolThreadLocalCache extends FastThreadLocal<PoolThreadCache> {
    private final boolean useCacheForAllThreads;
    
    PoolThreadLocalCache(boolean useCacheForAllThreads) {
      this.useCacheForAllThreads = useCacheForAllThreads;
    }
    
    protected synchronized PoolThreadCache initialValue()
    {
      PoolArena<byte[]> heapArena = leastUsedArena(heapArenas);
      PoolArena<ByteBuffer> directArena = leastUsedArena(directArenas);
      
      Thread current = Thread.currentThread();
      if ((useCacheForAllThreads) || ((current instanceof FastThreadLocalThread)))
      {

        PoolThreadCache cache = new PoolThreadCache(heapArena, directArena, smallCacheSize, normalCacheSize, PooledByteBufAllocator.DEFAULT_MAX_CACHED_BUFFER_CAPACITY, PooledByteBufAllocator.DEFAULT_CACHE_TRIM_INTERVAL);
        
        if (PooledByteBufAllocator.DEFAULT_CACHE_TRIM_INTERVAL_MILLIS > 0L) {
          EventExecutor executor = ThreadExecutorMap.currentExecutor();
          if (executor != null) {
            executor.scheduleAtFixedRate(trimTask, PooledByteBufAllocator.DEFAULT_CACHE_TRIM_INTERVAL_MILLIS, 
              PooledByteBufAllocator.DEFAULT_CACHE_TRIM_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
          }
        }
        return cache;
      }
      
      return new PoolThreadCache(heapArena, directArena, 0, 0, 0, 0);
    }
    
    protected void onRemoval(PoolThreadCache threadCache)
    {
      threadCache.free(false);
    }
    
    private <T> PoolArena<T> leastUsedArena(PoolArena<T>[] arenas) {
      if ((arenas == null) || (arenas.length == 0)) {
        return null;
      }
      
      PoolArena<T> minArena = arenas[0];
      for (int i = 1; i < arenas.length; i++) {
        PoolArena<T> arena = arenas[i];
        if (numThreadCaches.get() < numThreadCaches.get()) {
          minArena = arena;
        }
      }
      
      return minArena;
    }
  }
  
  public PooledByteBufAllocatorMetric metric()
  {
    return metric;
  }
  




  @Deprecated
  public int numHeapArenas()
  {
    return heapArenaMetrics.size();
  }
  




  @Deprecated
  public int numDirectArenas()
  {
    return directArenaMetrics.size();
  }
  




  @Deprecated
  public List<PoolArenaMetric> heapArenas()
  {
    return heapArenaMetrics;
  }
  




  @Deprecated
  public List<PoolArenaMetric> directArenas()
  {
    return directArenaMetrics;
  }
  




  @Deprecated
  public int numThreadLocalCaches()
  {
    PoolArena<?>[] arenas = heapArenas != null ? heapArenas : directArenas;
    if (arenas == null) {
      return 0;
    }
    
    int total = 0;
    for (PoolArena<?> arena : arenas) {
      total += numThreadCaches.get();
    }
    
    return total;
  }
  




  @Deprecated
  public int tinyCacheSize()
  {
    return 0;
  }
  




  @Deprecated
  public int smallCacheSize()
  {
    return smallCacheSize;
  }
  




  @Deprecated
  public int normalCacheSize()
  {
    return normalCacheSize;
  }
  




  @Deprecated
  public final int chunkSize()
  {
    return chunkSize;
  }
  
  final long usedHeapMemory() {
    return usedMemory(heapArenas);
  }
  
  final long usedDirectMemory() {
    return usedMemory(directArenas);
  }
  
  private static long usedMemory(PoolArena<?>[] arenas) {
    if (arenas == null) {
      return -1L;
    }
    long used = 0L;
    for (PoolArena<?> arena : arenas) {
      used += arena.numActiveBytes();
      if (used < 0L) {
        return Long.MAX_VALUE;
      }
    }
    return used;
  }
  
  final PoolThreadCache threadCache() {
    PoolThreadCache cache = (PoolThreadCache)threadCache.get();
    assert (cache != null);
    return cache;
  }
  





  public boolean trimCurrentThreadCache()
  {
    PoolThreadCache cache = (PoolThreadCache)threadCache.getIfExists();
    if (cache != null) {
      cache.trim();
      return true;
    }
    return false;
  }
  



  public String dumpStats()
  {
    int heapArenasLen = heapArenas == null ? 0 : heapArenas.length;
    


    StringBuilder buf = new StringBuilder(512).append(heapArenasLen).append(" heap arena(s):").append(StringUtil.NEWLINE);
    PoolArena<byte[]> localPoolArena1; PoolArena<byte[]> a; if (heapArenasLen > 0) {
      PoolArena[] arrayOfPoolArena1 = heapArenas;int i = arrayOfPoolArena1.length; for (localPoolArena1 = 0; localPoolArena1 < i; localPoolArena1++) { a = arrayOfPoolArena1[localPoolArena1];
        buf.append(a);
      }
    }
    
    int directArenasLen = directArenas == null ? 0 : directArenas.length;
    
    buf.append(directArenasLen)
      .append(" direct arena(s):")
      .append(StringUtil.NEWLINE);
    if (directArenasLen > 0) {
      PoolArena[] arrayOfPoolArena2 = directArenas;localPoolArena1 = arrayOfPoolArena2.length; for (a = 0; a < localPoolArena1; a++) { PoolArena<ByteBuffer> a = arrayOfPoolArena2[a];
        buf.append(a);
      }
    }
    
    return buf.toString();
  }
}
