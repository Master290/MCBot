package io.netty.buffer;


















final class HeapByteBufUtil
{
  static byte getByte(byte[] memory, int index)
  {
    return memory[index];
  }
  
  static short getShort(byte[] memory, int index) {
    return (short)(memory[index] << 8 | memory[(index + 1)] & 0xFF);
  }
  
  static short getShortLE(byte[] memory, int index) {
    return (short)(memory[index] & 0xFF | memory[(index + 1)] << 8);
  }
  
  static int getUnsignedMedium(byte[] memory, int index) {
    return (memory[index] & 0xFF) << 16 | (memory[(index + 1)] & 0xFF) << 8 | memory[(index + 2)] & 0xFF;
  }
  

  static int getUnsignedMediumLE(byte[] memory, int index)
  {
    return memory[index] & 0xFF | (memory[(index + 1)] & 0xFF) << 8 | (memory[(index + 2)] & 0xFF) << 16;
  }
  

  static int getInt(byte[] memory, int index)
  {
    return (memory[index] & 0xFF) << 24 | (memory[(index + 1)] & 0xFF) << 16 | (memory[(index + 2)] & 0xFF) << 8 | memory[(index + 3)] & 0xFF;
  }
  


  static int getIntLE(byte[] memory, int index)
  {
    return memory[index] & 0xFF | (memory[(index + 1)] & 0xFF) << 8 | (memory[(index + 2)] & 0xFF) << 16 | (memory[(index + 3)] & 0xFF) << 24;
  }
  


  static long getLong(byte[] memory, int index)
  {
    return (memory[index] & 0xFF) << 56 | (memory[(index + 1)] & 0xFF) << 48 | (memory[(index + 2)] & 0xFF) << 40 | (memory[(index + 3)] & 0xFF) << 32 | (memory[(index + 4)] & 0xFF) << 24 | (memory[(index + 5)] & 0xFF) << 16 | (memory[(index + 6)] & 0xFF) << 8 | memory[(index + 7)] & 0xFF;
  }
  






  static long getLongLE(byte[] memory, int index)
  {
    return memory[index] & 0xFF | (memory[(index + 1)] & 0xFF) << 8 | (memory[(index + 2)] & 0xFF) << 16 | (memory[(index + 3)] & 0xFF) << 24 | (memory[(index + 4)] & 0xFF) << 32 | (memory[(index + 5)] & 0xFF) << 40 | (memory[(index + 6)] & 0xFF) << 48 | (memory[(index + 7)] & 0xFF) << 56;
  }
  






  static void setByte(byte[] memory, int index, int value)
  {
    memory[index] = ((byte)value);
  }
  
  static void setShort(byte[] memory, int index, int value) {
    memory[index] = ((byte)(value >>> 8));
    memory[(index + 1)] = ((byte)value);
  }
  
  static void setShortLE(byte[] memory, int index, int value) {
    memory[index] = ((byte)value);
    memory[(index + 1)] = ((byte)(value >>> 8));
  }
  
  static void setMedium(byte[] memory, int index, int value) {
    memory[index] = ((byte)(value >>> 16));
    memory[(index + 1)] = ((byte)(value >>> 8));
    memory[(index + 2)] = ((byte)value);
  }
  
  static void setMediumLE(byte[] memory, int index, int value) {
    memory[index] = ((byte)value);
    memory[(index + 1)] = ((byte)(value >>> 8));
    memory[(index + 2)] = ((byte)(value >>> 16));
  }
  
  static void setInt(byte[] memory, int index, int value) {
    memory[index] = ((byte)(value >>> 24));
    memory[(index + 1)] = ((byte)(value >>> 16));
    memory[(index + 2)] = ((byte)(value >>> 8));
    memory[(index + 3)] = ((byte)value);
  }
  
  static void setIntLE(byte[] memory, int index, int value) {
    memory[index] = ((byte)value);
    memory[(index + 1)] = ((byte)(value >>> 8));
    memory[(index + 2)] = ((byte)(value >>> 16));
    memory[(index + 3)] = ((byte)(value >>> 24));
  }
  
  static void setLong(byte[] memory, int index, long value) {
    memory[index] = ((byte)(int)(value >>> 56));
    memory[(index + 1)] = ((byte)(int)(value >>> 48));
    memory[(index + 2)] = ((byte)(int)(value >>> 40));
    memory[(index + 3)] = ((byte)(int)(value >>> 32));
    memory[(index + 4)] = ((byte)(int)(value >>> 24));
    memory[(index + 5)] = ((byte)(int)(value >>> 16));
    memory[(index + 6)] = ((byte)(int)(value >>> 8));
    memory[(index + 7)] = ((byte)(int)value);
  }
  
  static void setLongLE(byte[] memory, int index, long value) {
    memory[index] = ((byte)(int)value);
    memory[(index + 1)] = ((byte)(int)(value >>> 8));
    memory[(index + 2)] = ((byte)(int)(value >>> 16));
    memory[(index + 3)] = ((byte)(int)(value >>> 24));
    memory[(index + 4)] = ((byte)(int)(value >>> 32));
    memory[(index + 5)] = ((byte)(int)(value >>> 40));
    memory[(index + 6)] = ((byte)(int)(value >>> 48));
    memory[(index + 7)] = ((byte)(int)(value >>> 56));
  }
  
  private HeapByteBufUtil() {}
}
