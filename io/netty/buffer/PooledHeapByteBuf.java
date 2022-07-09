package io.netty.buffer;

import io.netty.util.internal.ObjectPool;
import io.netty.util.internal.ObjectPool.Handle;
import io.netty.util.internal.ObjectPool.ObjectCreator;
import io.netty.util.internal.PlatformDependent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;















class PooledHeapByteBuf
  extends PooledByteBuf<byte[]>
{
  private static final ObjectPool<PooledHeapByteBuf> RECYCLER = ObjectPool.newPool(new ObjectPool.ObjectCreator()
  {
    public PooledHeapByteBuf newObject(ObjectPool.Handle<PooledHeapByteBuf> handle)
    {
      return new PooledHeapByteBuf(handle, 0);
    }
  });
  





  static PooledHeapByteBuf newInstance(int maxCapacity)
  {
    PooledHeapByteBuf buf = (PooledHeapByteBuf)RECYCLER.get();
    buf.reuse(maxCapacity);
    return buf;
  }
  
  PooledHeapByteBuf(ObjectPool.Handle<? extends PooledHeapByteBuf> recyclerHandle, int maxCapacity) {
    super(recyclerHandle, maxCapacity);
  }
  
  public final boolean isDirect()
  {
    return false;
  }
  
  protected byte _getByte(int index)
  {
    return HeapByteBufUtil.getByte((byte[])memory, idx(index));
  }
  
  protected short _getShort(int index)
  {
    return HeapByteBufUtil.getShort((byte[])memory, idx(index));
  }
  
  protected short _getShortLE(int index)
  {
    return HeapByteBufUtil.getShortLE((byte[])memory, idx(index));
  }
  
  protected int _getUnsignedMedium(int index)
  {
    return HeapByteBufUtil.getUnsignedMedium((byte[])memory, idx(index));
  }
  
  protected int _getUnsignedMediumLE(int index)
  {
    return HeapByteBufUtil.getUnsignedMediumLE((byte[])memory, idx(index));
  }
  
  protected int _getInt(int index)
  {
    return HeapByteBufUtil.getInt((byte[])memory, idx(index));
  }
  
  protected int _getIntLE(int index)
  {
    return HeapByteBufUtil.getIntLE((byte[])memory, idx(index));
  }
  
  protected long _getLong(int index)
  {
    return HeapByteBufUtil.getLong((byte[])memory, idx(index));
  }
  
  protected long _getLongLE(int index)
  {
    return HeapByteBufUtil.getLongLE((byte[])memory, idx(index));
  }
  
  public final ByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length)
  {
    checkDstIndex(index, length, dstIndex, dst.capacity());
    if (dst.hasMemoryAddress()) {
      PlatformDependent.copyMemory((byte[])memory, idx(index), dst.memoryAddress() + dstIndex, length);
    } else if (dst.hasArray()) {
      getBytes(index, dst.array(), dst.arrayOffset() + dstIndex, length);
    } else {
      dst.setBytes(dstIndex, (byte[])memory, idx(index), length);
    }
    return this;
  }
  
  public final ByteBuf getBytes(int index, byte[] dst, int dstIndex, int length)
  {
    checkDstIndex(index, length, dstIndex, dst.length);
    System.arraycopy(memory, idx(index), dst, dstIndex, length);
    return this;
  }
  
  public final ByteBuf getBytes(int index, ByteBuffer dst)
  {
    int length = dst.remaining();
    checkIndex(index, length);
    dst.put((byte[])memory, idx(index), length);
    return this;
  }
  
  public final ByteBuf getBytes(int index, OutputStream out, int length) throws IOException
  {
    checkIndex(index, length);
    out.write((byte[])memory, idx(index), length);
    return this;
  }
  
  protected void _setByte(int index, int value)
  {
    HeapByteBufUtil.setByte((byte[])memory, idx(index), value);
  }
  
  protected void _setShort(int index, int value)
  {
    HeapByteBufUtil.setShort((byte[])memory, idx(index), value);
  }
  
  protected void _setShortLE(int index, int value)
  {
    HeapByteBufUtil.setShortLE((byte[])memory, idx(index), value);
  }
  
  protected void _setMedium(int index, int value)
  {
    HeapByteBufUtil.setMedium((byte[])memory, idx(index), value);
  }
  
  protected void _setMediumLE(int index, int value)
  {
    HeapByteBufUtil.setMediumLE((byte[])memory, idx(index), value);
  }
  
  protected void _setInt(int index, int value)
  {
    HeapByteBufUtil.setInt((byte[])memory, idx(index), value);
  }
  
  protected void _setIntLE(int index, int value)
  {
    HeapByteBufUtil.setIntLE((byte[])memory, idx(index), value);
  }
  
  protected void _setLong(int index, long value)
  {
    HeapByteBufUtil.setLong((byte[])memory, idx(index), value);
  }
  
  protected void _setLongLE(int index, long value)
  {
    HeapByteBufUtil.setLongLE((byte[])memory, idx(index), value);
  }
  
  public final ByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length)
  {
    checkSrcIndex(index, length, srcIndex, src.capacity());
    if (src.hasMemoryAddress()) {
      PlatformDependent.copyMemory(src.memoryAddress() + srcIndex, (byte[])memory, idx(index), length);
    } else if (src.hasArray()) {
      setBytes(index, src.array(), src.arrayOffset() + srcIndex, length);
    } else {
      src.getBytes(srcIndex, (byte[])memory, idx(index), length);
    }
    return this;
  }
  
  public final ByteBuf setBytes(int index, byte[] src, int srcIndex, int length)
  {
    checkSrcIndex(index, length, srcIndex, src.length);
    System.arraycopy(src, srcIndex, memory, idx(index), length);
    return this;
  }
  
  public final ByteBuf setBytes(int index, ByteBuffer src)
  {
    int length = src.remaining();
    checkIndex(index, length);
    src.get((byte[])memory, idx(index), length);
    return this;
  }
  
  public final int setBytes(int index, InputStream in, int length) throws IOException
  {
    checkIndex(index, length);
    return in.read((byte[])memory, idx(index), length);
  }
  
  public final ByteBuf copy(int index, int length)
  {
    checkIndex(index, length);
    ByteBuf copy = alloc().heapBuffer(length, maxCapacity());
    return copy.writeBytes((byte[])memory, idx(index), length);
  }
  
  final ByteBuffer duplicateInternalNioBuffer(int index, int length)
  {
    checkIndex(index, length);
    return ByteBuffer.wrap((byte[])memory, idx(index), length).slice();
  }
  
  public final boolean hasArray()
  {
    return true;
  }
  
  public final byte[] array()
  {
    ensureAccessible();
    return (byte[])memory;
  }
  
  public final int arrayOffset()
  {
    return offset;
  }
  
  public final boolean hasMemoryAddress()
  {
    return false;
  }
  
  public final long memoryAddress()
  {
    throw new UnsupportedOperationException();
  }
  
  protected final ByteBuffer newInternalNioBuffer(byte[] memory)
  {
    return ByteBuffer.wrap(memory);
  }
}
