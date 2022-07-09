package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.SwappedByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ByteProcessor;
import io.netty.util.Signal;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;



















final class ReplayingDecoderByteBuf
  extends ByteBuf
{
  private static final Signal REPLAY = ReplayingDecoder.REPLAY;
  
  private ByteBuf buffer;
  
  private boolean terminated;
  private SwappedByteBuf swapped;
  static final ReplayingDecoderByteBuf EMPTY_BUFFER = new ReplayingDecoderByteBuf(Unpooled.EMPTY_BUFFER);
  
  static {
    EMPTY_BUFFER.terminate();
  }
  

  ReplayingDecoderByteBuf(ByteBuf buffer)
  {
    setCumulation(buffer);
  }
  
  void setCumulation(ByteBuf buffer) {
    this.buffer = buffer;
  }
  
  void terminate() {
    terminated = true;
  }
  
  public int capacity()
  {
    if (terminated) {
      return buffer.capacity();
    }
    return Integer.MAX_VALUE;
  }
  

  public ByteBuf capacity(int newCapacity)
  {
    throw reject();
  }
  
  public int maxCapacity()
  {
    return capacity();
  }
  
  public ByteBufAllocator alloc()
  {
    return buffer.alloc();
  }
  
  public boolean isReadOnly()
  {
    return false;
  }
  

  public ByteBuf asReadOnly()
  {
    return Unpooled.unmodifiableBuffer(this);
  }
  
  public boolean isDirect()
  {
    return buffer.isDirect();
  }
  
  public boolean hasArray()
  {
    return false;
  }
  
  public byte[] array()
  {
    throw new UnsupportedOperationException();
  }
  
  public int arrayOffset()
  {
    throw new UnsupportedOperationException();
  }
  
  public boolean hasMemoryAddress()
  {
    return false;
  }
  
  public long memoryAddress()
  {
    throw new UnsupportedOperationException();
  }
  
  public ByteBuf clear()
  {
    throw reject();
  }
  
  public boolean equals(Object obj)
  {
    return this == obj;
  }
  
  public int compareTo(ByteBuf buffer)
  {
    throw reject();
  }
  
  public ByteBuf copy()
  {
    throw reject();
  }
  
  public ByteBuf copy(int index, int length)
  {
    checkIndex(index, length);
    return buffer.copy(index, length);
  }
  
  public ByteBuf discardReadBytes()
  {
    throw reject();
  }
  
  public ByteBuf ensureWritable(int writableBytes)
  {
    throw reject();
  }
  
  public int ensureWritable(int minWritableBytes, boolean force)
  {
    throw reject();
  }
  
  public ByteBuf duplicate()
  {
    throw reject();
  }
  
  public ByteBuf retainedDuplicate()
  {
    throw reject();
  }
  
  public boolean getBoolean(int index)
  {
    checkIndex(index, 1);
    return buffer.getBoolean(index);
  }
  
  public byte getByte(int index)
  {
    checkIndex(index, 1);
    return buffer.getByte(index);
  }
  
  public short getUnsignedByte(int index)
  {
    checkIndex(index, 1);
    return buffer.getUnsignedByte(index);
  }
  
  public ByteBuf getBytes(int index, byte[] dst, int dstIndex, int length)
  {
    checkIndex(index, length);
    buffer.getBytes(index, dst, dstIndex, length);
    return this;
  }
  
  public ByteBuf getBytes(int index, byte[] dst)
  {
    checkIndex(index, dst.length);
    buffer.getBytes(index, dst);
    return this;
  }
  
  public ByteBuf getBytes(int index, ByteBuffer dst)
  {
    throw reject();
  }
  
  public ByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length)
  {
    checkIndex(index, length);
    buffer.getBytes(index, dst, dstIndex, length);
    return this;
  }
  
  public ByteBuf getBytes(int index, ByteBuf dst, int length)
  {
    throw reject();
  }
  
  public ByteBuf getBytes(int index, ByteBuf dst)
  {
    throw reject();
  }
  
  public int getBytes(int index, GatheringByteChannel out, int length)
  {
    throw reject();
  }
  
  public int getBytes(int index, FileChannel out, long position, int length)
  {
    throw reject();
  }
  
  public ByteBuf getBytes(int index, OutputStream out, int length)
  {
    throw reject();
  }
  
  public int getInt(int index)
  {
    checkIndex(index, 4);
    return buffer.getInt(index);
  }
  
  public int getIntLE(int index)
  {
    checkIndex(index, 4);
    return buffer.getIntLE(index);
  }
  
  public long getUnsignedInt(int index)
  {
    checkIndex(index, 4);
    return buffer.getUnsignedInt(index);
  }
  
  public long getUnsignedIntLE(int index)
  {
    checkIndex(index, 4);
    return buffer.getUnsignedIntLE(index);
  }
  
  public long getLong(int index)
  {
    checkIndex(index, 8);
    return buffer.getLong(index);
  }
  
  public long getLongLE(int index)
  {
    checkIndex(index, 8);
    return buffer.getLongLE(index);
  }
  
  public int getMedium(int index)
  {
    checkIndex(index, 3);
    return buffer.getMedium(index);
  }
  
  public int getMediumLE(int index)
  {
    checkIndex(index, 3);
    return buffer.getMediumLE(index);
  }
  
  public int getUnsignedMedium(int index)
  {
    checkIndex(index, 3);
    return buffer.getUnsignedMedium(index);
  }
  
  public int getUnsignedMediumLE(int index)
  {
    checkIndex(index, 3);
    return buffer.getUnsignedMediumLE(index);
  }
  
  public short getShort(int index)
  {
    checkIndex(index, 2);
    return buffer.getShort(index);
  }
  
  public short getShortLE(int index)
  {
    checkIndex(index, 2);
    return buffer.getShortLE(index);
  }
  
  public int getUnsignedShort(int index)
  {
    checkIndex(index, 2);
    return buffer.getUnsignedShort(index);
  }
  
  public int getUnsignedShortLE(int index)
  {
    checkIndex(index, 2);
    return buffer.getUnsignedShortLE(index);
  }
  
  public char getChar(int index)
  {
    checkIndex(index, 2);
    return buffer.getChar(index);
  }
  
  public float getFloat(int index)
  {
    checkIndex(index, 4);
    return buffer.getFloat(index);
  }
  
  public double getDouble(int index)
  {
    checkIndex(index, 8);
    return buffer.getDouble(index);
  }
  
  public CharSequence getCharSequence(int index, int length, Charset charset)
  {
    checkIndex(index, length);
    return buffer.getCharSequence(index, length, charset);
  }
  
  public int hashCode()
  {
    throw reject();
  }
  
  public int indexOf(int fromIndex, int toIndex, byte value)
  {
    if (fromIndex == toIndex) {
      return -1;
    }
    
    if (Math.max(fromIndex, toIndex) > buffer.writerIndex()) {
      throw REPLAY;
    }
    
    return buffer.indexOf(fromIndex, toIndex, value);
  }
  
  public int bytesBefore(byte value)
  {
    int bytes = buffer.bytesBefore(value);
    if (bytes < 0) {
      throw REPLAY;
    }
    return bytes;
  }
  
  public int bytesBefore(int length, byte value)
  {
    return bytesBefore(buffer.readerIndex(), length, value);
  }
  
  public int bytesBefore(int index, int length, byte value)
  {
    int writerIndex = buffer.writerIndex();
    if (index >= writerIndex) {
      throw REPLAY;
    }
    
    if (index <= writerIndex - length) {
      return buffer.bytesBefore(index, length, value);
    }
    
    int res = buffer.bytesBefore(index, writerIndex - index, value);
    if (res < 0) {
      throw REPLAY;
    }
    return res;
  }
  

  public int forEachByte(ByteProcessor processor)
  {
    int ret = buffer.forEachByte(processor);
    if (ret < 0) {
      throw REPLAY;
    }
    return ret;
  }
  

  public int forEachByte(int index, int length, ByteProcessor processor)
  {
    int writerIndex = buffer.writerIndex();
    if (index >= writerIndex) {
      throw REPLAY;
    }
    
    if (index <= writerIndex - length) {
      return buffer.forEachByte(index, length, processor);
    }
    
    int ret = buffer.forEachByte(index, writerIndex - index, processor);
    if (ret < 0) {
      throw REPLAY;
    }
    return ret;
  }
  

  public int forEachByteDesc(ByteProcessor processor)
  {
    if (terminated) {
      return buffer.forEachByteDesc(processor);
    }
    throw reject();
  }
  

  public int forEachByteDesc(int index, int length, ByteProcessor processor)
  {
    if (index + length > buffer.writerIndex()) {
      throw REPLAY;
    }
    
    return buffer.forEachByteDesc(index, length, processor);
  }
  
  public ByteBuf markReaderIndex()
  {
    buffer.markReaderIndex();
    return this;
  }
  
  public ByteBuf markWriterIndex()
  {
    throw reject();
  }
  
  public ByteOrder order()
  {
    return buffer.order();
  }
  
  public ByteBuf order(ByteOrder endianness)
  {
    if (ObjectUtil.checkNotNull(endianness, "endianness") == order()) {
      return this;
    }
    
    SwappedByteBuf swapped = this.swapped;
    if (swapped == null) {
      this.swapped = (swapped = new SwappedByteBuf(this));
    }
    return swapped;
  }
  
  public boolean isReadable()
  {
    return (!terminated) || (buffer.isReadable());
  }
  
  public boolean isReadable(int size)
  {
    return (!terminated) || (buffer.isReadable(size));
  }
  
  public int readableBytes()
  {
    if (terminated) {
      return buffer.readableBytes();
    }
    return Integer.MAX_VALUE - buffer.readerIndex();
  }
  

  public boolean readBoolean()
  {
    checkReadableBytes(1);
    return buffer.readBoolean();
  }
  
  public byte readByte()
  {
    checkReadableBytes(1);
    return buffer.readByte();
  }
  
  public short readUnsignedByte()
  {
    checkReadableBytes(1);
    return buffer.readUnsignedByte();
  }
  
  public ByteBuf readBytes(byte[] dst, int dstIndex, int length)
  {
    checkReadableBytes(length);
    buffer.readBytes(dst, dstIndex, length);
    return this;
  }
  
  public ByteBuf readBytes(byte[] dst)
  {
    checkReadableBytes(dst.length);
    buffer.readBytes(dst);
    return this;
  }
  
  public ByteBuf readBytes(ByteBuffer dst)
  {
    throw reject();
  }
  
  public ByteBuf readBytes(ByteBuf dst, int dstIndex, int length)
  {
    checkReadableBytes(length);
    buffer.readBytes(dst, dstIndex, length);
    return this;
  }
  
  public ByteBuf readBytes(ByteBuf dst, int length)
  {
    throw reject();
  }
  
  public ByteBuf readBytes(ByteBuf dst)
  {
    checkReadableBytes(dst.writableBytes());
    buffer.readBytes(dst);
    return this;
  }
  
  public int readBytes(GatheringByteChannel out, int length)
  {
    throw reject();
  }
  
  public int readBytes(FileChannel out, long position, int length)
  {
    throw reject();
  }
  
  public ByteBuf readBytes(int length)
  {
    checkReadableBytes(length);
    return buffer.readBytes(length);
  }
  
  public ByteBuf readSlice(int length)
  {
    checkReadableBytes(length);
    return buffer.readSlice(length);
  }
  
  public ByteBuf readRetainedSlice(int length)
  {
    checkReadableBytes(length);
    return buffer.readRetainedSlice(length);
  }
  
  public ByteBuf readBytes(OutputStream out, int length)
  {
    throw reject();
  }
  
  public int readerIndex()
  {
    return buffer.readerIndex();
  }
  
  public ByteBuf readerIndex(int readerIndex)
  {
    buffer.readerIndex(readerIndex);
    return this;
  }
  
  public int readInt()
  {
    checkReadableBytes(4);
    return buffer.readInt();
  }
  
  public int readIntLE()
  {
    checkReadableBytes(4);
    return buffer.readIntLE();
  }
  
  public long readUnsignedInt()
  {
    checkReadableBytes(4);
    return buffer.readUnsignedInt();
  }
  
  public long readUnsignedIntLE()
  {
    checkReadableBytes(4);
    return buffer.readUnsignedIntLE();
  }
  
  public long readLong()
  {
    checkReadableBytes(8);
    return buffer.readLong();
  }
  
  public long readLongLE()
  {
    checkReadableBytes(8);
    return buffer.readLongLE();
  }
  
  public int readMedium()
  {
    checkReadableBytes(3);
    return buffer.readMedium();
  }
  
  public int readMediumLE()
  {
    checkReadableBytes(3);
    return buffer.readMediumLE();
  }
  
  public int readUnsignedMedium()
  {
    checkReadableBytes(3);
    return buffer.readUnsignedMedium();
  }
  
  public int readUnsignedMediumLE()
  {
    checkReadableBytes(3);
    return buffer.readUnsignedMediumLE();
  }
  
  public short readShort()
  {
    checkReadableBytes(2);
    return buffer.readShort();
  }
  
  public short readShortLE()
  {
    checkReadableBytes(2);
    return buffer.readShortLE();
  }
  
  public int readUnsignedShort()
  {
    checkReadableBytes(2);
    return buffer.readUnsignedShort();
  }
  
  public int readUnsignedShortLE()
  {
    checkReadableBytes(2);
    return buffer.readUnsignedShortLE();
  }
  
  public char readChar()
  {
    checkReadableBytes(2);
    return buffer.readChar();
  }
  
  public float readFloat()
  {
    checkReadableBytes(4);
    return buffer.readFloat();
  }
  
  public double readDouble()
  {
    checkReadableBytes(8);
    return buffer.readDouble();
  }
  
  public CharSequence readCharSequence(int length, Charset charset)
  {
    checkReadableBytes(length);
    return buffer.readCharSequence(length, charset);
  }
  
  public ByteBuf resetReaderIndex()
  {
    buffer.resetReaderIndex();
    return this;
  }
  
  public ByteBuf resetWriterIndex()
  {
    throw reject();
  }
  
  public ByteBuf setBoolean(int index, boolean value)
  {
    throw reject();
  }
  
  public ByteBuf setByte(int index, int value)
  {
    throw reject();
  }
  
  public ByteBuf setBytes(int index, byte[] src, int srcIndex, int length)
  {
    throw reject();
  }
  
  public ByteBuf setBytes(int index, byte[] src)
  {
    throw reject();
  }
  
  public ByteBuf setBytes(int index, ByteBuffer src)
  {
    throw reject();
  }
  
  public ByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length)
  {
    throw reject();
  }
  
  public ByteBuf setBytes(int index, ByteBuf src, int length)
  {
    throw reject();
  }
  
  public ByteBuf setBytes(int index, ByteBuf src)
  {
    throw reject();
  }
  
  public int setBytes(int index, InputStream in, int length)
  {
    throw reject();
  }
  
  public ByteBuf setZero(int index, int length)
  {
    throw reject();
  }
  
  public int setBytes(int index, ScatteringByteChannel in, int length)
  {
    throw reject();
  }
  
  public int setBytes(int index, FileChannel in, long position, int length)
  {
    throw reject();
  }
  
  public ByteBuf setIndex(int readerIndex, int writerIndex)
  {
    throw reject();
  }
  
  public ByteBuf setInt(int index, int value)
  {
    throw reject();
  }
  
  public ByteBuf setIntLE(int index, int value)
  {
    throw reject();
  }
  
  public ByteBuf setLong(int index, long value)
  {
    throw reject();
  }
  
  public ByteBuf setLongLE(int index, long value)
  {
    throw reject();
  }
  
  public ByteBuf setMedium(int index, int value)
  {
    throw reject();
  }
  
  public ByteBuf setMediumLE(int index, int value)
  {
    throw reject();
  }
  
  public ByteBuf setShort(int index, int value)
  {
    throw reject();
  }
  
  public ByteBuf setShortLE(int index, int value)
  {
    throw reject();
  }
  
  public ByteBuf setChar(int index, int value)
  {
    throw reject();
  }
  
  public ByteBuf setFloat(int index, float value)
  {
    throw reject();
  }
  
  public ByteBuf setDouble(int index, double value)
  {
    throw reject();
  }
  
  public ByteBuf skipBytes(int length)
  {
    checkReadableBytes(length);
    buffer.skipBytes(length);
    return this;
  }
  
  public ByteBuf slice()
  {
    throw reject();
  }
  
  public ByteBuf retainedSlice()
  {
    throw reject();
  }
  
  public ByteBuf slice(int index, int length)
  {
    checkIndex(index, length);
    return buffer.slice(index, length);
  }
  
  public ByteBuf retainedSlice(int index, int length)
  {
    checkIndex(index, length);
    return buffer.slice(index, length);
  }
  
  public int nioBufferCount()
  {
    return buffer.nioBufferCount();
  }
  
  public ByteBuffer nioBuffer()
  {
    throw reject();
  }
  
  public ByteBuffer nioBuffer(int index, int length)
  {
    checkIndex(index, length);
    return buffer.nioBuffer(index, length);
  }
  
  public ByteBuffer[] nioBuffers()
  {
    throw reject();
  }
  
  public ByteBuffer[] nioBuffers(int index, int length)
  {
    checkIndex(index, length);
    return buffer.nioBuffers(index, length);
  }
  
  public ByteBuffer internalNioBuffer(int index, int length)
  {
    checkIndex(index, length);
    return buffer.internalNioBuffer(index, length);
  }
  
  public String toString(int index, int length, Charset charset)
  {
    checkIndex(index, length);
    return buffer.toString(index, length, charset);
  }
  
  public String toString(Charset charsetName)
  {
    throw reject();
  }
  
  public String toString()
  {
    return 
    



      StringUtil.simpleClassName(this) + '(' + "ridx=" + readerIndex() + ", widx=" + writerIndex() + ')';
  }
  

  public boolean isWritable()
  {
    return false;
  }
  
  public boolean isWritable(int size)
  {
    return false;
  }
  
  public int writableBytes()
  {
    return 0;
  }
  
  public int maxWritableBytes()
  {
    return 0;
  }
  
  public ByteBuf writeBoolean(boolean value)
  {
    throw reject();
  }
  
  public ByteBuf writeByte(int value)
  {
    throw reject();
  }
  
  public ByteBuf writeBytes(byte[] src, int srcIndex, int length)
  {
    throw reject();
  }
  
  public ByteBuf writeBytes(byte[] src)
  {
    throw reject();
  }
  
  public ByteBuf writeBytes(ByteBuffer src)
  {
    throw reject();
  }
  
  public ByteBuf writeBytes(ByteBuf src, int srcIndex, int length)
  {
    throw reject();
  }
  
  public ByteBuf writeBytes(ByteBuf src, int length)
  {
    throw reject();
  }
  
  public ByteBuf writeBytes(ByteBuf src)
  {
    throw reject();
  }
  
  public int writeBytes(InputStream in, int length)
  {
    throw reject();
  }
  
  public int writeBytes(ScatteringByteChannel in, int length)
  {
    throw reject();
  }
  
  public int writeBytes(FileChannel in, long position, int length)
  {
    throw reject();
  }
  
  public ByteBuf writeInt(int value)
  {
    throw reject();
  }
  
  public ByteBuf writeIntLE(int value)
  {
    throw reject();
  }
  
  public ByteBuf writeLong(long value)
  {
    throw reject();
  }
  
  public ByteBuf writeLongLE(long value)
  {
    throw reject();
  }
  
  public ByteBuf writeMedium(int value)
  {
    throw reject();
  }
  
  public ByteBuf writeMediumLE(int value)
  {
    throw reject();
  }
  
  public ByteBuf writeZero(int length)
  {
    throw reject();
  }
  
  public int writerIndex()
  {
    return buffer.writerIndex();
  }
  
  public ByteBuf writerIndex(int writerIndex)
  {
    throw reject();
  }
  
  public ByteBuf writeShort(int value)
  {
    throw reject();
  }
  
  public ByteBuf writeShortLE(int value)
  {
    throw reject();
  }
  
  public ByteBuf writeChar(int value)
  {
    throw reject();
  }
  
  public ByteBuf writeFloat(float value)
  {
    throw reject();
  }
  
  public ByteBuf writeDouble(double value)
  {
    throw reject();
  }
  
  public int setCharSequence(int index, CharSequence sequence, Charset charset)
  {
    throw reject();
  }
  
  public int writeCharSequence(CharSequence sequence, Charset charset)
  {
    throw reject();
  }
  
  private void checkIndex(int index, int length) {
    if (index + length > buffer.writerIndex()) {
      throw REPLAY;
    }
  }
  
  private void checkReadableBytes(int readableBytes) {
    if (buffer.readableBytes() < readableBytes) {
      throw REPLAY;
    }
  }
  
  public ByteBuf discardSomeReadBytes()
  {
    throw reject();
  }
  
  public int refCnt()
  {
    return buffer.refCnt();
  }
  
  public ByteBuf retain()
  {
    throw reject();
  }
  
  public ByteBuf retain(int increment)
  {
    throw reject();
  }
  
  public ByteBuf touch()
  {
    buffer.touch();
    return this;
  }
  
  public ByteBuf touch(Object hint)
  {
    buffer.touch(hint);
    return this;
  }
  
  public boolean release()
  {
    throw reject();
  }
  
  public boolean release(int decrement)
  {
    throw reject();
  }
  
  public ByteBuf unwrap()
  {
    throw reject();
  }
  
  private static UnsupportedOperationException reject() {
    return new UnsupportedOperationException("not a replayable operation");
  }
  
  ReplayingDecoderByteBuf() {}
}
