package io.netty.channel.unix;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;













public final class PreferredDirectByteBufAllocator
  implements ByteBufAllocator
{
  private ByteBufAllocator allocator;
  
  public PreferredDirectByteBufAllocator() {}
  
  public void updateAllocator(ByteBufAllocator allocator)
  {
    this.allocator = allocator;
  }
  
  public ByteBuf buffer()
  {
    return allocator.directBuffer();
  }
  
  public ByteBuf buffer(int initialCapacity)
  {
    return allocator.directBuffer(initialCapacity);
  }
  
  public ByteBuf buffer(int initialCapacity, int maxCapacity)
  {
    return allocator.directBuffer(initialCapacity, maxCapacity);
  }
  
  public ByteBuf ioBuffer()
  {
    return allocator.directBuffer();
  }
  
  public ByteBuf ioBuffer(int initialCapacity)
  {
    return allocator.directBuffer(initialCapacity);
  }
  
  public ByteBuf ioBuffer(int initialCapacity, int maxCapacity)
  {
    return allocator.directBuffer(initialCapacity, maxCapacity);
  }
  
  public ByteBuf heapBuffer()
  {
    return allocator.heapBuffer();
  }
  
  public ByteBuf heapBuffer(int initialCapacity)
  {
    return allocator.heapBuffer(initialCapacity);
  }
  
  public ByteBuf heapBuffer(int initialCapacity, int maxCapacity)
  {
    return allocator.heapBuffer(initialCapacity, maxCapacity);
  }
  
  public ByteBuf directBuffer()
  {
    return allocator.directBuffer();
  }
  
  public ByteBuf directBuffer(int initialCapacity)
  {
    return allocator.directBuffer(initialCapacity);
  }
  
  public ByteBuf directBuffer(int initialCapacity, int maxCapacity)
  {
    return allocator.directBuffer(initialCapacity, maxCapacity);
  }
  
  public CompositeByteBuf compositeBuffer()
  {
    return allocator.compositeDirectBuffer();
  }
  
  public CompositeByteBuf compositeBuffer(int maxNumComponents)
  {
    return allocator.compositeDirectBuffer(maxNumComponents);
  }
  
  public CompositeByteBuf compositeHeapBuffer()
  {
    return allocator.compositeHeapBuffer();
  }
  
  public CompositeByteBuf compositeHeapBuffer(int maxNumComponents)
  {
    return allocator.compositeHeapBuffer(maxNumComponents);
  }
  
  public CompositeByteBuf compositeDirectBuffer()
  {
    return allocator.compositeDirectBuffer();
  }
  
  public CompositeByteBuf compositeDirectBuffer(int maxNumComponents)
  {
    return allocator.compositeDirectBuffer(maxNumComponents);
  }
  
  public boolean isDirectBufferPooled()
  {
    return allocator.isDirectBufferPooled();
  }
  
  public int calculateNewCapacity(int minNewCapacity, int maxCapacity)
  {
    return allocator.calculateNewCapacity(minNewCapacity, maxCapacity);
  }
}
