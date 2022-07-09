package io.netty.buffer;

import io.netty.util.internal.PlatformDependent;
import java.nio.ByteBuffer;















class UnpooledUnsafeNoCleanerDirectByteBuf
  extends UnpooledUnsafeDirectByteBuf
{
  UnpooledUnsafeNoCleanerDirectByteBuf(ByteBufAllocator alloc, int initialCapacity, int maxCapacity)
  {
    super(alloc, initialCapacity, maxCapacity);
  }
  
  protected ByteBuffer allocateDirect(int initialCapacity)
  {
    return PlatformDependent.allocateDirectNoCleaner(initialCapacity);
  }
  
  ByteBuffer reallocateDirect(ByteBuffer oldBuffer, int initialCapacity) {
    return PlatformDependent.reallocateDirectNoCleaner(oldBuffer, initialCapacity);
  }
  
  protected void freeDirect(ByteBuffer buffer)
  {
    PlatformDependent.freeDirectNoCleaner(buffer);
  }
  
  public ByteBuf capacity(int newCapacity)
  {
    checkNewCapacity(newCapacity);
    
    int oldCapacity = capacity();
    if (newCapacity == oldCapacity) {
      return this;
    }
    
    trimIndicesToCapacity(newCapacity);
    setByteBuffer(reallocateDirect(buffer, newCapacity), false);
    return this;
  }
}
