package io.netty.buffer;

import io.netty.util.internal.ObjectPool;
import io.netty.util.internal.ObjectPool.Handle;
import io.netty.util.internal.ObjectPool.ObjectCreator;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
















final class PooledDirectByteBuf
  extends PooledByteBuf<ByteBuffer>
{
  private static final ObjectPool<PooledDirectByteBuf> RECYCLER = ObjectPool.newPool(new ObjectPool.ObjectCreator()
  {
    public PooledDirectByteBuf newObject(ObjectPool.Handle<PooledDirectByteBuf> handle)
    {
      return new PooledDirectByteBuf(handle, 0, null);
    }
  });
  





  static PooledDirectByteBuf newInstance(int maxCapacity)
  {
    PooledDirectByteBuf buf = (PooledDirectByteBuf)RECYCLER.get();
    buf.reuse(maxCapacity);
    return buf;
  }
  
  private PooledDirectByteBuf(ObjectPool.Handle<PooledDirectByteBuf> recyclerHandle, int maxCapacity) {
    super(recyclerHandle, maxCapacity);
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
    return ((ByteBuffer)memory).get(idx(index));
  }
  
  protected short _getShort(int index)
  {
    return ((ByteBuffer)memory).getShort(idx(index));
  }
  
  protected short _getShortLE(int index)
  {
    return ByteBufUtil.swapShort(_getShort(index));
  }
  
  protected int _getUnsignedMedium(int index)
  {
    index = idx(index);
    return (((ByteBuffer)memory).get(index) & 0xFF) << 16 | 
      (((ByteBuffer)memory).get(index + 1) & 0xFF) << 8 | ((ByteBuffer)memory)
      .get(index + 2) & 0xFF;
  }
  
  protected int _getUnsignedMediumLE(int index)
  {
    index = idx(index);
    return ((ByteBuffer)memory).get(index) & 0xFF | 
      (((ByteBuffer)memory).get(index + 1) & 0xFF) << 8 | 
      (((ByteBuffer)memory).get(index + 2) & 0xFF) << 16;
  }
  
  protected int _getInt(int index)
  {
    return ((ByteBuffer)memory).getInt(idx(index));
  }
  
  protected int _getIntLE(int index)
  {
    return ByteBufUtil.swapInt(_getInt(index));
  }
  
  protected long _getLong(int index)
  {
    return ((ByteBuffer)memory).getLong(idx(index));
  }
  
  protected long _getLongLE(int index)
  {
    return ByteBufUtil.swapLong(_getLong(index));
  }
  
  public ByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length)
  {
    checkDstIndex(index, length, dstIndex, dst.capacity());
    if (dst.hasArray()) {
      getBytes(index, dst.array(), dst.arrayOffset() + dstIndex, length);
    } else if (dst.nioBufferCount() > 0) {
      for (ByteBuffer bb : dst.nioBuffers(dstIndex, length)) {
        int bbLen = bb.remaining();
        getBytes(index, bb);
        index += bbLen;
      }
    } else {
      dst.setBytes(dstIndex, this, index, length);
    }
    return this;
  }
  
  public ByteBuf getBytes(int index, byte[] dst, int dstIndex, int length)
  {
    checkDstIndex(index, length, dstIndex, dst.length);
    _internalNioBuffer(index, length, true).get(dst, dstIndex, length);
    return this;
  }
  
  public ByteBuf readBytes(byte[] dst, int dstIndex, int length)
  {
    checkDstIndex(length, dstIndex, dst.length);
    _internalNioBuffer(readerIndex, length, false).get(dst, dstIndex, length);
    readerIndex += length;
    return this;
  }
  
  public ByteBuf getBytes(int index, ByteBuffer dst)
  {
    dst.put(duplicateInternalNioBuffer(index, dst.remaining()));
    return this;
  }
  
  public ByteBuf readBytes(ByteBuffer dst)
  {
    int length = dst.remaining();
    checkReadableBytes(length);
    dst.put(_internalNioBuffer(readerIndex, length, false));
    readerIndex += length;
    return this;
  }
  
  public ByteBuf getBytes(int index, OutputStream out, int length) throws IOException
  {
    getBytes(index, out, length, false);
    return this;
  }
  
  private void getBytes(int index, OutputStream out, int length, boolean internal) throws IOException {
    checkIndex(index, length);
    if (length == 0) {
      return;
    }
    ByteBufUtil.readBytes(alloc(), internal ? internalNioBuffer() : ((ByteBuffer)memory).duplicate(), idx(index), length, out);
  }
  
  public ByteBuf readBytes(OutputStream out, int length) throws IOException
  {
    checkReadableBytes(length);
    getBytes(readerIndex, out, length, true);
    readerIndex += length;
    return this;
  }
  
  protected void _setByte(int index, int value)
  {
    ((ByteBuffer)memory).put(idx(index), (byte)value);
  }
  
  protected void _setShort(int index, int value)
  {
    ((ByteBuffer)memory).putShort(idx(index), (short)value);
  }
  
  protected void _setShortLE(int index, int value)
  {
    _setShort(index, ByteBufUtil.swapShort((short)value));
  }
  
  protected void _setMedium(int index, int value)
  {
    index = idx(index);
    ((ByteBuffer)memory).put(index, (byte)(value >>> 16));
    ((ByteBuffer)memory).put(index + 1, (byte)(value >>> 8));
    ((ByteBuffer)memory).put(index + 2, (byte)value);
  }
  
  protected void _setMediumLE(int index, int value)
  {
    index = idx(index);
    ((ByteBuffer)memory).put(index, (byte)value);
    ((ByteBuffer)memory).put(index + 1, (byte)(value >>> 8));
    ((ByteBuffer)memory).put(index + 2, (byte)(value >>> 16));
  }
  
  protected void _setInt(int index, int value)
  {
    ((ByteBuffer)memory).putInt(idx(index), value);
  }
  
  protected void _setIntLE(int index, int value)
  {
    _setInt(index, ByteBufUtil.swapInt(value));
  }
  
  protected void _setLong(int index, long value)
  {
    ((ByteBuffer)memory).putLong(idx(index), value);
  }
  
  protected void _setLongLE(int index, long value)
  {
    _setLong(index, ByteBufUtil.swapLong(value));
  }
  
  public ByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length)
  {
    checkSrcIndex(index, length, srcIndex, src.capacity());
    if (src.hasArray()) {
      setBytes(index, src.array(), src.arrayOffset() + srcIndex, length);
    } else if (src.nioBufferCount() > 0) {
      for (ByteBuffer bb : src.nioBuffers(srcIndex, length)) {
        int bbLen = bb.remaining();
        setBytes(index, bb);
        index += bbLen;
      }
    } else {
      src.getBytes(srcIndex, this, index, length);
    }
    return this;
  }
  
  public ByteBuf setBytes(int index, byte[] src, int srcIndex, int length)
  {
    checkSrcIndex(index, length, srcIndex, src.length);
    _internalNioBuffer(index, length, false).put(src, srcIndex, length);
    return this;
  }
  
  public ByteBuf setBytes(int index, ByteBuffer src)
  {
    int length = src.remaining();
    checkIndex(index, length);
    ByteBuffer tmpBuf = internalNioBuffer();
    if (src == tmpBuf) {
      src = src.duplicate();
    }
    
    index = idx(index);
    tmpBuf.limit(index + length).position(index);
    tmpBuf.put(src);
    return this;
  }
  
  public int setBytes(int index, InputStream in, int length) throws IOException
  {
    checkIndex(index, length);
    byte[] tmp = ByteBufUtil.threadLocalTempArray(length);
    int readBytes = in.read(tmp, 0, length);
    if (readBytes <= 0) {
      return readBytes;
    }
    ByteBuffer tmpBuf = internalNioBuffer();
    tmpBuf.position(idx(index));
    tmpBuf.put(tmp, 0, readBytes);
    return readBytes;
  }
  
  public ByteBuf copy(int index, int length)
  {
    checkIndex(index, length);
    ByteBuf copy = alloc().directBuffer(length, maxCapacity());
    return copy.writeBytes(this, index, length);
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
    return false;
  }
  
  public long memoryAddress()
  {
    throw new UnsupportedOperationException();
  }
}
