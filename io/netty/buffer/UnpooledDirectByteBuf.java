package io.netty.buffer;

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




























public class UnpooledDirectByteBuf
  extends AbstractReferenceCountedByteBuf
{
  private final ByteBufAllocator alloc;
  ByteBuffer buffer;
  private ByteBuffer tmpNioBuf;
  private int capacity;
  private boolean doNotFree;
  
  public UnpooledDirectByteBuf(ByteBufAllocator alloc, int initialCapacity, int maxCapacity)
  {
    super(maxCapacity);
    ObjectUtil.checkNotNull(alloc, "alloc");
    ObjectUtil.checkPositiveOrZero(initialCapacity, "initialCapacity");
    ObjectUtil.checkPositiveOrZero(maxCapacity, "maxCapacity");
    if (initialCapacity > maxCapacity) {
      throw new IllegalArgumentException(String.format("initialCapacity(%d) > maxCapacity(%d)", new Object[] {
        Integer.valueOf(initialCapacity), Integer.valueOf(maxCapacity) }));
    }
    
    this.alloc = alloc;
    setByteBuffer(allocateDirect(initialCapacity), false);
  }
  




  protected UnpooledDirectByteBuf(ByteBufAllocator alloc, ByteBuffer initialBuffer, int maxCapacity)
  {
    this(alloc, initialBuffer, maxCapacity, false, true);
  }
  
  UnpooledDirectByteBuf(ByteBufAllocator alloc, ByteBuffer initialBuffer, int maxCapacity, boolean doFree, boolean slice)
  {
    super(maxCapacity);
    ObjectUtil.checkNotNull(alloc, "alloc");
    ObjectUtil.checkNotNull(initialBuffer, "initialBuffer");
    if (!initialBuffer.isDirect()) {
      throw new IllegalArgumentException("initialBuffer is not a direct buffer.");
    }
    if (initialBuffer.isReadOnly()) {
      throw new IllegalArgumentException("initialBuffer is a read-only buffer.");
    }
    
    int initialCapacity = initialBuffer.remaining();
    if (initialCapacity > maxCapacity) {
      throw new IllegalArgumentException(String.format("initialCapacity(%d) > maxCapacity(%d)", new Object[] {
        Integer.valueOf(initialCapacity), Integer.valueOf(maxCapacity) }));
    }
    
    this.alloc = alloc;
    doNotFree = (!doFree);
    setByteBuffer((slice ? initialBuffer.slice() : initialBuffer).order(ByteOrder.BIG_ENDIAN), false);
    writerIndex(initialCapacity);
  }
  


  protected ByteBuffer allocateDirect(int initialCapacity)
  {
    return ByteBuffer.allocateDirect(initialCapacity);
  }
  


  protected void freeDirect(ByteBuffer buffer)
  {
    PlatformDependent.freeDirectBuffer(buffer);
  }
  
  void setByteBuffer(ByteBuffer buffer, boolean tryFree) {
    if (tryFree) {
      ByteBuffer oldBuffer = this.buffer;
      if (oldBuffer != null) {
        if (doNotFree) {
          doNotFree = false;
        } else {
          freeDirect(oldBuffer);
        }
      }
    }
    
    this.buffer = buffer;
    tmpNioBuf = null;
    capacity = buffer.remaining();
  }
  
  public boolean isDirect()
  {
    return true;
  }
  
  public int capacity()
  {
    return capacity;
  }
  
  public ByteBuf capacity(int newCapacity)
  {
    checkNewCapacity(newCapacity);
    int oldCapacity = capacity;
    if (newCapacity == oldCapacity)
      return this;
    int bytesToCopy;
    int bytesToCopy;
    if (newCapacity > oldCapacity) {
      bytesToCopy = oldCapacity;
    } else {
      trimIndicesToCapacity(newCapacity);
      bytesToCopy = newCapacity;
    }
    ByteBuffer oldBuffer = buffer;
    ByteBuffer newBuffer = allocateDirect(newCapacity);
    oldBuffer.position(0).limit(bytesToCopy);
    newBuffer.position(0).limit(bytesToCopy);
    newBuffer.put(oldBuffer).clear();
    setByteBuffer(newBuffer, true);
    return this;
  }
  
  public ByteBufAllocator alloc()
  {
    return alloc;
  }
  
  public ByteOrder order()
  {
    return ByteOrder.BIG_ENDIAN;
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
    getBytes(index, dst, dstIndex, length, false);
    return this;
  }
  
  void getBytes(int index, byte[] dst, int dstIndex, int length, boolean internal) {
    checkDstIndex(index, length, dstIndex, dst.length);
    ByteBuffer tmpBuf;
    ByteBuffer tmpBuf;
    if (internal) {
      tmpBuf = internalNioBuffer();
    } else {
      tmpBuf = buffer.duplicate();
    }
    tmpBuf.clear().position(index).limit(index + length);
    tmpBuf.get(dst, dstIndex, length);
  }
  
  public ByteBuf readBytes(byte[] dst, int dstIndex, int length)
  {
    checkReadableBytes(length);
    getBytes(readerIndex, dst, dstIndex, length, true);
    readerIndex += length;
    return this;
  }
  
  public ByteBuf getBytes(int index, ByteBuffer dst)
  {
    getBytes(index, dst, false);
    return this;
  }
  
  void getBytes(int index, ByteBuffer dst, boolean internal) {
    checkIndex(index, dst.remaining());
    ByteBuffer tmpBuf;
    ByteBuffer tmpBuf;
    if (internal) {
      tmpBuf = internalNioBuffer();
    } else {
      tmpBuf = buffer.duplicate();
    }
    tmpBuf.clear().position(index).limit(index + dst.remaining());
    dst.put(tmpBuf);
  }
  
  public ByteBuf readBytes(ByteBuffer dst)
  {
    int length = dst.remaining();
    checkReadableBytes(length);
    getBytes(readerIndex, dst, true);
    readerIndex += length;
    return this;
  }
  
  public ByteBuf setByte(int index, int value)
  {
    ensureAccessible();
    _setByte(index, value);
    return this;
  }
  
  protected void _setByte(int index, int value)
  {
    buffer.put(index, (byte)value);
  }
  
  public ByteBuf setShort(int index, int value)
  {
    ensureAccessible();
    _setShort(index, value);
    return this;
  }
  
  protected void _setShort(int index, int value)
  {
    buffer.putShort(index, (short)value);
  }
  
  protected void _setShortLE(int index, int value)
  {
    buffer.putShort(index, ByteBufUtil.swapShort((short)value));
  }
  
  public ByteBuf setMedium(int index, int value)
  {
    ensureAccessible();
    _setMedium(index, value);
    return this;
  }
  
  protected void _setMedium(int index, int value)
  {
    setByte(index, (byte)(value >>> 16));
    setByte(index + 1, (byte)(value >>> 8));
    setByte(index + 2, (byte)value);
  }
  
  protected void _setMediumLE(int index, int value)
  {
    setByte(index, (byte)value);
    setByte(index + 1, (byte)(value >>> 8));
    setByte(index + 2, (byte)(value >>> 16));
  }
  
  public ByteBuf setInt(int index, int value)
  {
    ensureAccessible();
    _setInt(index, value);
    return this;
  }
  
  protected void _setInt(int index, int value)
  {
    buffer.putInt(index, value);
  }
  
  protected void _setIntLE(int index, int value)
  {
    buffer.putInt(index, ByteBufUtil.swapInt(value));
  }
  
  public ByteBuf setLong(int index, long value)
  {
    ensureAccessible();
    _setLong(index, value);
    return this;
  }
  
  protected void _setLong(int index, long value)
  {
    buffer.putLong(index, value);
  }
  
  protected void _setLongLE(int index, long value)
  {
    buffer.putLong(index, ByteBufUtil.swapLong(value));
  }
  
  public ByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length)
  {
    checkSrcIndex(index, length, srcIndex, src.capacity());
    if (src.nioBufferCount() > 0) {
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
    ByteBuffer tmpBuf = internalNioBuffer();
    tmpBuf.clear().position(index).limit(index + length);
    tmpBuf.put(src, srcIndex, length);
    return this;
  }
  
  public ByteBuf setBytes(int index, ByteBuffer src)
  {
    ensureAccessible();
    ByteBuffer tmpBuf = internalNioBuffer();
    if (src == tmpBuf) {
      src = src.duplicate();
    }
    
    tmpBuf.clear().position(index).limit(index + src.remaining());
    tmpBuf.put(src);
    return this;
  }
  
  public ByteBuf getBytes(int index, OutputStream out, int length) throws IOException
  {
    getBytes(index, out, length, false);
    return this;
  }
  
  void getBytes(int index, OutputStream out, int length, boolean internal) throws IOException {
    ensureAccessible();
    if (length == 0) {
      return;
    }
    ByteBufUtil.readBytes(alloc(), internal ? internalNioBuffer() : buffer.duplicate(), index, length, out);
  }
  
  public ByteBuf readBytes(OutputStream out, int length) throws IOException
  {
    checkReadableBytes(length);
    getBytes(readerIndex, out, length, true);
    readerIndex += length;
    return this;
  }
  
  public int getBytes(int index, GatheringByteChannel out, int length) throws IOException
  {
    return getBytes(index, out, length, false);
  }
  
  private int getBytes(int index, GatheringByteChannel out, int length, boolean internal) throws IOException {
    ensureAccessible();
    if (length == 0) {
      return 0;
    }
    ByteBuffer tmpBuf;
    ByteBuffer tmpBuf;
    if (internal) {
      tmpBuf = internalNioBuffer();
    } else {
      tmpBuf = buffer.duplicate();
    }
    tmpBuf.clear().position(index).limit(index + length);
    return out.write(tmpBuf);
  }
  
  public int getBytes(int index, FileChannel out, long position, int length) throws IOException
  {
    return getBytes(index, out, position, length, false);
  }
  
  private int getBytes(int index, FileChannel out, long position, int length, boolean internal) throws IOException {
    ensureAccessible();
    if (length == 0) {
      return 0;
    }
    
    ByteBuffer tmpBuf = internal ? internalNioBuffer() : buffer.duplicate();
    tmpBuf.clear().position(index).limit(index + length);
    return out.write(tmpBuf, position);
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
  
  public int setBytes(int index, InputStream in, int length) throws IOException
  {
    ensureAccessible();
    if (buffer.hasArray()) {
      return in.read(buffer.array(), buffer.arrayOffset() + index, length);
    }
    byte[] tmp = ByteBufUtil.threadLocalTempArray(length);
    int readBytes = in.read(tmp, 0, length);
    if (readBytes <= 0) {
      return readBytes;
    }
    ByteBuffer tmpBuf = internalNioBuffer();
    tmpBuf.clear().position(index);
    tmpBuf.put(tmp, 0, readBytes);
    return readBytes;
  }
  
  public int setBytes(int index, ScatteringByteChannel in, int length)
    throws IOException
  {
    ensureAccessible();
    ByteBuffer tmpBuf = internalNioBuffer();
    tmpBuf.clear().position(index).limit(index + length);
    try {
      return in.read(tmpBuf);
    } catch (ClosedChannelException ignored) {}
    return -1;
  }
  
  public int setBytes(int index, FileChannel in, long position, int length)
    throws IOException
  {
    ensureAccessible();
    ByteBuffer tmpBuf = internalNioBuffer();
    tmpBuf.clear().position(index).limit(index + length);
    try {
      return in.read(tmpBuf, position);
    } catch (ClosedChannelException ignored) {}
    return -1;
  }
  

  public int nioBufferCount()
  {
    return 1;
  }
  
  public ByteBuffer[] nioBuffers(int index, int length)
  {
    return new ByteBuffer[] { nioBuffer(index, length) };
  }
  
  public final boolean isContiguous()
  {
    return true;
  }
  
  public ByteBuf copy(int index, int length)
  {
    ensureAccessible();
    try
    {
      src = (ByteBuffer)buffer.duplicate().clear().position(index).limit(index + length);
    } catch (IllegalArgumentException ignored) { ByteBuffer src;
      throw new IndexOutOfBoundsException("Too many bytes to read - Need " + (index + length));
    }
    ByteBuffer src;
    return alloc().directBuffer(length, maxCapacity()).writeBytes(src);
  }
  
  public ByteBuffer internalNioBuffer(int index, int length)
  {
    checkIndex(index, length);
    return (ByteBuffer)internalNioBuffer().clear().position(index).limit(index + length);
  }
  
  private ByteBuffer internalNioBuffer() {
    ByteBuffer tmpNioBuf = this.tmpNioBuf;
    if (tmpNioBuf == null) {
      this.tmpNioBuf = (tmpNioBuf = buffer.duplicate());
    }
    return tmpNioBuf;
  }
  
  public ByteBuffer nioBuffer(int index, int length)
  {
    checkIndex(index, length);
    return ((ByteBuffer)buffer.duplicate().position(index).limit(index + length)).slice();
  }
  
  protected void deallocate()
  {
    ByteBuffer buffer = this.buffer;
    if (buffer == null) {
      return;
    }
    
    this.buffer = null;
    
    if (!doNotFree) {
      freeDirect(buffer);
    }
  }
  
  public ByteBuf unwrap()
  {
    return null;
  }
}
