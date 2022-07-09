package io.netty.buffer;

import io.netty.util.internal.EmptyArrays;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;


























public class UnpooledHeapByteBuf
  extends AbstractReferenceCountedByteBuf
{
  private final ByteBufAllocator alloc;
  byte[] array;
  private ByteBuffer tmpNioBuf;
  
  public UnpooledHeapByteBuf(ByteBufAllocator alloc, int initialCapacity, int maxCapacity)
  {
    super(maxCapacity);
    
    if (initialCapacity > maxCapacity) {
      throw new IllegalArgumentException(String.format("initialCapacity(%d) > maxCapacity(%d)", new Object[] {
        Integer.valueOf(initialCapacity), Integer.valueOf(maxCapacity) }));
    }
    
    this.alloc = ((ByteBufAllocator)ObjectUtil.checkNotNull(alloc, "alloc"));
    setArray(allocateArray(initialCapacity));
    setIndex(0, 0);
  }
  





  protected UnpooledHeapByteBuf(ByteBufAllocator alloc, byte[] initialArray, int maxCapacity)
  {
    super(maxCapacity);
    
    ObjectUtil.checkNotNull(alloc, "alloc");
    ObjectUtil.checkNotNull(initialArray, "initialArray");
    if (initialArray.length > maxCapacity) {
      throw new IllegalArgumentException(String.format("initialCapacity(%d) > maxCapacity(%d)", new Object[] {
        Integer.valueOf(initialArray.length), Integer.valueOf(maxCapacity) }));
    }
    
    this.alloc = alloc;
    setArray(initialArray);
    setIndex(0, initialArray.length);
  }
  
  protected byte[] allocateArray(int initialCapacity) {
    return new byte[initialCapacity];
  }
  

  protected void freeArray(byte[] array) {}
  
  private void setArray(byte[] initialArray)
  {
    array = initialArray;
    tmpNioBuf = null;
  }
  
  public ByteBufAllocator alloc()
  {
    return alloc;
  }
  
  public ByteOrder order()
  {
    return ByteOrder.BIG_ENDIAN;
  }
  
  public boolean isDirect()
  {
    return false;
  }
  
  public int capacity()
  {
    return array.length;
  }
  
  public ByteBuf capacity(int newCapacity)
  {
    checkNewCapacity(newCapacity);
    byte[] oldArray = array;
    int oldCapacity = oldArray.length;
    if (newCapacity == oldCapacity) {
      return this;
    }
    int bytesToCopy;
    int bytesToCopy;
    if (newCapacity > oldCapacity) {
      bytesToCopy = oldCapacity;
    } else {
      trimIndicesToCapacity(newCapacity);
      bytesToCopy = newCapacity;
    }
    byte[] newArray = allocateArray(newCapacity);
    System.arraycopy(oldArray, 0, newArray, 0, bytesToCopy);
    setArray(newArray);
    freeArray(oldArray);
    return this;
  }
  
  public boolean hasArray()
  {
    return true;
  }
  
  public byte[] array()
  {
    ensureAccessible();
    return array;
  }
  
  public int arrayOffset()
  {
    return 0;
  }
  
  public boolean hasMemoryAddress()
  {
    return false;
  }
  
  public long memoryAddress()
  {
    throw new UnsupportedOperationException();
  }
  
  public ByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length)
  {
    checkDstIndex(index, length, dstIndex, dst.capacity());
    if (dst.hasMemoryAddress()) {
      PlatformDependent.copyMemory(array, index, dst.memoryAddress() + dstIndex, length);
    } else if (dst.hasArray()) {
      getBytes(index, dst.array(), dst.arrayOffset() + dstIndex, length);
    } else {
      dst.setBytes(dstIndex, array, index, length);
    }
    return this;
  }
  
  public ByteBuf getBytes(int index, byte[] dst, int dstIndex, int length)
  {
    checkDstIndex(index, length, dstIndex, dst.length);
    System.arraycopy(array, index, dst, dstIndex, length);
    return this;
  }
  
  public ByteBuf getBytes(int index, ByteBuffer dst)
  {
    ensureAccessible();
    dst.put(array, index, dst.remaining());
    return this;
  }
  
  public ByteBuf getBytes(int index, OutputStream out, int length) throws IOException
  {
    ensureAccessible();
    out.write(array, index, length);
    return this;
  }
  
  public int getBytes(int index, GatheringByteChannel out, int length) throws IOException
  {
    ensureAccessible();
    return getBytes(index, out, length, false);
  }
  
  public int getBytes(int index, FileChannel out, long position, int length) throws IOException
  {
    ensureAccessible();
    return getBytes(index, out, position, length, false);
  }
  
  private int getBytes(int index, GatheringByteChannel out, int length, boolean internal) throws IOException {
    ensureAccessible();
    ByteBuffer tmpBuf;
    ByteBuffer tmpBuf; if (internal) {
      tmpBuf = internalNioBuffer();
    } else {
      tmpBuf = ByteBuffer.wrap(array);
    }
    return out.write((ByteBuffer)tmpBuf.clear().position(index).limit(index + length));
  }
  
  private int getBytes(int index, FileChannel out, long position, int length, boolean internal) throws IOException {
    ensureAccessible();
    ByteBuffer tmpBuf = internal ? internalNioBuffer() : ByteBuffer.wrap(array);
    return out.write((ByteBuffer)tmpBuf.clear().position(index).limit(index + length), position);
  }
  
  public int readBytes(GatheringByteChannel out, int length) throws IOException
  {
    checkReadableBytes(length);
    int readBytes = getBytes(readerIndex, out, length, true);
    readerIndex += readBytes;
    return readBytes;
  }
  
  public int readBytes(FileChannel out, long position, int length) throws IOException
  {
    checkReadableBytes(length);
    int readBytes = getBytes(readerIndex, out, position, length, true);
    readerIndex += readBytes;
    return readBytes;
  }
  
  public ByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length)
  {
    checkSrcIndex(index, length, srcIndex, src.capacity());
    if (src.hasMemoryAddress()) {
      PlatformDependent.copyMemory(src.memoryAddress() + srcIndex, array, index, length);
    } else if (src.hasArray()) {
      setBytes(index, src.array(), src.arrayOffset() + srcIndex, length);
    } else {
      src.getBytes(srcIndex, array, index, length);
    }
    return this;
  }
  
  public ByteBuf setBytes(int index, byte[] src, int srcIndex, int length)
  {
    checkSrcIndex(index, length, srcIndex, src.length);
    System.arraycopy(src, srcIndex, array, index, length);
    return this;
  }
  
  public ByteBuf setBytes(int index, ByteBuffer src)
  {
    ensureAccessible();
    src.get(array, index, src.remaining());
    return this;
  }
  
  public int setBytes(int index, InputStream in, int length) throws IOException
  {
    ensureAccessible();
    return in.read(array, index, length);
  }
  
  public int setBytes(int index, ScatteringByteChannel in, int length) throws IOException
  {
    ensureAccessible();
    try {
      return in.read((ByteBuffer)internalNioBuffer().clear().position(index).limit(index + length));
    } catch (ClosedChannelException ignored) {}
    return -1;
  }
  
  public int setBytes(int index, FileChannel in, long position, int length)
    throws IOException
  {
    ensureAccessible();
    try {
      return in.read((ByteBuffer)internalNioBuffer().clear().position(index).limit(index + length), position);
    } catch (ClosedChannelException ignored) {}
    return -1;
  }
  

  public int nioBufferCount()
  {
    return 1;
  }
  
  public ByteBuffer nioBuffer(int index, int length)
  {
    ensureAccessible();
    return ByteBuffer.wrap(array, index, length).slice();
  }
  
  public ByteBuffer[] nioBuffers(int index, int length)
  {
    return new ByteBuffer[] { nioBuffer(index, length) };
  }
  
  public ByteBuffer internalNioBuffer(int index, int length)
  {
    checkIndex(index, length);
    return (ByteBuffer)internalNioBuffer().clear().position(index).limit(index + length);
  }
  
  public final boolean isContiguous()
  {
    return true;
  }
  
  public byte getByte(int index)
  {
    ensureAccessible();
    return _getByte(index);
  }
  
  protected byte _getByte(int index)
  {
    return HeapByteBufUtil.getByte(array, index);
  }
  
  public short getShort(int index)
  {
    ensureAccessible();
    return _getShort(index);
  }
  
  protected short _getShort(int index)
  {
    return HeapByteBufUtil.getShort(array, index);
  }
  
  public short getShortLE(int index)
  {
    ensureAccessible();
    return _getShortLE(index);
  }
  
  protected short _getShortLE(int index)
  {
    return HeapByteBufUtil.getShortLE(array, index);
  }
  
  public int getUnsignedMedium(int index)
  {
    ensureAccessible();
    return _getUnsignedMedium(index);
  }
  
  protected int _getUnsignedMedium(int index)
  {
    return HeapByteBufUtil.getUnsignedMedium(array, index);
  }
  
  public int getUnsignedMediumLE(int index)
  {
    ensureAccessible();
    return _getUnsignedMediumLE(index);
  }
  
  protected int _getUnsignedMediumLE(int index)
  {
    return HeapByteBufUtil.getUnsignedMediumLE(array, index);
  }
  
  public int getInt(int index)
  {
    ensureAccessible();
    return _getInt(index);
  }
  
  protected int _getInt(int index)
  {
    return HeapByteBufUtil.getInt(array, index);
  }
  
  public int getIntLE(int index)
  {
    ensureAccessible();
    return _getIntLE(index);
  }
  
  protected int _getIntLE(int index)
  {
    return HeapByteBufUtil.getIntLE(array, index);
  }
  
  public long getLong(int index)
  {
    ensureAccessible();
    return _getLong(index);
  }
  
  protected long _getLong(int index)
  {
    return HeapByteBufUtil.getLong(array, index);
  }
  
  public long getLongLE(int index)
  {
    ensureAccessible();
    return _getLongLE(index);
  }
  
  protected long _getLongLE(int index)
  {
    return HeapByteBufUtil.getLongLE(array, index);
  }
  
  public ByteBuf setByte(int index, int value)
  {
    ensureAccessible();
    _setByte(index, value);
    return this;
  }
  
  protected void _setByte(int index, int value)
  {
    HeapByteBufUtil.setByte(array, index, value);
  }
  
  public ByteBuf setShort(int index, int value)
  {
    ensureAccessible();
    _setShort(index, value);
    return this;
  }
  
  protected void _setShort(int index, int value)
  {
    HeapByteBufUtil.setShort(array, index, value);
  }
  
  public ByteBuf setShortLE(int index, int value)
  {
    ensureAccessible();
    _setShortLE(index, value);
    return this;
  }
  
  protected void _setShortLE(int index, int value)
  {
    HeapByteBufUtil.setShortLE(array, index, value);
  }
  
  public ByteBuf setMedium(int index, int value)
  {
    ensureAccessible();
    _setMedium(index, value);
    return this;
  }
  
  protected void _setMedium(int index, int value)
  {
    HeapByteBufUtil.setMedium(array, index, value);
  }
  
  public ByteBuf setMediumLE(int index, int value)
  {
    ensureAccessible();
    _setMediumLE(index, value);
    return this;
  }
  
  protected void _setMediumLE(int index, int value)
  {
    HeapByteBufUtil.setMediumLE(array, index, value);
  }
  
  public ByteBuf setInt(int index, int value)
  {
    ensureAccessible();
    _setInt(index, value);
    return this;
  }
  
  protected void _setInt(int index, int value)
  {
    HeapByteBufUtil.setInt(array, index, value);
  }
  
  public ByteBuf setIntLE(int index, int value)
  {
    ensureAccessible();
    _setIntLE(index, value);
    return this;
  }
  
  protected void _setIntLE(int index, int value)
  {
    HeapByteBufUtil.setIntLE(array, index, value);
  }
  
  public ByteBuf setLong(int index, long value)
  {
    ensureAccessible();
    _setLong(index, value);
    return this;
  }
  
  protected void _setLong(int index, long value)
  {
    HeapByteBufUtil.setLong(array, index, value);
  }
  
  public ByteBuf setLongLE(int index, long value)
  {
    ensureAccessible();
    _setLongLE(index, value);
    return this;
  }
  
  protected void _setLongLE(int index, long value)
  {
    HeapByteBufUtil.setLongLE(array, index, value);
  }
  
  public ByteBuf copy(int index, int length)
  {
    checkIndex(index, length);
    return alloc().heapBuffer(length, maxCapacity()).writeBytes(array, index, length);
  }
  
  private ByteBuffer internalNioBuffer() {
    ByteBuffer tmpNioBuf = this.tmpNioBuf;
    if (tmpNioBuf == null) {
      this.tmpNioBuf = (tmpNioBuf = ByteBuffer.wrap(array));
    }
    return tmpNioBuf;
  }
  
  protected void deallocate()
  {
    freeArray(array);
    array = EmptyArrays.EMPTY_BYTES;
  }
  
  public ByteBuf unwrap()
  {
    return null;
  }
}
