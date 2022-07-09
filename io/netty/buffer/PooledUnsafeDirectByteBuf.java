package io.netty.buffer;

import io.netty.util.internal.ObjectPool;
import io.netty.util.internal.ObjectPool.Handle;
import io.netty.util.internal.ObjectPool.ObjectCreator;
import io.netty.util.internal.PlatformDependent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
















final class PooledUnsafeDirectByteBuf
  extends PooledByteBuf<ByteBuffer>
{
  private static final ObjectPool<PooledUnsafeDirectByteBuf> RECYCLER = ObjectPool.newPool(new ObjectPool.ObjectCreator()
  {
    public PooledUnsafeDirectByteBuf newObject(ObjectPool.Handle<PooledUnsafeDirectByteBuf> handle)
    {
      return new PooledUnsafeDirectByteBuf(handle, 0, null);
    }
  });
  

  private long memoryAddress;
  


  static PooledUnsafeDirectByteBuf newInstance(int maxCapacity)
  {
    PooledUnsafeDirectByteBuf buf = (PooledUnsafeDirectByteBuf)RECYCLER.get();
    buf.reuse(maxCapacity);
    return buf;
  }
  

  private PooledUnsafeDirectByteBuf(ObjectPool.Handle<PooledUnsafeDirectByteBuf> recyclerHandle, int maxCapacity)
  {
    super(recyclerHandle, maxCapacity);
  }
  

  void init(PoolChunk<ByteBuffer> chunk, ByteBuffer nioBuffer, long handle, int offset, int length, int maxLength, PoolThreadCache cache)
  {
    super.init(chunk, nioBuffer, handle, offset, length, maxLength, cache);
    initMemoryAddress();
  }
  
  void initUnpooled(PoolChunk<ByteBuffer> chunk, int length)
  {
    super.initUnpooled(chunk, length);
    initMemoryAddress();
  }
  
  private void initMemoryAddress() {
    memoryAddress = (PlatformDependent.directBufferAddress((ByteBuffer)memory) + offset);
  }
  
  protected ByteBuffer newInternalNioBuffer(ByteBuffer memory)
  {
    return memory.duplicate();
  }
  
  public boolean isDirect()
  {
    return true;
  }
  
  protected byte _getByte(int index)
  {
    return UnsafeByteBufUtil.getByte(addr(index));
  }
  
  protected short _getShort(int index)
  {
    return UnsafeByteBufUtil.getShort(addr(index));
  }
  
  protected short _getShortLE(int index)
  {
    return UnsafeByteBufUtil.getShortLE(addr(index));
  }
  
  protected int _getUnsignedMedium(int index)
  {
    return UnsafeByteBufUtil.getUnsignedMedium(addr(index));
  }
  
  protected int _getUnsignedMediumLE(int index)
  {
    return UnsafeByteBufUtil.getUnsignedMediumLE(addr(index));
  }
  
  protected int _getInt(int index)
  {
    return UnsafeByteBufUtil.getInt(addr(index));
  }
  
  protected int _getIntLE(int index)
  {
    return UnsafeByteBufUtil.getIntLE(addr(index));
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
  
  public ByteBuf getBytes(int index, byte[] dst, int dstIndex, int length)
  {
    UnsafeByteBufUtil.getBytes(this, addr(index), index, dst, dstIndex, length);
    return this;
  }
  
  public ByteBuf getBytes(int index, ByteBuffer dst)
  {
    UnsafeByteBufUtil.getBytes(this, addr(index), index, dst);
    return this;
  }
  
  public ByteBuf getBytes(int index, OutputStream out, int length) throws IOException
  {
    UnsafeByteBufUtil.getBytes(this, addr(index), index, out, length);
    return this;
  }
  
  protected void _setByte(int index, int value)
  {
    UnsafeByteBufUtil.setByte(addr(index), (byte)value);
  }
  
  protected void _setShort(int index, int value)
  {
    UnsafeByteBufUtil.setShort(addr(index), value);
  }
  
  protected void _setShortLE(int index, int value)
  {
    UnsafeByteBufUtil.setShortLE(addr(index), value);
  }
  
  protected void _setMedium(int index, int value)
  {
    UnsafeByteBufUtil.setMedium(addr(index), value);
  }
  
  protected void _setMediumLE(int index, int value)
  {
    UnsafeByteBufUtil.setMediumLE(addr(index), value);
  }
  
  protected void _setInt(int index, int value)
  {
    UnsafeByteBufUtil.setInt(addr(index), value);
  }
  
  protected void _setIntLE(int index, int value)
  {
    UnsafeByteBufUtil.setIntLE(addr(index), value);
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
  
  public int setBytes(int index, InputStream in, int length) throws IOException
  {
    return UnsafeByteBufUtil.setBytes(this, addr(index), index, in, length);
  }
  
  public ByteBuf copy(int index, int length)
  {
    return UnsafeByteBufUtil.copy(this, addr(index), index, length);
  }
  
  public boolean hasArray()
  {
    return false;
  }
  
  public byte[] array()
  {
    throw new UnsupportedOperationException("direct buffer");
  }
  
  public int arrayOffset()
  {
    throw new UnsupportedOperationException("direct buffer");
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
  
  private long addr(int index) {
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
