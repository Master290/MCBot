package io.netty.buffer;

import io.netty.util.ByteProcessor;
import io.netty.util.internal.ObjectUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;





















@Deprecated
public class SwappedByteBuf
  extends ByteBuf
{
  private final ByteBuf buf;
  private final ByteOrder order;
  
  public SwappedByteBuf(ByteBuf buf)
  {
    this.buf = ((ByteBuf)ObjectUtil.checkNotNull(buf, "buf"));
    if (buf.order() == ByteOrder.BIG_ENDIAN) {
      order = ByteOrder.LITTLE_ENDIAN;
    } else {
      order = ByteOrder.BIG_ENDIAN;
    }
  }
  
  public ByteOrder order()
  {
    return order;
  }
  
  public ByteBuf order(ByteOrder endianness)
  {
    if (ObjectUtil.checkNotNull(endianness, "endianness") == order) {
      return this;
    }
    return buf;
  }
  
  public ByteBuf unwrap()
  {
    return buf;
  }
  
  public ByteBufAllocator alloc()
  {
    return buf.alloc();
  }
  
  public int capacity()
  {
    return buf.capacity();
  }
  
  public ByteBuf capacity(int newCapacity)
  {
    buf.capacity(newCapacity);
    return this;
  }
  
  public int maxCapacity()
  {
    return buf.maxCapacity();
  }
  
  public boolean isReadOnly()
  {
    return buf.isReadOnly();
  }
  
  public ByteBuf asReadOnly()
  {
    return Unpooled.unmodifiableBuffer(this);
  }
  
  public boolean isDirect()
  {
    return buf.isDirect();
  }
  
  public int readerIndex()
  {
    return buf.readerIndex();
  }
  
  public ByteBuf readerIndex(int readerIndex)
  {
    buf.readerIndex(readerIndex);
    return this;
  }
  
  public int writerIndex()
  {
    return buf.writerIndex();
  }
  
  public ByteBuf writerIndex(int writerIndex)
  {
    buf.writerIndex(writerIndex);
    return this;
  }
  
  public ByteBuf setIndex(int readerIndex, int writerIndex)
  {
    buf.setIndex(readerIndex, writerIndex);
    return this;
  }
  
  public int readableBytes()
  {
    return buf.readableBytes();
  }
  
  public int writableBytes()
  {
    return buf.writableBytes();
  }
  
  public int maxWritableBytes()
  {
    return buf.maxWritableBytes();
  }
  
  public int maxFastWritableBytes()
  {
    return buf.maxFastWritableBytes();
  }
  
  public boolean isReadable()
  {
    return buf.isReadable();
  }
  
  public boolean isReadable(int size)
  {
    return buf.isReadable(size);
  }
  
  public boolean isWritable()
  {
    return buf.isWritable();
  }
  
  public boolean isWritable(int size)
  {
    return buf.isWritable(size);
  }
  
  public ByteBuf clear()
  {
    buf.clear();
    return this;
  }
  
  public ByteBuf markReaderIndex()
  {
    buf.markReaderIndex();
    return this;
  }
  
  public ByteBuf resetReaderIndex()
  {
    buf.resetReaderIndex();
    return this;
  }
  
  public ByteBuf markWriterIndex()
  {
    buf.markWriterIndex();
    return this;
  }
  
  public ByteBuf resetWriterIndex()
  {
    buf.resetWriterIndex();
    return this;
  }
  
  public ByteBuf discardReadBytes()
  {
    buf.discardReadBytes();
    return this;
  }
  
  public ByteBuf discardSomeReadBytes()
  {
    buf.discardSomeReadBytes();
    return this;
  }
  
  public ByteBuf ensureWritable(int writableBytes)
  {
    buf.ensureWritable(writableBytes);
    return this;
  }
  
  public int ensureWritable(int minWritableBytes, boolean force)
  {
    return buf.ensureWritable(minWritableBytes, force);
  }
  
  public boolean getBoolean(int index)
  {
    return buf.getBoolean(index);
  }
  
  public byte getByte(int index)
  {
    return buf.getByte(index);
  }
  
  public short getUnsignedByte(int index)
  {
    return buf.getUnsignedByte(index);
  }
  
  public short getShort(int index)
  {
    return ByteBufUtil.swapShort(buf.getShort(index));
  }
  
  public short getShortLE(int index)
  {
    return buf.getShort(index);
  }
  
  public int getUnsignedShort(int index)
  {
    return getShort(index) & 0xFFFF;
  }
  
  public int getUnsignedShortLE(int index)
  {
    return getShortLE(index) & 0xFFFF;
  }
  
  public int getMedium(int index)
  {
    return ByteBufUtil.swapMedium(buf.getMedium(index));
  }
  
  public int getMediumLE(int index)
  {
    return buf.getMedium(index);
  }
  
  public int getUnsignedMedium(int index)
  {
    return getMedium(index) & 0xFFFFFF;
  }
  
  public int getUnsignedMediumLE(int index)
  {
    return getMediumLE(index) & 0xFFFFFF;
  }
  
  public int getInt(int index)
  {
    return ByteBufUtil.swapInt(buf.getInt(index));
  }
  
  public int getIntLE(int index)
  {
    return buf.getInt(index);
  }
  
  public long getUnsignedInt(int index)
  {
    return getInt(index) & 0xFFFFFFFF;
  }
  
  public long getUnsignedIntLE(int index)
  {
    return getIntLE(index) & 0xFFFFFFFF;
  }
  
  public long getLong(int index)
  {
    return ByteBufUtil.swapLong(buf.getLong(index));
  }
  
  public long getLongLE(int index)
  {
    return buf.getLong(index);
  }
  
  public char getChar(int index)
  {
    return (char)getShort(index);
  }
  
  public float getFloat(int index)
  {
    return Float.intBitsToFloat(getInt(index));
  }
  
  public double getDouble(int index)
  {
    return Double.longBitsToDouble(getLong(index));
  }
  
  public ByteBuf getBytes(int index, ByteBuf dst)
  {
    buf.getBytes(index, dst);
    return this;
  }
  
  public ByteBuf getBytes(int index, ByteBuf dst, int length)
  {
    buf.getBytes(index, dst, length);
    return this;
  }
  
  public ByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length)
  {
    buf.getBytes(index, dst, dstIndex, length);
    return this;
  }
  
  public ByteBuf getBytes(int index, byte[] dst)
  {
    buf.getBytes(index, dst);
    return this;
  }
  
  public ByteBuf getBytes(int index, byte[] dst, int dstIndex, int length)
  {
    buf.getBytes(index, dst, dstIndex, length);
    return this;
  }
  
  public ByteBuf getBytes(int index, ByteBuffer dst)
  {
    buf.getBytes(index, dst);
    return this;
  }
  
  public ByteBuf getBytes(int index, OutputStream out, int length) throws IOException
  {
    buf.getBytes(index, out, length);
    return this;
  }
  
  public int getBytes(int index, GatheringByteChannel out, int length) throws IOException
  {
    return buf.getBytes(index, out, length);
  }
  
  public int getBytes(int index, FileChannel out, long position, int length) throws IOException
  {
    return buf.getBytes(index, out, position, length);
  }
  
  public CharSequence getCharSequence(int index, int length, Charset charset)
  {
    return buf.getCharSequence(index, length, charset);
  }
  
  public ByteBuf setBoolean(int index, boolean value)
  {
    buf.setBoolean(index, value);
    return this;
  }
  
  public ByteBuf setByte(int index, int value)
  {
    buf.setByte(index, value);
    return this;
  }
  
  public ByteBuf setShort(int index, int value)
  {
    buf.setShort(index, ByteBufUtil.swapShort((short)value));
    return this;
  }
  
  public ByteBuf setShortLE(int index, int value)
  {
    buf.setShort(index, (short)value);
    return this;
  }
  
  public ByteBuf setMedium(int index, int value)
  {
    buf.setMedium(index, ByteBufUtil.swapMedium(value));
    return this;
  }
  
  public ByteBuf setMediumLE(int index, int value)
  {
    buf.setMedium(index, value);
    return this;
  }
  
  public ByteBuf setInt(int index, int value)
  {
    buf.setInt(index, ByteBufUtil.swapInt(value));
    return this;
  }
  
  public ByteBuf setIntLE(int index, int value)
  {
    buf.setInt(index, value);
    return this;
  }
  
  public ByteBuf setLong(int index, long value)
  {
    buf.setLong(index, ByteBufUtil.swapLong(value));
    return this;
  }
  
  public ByteBuf setLongLE(int index, long value)
  {
    buf.setLong(index, value);
    return this;
  }
  
  public ByteBuf setChar(int index, int value)
  {
    setShort(index, value);
    return this;
  }
  
  public ByteBuf setFloat(int index, float value)
  {
    setInt(index, Float.floatToRawIntBits(value));
    return this;
  }
  
  public ByteBuf setDouble(int index, double value)
  {
    setLong(index, Double.doubleToRawLongBits(value));
    return this;
  }
  
  public ByteBuf setBytes(int index, ByteBuf src)
  {
    buf.setBytes(index, src);
    return this;
  }
  
  public ByteBuf setBytes(int index, ByteBuf src, int length)
  {
    buf.setBytes(index, src, length);
    return this;
  }
  
  public ByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length)
  {
    buf.setBytes(index, src, srcIndex, length);
    return this;
  }
  
  public ByteBuf setBytes(int index, byte[] src)
  {
    buf.setBytes(index, src);
    return this;
  }
  
  public ByteBuf setBytes(int index, byte[] src, int srcIndex, int length)
  {
    buf.setBytes(index, src, srcIndex, length);
    return this;
  }
  
  public ByteBuf setBytes(int index, ByteBuffer src)
  {
    buf.setBytes(index, src);
    return this;
  }
  
  public int setBytes(int index, InputStream in, int length) throws IOException
  {
    return buf.setBytes(index, in, length);
  }
  
  public int setBytes(int index, ScatteringByteChannel in, int length) throws IOException
  {
    return buf.setBytes(index, in, length);
  }
  
  public int setBytes(int index, FileChannel in, long position, int length) throws IOException
  {
    return buf.setBytes(index, in, position, length);
  }
  
  public ByteBuf setZero(int index, int length)
  {
    buf.setZero(index, length);
    return this;
  }
  
  public int setCharSequence(int index, CharSequence sequence, Charset charset)
  {
    return buf.setCharSequence(index, sequence, charset);
  }
  
  public boolean readBoolean()
  {
    return buf.readBoolean();
  }
  
  public byte readByte()
  {
    return buf.readByte();
  }
  
  public short readUnsignedByte()
  {
    return buf.readUnsignedByte();
  }
  
  public short readShort()
  {
    return ByteBufUtil.swapShort(buf.readShort());
  }
  
  public short readShortLE()
  {
    return buf.readShortLE();
  }
  
  public int readUnsignedShort()
  {
    return readShort() & 0xFFFF;
  }
  
  public int readUnsignedShortLE()
  {
    return readShortLE() & 0xFFFF;
  }
  
  public int readMedium()
  {
    return ByteBufUtil.swapMedium(buf.readMedium());
  }
  
  public int readMediumLE()
  {
    return buf.readMediumLE();
  }
  
  public int readUnsignedMedium()
  {
    return readMedium() & 0xFFFFFF;
  }
  
  public int readUnsignedMediumLE()
  {
    return readMediumLE() & 0xFFFFFF;
  }
  
  public int readInt()
  {
    return ByteBufUtil.swapInt(buf.readInt());
  }
  
  public int readIntLE()
  {
    return buf.readIntLE();
  }
  
  public long readUnsignedInt()
  {
    return readInt() & 0xFFFFFFFF;
  }
  
  public long readUnsignedIntLE()
  {
    return readIntLE() & 0xFFFFFFFF;
  }
  
  public long readLong()
  {
    return ByteBufUtil.swapLong(buf.readLong());
  }
  
  public long readLongLE()
  {
    return buf.readLongLE();
  }
  
  public char readChar()
  {
    return (char)readShort();
  }
  
  public float readFloat()
  {
    return Float.intBitsToFloat(readInt());
  }
  
  public double readDouble()
  {
    return Double.longBitsToDouble(readLong());
  }
  
  public ByteBuf readBytes(int length)
  {
    return buf.readBytes(length).order(order());
  }
  
  public ByteBuf readSlice(int length)
  {
    return buf.readSlice(length).order(order);
  }
  
  public ByteBuf readRetainedSlice(int length)
  {
    return buf.readRetainedSlice(length).order(order);
  }
  
  public ByteBuf readBytes(ByteBuf dst)
  {
    buf.readBytes(dst);
    return this;
  }
  
  public ByteBuf readBytes(ByteBuf dst, int length)
  {
    buf.readBytes(dst, length);
    return this;
  }
  
  public ByteBuf readBytes(ByteBuf dst, int dstIndex, int length)
  {
    buf.readBytes(dst, dstIndex, length);
    return this;
  }
  
  public ByteBuf readBytes(byte[] dst)
  {
    buf.readBytes(dst);
    return this;
  }
  
  public ByteBuf readBytes(byte[] dst, int dstIndex, int length)
  {
    buf.readBytes(dst, dstIndex, length);
    return this;
  }
  
  public ByteBuf readBytes(ByteBuffer dst)
  {
    buf.readBytes(dst);
    return this;
  }
  
  public ByteBuf readBytes(OutputStream out, int length) throws IOException
  {
    buf.readBytes(out, length);
    return this;
  }
  
  public int readBytes(GatheringByteChannel out, int length) throws IOException
  {
    return buf.readBytes(out, length);
  }
  
  public int readBytes(FileChannel out, long position, int length) throws IOException
  {
    return buf.readBytes(out, position, length);
  }
  
  public CharSequence readCharSequence(int length, Charset charset)
  {
    return buf.readCharSequence(length, charset);
  }
  
  public ByteBuf skipBytes(int length)
  {
    buf.skipBytes(length);
    return this;
  }
  
  public ByteBuf writeBoolean(boolean value)
  {
    buf.writeBoolean(value);
    return this;
  }
  
  public ByteBuf writeByte(int value)
  {
    buf.writeByte(value);
    return this;
  }
  
  public ByteBuf writeShort(int value)
  {
    buf.writeShort(ByteBufUtil.swapShort((short)value));
    return this;
  }
  
  public ByteBuf writeShortLE(int value)
  {
    buf.writeShortLE((short)value);
    return this;
  }
  
  public ByteBuf writeMedium(int value)
  {
    buf.writeMedium(ByteBufUtil.swapMedium(value));
    return this;
  }
  
  public ByteBuf writeMediumLE(int value)
  {
    buf.writeMediumLE(value);
    return this;
  }
  
  public ByteBuf writeInt(int value)
  {
    buf.writeInt(ByteBufUtil.swapInt(value));
    return this;
  }
  
  public ByteBuf writeIntLE(int value)
  {
    buf.writeIntLE(value);
    return this;
  }
  
  public ByteBuf writeLong(long value)
  {
    buf.writeLong(ByteBufUtil.swapLong(value));
    return this;
  }
  
  public ByteBuf writeLongLE(long value)
  {
    buf.writeLongLE(value);
    return this;
  }
  
  public ByteBuf writeChar(int value)
  {
    writeShort(value);
    return this;
  }
  
  public ByteBuf writeFloat(float value)
  {
    writeInt(Float.floatToRawIntBits(value));
    return this;
  }
  
  public ByteBuf writeDouble(double value)
  {
    writeLong(Double.doubleToRawLongBits(value));
    return this;
  }
  
  public ByteBuf writeBytes(ByteBuf src)
  {
    buf.writeBytes(src);
    return this;
  }
  
  public ByteBuf writeBytes(ByteBuf src, int length)
  {
    buf.writeBytes(src, length);
    return this;
  }
  
  public ByteBuf writeBytes(ByteBuf src, int srcIndex, int length)
  {
    buf.writeBytes(src, srcIndex, length);
    return this;
  }
  
  public ByteBuf writeBytes(byte[] src)
  {
    buf.writeBytes(src);
    return this;
  }
  
  public ByteBuf writeBytes(byte[] src, int srcIndex, int length)
  {
    buf.writeBytes(src, srcIndex, length);
    return this;
  }
  
  public ByteBuf writeBytes(ByteBuffer src)
  {
    buf.writeBytes(src);
    return this;
  }
  
  public int writeBytes(InputStream in, int length) throws IOException
  {
    return buf.writeBytes(in, length);
  }
  
  public int writeBytes(ScatteringByteChannel in, int length) throws IOException
  {
    return buf.writeBytes(in, length);
  }
  
  public int writeBytes(FileChannel in, long position, int length) throws IOException
  {
    return buf.writeBytes(in, position, length);
  }
  
  public ByteBuf writeZero(int length)
  {
    buf.writeZero(length);
    return this;
  }
  
  public int writeCharSequence(CharSequence sequence, Charset charset)
  {
    return buf.writeCharSequence(sequence, charset);
  }
  
  public int indexOf(int fromIndex, int toIndex, byte value)
  {
    return buf.indexOf(fromIndex, toIndex, value);
  }
  
  public int bytesBefore(byte value)
  {
    return buf.bytesBefore(value);
  }
  
  public int bytesBefore(int length, byte value)
  {
    return buf.bytesBefore(length, value);
  }
  
  public int bytesBefore(int index, int length, byte value)
  {
    return buf.bytesBefore(index, length, value);
  }
  
  public int forEachByte(ByteProcessor processor)
  {
    return buf.forEachByte(processor);
  }
  
  public int forEachByte(int index, int length, ByteProcessor processor)
  {
    return buf.forEachByte(index, length, processor);
  }
  
  public int forEachByteDesc(ByteProcessor processor)
  {
    return buf.forEachByteDesc(processor);
  }
  
  public int forEachByteDesc(int index, int length, ByteProcessor processor)
  {
    return buf.forEachByteDesc(index, length, processor);
  }
  
  public ByteBuf copy()
  {
    return buf.copy().order(order);
  }
  
  public ByteBuf copy(int index, int length)
  {
    return buf.copy(index, length).order(order);
  }
  
  public ByteBuf slice()
  {
    return buf.slice().order(order);
  }
  
  public ByteBuf retainedSlice()
  {
    return buf.retainedSlice().order(order);
  }
  
  public ByteBuf slice(int index, int length)
  {
    return buf.slice(index, length).order(order);
  }
  
  public ByteBuf retainedSlice(int index, int length)
  {
    return buf.retainedSlice(index, length).order(order);
  }
  
  public ByteBuf duplicate()
  {
    return buf.duplicate().order(order);
  }
  
  public ByteBuf retainedDuplicate()
  {
    return buf.retainedDuplicate().order(order);
  }
  
  public int nioBufferCount()
  {
    return buf.nioBufferCount();
  }
  
  public ByteBuffer nioBuffer()
  {
    return buf.nioBuffer().order(order);
  }
  
  public ByteBuffer nioBuffer(int index, int length)
  {
    return buf.nioBuffer(index, length).order(order);
  }
  
  public ByteBuffer internalNioBuffer(int index, int length)
  {
    return nioBuffer(index, length);
  }
  
  public ByteBuffer[] nioBuffers()
  {
    ByteBuffer[] nioBuffers = buf.nioBuffers();
    for (int i = 0; i < nioBuffers.length; i++) {
      nioBuffers[i] = nioBuffers[i].order(order);
    }
    return nioBuffers;
  }
  
  public ByteBuffer[] nioBuffers(int index, int length)
  {
    ByteBuffer[] nioBuffers = buf.nioBuffers(index, length);
    for (int i = 0; i < nioBuffers.length; i++) {
      nioBuffers[i] = nioBuffers[i].order(order);
    }
    return nioBuffers;
  }
  
  public boolean hasArray()
  {
    return buf.hasArray();
  }
  
  public byte[] array()
  {
    return buf.array();
  }
  
  public int arrayOffset()
  {
    return buf.arrayOffset();
  }
  
  public boolean hasMemoryAddress()
  {
    return buf.hasMemoryAddress();
  }
  
  public boolean isContiguous()
  {
    return buf.isContiguous();
  }
  
  public long memoryAddress()
  {
    return buf.memoryAddress();
  }
  
  public String toString(Charset charset)
  {
    return buf.toString(charset);
  }
  
  public String toString(int index, int length, Charset charset)
  {
    return buf.toString(index, length, charset);
  }
  
  public int refCnt()
  {
    return buf.refCnt();
  }
  
  final boolean isAccessible()
  {
    return buf.isAccessible();
  }
  
  public ByteBuf retain()
  {
    buf.retain();
    return this;
  }
  
  public ByteBuf retain(int increment)
  {
    buf.retain(increment);
    return this;
  }
  
  public ByteBuf touch()
  {
    buf.touch();
    return this;
  }
  
  public ByteBuf touch(Object hint)
  {
    buf.touch(hint);
    return this;
  }
  
  public boolean release()
  {
    return buf.release();
  }
  
  public boolean release(int decrement)
  {
    return buf.release(decrement);
  }
  
  public int hashCode()
  {
    return buf.hashCode();
  }
  
  public boolean equals(Object obj)
  {
    if ((obj instanceof ByteBuf)) {
      return ByteBufUtil.equals(this, (ByteBuf)obj);
    }
    return false;
  }
  
  public int compareTo(ByteBuf buffer)
  {
    return ByteBufUtil.compare(this, buffer);
  }
  
  public String toString()
  {
    return "Swapped(" + buf + ')';
  }
}
