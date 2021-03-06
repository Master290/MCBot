package io.netty.buffer;

import io.netty.util.internal.StringUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ReadOnlyBufferException;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;


















class ReadOnlyByteBufferBuf
  extends AbstractReferenceCountedByteBuf
{
  protected final ByteBuffer buffer;
  private final ByteBufAllocator allocator;
  private ByteBuffer tmpNioBuf;
  
  ReadOnlyByteBufferBuf(ByteBufAllocator allocator, ByteBuffer buffer)
  {
    super(buffer.remaining());
    if (!buffer.isReadOnly()) {
      throw new IllegalArgumentException("must be a readonly buffer: " + StringUtil.simpleClassName(buffer));
    }
    
    this.allocator = allocator;
    this.buffer = buffer.slice().order(ByteOrder.BIG_ENDIAN);
    writerIndex(this.buffer.limit());
  }
  

  protected void deallocate() {}
  
  public boolean isWritable()
  {
    return false;
  }
  
  public boolean isWritable(int numBytes)
  {
    return false;
  }
  
  public ByteBuf ensureWritable(int minWritableBytes)
  {
    throw new ReadOnlyBufferException();
  }
  
  public int ensureWritable(int minWritableBytes, boolean force)
  {
    return 1;
  }
  
  public byte getByte(int index)
  {
    ensureAccessible();
    return _getByte(index);
  }
  
  protected byte _getByte(int index)
  {
    return buffer.get(index);
  }
  
  public short getShort(int index)
  {
    ensureAccessible();
    return _getShort(index);
  }
  
  protected short _getShort(int index)
  {
    return buffer.getShort(index);
  }
  
  public short getShortLE(int index)
  {
    ensureAccessible();
    return _getShortLE(index);
  }
  
  protected short _getShortLE(int index)
  {
    return ByteBufUtil.swapShort(buffer.getShort(index));
  }
  
  public int getUnsignedMedium(int index)
  {
    ensureAccessible();
    return _getUnsignedMedium(index);
  }
  
  protected int _getUnsignedMedium(int index)
  {
    return 
    
      (getByte(index) & 0xFF) << 16 | (getByte(index + 1) & 0xFF) << 8 | getByte(index + 2) & 0xFF;
  }
  
  public int getUnsignedMediumLE(int index)
  {
    ensureAccessible();
    return _getUnsignedMediumLE(index);
  }
  
  protected int _getUnsignedMediumLE(int index)
  {
    return 
    
      getByte(index) & 0xFF | (getByte(index + 1) & 0xFF) << 8 | (getByte(index + 2) & 0xFF) << 16;
  }
  
  public int getInt(int index)
  {
    ensureAccessible();
    return _getInt(index);
  }
  
  protected int _getInt(int index)
  {
    return buffer.getInt(index);
  }
  
  public int getIntLE(int index)
  {
    ensureAccessible();
    return _getIntLE(index);
  }
  
  protected int _getIntLE(int index)
  {
    return ByteBufUtil.swapInt(buffer.getInt(index));
  }
  
  public long getLong(int index)
  {
    ensureAccessible();
    return _getLong(index);
  }
  
  protected long _getLong(int index)
  {
    return buffer.getLong(index);
  }
  
  public long getLongLE(int index)
  {
    ensureAccessible();
    return _getLongLE(index);
  }
  
  protected long _getLongLE(int index)
  {
    return ByteBufUtil.swapLong(buffer.getLong(index));
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
    
    ByteBuffer tmpBuf = internalNioBuffer();
    tmpBuf.clear().position(index).limit(index + length);
    tmpBuf.get(dst, dstIndex, length);
    return this;
  }
  
  public ByteBuf getBytes(int index, ByteBuffer dst)
  {
    checkIndex(index, dst.remaining());
    
    ByteBuffer tmpBuf = internalNioBuffer();
    tmpBuf.clear().position(index).limit(index + dst.remaining());
    dst.put(tmpBuf);
    return this;
  }
  
  public ByteBuf setByte(int index, int value)
  {
    throw new ReadOnlyBufferException();
  }
  
  protected void _setByte(int index, int value)
  {
    throw new ReadOnlyBufferException();
  }
  
  public ByteBuf setShort(int index, int value)
  {
    throw new ReadOnlyBufferException();
  }
  
  protected void _setShort(int index, int value)
  {
    throw new ReadOnlyBufferException();
  }
  
  public ByteBuf setShortLE(int index, int value)
  {
    throw new ReadOnlyBufferException();
  }
  
  protected void _setShortLE(int index, int value)
  {
    throw new ReadOnlyBufferException();
  }
  
  public ByteBuf setMedium(int index, int value)
  {
    throw new ReadOnlyBufferException();
  }
  
  protected void _setMedium(int index, int value)
  {
    throw new ReadOnlyBufferException();
  }
  
  public ByteBuf setMediumLE(int index, int value)
  {
    throw new ReadOnlyBufferException();
  }
  
  protected void _setMediumLE(int index, int value)
  {
    throw new ReadOnlyBufferException();
  }
  
  public ByteBuf setInt(int index, int value)
  {
    throw new ReadOnlyBufferException();
  }
  
  protected void _setInt(int index, int value)
  {
    throw new ReadOnlyBufferException();
  }
  
  public ByteBuf setIntLE(int index, int value)
  {
    throw new ReadOnlyBufferException();
  }
  
  protected void _setIntLE(int index, int value)
  {
    throw new ReadOnlyBufferException();
  }
  
  public ByteBuf setLong(int index, long value)
  {
    throw new ReadOnlyBufferException();
  }
  
  protected void _setLong(int index, long value)
  {
    throw new ReadOnlyBufferException();
  }
  
  public ByteBuf setLongLE(int index, long value)
  {
    throw new ReadOnlyBufferException();
  }
  
  protected void _setLongLE(int index, long value)
  {
    throw new ReadOnlyBufferException();
  }
  
  public int capacity()
  {
    return maxCapacity();
  }
  
  public ByteBuf capacity(int newCapacity)
  {
    throw new ReadOnlyBufferException();
  }
  
  public ByteBufAllocator alloc()
  {
    return allocator;
  }
  
  public ByteOrder order()
  {
    return ByteOrder.BIG_ENDIAN;
  }
  
  public ByteBuf unwrap()
  {
    return null;
  }
  
  public boolean isReadOnly()
  {
    return buffer.isReadOnly();
  }
  
  public boolean isDirect()
  {
    return buffer.isDirect();
  }
  
  public ByteBuf getBytes(int index, OutputStream out, int length) throws IOException
  {
    ensureAccessible();
    if (length == 0) {
      return this;
    }
    
    if (buffer.hasArray()) {
      out.write(buffer.array(), index + buffer.arrayOffset(), length);
    } else {
      byte[] tmp = ByteBufUtil.threadLocalTempArray(length);
      ByteBuffer tmpBuf = internalNioBuffer();
      tmpBuf.clear().position(index);
      tmpBuf.get(tmp, 0, length);
      out.write(tmp, 0, length);
    }
    return this;
  }
  
  public int getBytes(int index, GatheringByteChannel out, int length) throws IOException
  {
    ensureAccessible();
    if (length == 0) {
      return 0;
    }
    
    ByteBuffer tmpBuf = internalNioBuffer();
    tmpBuf.clear().position(index).limit(index + length);
    return out.write(tmpBuf);
  }
  
  public int getBytes(int index, FileChannel out, long position, int length) throws IOException
  {
    ensureAccessible();
    if (length == 0) {
      return 0;
    }
    
    ByteBuffer tmpBuf = internalNioBuffer();
    tmpBuf.clear().position(index).limit(index + length);
    return out.write(tmpBuf, position);
  }
  
  public ByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length)
  {
    throw new ReadOnlyBufferException();
  }
  
  public ByteBuf setBytes(int index, byte[] src, int srcIndex, int length)
  {
    throw new ReadOnlyBufferException();
  }
  
  public ByteBuf setBytes(int index, ByteBuffer src)
  {
    throw new ReadOnlyBufferException();
  }
  
  public int setBytes(int index, InputStream in, int length) throws IOException
  {
    throw new ReadOnlyBufferException();
  }
  
  public int setBytes(int index, ScatteringByteChannel in, int length) throws IOException
  {
    throw new ReadOnlyBufferException();
  }
  
  public int setBytes(int index, FileChannel in, long position, int length) throws IOException
  {
    throw new ReadOnlyBufferException();
  }
  
  protected final ByteBuffer internalNioBuffer() {
    ByteBuffer tmpNioBuf = this.tmpNioBuf;
    if (tmpNioBuf == null) {
      this.tmpNioBuf = (tmpNioBuf = buffer.duplicate());
    }
    return tmpNioBuf;
  }
  
  public ByteBuf copy(int index, int length)
  {
    ensureAccessible();
    try
    {
      src = (ByteBuffer)internalNioBuffer().clear().position(index).limit(index + length);
    } catch (IllegalArgumentException ignored) { ByteBuffer src;
      throw new IndexOutOfBoundsException("Too many bytes to read - Need " + (index + length));
    }
    ByteBuffer src;
    ByteBuf dst = src.isDirect() ? alloc().directBuffer(length) : alloc().heapBuffer(length);
    dst.writeBytes(src);
    return dst;
  }
  
  public int nioBufferCount()
  {
    return 1;
  }
  
  public ByteBuffer[] nioBuffers(int index, int length)
  {
    return new ByteBuffer[] { nioBuffer(index, length) };
  }
  
  public ByteBuffer nioBuffer(int index, int length)
  {
    checkIndex(index, length);
    return (ByteBuffer)buffer.duplicate().position(index).limit(index + length);
  }
  
  public ByteBuffer internalNioBuffer(int index, int length)
  {
    ensureAccessible();
    return (ByteBuffer)internalNioBuffer().clear().position(index).limit(index + length);
  }
  
  public final boolean isContiguous()
  {
    return true;
  }
  
  public boolean hasArray()
  {
    return buffer.hasArray();
  }
  
  public byte[] array()
  {
    return buffer.array();
  }
  
  public int arrayOffset()
  {
    return buffer.arrayOffset();
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
