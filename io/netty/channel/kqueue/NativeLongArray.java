package io.netty.channel.kqueue;

import io.netty.channel.unix.Limits;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import java.nio.ByteBuffer;

















final class NativeLongArray
{
  private ByteBuffer memory;
  private long memoryAddress;
  private int capacity;
  private int size;
  
  NativeLongArray(int capacity)
  {
    this.capacity = ObjectUtil.checkPositive(capacity, "capacity");
    memory = io.netty.channel.unix.Buffer.allocateDirectWithNativeOrder(calculateBufferCapacity(capacity));
    memoryAddress = io.netty.channel.unix.Buffer.memoryAddress(memory);
  }
  
  private static int idx(int index) {
    return index * Limits.SIZEOF_JLONG;
  }
  
  private static int calculateBufferCapacity(int capacity) {
    return capacity * Limits.SIZEOF_JLONG;
  }
  
  void add(long value) {
    reallocIfNeeded();
    if (PlatformDependent.hasUnsafe()) {
      PlatformDependent.putLong(memoryOffset(size), value);
    } else {
      memory.putLong(idx(size), value);
    }
    size += 1;
  }
  
  void clear() {
    size = 0;
  }
  
  boolean isEmpty() {
    return size == 0;
  }
  
  int size() {
    return size;
  }
  
  void free() {
    io.netty.channel.unix.Buffer.free(memory);
    memoryAddress = 0L;
  }
  
  long memoryAddress() {
    return memoryAddress;
  }
  
  long memoryAddressEnd() {
    return memoryOffset(size);
  }
  
  private long memoryOffset(int index) {
    return memoryAddress + idx(index);
  }
  
  private void reallocIfNeeded() {
    if (size == capacity)
    {
      int newLength = capacity <= 65536 ? capacity << 1 : capacity + capacity >> 1;
      ByteBuffer buffer = io.netty.channel.unix.Buffer.allocateDirectWithNativeOrder(calculateBufferCapacity(newLength));
      

      memory.position(0).limit(size);
      buffer.put(memory);
      buffer.position(0);
      
      io.netty.channel.unix.Buffer.free(memory);
      memory = buffer;
      memoryAddress = io.netty.channel.unix.Buffer.memoryAddress(buffer);
      capacity = newLength;
    }
  }
  
  public String toString()
  {
    return "memoryAddress: " + memoryAddress + " capacity: " + capacity + " size: " + size;
  }
}
