package io.netty.channel.epoll;

import io.netty.channel.unix.Buffer;
import io.netty.util.internal.PlatformDependent;
import java.nio.ByteBuffer;



































final class EpollEventArray
{
  private static final int EPOLL_EVENT_SIZE = ;
  
  private static final int EPOLL_DATA_OFFSET = Native.offsetofEpollData();
  private ByteBuffer memory;
  private long memoryAddress;
  private int length;
  
  EpollEventArray(int length)
  {
    if (length < 1) {
      throw new IllegalArgumentException("length must be >= 1 but was " + length);
    }
    this.length = length;
    memory = Buffer.allocateDirectWithNativeOrder(calculateBufferCapacity(length));
    memoryAddress = Buffer.memoryAddress(memory);
  }
  


  long memoryAddress()
  {
    return memoryAddress;
  }
  



  int length()
  {
    return length;
  }
  



  void increase()
  {
    length <<= 1;
    
    ByteBuffer buffer = Buffer.allocateDirectWithNativeOrder(calculateBufferCapacity(length));
    Buffer.free(memory);
    memory = buffer;
    memoryAddress = Buffer.memoryAddress(buffer);
  }
  


  void free()
  {
    Buffer.free(memory);
    memoryAddress = 0L;
  }
  


  int events(int index)
  {
    return getInt(index, 0);
  }
  


  int fd(int index)
  {
    return getInt(index, EPOLL_DATA_OFFSET);
  }
  
  private int getInt(int index, int offset) {
    if (PlatformDependent.hasUnsafe()) {
      long n = index * EPOLL_EVENT_SIZE;
      return PlatformDependent.getInt(memoryAddress + n + offset);
    }
    return memory.getInt(index * EPOLL_EVENT_SIZE + offset);
  }
  
  private static int calculateBufferCapacity(int capacity) {
    return capacity * EPOLL_EVENT_SIZE;
  }
}
