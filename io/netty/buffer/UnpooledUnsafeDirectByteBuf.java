package io.netty.buffer;

import io.netty.util.internal.PlatformDependent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;


























public class UnpooledUnsafeDirectByteBuf
  extends UnpooledDirectByteBuf
{
  long memoryAddress;
  
  public UnpooledUnsafeDirectByteBuf(ByteBufAllocator alloc, int initialCapacity, int maxCapacity)
  {
    super(alloc, initialCapacity, maxCapacity);
  }
  













  protected UnpooledUnsafeDirectByteBuf(ByteBufAllocator alloc, ByteBuffer initialBuffer, int maxCapacity)
  {
    super(alloc, initialBuffer, maxCapacity, false, true);
  }
  
  UnpooledUnsafeDirectByteBuf(ByteBufAllocator alloc, ByteBuffer initialBuffer, int maxCapacity, boolean doFree) {
    super(alloc, initialBuffer, maxCapacity, doFree, false);
  }
  
  final void setByteBuffer(ByteBuffer buffer, boolean tryFree)
  {
    super.setByteBuffer(buffer, tryFree);
    memoryAddress = PlatformDependent.directBufferAddress(buffer);
  }
  
  public boolean hasMemoryAddress()
  {
    return true;
  }
  
  public long memoryAddress()
  {
    ensureAccessible();
    return memoryAddress;
  }
  
  public byte getByte(int index)
  {
    checkIndex(index);
    return _getByte(index);
  }
  
  protected byte _getByte(int index)
  {
    return UnsafeByteBufUtil.getByte(addr(index));
  }
  
  public short getShort(int index)
  {
    checkIndex(index, 2);
    return _getShort(index);
  }
  
  protected short _getShort(int index)
  {
    return UnsafeByteBufUtil.getShort(addr(index));
  }
  
  protected short _getShortLE(int index)
  {
    return UnsafeByteBufUtil.getShortLE(addr(index));
  }
  
  public int getUnsignedMedium(int index)
  {
    checkIndex(index, 3);
    return _getUnsignedMedium(index);
  }
  
  protected int _getUnsignedMedium(int index)
  {
    return UnsafeByteBufUtil.getUnsignedMedium(addr(index));
  }
  
  protected int _getUnsignedMediumLE(int index)
  {
    return UnsafeByteBufUtil.getUnsignedMediumLE(addr(index));
  }
  
  public int getInt(int index)
  {
    checkIndex(index, 4);
    return _getInt(index);
  }
  
  protected int _getInt(int index)
  {
    return UnsafeByteBufUtil.getInt(addr(index));
  }
  
  protected int _getIntLE(int index)
  {
    return UnsafeByteBufUtil.getIntLE(addr(index));
  }
  
  public long getLong(int index)
  {
    checkIndex(index, 8);
    return _getLong(index);
  }
  
  protected long _getLong(int index)
  {
    return UnsafeByteBufUtil.getLong(addr(index));
  }
  
  protected long _getLongLE(int index)
  {
    return UnsafeByteBufUtil.getLongLE(addr(index));
  }
  
  public ByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length)
  {
    UnsafeByteBufUtil.getBytes(this, addr(index), index, dst, dstIndex, length);
    return this;
  }
  
  void getBytes(int index, byte[] dst, int dstIndex, int length, boolean internal)
  {
    UnsafeByteBufUtil.getBytes(this, addr(index), index, dst, dstIndex, length);
  }
  
  void getBytes(int index, ByteBuffer dst, boolean internal)
  {
    UnsafeByteBufUtil.getBytes(this, addr(index), index, dst);
  }
  
  public ByteBuf setByte(int index, int value)
  {
    checkIndex(index);
    _setByte(index, value);
    return this;
  }
  
  protected void _setByte(int index, int value)
  {
    UnsafeByteBufUtil.setByte(addr(index), value);
  }
  
  public ByteBuf setShort(int index, int value)
  {
    checkIndex(index, 2);
    _setShort(index, value);
    return this;
  }
  
  protected void _setShort(int index, int value)
  {
    UnsafeByteBufUtil.setShort(addr(index), value);
  }
  
  protected void _setShortLE(int index, int value)
  {
    UnsafeByteBufUtil.setShortLE(addr(index), value);
  }
  
  public ByteBuf setMedium(int index, int value)
  {
    checkIndex(index, 3);
    _setMedium(index, value);
    return this;
  }
  
  protected void _setMedium(int index, int value)
  {
    UnsafeByteBufUtil.setMedium(addr(index), value);
  }
  
  protected void _setMediumLE(int index, int value)
  {
    UnsafeByteBufUtil.setMediumLE(addr(index), value);
  }
  
  public ByteBuf setInt(int index, int value)
  {
    checkIndex(index, 4);
    _setInt(index, value);
    return this;
  }
  
  protected void _setInt(int index, int value)
  {
    UnsafeByteBufUtil.setInt(addr(index), value);
  }
  
  protected void _setIntLE(int index, int value)
  {
    UnsafeByteBufUtil.setIntLE(addr(index), value);
  }
  
  public ByteBuf setLong(int index, long value)
  {
    checkIndex(index, 8);
    _setLong(index, value);
    return this;
  }
  
  protected void _setLong(int index, long value)
  {
    UnsafeByteBufUtil.setLong(addr(index), value);
  }
  
  protected void _setLongLE(int index, long value)
  {
    UnsafeByteBufUtil.setLongLE(addr(index), value);
  }
  
  public ByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length)
  {
    UnsafeByteBufUtil.setBytes(this, addr(index), index, src, srcIndex, length);
    return this;
  }
  
  public ByteBuf setBytes(int index, byte[] src, int srcIndex, int length)
  {
    UnsafeByteBufUtil.setBytes(this, addr(index), index, src, srcIndex, length);
    return this;
  }
  
  public ByteBuf setBytes(int index, ByteBuffer src)
  {
    UnsafeByteBufUtil.setBytes(this, addr(index), index, src);
    return this;
  }
  
  void getBytes(int index, OutputStream out, int length, boolean internal) throws IOException
  {
    UnsafeByteBufUtil.getBytes(this, addr(index), index, out, length);
  }
  
  public int setBytes(int index, InputStream in, int length) throws IOException
  {
    return UnsafeByteBufUtil.setBytes(this, addr(index), index, in, length);
  }
  
  public ByteBuf copy(int index, int length)
  {
    return UnsafeByteBufUtil.copy(this, addr(index), index, length);
  }
  
  final long addr(int index) {
    return memoryAddress + index;
  }
  
  protected SwappedByteBuf newSwappedByteBuf()
  {
    if (PlatformDependent.isUnaligned())
    {
      return new UnsafeDirectSwappedByteBuf(this);
    }
    return super.newSwappedByteBuf();
  }
  
  public ByteBuf setZero(int index, int length)
  {
    checkIndex(index, length);
    UnsafeByteBufUtil.setZero(addr(index), length);
    return this;
  }
  
  public ByteBuf writeZero(int length)
  {
    ensureWritable(length);
    int wIndex = writerIndex;
    UnsafeByteBufUtil.setZero(addr(wIndex), length);
    writerIndex = (wIndex + length);
    return this;
  }
}
