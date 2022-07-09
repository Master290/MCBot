package io.netty.channel.kqueue;

import io.netty.util.internal.PlatformDependent;
import java.nio.ByteBuffer;





























final class KQueueEventArray
{
  private static final int KQUEUE_EVENT_SIZE = ;
  private static final int KQUEUE_IDENT_OFFSET = Native.offsetofKEventIdent();
  private static final int KQUEUE_FILTER_OFFSET = Native.offsetofKEventFilter();
  private static final int KQUEUE_FFLAGS_OFFSET = Native.offsetofKEventFFlags();
  private static final int KQUEUE_FLAGS_OFFSET = Native.offsetofKEventFlags();
  private static final int KQUEUE_DATA_OFFSET = Native.offsetofKeventData();
  private ByteBuffer memory;
  private long memoryAddress;
  private int size;
  private int capacity;
  
  KQueueEventArray(int capacity)
  {
    if (capacity < 1) {
      throw new IllegalArgumentException("capacity must be >= 1 but was " + capacity);
    }
    memory = io.netty.channel.unix.Buffer.allocateDirectWithNativeOrder(calculateBufferCapacity(capacity));
    memoryAddress = io.netty.channel.unix.Buffer.memoryAddress(memory);
    this.capacity = capacity;
  }
  


  long memoryAddress()
  {
    return memoryAddress;
  }
  



  int capacity()
  {
    return capacity;
  }
  
  int size() {
    return size;
  }
  
  void clear() {
    size = 0;
  }
  
  void evSet(AbstractKQueueChannel ch, short filter, short flags, int fflags) {
    reallocIfNeeded();
    evSet(getKEventOffset(size++) + memoryAddress, socket.intValue(), filter, flags, fflags);
  }
  
  private void reallocIfNeeded() {
    if (size == capacity) {
      realloc(true);
    }
  }
  



  void realloc(boolean throwIfFail)
  {
    int newLength = capacity <= 65536 ? capacity << 1 : capacity + capacity >> 1;
    try
    {
      ByteBuffer buffer = io.netty.channel.unix.Buffer.allocateDirectWithNativeOrder(calculateBufferCapacity(newLength));
      

      memory.position(0).limit(size);
      buffer.put(memory);
      buffer.position(0);
      
      io.netty.channel.unix.Buffer.free(memory);
      memory = buffer;
      memoryAddress = io.netty.channel.unix.Buffer.memoryAddress(buffer);
    } catch (OutOfMemoryError e) {
      if (throwIfFail) {
        OutOfMemoryError error = new OutOfMemoryError("unable to allocate " + newLength + " new bytes! Existing capacity is: " + capacity);
        
        error.initCause(e);
        throw error;
      }
    }
  }
  


  void free()
  {
    io.netty.channel.unix.Buffer.free(memory);
    memoryAddress = (this.size = this.capacity = 0);
  }
  
  private static int getKEventOffset(int index) {
    return index * KQUEUE_EVENT_SIZE;
  }
  
  private long getKEventOffsetAddress(int index) {
    return getKEventOffset(index) + memoryAddress;
  }
  
  private short getShort(int index, int offset) {
    if (PlatformDependent.hasUnsafe()) {
      return PlatformDependent.getShort(getKEventOffsetAddress(index) + offset);
    }
    return memory.getShort(getKEventOffset(index) + offset);
  }
  
  short flags(int index) {
    return getShort(index, KQUEUE_FLAGS_OFFSET);
  }
  
  short filter(int index) {
    return getShort(index, KQUEUE_FILTER_OFFSET);
  }
  
  short fflags(int index) {
    return getShort(index, KQUEUE_FFLAGS_OFFSET);
  }
  
  int fd(int index) {
    if (PlatformDependent.hasUnsafe()) {
      return PlatformDependent.getInt(getKEventOffsetAddress(index) + KQUEUE_IDENT_OFFSET);
    }
    return memory.getInt(getKEventOffset(index) + KQUEUE_IDENT_OFFSET);
  }
  
  long data(int index) {
    if (PlatformDependent.hasUnsafe()) {
      return PlatformDependent.getLong(getKEventOffsetAddress(index) + KQUEUE_DATA_OFFSET);
    }
    return memory.getLong(getKEventOffset(index) + KQUEUE_DATA_OFFSET);
  }
  
  private static int calculateBufferCapacity(int capacity) {
    return capacity * KQUEUE_EVENT_SIZE;
  }
  
  private static native void evSet(long paramLong, int paramInt1, short paramShort1, short paramShort2, int paramInt2);
}
