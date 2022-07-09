package io.netty.buffer;

import io.netty.util.internal.EmptyArrays;
import io.netty.util.internal.RecyclableArrayList;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ReadOnlyBufferException;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.util.Collections;



















final class FixedCompositeByteBuf
  extends AbstractReferenceCountedByteBuf
{
  private static final ByteBuf[] EMPTY = { Unpooled.EMPTY_BUFFER };
  private final int nioBufferCount;
  private final int capacity;
  private final ByteBufAllocator allocator;
  private final ByteOrder order;
  private final ByteBuf[] buffers;
  private final boolean direct;
  
  FixedCompositeByteBuf(ByteBufAllocator allocator, ByteBuf... buffers) {
    super(Integer.MAX_VALUE);
    if (buffers.length == 0) {
      this.buffers = EMPTY;
      order = ByteOrder.BIG_ENDIAN;
      this.nioBufferCount = 1;
      this.capacity = 0;
      this.direct = Unpooled.EMPTY_BUFFER.isDirect();
    } else {
      ByteBuf b = buffers[0];
      this.buffers = buffers;
      boolean direct = true;
      int nioBufferCount = b.nioBufferCount();
      int capacity = b.readableBytes();
      order = b.order();
      for (int i = 1; i < buffers.length; i++) {
        b = buffers[i];
        if (buffers[i].order() != order) {
          throw new IllegalArgumentException("All ByteBufs need to have same ByteOrder");
        }
        nioBufferCount += b.nioBufferCount();
        capacity += b.readableBytes();
        if (!b.isDirect()) {
          direct = false;
        }
      }
      this.nioBufferCount = nioBufferCount;
      this.capacity = capacity;
      this.direct = direct;
    }
    setIndex(0, capacity());
    this.allocator = allocator;
  }
  
  public boolean isWritable()
  {
    return false;
  }
  
  public boolean isWritable(int size)
  {
    return false;
  }
  
  public ByteBuf discardReadBytes()
  {
    throw new ReadOnlyBufferException();
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
  
  protected void _setLongLE(int index, long value)
  {
    throw new ReadOnlyBufferException();
  }
  
  public int setBytes(int index, InputStream in, int length)
  {
    throw new ReadOnlyBufferException();
  }
  
  public int setBytes(int index, ScatteringByteChannel in, int length)
  {
    throw new ReadOnlyBufferException();
  }
  
  public int setBytes(int index, FileChannel in, long position, int length)
  {
    throw new ReadOnlyBufferException();
  }
  
  public int capacity()
  {
    return capacity;
  }
  
  public int maxCapacity()
  {
    return capacity;
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
    return order;
  }
  
  public ByteBuf unwrap()
  {
    return null;
  }
  
  public boolean isDirect()
  {
    return direct;
  }
  
  private Component findComponent(int index) {
    int readable = 0;
    for (int i = 0; i < buffers.length; i++) {
      Component comp = null;
      ByteBuf b = buffers[i];
      if ((b instanceof Component)) {
        comp = (Component)b;
        b = buf;
      }
      readable += b.readableBytes();
      if (index < readable) {
        if (comp == null)
        {

          comp = new Component(i, readable - b.readableBytes(), b);
          buffers[i] = comp;
        }
        return comp;
      }
    }
    throw new IllegalStateException();
  }
  


  private ByteBuf buffer(int i)
  {
    ByteBuf b = buffers[i];
    return (b instanceof Component) ? buf : b;
  }
  
  public byte getByte(int index)
  {
    return _getByte(index);
  }
  
  protected byte _getByte(int index)
  {
    Component c = findComponent(index);
    return buf.getByte(index - offset);
  }
  
  protected short _getShort(int index)
  {
    Component c = findComponent(index);
    if (index + 2 <= endOffset)
      return buf.getShort(index - offset);
    if (order() == ByteOrder.BIG_ENDIAN) {
      return (short)((_getByte(index) & 0xFF) << 8 | _getByte(index + 1) & 0xFF);
    }
    return (short)(_getByte(index) & 0xFF | (_getByte(index + 1) & 0xFF) << 8);
  }
  

  protected short _getShortLE(int index)
  {
    Component c = findComponent(index);
    if (index + 2 <= endOffset)
      return buf.getShortLE(index - offset);
    if (order() == ByteOrder.BIG_ENDIAN) {
      return (short)(_getByte(index) & 0xFF | (_getByte(index + 1) & 0xFF) << 8);
    }
    return (short)((_getByte(index) & 0xFF) << 8 | _getByte(index + 1) & 0xFF);
  }
  

  protected int _getUnsignedMedium(int index)
  {
    Component c = findComponent(index);
    if (index + 3 <= endOffset)
      return buf.getUnsignedMedium(index - offset);
    if (order() == ByteOrder.BIG_ENDIAN) {
      return (_getShort(index) & 0xFFFF) << 8 | _getByte(index + 2) & 0xFF;
    }
    return _getShort(index) & 0xFFFF | (_getByte(index + 2) & 0xFF) << 16;
  }
  

  protected int _getUnsignedMediumLE(int index)
  {
    Component c = findComponent(index);
    if (index + 3 <= endOffset)
      return buf.getUnsignedMediumLE(index - offset);
    if (order() == ByteOrder.BIG_ENDIAN) {
      return _getShortLE(index) & 0xFFFF | (_getByte(index + 2) & 0xFF) << 16;
    }
    return (_getShortLE(index) & 0xFFFF) << 8 | _getByte(index + 2) & 0xFF;
  }
  

  protected int _getInt(int index)
  {
    Component c = findComponent(index);
    if (index + 4 <= endOffset)
      return buf.getInt(index - offset);
    if (order() == ByteOrder.BIG_ENDIAN) {
      return (_getShort(index) & 0xFFFF) << 16 | _getShort(index + 2) & 0xFFFF;
    }
    return _getShort(index) & 0xFFFF | (_getShort(index + 2) & 0xFFFF) << 16;
  }
  

  protected int _getIntLE(int index)
  {
    Component c = findComponent(index);
    if (index + 4 <= endOffset)
      return buf.getIntLE(index - offset);
    if (order() == ByteOrder.BIG_ENDIAN) {
      return _getShortLE(index) & 0xFFFF | (_getShortLE(index + 2) & 0xFFFF) << 16;
    }
    return (_getShortLE(index) & 0xFFFF) << 16 | _getShortLE(index + 2) & 0xFFFF;
  }
  

  protected long _getLong(int index)
  {
    Component c = findComponent(index);
    if (index + 8 <= endOffset)
      return buf.getLong(index - offset);
    if (order() == ByteOrder.BIG_ENDIAN) {
      return (_getInt(index) & 0xFFFFFFFF) << 32 | _getInt(index + 4) & 0xFFFFFFFF;
    }
    return _getInt(index) & 0xFFFFFFFF | (_getInt(index + 4) & 0xFFFFFFFF) << 32;
  }
  

  protected long _getLongLE(int index)
  {
    Component c = findComponent(index);
    if (index + 8 <= endOffset)
      return buf.getLongLE(index - offset);
    if (order() == ByteOrder.BIG_ENDIAN) {
      return _getIntLE(index) & 0xFFFFFFFF | (_getIntLE(index + 4) & 0xFFFFFFFF) << 32;
    }
    return (_getIntLE(index) & 0xFFFFFFFF) << 32 | _getIntLE(index + 4) & 0xFFFFFFFF;
  }
  

  public ByteBuf getBytes(int index, byte[] dst, int dstIndex, int length)
  {
    checkDstIndex(index, length, dstIndex, dst.length);
    if (length == 0) {
      return this;
    }
    
    Component c = findComponent(index);
    int i = index;
    int adjustment = offset;
    ByteBuf s = buf;
    for (;;) {
      int localLength = Math.min(length, s.readableBytes() - (index - adjustment));
      s.getBytes(index - adjustment, dst, dstIndex, localLength);
      index += localLength;
      dstIndex += localLength;
      length -= localLength;
      adjustment += s.readableBytes();
      if (length <= 0) {
        break;
      }
      s = buffer(++i);
    }
    return this;
  }
  
  public ByteBuf getBytes(int index, ByteBuffer dst)
  {
    int limit = dst.limit();
    int length = dst.remaining();
    
    checkIndex(index, length);
    if (length == 0) {
      return this;
    }
    try
    {
      Component c = findComponent(index);
      int i = index;
      int adjustment = offset;
      ByteBuf s = buf;
      for (;;) {
        int localLength = Math.min(length, s.readableBytes() - (index - adjustment));
        dst.limit(dst.position() + localLength);
        s.getBytes(index - adjustment, dst);
        index += localLength;
        length -= localLength;
        adjustment += s.readableBytes();
        if (length <= 0) {
          break;
        }
        s = buffer(++i);
      }
    } finally {
      dst.limit(limit);
    }
    return this;
  }
  
  public ByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length)
  {
    checkDstIndex(index, length, dstIndex, dst.capacity());
    if (length == 0) {
      return this;
    }
    
    Component c = findComponent(index);
    int i = index;
    int adjustment = offset;
    ByteBuf s = buf;
    for (;;) {
      int localLength = Math.min(length, s.readableBytes() - (index - adjustment));
      s.getBytes(index - adjustment, dst, dstIndex, localLength);
      index += localLength;
      dstIndex += localLength;
      length -= localLength;
      adjustment += s.readableBytes();
      if (length <= 0) {
        break;
      }
      s = buffer(++i);
    }
    return this;
  }
  
  public int getBytes(int index, GatheringByteChannel out, int length)
    throws IOException
  {
    int count = nioBufferCount();
    if (count == 1) {
      return out.write(internalNioBuffer(index, length));
    }
    long writtenBytes = out.write(nioBuffers(index, length));
    if (writtenBytes > 2147483647L) {
      return Integer.MAX_VALUE;
    }
    return (int)writtenBytes;
  }
  


  public int getBytes(int index, FileChannel out, long position, int length)
    throws IOException
  {
    int count = nioBufferCount();
    if (count == 1) {
      return out.write(internalNioBuffer(index, length), position);
    }
    long writtenBytes = 0L;
    for (ByteBuffer buf : nioBuffers(index, length)) {
      writtenBytes += out.write(buf, position + writtenBytes);
    }
    if (writtenBytes > 2147483647L) {
      return Integer.MAX_VALUE;
    }
    return (int)writtenBytes;
  }
  

  public ByteBuf getBytes(int index, OutputStream out, int length)
    throws IOException
  {
    checkIndex(index, length);
    if (length == 0) {
      return this;
    }
    
    Component c = findComponent(index);
    int i = index;
    int adjustment = offset;
    ByteBuf s = buf;
    for (;;) {
      int localLength = Math.min(length, s.readableBytes() - (index - adjustment));
      s.getBytes(index - adjustment, out, localLength);
      index += localLength;
      length -= localLength;
      adjustment += s.readableBytes();
      if (length <= 0) {
        break;
      }
      s = buffer(++i);
    }
    return this;
  }
  
  public ByteBuf copy(int index, int length)
  {
    checkIndex(index, length);
    boolean release = true;
    ByteBuf buf = alloc().buffer(length);
    try {
      buf.writeBytes(this, index, length);
      release = false;
      return buf;
    } finally {
      if (release) {
        buf.release();
      }
    }
  }
  
  public int nioBufferCount()
  {
    return nioBufferCount;
  }
  
  public ByteBuffer nioBuffer(int index, int length)
  {
    checkIndex(index, length);
    if (this.buffers.length == 1) {
      ByteBuf buf = buffer(0);
      if (buf.nioBufferCount() == 1) {
        return buf.nioBuffer(index, length);
      }
    }
    ByteBuffer merged = ByteBuffer.allocate(length).order(order());
    ByteBuffer[] buffers = nioBuffers(index, length);
    

    for (int i = 0; i < buffers.length; i++) {
      merged.put(buffers[i]);
    }
    
    merged.flip();
    return merged;
  }
  
  public ByteBuffer internalNioBuffer(int index, int length)
  {
    if (buffers.length == 1) {
      return buffer(0).internalNioBuffer(index, length);
    }
    throw new UnsupportedOperationException();
  }
  
  public ByteBuffer[] nioBuffers(int index, int length)
  {
    checkIndex(index, length);
    if (length == 0) {
      return EmptyArrays.EMPTY_BYTE_BUFFERS;
    }
    
    RecyclableArrayList array = RecyclableArrayList.newInstance(buffers.length);
    try {
      Component c = findComponent(index);
      int i = index;
      int adjustment = offset;
      ByteBuf s = buf;
      int localLength;
      for (;;) { localLength = Math.min(length, s.readableBytes() - (index - adjustment));
        switch (s.nioBufferCount()) {
        case 0: 
          throw new UnsupportedOperationException();
        case 1: 
          array.add(s.nioBuffer(index - adjustment, localLength));
          break;
        default: 
          Collections.addAll(array, s.nioBuffers(index - adjustment, localLength));
        }
        
        index += localLength;
        length -= localLength;
        adjustment += s.readableBytes();
        if (length <= 0) {
          break;
        }
        s = buffer(++i);
      }
      
      return (ByteBuffer[])array.toArray(new ByteBuffer[0]);
    } finally {
      array.recycle();
    }
  }
  
  public boolean hasArray()
  {
    switch (buffers.length) {
    case 0: 
      return true;
    case 1: 
      return buffer(0).hasArray();
    }
    return false;
  }
  

  public byte[] array()
  {
    switch (buffers.length) {
    case 0: 
      return EmptyArrays.EMPTY_BYTES;
    case 1: 
      return buffer(0).array();
    }
    throw new UnsupportedOperationException();
  }
  

  public int arrayOffset()
  {
    switch (buffers.length) {
    case 0: 
      return 0;
    case 1: 
      return buffer(0).arrayOffset();
    }
    throw new UnsupportedOperationException();
  }
  

  public boolean hasMemoryAddress()
  {
    switch (buffers.length) {
    case 0: 
      return Unpooled.EMPTY_BUFFER.hasMemoryAddress();
    case 1: 
      return buffer(0).hasMemoryAddress();
    }
    return false;
  }
  

  public long memoryAddress()
  {
    switch (buffers.length) {
    case 0: 
      return Unpooled.EMPTY_BUFFER.memoryAddress();
    case 1: 
      return buffer(0).memoryAddress();
    }
    throw new UnsupportedOperationException();
  }
  

  protected void deallocate()
  {
    for (int i = 0; i < buffers.length; i++) {
      buffer(i).release();
    }
  }
  
  public String toString()
  {
    String result = super.toString();
    result = result.substring(0, result.length() - 1);
    return result + ", components=" + buffers.length + ')';
  }
  
  private static final class Component extends WrappedByteBuf {
    private final int index;
    private final int offset;
    private final int endOffset;
    
    Component(int index, int offset, ByteBuf buf) {
      super();
      this.index = index;
      this.offset = offset;
      endOffset = (offset + buf.readableBytes());
    }
  }
}
