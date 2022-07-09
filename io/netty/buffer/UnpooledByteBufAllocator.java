package io.netty.buffer;

import io.netty.util.internal.LongCounter;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.StringUtil;
import java.nio.ByteBuffer;


















public final class UnpooledByteBufAllocator
  extends AbstractByteBufAllocator
  implements ByteBufAllocatorMetricProvider
{
  private final UnpooledByteBufAllocatorMetric metric = new UnpooledByteBufAllocatorMetric(null);
  

  private final boolean disableLeakDetector;
  
  private final boolean noCleaner;
  
  public static final UnpooledByteBufAllocator DEFAULT = new UnpooledByteBufAllocator(
    PlatformDependent.directBufferPreferred());
  





  public UnpooledByteBufAllocator(boolean preferDirect)
  {
    this(preferDirect, false);
  }
  








  public UnpooledByteBufAllocator(boolean preferDirect, boolean disableLeakDetector)
  {
    this(preferDirect, disableLeakDetector, PlatformDependent.useDirectBufferNoCleaner());
  }
  










  public UnpooledByteBufAllocator(boolean preferDirect, boolean disableLeakDetector, boolean tryNoCleaner)
  {
    super(preferDirect);
    this.disableLeakDetector = disableLeakDetector;
    
    noCleaner = ((tryNoCleaner) && (PlatformDependent.hasUnsafe()) && (PlatformDependent.hasDirectBufferNoCleanerConstructor()));
  }
  
  protected ByteBuf newHeapBuffer(int initialCapacity, int maxCapacity)
  {
    return PlatformDependent.hasUnsafe() ? new InstrumentedUnpooledUnsafeHeapByteBuf(this, initialCapacity, maxCapacity) : new InstrumentedUnpooledHeapByteBuf(this, initialCapacity, maxCapacity);
  }
  

  protected ByteBuf newDirectBuffer(int initialCapacity, int maxCapacity)
  {
    ByteBuf buf;
    ByteBuf buf;
    if (PlatformDependent.hasUnsafe()) {
      buf = noCleaner ? new InstrumentedUnpooledUnsafeNoCleanerDirectByteBuf(this, initialCapacity, maxCapacity) : new InstrumentedUnpooledUnsafeDirectByteBuf(this, initialCapacity, maxCapacity);
    }
    else {
      buf = new InstrumentedUnpooledDirectByteBuf(this, initialCapacity, maxCapacity);
    }
    return disableLeakDetector ? buf : toLeakAwareBuffer(buf);
  }
  
  public CompositeByteBuf compositeHeapBuffer(int maxNumComponents)
  {
    CompositeByteBuf buf = new CompositeByteBuf(this, false, maxNumComponents);
    return disableLeakDetector ? buf : toLeakAwareBuffer(buf);
  }
  
  public CompositeByteBuf compositeDirectBuffer(int maxNumComponents)
  {
    CompositeByteBuf buf = new CompositeByteBuf(this, true, maxNumComponents);
    return disableLeakDetector ? buf : toLeakAwareBuffer(buf);
  }
  
  public boolean isDirectBufferPooled()
  {
    return false;
  }
  
  public ByteBufAllocatorMetric metric()
  {
    return metric;
  }
  
  void incrementDirect(int amount) {
    metric.directCounter.add(amount);
  }
  
  void decrementDirect(int amount) {
    metric.directCounter.add(-amount);
  }
  
  void incrementHeap(int amount) {
    metric.heapCounter.add(amount);
  }
  
  void decrementHeap(int amount) {
    metric.heapCounter.add(-amount);
  }
  
  private static final class InstrumentedUnpooledUnsafeHeapByteBuf extends UnpooledUnsafeHeapByteBuf {
    InstrumentedUnpooledUnsafeHeapByteBuf(UnpooledByteBufAllocator alloc, int initialCapacity, int maxCapacity) {
      super(initialCapacity, maxCapacity);
    }
    
    protected byte[] allocateArray(int initialCapacity)
    {
      byte[] bytes = super.allocateArray(initialCapacity);
      ((UnpooledByteBufAllocator)alloc()).incrementHeap(bytes.length);
      return bytes;
    }
    
    protected void freeArray(byte[] array)
    {
      int length = array.length;
      super.freeArray(array);
      ((UnpooledByteBufAllocator)alloc()).decrementHeap(length);
    }
  }
  
  private static final class InstrumentedUnpooledHeapByteBuf extends UnpooledHeapByteBuf {
    InstrumentedUnpooledHeapByteBuf(UnpooledByteBufAllocator alloc, int initialCapacity, int maxCapacity) {
      super(initialCapacity, maxCapacity);
    }
    
    protected byte[] allocateArray(int initialCapacity)
    {
      byte[] bytes = super.allocateArray(initialCapacity);
      ((UnpooledByteBufAllocator)alloc()).incrementHeap(bytes.length);
      return bytes;
    }
    
    protected void freeArray(byte[] array)
    {
      int length = array.length;
      super.freeArray(array);
      ((UnpooledByteBufAllocator)alloc()).decrementHeap(length);
    }
  }
  
  private static final class InstrumentedUnpooledUnsafeNoCleanerDirectByteBuf extends UnpooledUnsafeNoCleanerDirectByteBuf
  {
    InstrumentedUnpooledUnsafeNoCleanerDirectByteBuf(UnpooledByteBufAllocator alloc, int initialCapacity, int maxCapacity)
    {
      super(initialCapacity, maxCapacity);
    }
    
    protected ByteBuffer allocateDirect(int initialCapacity)
    {
      ByteBuffer buffer = super.allocateDirect(initialCapacity);
      ((UnpooledByteBufAllocator)alloc()).incrementDirect(buffer.capacity());
      return buffer;
    }
    
    ByteBuffer reallocateDirect(ByteBuffer oldBuffer, int initialCapacity)
    {
      int capacity = oldBuffer.capacity();
      ByteBuffer buffer = super.reallocateDirect(oldBuffer, initialCapacity);
      ((UnpooledByteBufAllocator)alloc()).incrementDirect(buffer.capacity() - capacity);
      return buffer;
    }
    
    protected void freeDirect(ByteBuffer buffer)
    {
      int capacity = buffer.capacity();
      super.freeDirect(buffer);
      ((UnpooledByteBufAllocator)alloc()).decrementDirect(capacity);
    }
  }
  
  private static final class InstrumentedUnpooledUnsafeDirectByteBuf extends UnpooledUnsafeDirectByteBuf
  {
    InstrumentedUnpooledUnsafeDirectByteBuf(UnpooledByteBufAllocator alloc, int initialCapacity, int maxCapacity) {
      super(initialCapacity, maxCapacity);
    }
    
    protected ByteBuffer allocateDirect(int initialCapacity)
    {
      ByteBuffer buffer = super.allocateDirect(initialCapacity);
      ((UnpooledByteBufAllocator)alloc()).incrementDirect(buffer.capacity());
      return buffer;
    }
    
    protected void freeDirect(ByteBuffer buffer)
    {
      int capacity = buffer.capacity();
      super.freeDirect(buffer);
      ((UnpooledByteBufAllocator)alloc()).decrementDirect(capacity);
    }
  }
  
  private static final class InstrumentedUnpooledDirectByteBuf extends UnpooledDirectByteBuf
  {
    InstrumentedUnpooledDirectByteBuf(UnpooledByteBufAllocator alloc, int initialCapacity, int maxCapacity) {
      super(initialCapacity, maxCapacity);
    }
    
    protected ByteBuffer allocateDirect(int initialCapacity)
    {
      ByteBuffer buffer = super.allocateDirect(initialCapacity);
      ((UnpooledByteBufAllocator)alloc()).incrementDirect(buffer.capacity());
      return buffer;
    }
    
    protected void freeDirect(ByteBuffer buffer)
    {
      int capacity = buffer.capacity();
      super.freeDirect(buffer);
      ((UnpooledByteBufAllocator)alloc()).decrementDirect(capacity);
    }
  }
  
  private static final class UnpooledByteBufAllocatorMetric implements ByteBufAllocatorMetric {
    final LongCounter directCounter = PlatformDependent.newLongCounter();
    final LongCounter heapCounter = PlatformDependent.newLongCounter();
    
    private UnpooledByteBufAllocatorMetric() {}
    
    public long usedHeapMemory() { return heapCounter.value(); }
    

    public long usedDirectMemory()
    {
      return directCounter.value();
    }
    
    public String toString()
    {
      return 
        StringUtil.simpleClassName(this) + "(usedHeapMemory: " + usedHeapMemory() + "; usedDirectMemory: " + usedDirectMemory() + ')';
    }
  }
}
