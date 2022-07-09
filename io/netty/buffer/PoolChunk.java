package io.netty.buffer;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;






















































































































































final class PoolChunk<T>
  implements PoolChunkMetric
{
  private static final int SIZE_BIT_LENGTH = 15;
  private static final int INUSED_BIT_LENGTH = 1;
  private static final int SUBPAGE_BIT_LENGTH = 1;
  private static final int BITMAP_IDX_BIT_LENGTH = 32;
  static final int IS_SUBPAGE_SHIFT = 32;
  static final int IS_USED_SHIFT = 33;
  static final int SIZE_SHIFT = 34;
  static final int RUN_OFFSET_SHIFT = 49;
  final PoolArena<T> arena;
  final Object base;
  final T memory;
  final boolean unpooled;
  private final LongLongHashMap runsAvailMap;
  private final LongPriorityQueue[] runsAvail;
  private final PoolSubpage<T>[] subpages;
  private final int pageSize;
  private final int pageShifts;
  private final int chunkSize;
  private final Deque<ByteBuffer> cachedNioBuffers;
  int freeBytes;
  PoolChunkList<T> parent;
  PoolChunk<T> prev;
  PoolChunk<T> next;
  
  PoolChunk(PoolArena<T> arena, Object base, T memory, int pageSize, int pageShifts, int chunkSize, int maxPageIdx)
  {
    unpooled = false;
    this.arena = arena;
    this.base = base;
    this.memory = memory;
    this.pageSize = pageSize;
    this.pageShifts = pageShifts;
    this.chunkSize = chunkSize;
    freeBytes = chunkSize;
    
    runsAvail = newRunsAvailqueueArray(maxPageIdx);
    runsAvailMap = new LongLongHashMap(-1L);
    subpages = new PoolSubpage[chunkSize >> pageShifts];
    

    int pages = chunkSize >> pageShifts;
    long initHandle = pages << 34;
    insertAvailRun(0, pages, initHandle);
    
    cachedNioBuffers = new ArrayDeque(8);
  }
  
  PoolChunk(PoolArena<T> arena, Object base, T memory, int size)
  {
    unpooled = true;
    this.arena = arena;
    this.base = base;
    this.memory = memory;
    pageSize = 0;
    pageShifts = 0;
    runsAvailMap = null;
    runsAvail = null;
    subpages = null;
    chunkSize = size;
    cachedNioBuffers = null;
  }
  
  private static LongPriorityQueue[] newRunsAvailqueueArray(int size) {
    LongPriorityQueue[] queueArray = new LongPriorityQueue[size];
    for (int i = 0; i < queueArray.length; i++) {
      queueArray[i] = new LongPriorityQueue();
    }
    return queueArray;
  }
  
  private void insertAvailRun(int runOffset, int pages, long handle) {
    int pageIdxFloor = arena.pages2pageIdxFloor(pages);
    LongPriorityQueue queue = runsAvail[pageIdxFloor];
    queue.offer(handle);
    

    insertAvailRun0(runOffset, handle);
    if (pages > 1)
    {
      insertAvailRun0(lastPage(runOffset, pages), handle);
    }
  }
  
  private void insertAvailRun0(int runOffset, long handle) {
    long pre = runsAvailMap.put(runOffset, handle);
    assert (pre == -1L);
  }
  
  private void removeAvailRun(long handle) {
    int pageIdxFloor = arena.pages2pageIdxFloor(runPages(handle));
    LongPriorityQueue queue = runsAvail[pageIdxFloor];
    removeAvailRun(queue, handle);
  }
  
  private void removeAvailRun(LongPriorityQueue queue, long handle) {
    queue.remove(handle);
    
    int runOffset = runOffset(handle);
    int pages = runPages(handle);
    
    runsAvailMap.remove(runOffset);
    if (pages > 1)
    {
      runsAvailMap.remove(lastPage(runOffset, pages));
    }
  }
  
  private static int lastPage(int runOffset, int pages) {
    return runOffset + pages - 1;
  }
  
  private long getAvailRunByOffset(int runOffset) {
    return runsAvailMap.get(runOffset);
  }
  
  public int usage()
  {
    int freeBytes;
    synchronized (arena) {
      freeBytes = this.freeBytes; }
    int freeBytes;
    return usage(freeBytes);
  }
  
  private int usage(int freeBytes) {
    if (freeBytes == 0) {
      return 100;
    }
    
    int freePercentage = (int)(freeBytes * 100L / chunkSize);
    if (freePercentage == 0) {
      return 99;
    }
    return 100 - freePercentage;
  }
  
  boolean allocate(PooledByteBuf<T> buf, int reqCapacity, int sizeIdx, PoolThreadCache cache) {
    long handle;
    if (sizeIdx <= arena.smallMaxSizeIdx)
    {
      long handle = allocateSubpage(sizeIdx);
      if (handle < 0L) {
        return false;
      }
      if ((!$assertionsDisabled) && (!isSubpage(handle))) throw new AssertionError();
    }
    else
    {
      int runSize = arena.sizeIdx2size(sizeIdx);
      handle = allocateRun(runSize);
      if (handle < 0L) {
        return false;
      }
    }
    
    ByteBuffer nioBuffer = cachedNioBuffers != null ? (ByteBuffer)cachedNioBuffers.pollLast() : null;
    initBuf(buf, nioBuffer, handle, reqCapacity, cache);
    return true;
  }
  
  private long allocateRun(int runSize) {
    int pages = runSize >> pageShifts;
    int pageIdx = arena.pages2pageIdx(pages);
    
    synchronized (runsAvail)
    {
      int queueIdx = runFirstBestFit(pageIdx);
      if (queueIdx == -1) {
        return -1L;
      }
      

      LongPriorityQueue queue = runsAvail[queueIdx];
      long handle = queue.poll();
      
      assert ((handle != -1L) && (!isUsed(handle))) : ("invalid handle: " + handle);
      
      removeAvailRun(queue, handle);
      
      if (handle != -1L) {
        handle = splitLargeRun(handle, pages);
      }
      
      freeBytes -= runSize(pageShifts, handle);
      return handle;
    }
  }
  
  private int calculateRunSize(int sizeIdx) {
    int maxElements = 1 << pageShifts - 4;
    int runSize = 0;
    

    int elemSize = arena.sizeIdx2size(sizeIdx);
    int nElements;
    do
    {
      runSize += pageSize;
      nElements = runSize / elemSize;
    } while ((nElements < maxElements) && (runSize != nElements * elemSize));
    
    while (nElements > maxElements) {
      runSize -= pageSize;
      nElements = runSize / elemSize;
    }
    
    assert (nElements > 0);
    assert (runSize <= chunkSize);
    assert (runSize >= elemSize);
    
    return runSize;
  }
  
  private int runFirstBestFit(int pageIdx) {
    if (freeBytes == chunkSize) {
      return arena.nPSizes - 1;
    }
    for (int i = pageIdx; i < arena.nPSizes; i++) {
      LongPriorityQueue queue = runsAvail[i];
      if ((queue != null) && (!queue.isEmpty())) {
        return i;
      }
    }
    return -1;
  }
  
  private long splitLargeRun(long handle, int needPages) {
    assert (needPages > 0);
    
    int totalPages = runPages(handle);
    assert (needPages <= totalPages);
    
    int remPages = totalPages - needPages;
    
    if (remPages > 0) {
      int runOffset = runOffset(handle);
      

      int availOffset = runOffset + needPages;
      long availRun = toRunHandle(availOffset, remPages, 0);
      insertAvailRun(availOffset, remPages, availRun);
      

      return toRunHandle(runOffset, needPages, 1);
    }
    

    handle |= 0x200000000;
    return handle;
  }
  









  private long allocateSubpage(int sizeIdx)
  {
    PoolSubpage<T> head = arena.findSubpagePoolHead(sizeIdx);
    synchronized (head)
    {
      int runSize = calculateRunSize(sizeIdx);
      
      long runHandle = allocateRun(runSize);
      if (runHandle < 0L) {
        return -1L;
      }
      
      int runOffset = runOffset(runHandle);
      assert (subpages[runOffset] == null);
      int elemSize = arena.sizeIdx2size(sizeIdx);
      

      PoolSubpage<T> subpage = new PoolSubpage(head, this, pageShifts, runOffset, runSize(pageShifts, runHandle), elemSize);
      
      subpages[runOffset] = subpage;
      return subpage.allocate();
    }
  }
  






  void free(long handle, int normCapacity, ByteBuffer nioBuffer)
  {
    if (isSubpage(handle)) {
      int sizeIdx = arena.size2SizeIdx(normCapacity);
      PoolSubpage<T> head = arena.findSubpagePoolHead(sizeIdx);
      
      int sIdx = runOffset(handle);
      PoolSubpage<T> subpage = subpages[sIdx];
      assert ((subpage != null) && (doNotDestroy));
      


      synchronized (head) {
        if (subpage.free(head, bitmapIdx(handle)))
        {
          return;
        }
        assert (!doNotDestroy);
        
        subpages[sIdx] = null;
      }
    }
    

    int pages = runPages(handle);
    
    synchronized (runsAvail)
    {

      long finalRun = collapseRuns(handle);
      

      finalRun &= 0xFFFFFFFDFFFFFFFF;
      
      finalRun &= 0xFFFFFFFEFFFFFFFF;
      
      insertAvailRun(runOffset(finalRun), runPages(finalRun), finalRun);
      freeBytes += (pages << pageShifts);
    }
    
    if ((nioBuffer != null) && (cachedNioBuffers != null) && 
      (cachedNioBuffers.size() < PooledByteBufAllocator.DEFAULT_MAX_CACHED_BYTEBUFFERS_PER_CHUNK)) {
      cachedNioBuffers.offer(nioBuffer);
    }
  }
  
  private long collapseRuns(long handle) {
    return collapseNext(collapsePast(handle));
  }
  
  private long collapsePast(long handle) {
    for (;;) {
      int runOffset = runOffset(handle);
      int runPages = runPages(handle);
      
      long pastRun = getAvailRunByOffset(runOffset - 1);
      if (pastRun == -1L) {
        return handle;
      }
      
      int pastOffset = runOffset(pastRun);
      int pastPages = runPages(pastRun);
      

      if ((pastRun != handle) && (pastOffset + pastPages == runOffset))
      {
        removeAvailRun(pastRun);
        handle = toRunHandle(pastOffset, pastPages + runPages, 0);
      } else {
        return handle;
      }
    }
  }
  
  private long collapseNext(long handle) {
    for (;;) {
      int runOffset = runOffset(handle);
      int runPages = runPages(handle);
      
      long nextRun = getAvailRunByOffset(runOffset + runPages);
      if (nextRun == -1L) {
        return handle;
      }
      
      int nextOffset = runOffset(nextRun);
      int nextPages = runPages(nextRun);
      

      if ((nextRun != handle) && (runOffset + runPages == nextOffset))
      {
        removeAvailRun(nextRun);
        handle = toRunHandle(runOffset, runPages + nextPages, 0);
      } else {
        return handle;
      }
    }
  }
  
  private static long toRunHandle(int runOffset, int runPages, int inUsed) {
    return runOffset << 49 | runPages << 34 | inUsed << 33;
  }
  


  void initBuf(PooledByteBuf<T> buf, ByteBuffer nioBuffer, long handle, int reqCapacity, PoolThreadCache threadCache)
  {
    if (isRun(handle)) {
      buf.init(this, nioBuffer, handle, runOffset(handle) << pageShifts, reqCapacity, 
        runSize(pageShifts, handle), arena.parent.threadCache());
    } else {
      initBufWithSubpage(buf, nioBuffer, handle, reqCapacity, threadCache);
    }
  }
  
  void initBufWithSubpage(PooledByteBuf<T> buf, ByteBuffer nioBuffer, long handle, int reqCapacity, PoolThreadCache threadCache)
  {
    int runOffset = runOffset(handle);
    int bitmapIdx = bitmapIdx(handle);
    
    PoolSubpage<T> s = subpages[runOffset];
    assert (doNotDestroy);
    assert (reqCapacity <= elemSize);
    
    int offset = (runOffset << pageShifts) + bitmapIdx * elemSize;
    buf.init(this, nioBuffer, handle, offset, reqCapacity, elemSize, threadCache);
  }
  
  public int chunkSize()
  {
    return chunkSize;
  }
  
  public int freeBytes()
  {
    synchronized (arena) {
      return freeBytes;
    }
  }
  
  public String toString()
  {
    int freeBytes;
    synchronized (arena) {
      freeBytes = this.freeBytes;
    }
    int freeBytes;
    return 
    







      "Chunk(" + Integer.toHexString(System.identityHashCode(this)) + ": " + usage(freeBytes) + "%, " + (chunkSize - freeBytes) + '/' + chunkSize + ')';
  }
  
  void destroy()
  {
    arena.destroyChunk(this);
  }
  
  static int runOffset(long handle) {
    return (int)(handle >> 49);
  }
  
  static int runSize(int pageShifts, long handle) {
    return runPages(handle) << pageShifts;
  }
  
  static int runPages(long handle) {
    return (int)(handle >> 34 & 0x7FFF);
  }
  
  static boolean isUsed(long handle) {
    return (handle >> 33 & 1L) == 1L;
  }
  
  static boolean isRun(long handle) {
    return !isSubpage(handle);
  }
  
  static boolean isSubpage(long handle) {
    return (handle >> 32 & 1L) == 1L;
  }
  
  static int bitmapIdx(long handle) {
    return (int)handle;
  }
}
