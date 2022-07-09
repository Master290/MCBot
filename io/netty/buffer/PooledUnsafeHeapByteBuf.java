package io.netty.buffer;

import io.netty.util.internal.ObjectPool;
import io.netty.util.internal.ObjectPool.Handle;
import io.netty.util.internal.ObjectPool.ObjectCreator;
import io.netty.util.internal.PlatformDependent;















final class PooledUnsafeHeapByteBuf
  extends PooledHeapByteBuf
{
  private static final ObjectPool<PooledUnsafeHeapByteBuf> RECYCLER = ObjectPool.newPool(new ObjectPool.ObjectCreator()
  {
    public PooledUnsafeHeapByteBuf newObject(ObjectPool.Handle<PooledUnsafeHeapByteBuf> handle)
    {
      return new PooledUnsafeHeapByteBuf(handle, 0, null);
    }
  });
  





  static PooledUnsafeHeapByteBuf newUnsafeInstance(int maxCapacity)
  {
    PooledUnsafeHeapByteBuf buf = (PooledUnsafeHeapByteBuf)RECYCLER.get();
    buf.reuse(maxCapacity);
    return buf;
  }
  
  private PooledUnsafeHeapByteBuf(ObjectPool.Handle<PooledUnsafeHeapByteBuf> recyclerHandle, int maxCapacity) {
    super(recyclerHandle, maxCapacity);
  }
  
  protected byte _getByte(int index)
  {
    return UnsafeByteBufUtil.getByte((byte[])memory, idx(index));
  }
  
  protected short _getShort(int index)
  {
    return UnsafeByteBufUtil.getShort((byte[])memory, idx(index));
  }
  
  protected short _getShortLE(int index)
  {
    return UnsafeByteBufUtil.getShortLE((byte[])memory, idx(index));
  }
  
  protected int _getUnsignedMedium(int index)
  {
    return UnsafeByteBufUtil.getUnsignedMedium((byte[])memory, idx(index));
  }
  
  protected int _getUnsignedMediumLE(int index)
  {
    return UnsafeByteBufUtil.getUnsignedMediumLE((byte[])memory, idx(index));
  }
  
  protected int _getInt(int index)
  {
    return UnsafeByteBufUtil.getInt((byte[])memory, idx(index));
  }
  
  protected int _getIntLE(int index)
  {
    return UnsafeByteBufUtil.getIntLE((byte[])memory, idx(index));
  }
  
  protected long _getLong(int index)
  {
    return UnsafeByteBufUtil.getLong((byte[])memory, idx(index));
  }
  
  protected long _getLongLE(int index)
  {
    return UnsafeByteBufUtil.getLongLE((byte[])memory, idx(index));
  }
  
  protected void _setByte(int index, int value)
  {
    UnsafeByteBufUtil.setByte((byte[])memory, idx(index), value);
  }
  
  protected void _setShort(int index, int value)
  {
    UnsafeByteBufUtil.setShort((byte[])memory, idx(index), value);
  }
  
  protected void _setShortLE(int index, int value)
  {
    UnsafeByteBufUtil.setShortLE((byte[])memory, idx(index), value);
  }
  
  protected void _setMedium(int index, int value)
  {
    UnsafeByteBufUtil.setMedium((byte[])memory, idx(index), value);
  }
  
  protected void _setMediumLE(int index, int value)
  {
    UnsafeByteBufUtil.setMediumLE((byte[])memory, idx(index), value);
  }
  
  protected void _setInt(int index, int value)
  {
    UnsafeByteBufUtil.setInt((byte[])memory, idx(index), value);
  }
  
  protected void _setIntLE(int index, int value)
  {
    UnsafeByteBufUtil.setIntLE((byte[])memory, idx(index), value);
  }
  
  protected void _setLong(int index, long value)
  {
    UnsafeByteBufUtil.setLong((byte[])memory, idx(index), value);
  }
  
  protected void _setLongLE(int index, long value)
  {
    UnsafeByteBufUtil.setLongLE((byte[])memory, idx(index), value);
  }
  
  public ByteBuf setZero(int index, int length)
  {
    if (PlatformDependent.javaVersion() >= 7) {
      checkIndex(index, length);
      
      UnsafeByteBufUtil.setZero((byte[])memory, idx(index), length);
      return this;
    }
    return super.setZero(index, length);
  }
  
  public ByteBuf writeZero(int length)
  {
    if (PlatformDependent.javaVersion() >= 7)
    {
      ensureWritable(length);
      int wIndex = writerIndex;
      UnsafeByteBufUtil.setZero((byte[])memory, idx(wIndex), length);
      writerIndex = (wIndex + length);
      return this;
    }
    return super.writeZero(length);
  }
  
  @Deprecated
  protected SwappedByteBuf newSwappedByteBuf()
  {
    if (PlatformDependent.isUnaligned())
    {
      return new UnsafeHeapSwappedByteBuf(this);
    }
    return super.newSwappedByteBuf();
  }
}
