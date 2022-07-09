package io.netty.buffer;

import io.netty.util.ByteProcessor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;















class WrappedCompositeByteBuf
  extends CompositeByteBuf
{
  private final CompositeByteBuf wrapped;
  
  WrappedCompositeByteBuf(CompositeByteBuf wrapped)
  {
    super(wrapped.alloc());
    this.wrapped = wrapped;
  }
  
  public boolean release()
  {
    return wrapped.release();
  }
  
  public boolean release(int decrement)
  {
    return wrapped.release(decrement);
  }
  
  public final int maxCapacity()
  {
    return wrapped.maxCapacity();
  }
  
  public final int readerIndex()
  {
    return wrapped.readerIndex();
  }
  
  public final int writerIndex()
  {
    return wrapped.writerIndex();
  }
  
  public final boolean isReadable()
  {
    return wrapped.isReadable();
  }
  
  public final boolean isReadable(int numBytes)
  {
    return wrapped.isReadable(numBytes);
  }
  
  public final boolean isWritable()
  {
    return wrapped.isWritable();
  }
  
  public final boolean isWritable(int numBytes)
  {
    return wrapped.isWritable(numBytes);
  }
  
  public final int readableBytes()
  {
    return wrapped.readableBytes();
  }
  
  public final int writableBytes()
  {
    return wrapped.writableBytes();
  }
  
  public final int maxWritableBytes()
  {
    return wrapped.maxWritableBytes();
  }
  
  public int maxFastWritableBytes()
  {
    return wrapped.maxFastWritableBytes();
  }
  
  public int ensureWritable(int minWritableBytes, boolean force)
  {
    return wrapped.ensureWritable(minWritableBytes, force);
  }
  
  public ByteBuf order(ByteOrder endianness)
  {
    return wrapped.order(endianness);
  }
  
  public boolean getBoolean(int index)
  {
    return wrapped.getBoolean(index);
  }
  
  public short getUnsignedByte(int index)
  {
    return wrapped.getUnsignedByte(index);
  }
  
  public short getShort(int index)
  {
    return wrapped.getShort(index);
  }
  
  public short getShortLE(int index)
  {
    return wrapped.getShortLE(index);
  }
  
  public int getUnsignedShort(int index)
  {
    return wrapped.getUnsignedShort(index);
  }
  
  public int getUnsignedShortLE(int index)
  {
    return wrapped.getUnsignedShortLE(index);
  }
  
  public int getUnsignedMedium(int index)
  {
    return wrapped.getUnsignedMedium(index);
  }
  
  public int getUnsignedMediumLE(int index)
  {
    return wrapped.getUnsignedMediumLE(index);
  }
  
  public int getMedium(int index)
  {
    return wrapped.getMedium(index);
  }
  
  public int getMediumLE(int index)
  {
    return wrapped.getMediumLE(index);
  }
  
  public int getInt(int index)
  {
    return wrapped.getInt(index);
  }
  
  public int getIntLE(int index)
  {
    return wrapped.getIntLE(index);
  }
  
  public long getUnsignedInt(int index)
  {
    return wrapped.getUnsignedInt(index);
  }
  
  public long getUnsignedIntLE(int index)
  {
    return wrapped.getUnsignedIntLE(index);
  }
  
  public long getLong(int index)
  {
    return wrapped.getLong(index);
  }
  
  public long getLongLE(int index)
  {
    return wrapped.getLongLE(index);
  }
  
  public char getChar(int index)
  {
    return wrapped.getChar(index);
  }
  
  public float getFloat(int index)
  {
    return wrapped.getFloat(index);
  }
  
  public double getDouble(int index)
  {
    return wrapped.getDouble(index);
  }
  
  public ByteBuf setShortLE(int index, int value)
  {
    return wrapped.setShortLE(index, value);
  }
  
  public ByteBuf setMediumLE(int index, int value)
  {
    return wrapped.setMediumLE(index, value);
  }
  
  public ByteBuf setIntLE(int index, int value)
  {
    return wrapped.setIntLE(index, value);
  }
  
  public ByteBuf setLongLE(int index, long value)
  {
    return wrapped.setLongLE(index, value);
  }
  
  public byte readByte()
  {
    return wrapped.readByte();
  }
  
  public boolean readBoolean()
  {
    return wrapped.readBoolean();
  }
  
  public short readUnsignedByte()
  {
    return wrapped.readUnsignedByte();
  }
  
  public short readShort()
  {
    return wrapped.readShort();
  }
  
  public short readShortLE()
  {
    return wrapped.readShortLE();
  }
  
  public int readUnsignedShort()
  {
    return wrapped.readUnsignedShort();
  }
  
  public int readUnsignedShortLE()
  {
    return wrapped.readUnsignedShortLE();
  }
  
  public int readMedium()
  {
    return wrapped.readMedium();
  }
  
  public int readMediumLE()
  {
    return wrapped.readMediumLE();
  }
  
  public int readUnsignedMedium()
  {
    return wrapped.readUnsignedMedium();
  }
  
  public int readUnsignedMediumLE()
  {
    return wrapped.readUnsignedMediumLE();
  }
  
  public int readInt()
  {
    return wrapped.readInt();
  }
  
  public int readIntLE()
  {
    return wrapped.readIntLE();
  }
  
  public long readUnsignedInt()
  {
    return wrapped.readUnsignedInt();
  }
  
  public long readUnsignedIntLE()
  {
    return wrapped.readUnsignedIntLE();
  }
  
  public long readLong()
  {
    return wrapped.readLong();
  }
  
  public long readLongLE()
  {
    return wrapped.readLongLE();
  }
  
  public char readChar()
  {
    return wrapped.readChar();
  }
  
  public float readFloat()
  {
    return wrapped.readFloat();
  }
  
  public double readDouble()
  {
    return wrapped.readDouble();
  }
  
  public ByteBuf readBytes(int length)
  {
    return wrapped.readBytes(length);
  }
  
  public ByteBuf slice()
  {
    return wrapped.slice();
  }
  
  public ByteBuf retainedSlice()
  {
    return wrapped.retainedSlice();
  }
  
  public ByteBuf slice(int index, int length)
  {
    return wrapped.slice(index, length);
  }
  
  public ByteBuf retainedSlice(int index, int length)
  {
    return wrapped.retainedSlice(index, length);
  }
  
  public ByteBuffer nioBuffer()
  {
    return wrapped.nioBuffer();
  }
  
  public String toString(Charset charset)
  {
    return wrapped.toString(charset);
  }
  
  public String toString(int index, int length, Charset charset)
  {
    return wrapped.toString(index, length, charset);
  }
  
  public int indexOf(int fromIndex, int toIndex, byte value)
  {
    return wrapped.indexOf(fromIndex, toIndex, value);
  }
  
  public int bytesBefore(byte value)
  {
    return wrapped.bytesBefore(value);
  }
  
  public int bytesBefore(int length, byte value)
  {
    return wrapped.bytesBefore(length, value);
  }
  
  public int bytesBefore(int index, int length, byte value)
  {
    return wrapped.bytesBefore(index, length, value);
  }
  
  public int forEachByte(ByteProcessor processor)
  {
    return wrapped.forEachByte(processor);
  }
  
  public int forEachByte(int index, int length, ByteProcessor processor)
  {
    return wrapped.forEachByte(index, length, processor);
  }
  
  public int forEachByteDesc(ByteProcessor processor)
  {
    return wrapped.forEachByteDesc(processor);
  }
  
  public int forEachByteDesc(int index, int length, ByteProcessor processor)
  {
    return wrapped.forEachByteDesc(index, length, processor);
  }
  
  public final int hashCode()
  {
    return wrapped.hashCode();
  }
  
  public final boolean equals(Object o)
  {
    return wrapped.equals(o);
  }
  
  public final int compareTo(ByteBuf that)
  {
    return wrapped.compareTo(that);
  }
  
  public final int refCnt()
  {
    return wrapped.refCnt();
  }
  
  final boolean isAccessible()
  {
    return wrapped.isAccessible();
  }
  
  public ByteBuf duplicate()
  {
    return wrapped.duplicate();
  }
  
  public ByteBuf retainedDuplicate()
  {
    return wrapped.retainedDuplicate();
  }
  
  public ByteBuf readSlice(int length)
  {
    return wrapped.readSlice(length);
  }
  
  public ByteBuf readRetainedSlice(int length)
  {
    return wrapped.readRetainedSlice(length);
  }
  
  public int readBytes(GatheringByteChannel out, int length) throws IOException
  {
    return wrapped.readBytes(out, length);
  }
  
  public ByteBuf writeShortLE(int value)
  {
    return wrapped.writeShortLE(value);
  }
  
  public ByteBuf writeMediumLE(int value)
  {
    return wrapped.writeMediumLE(value);
  }
  
  public ByteBuf writeIntLE(int value)
  {
    return wrapped.writeIntLE(value);
  }
  
  public ByteBuf writeLongLE(long value)
  {
    return wrapped.writeLongLE(value);
  }
  
  public int writeBytes(InputStream in, int length) throws IOException
  {
    return wrapped.writeBytes(in, length);
  }
  
  public int writeBytes(ScatteringByteChannel in, int length) throws IOException
  {
    return wrapped.writeBytes(in, length);
  }
  
  public ByteBuf copy()
  {
    return wrapped.copy();
  }
  
  public CompositeByteBuf addComponent(ByteBuf buffer)
  {
    wrapped.addComponent(buffer);
    return this;
  }
  
  public CompositeByteBuf addComponents(ByteBuf... buffers)
  {
    wrapped.addComponents(buffers);
    return this;
  }
  
  public CompositeByteBuf addComponents(Iterable<ByteBuf> buffers)
  {
    wrapped.addComponents(buffers);
    return this;
  }
  
  public CompositeByteBuf addComponent(int cIndex, ByteBuf buffer)
  {
    wrapped.addComponent(cIndex, buffer);
    return this;
  }
  
  public CompositeByteBuf addComponents(int cIndex, ByteBuf... buffers)
  {
    wrapped.addComponents(cIndex, buffers);
    return this;
  }
  
  public CompositeByteBuf addComponents(int cIndex, Iterable<ByteBuf> buffers)
  {
    wrapped.addComponents(cIndex, buffers);
    return this;
  }
  
  public CompositeByteBuf addComponent(boolean increaseWriterIndex, ByteBuf buffer)
  {
    wrapped.addComponent(increaseWriterIndex, buffer);
    return this;
  }
  
  public CompositeByteBuf addComponents(boolean increaseWriterIndex, ByteBuf... buffers)
  {
    wrapped.addComponents(increaseWriterIndex, buffers);
    return this;
  }
  
  public CompositeByteBuf addComponents(boolean increaseWriterIndex, Iterable<ByteBuf> buffers)
  {
    wrapped.addComponents(increaseWriterIndex, buffers);
    return this;
  }
  
  public CompositeByteBuf addComponent(boolean increaseWriterIndex, int cIndex, ByteBuf buffer)
  {
    wrapped.addComponent(increaseWriterIndex, cIndex, buffer);
    return this;
  }
  
  public CompositeByteBuf addFlattenedComponents(boolean increaseWriterIndex, ByteBuf buffer)
  {
    wrapped.addFlattenedComponents(increaseWriterIndex, buffer);
    return this;
  }
  
  public CompositeByteBuf removeComponent(int cIndex)
  {
    wrapped.removeComponent(cIndex);
    return this;
  }
  
  public CompositeByteBuf removeComponents(int cIndex, int numComponents)
  {
    wrapped.removeComponents(cIndex, numComponents);
    return this;
  }
  
  public Iterator<ByteBuf> iterator()
  {
    return wrapped.iterator();
  }
  
  public List<ByteBuf> decompose(int offset, int length)
  {
    return wrapped.decompose(offset, length);
  }
  
  public final boolean isDirect()
  {
    return wrapped.isDirect();
  }
  
  public final boolean hasArray()
  {
    return wrapped.hasArray();
  }
  
  public final byte[] array()
  {
    return wrapped.array();
  }
  
  public final int arrayOffset()
  {
    return wrapped.arrayOffset();
  }
  
  public final boolean hasMemoryAddress()
  {
    return wrapped.hasMemoryAddress();
  }
  
  public final long memoryAddress()
  {
    return wrapped.memoryAddress();
  }
  
  public final int capacity()
  {
    return wrapped.capacity();
  }
  
  public CompositeByteBuf capacity(int newCapacity)
  {
    wrapped.capacity(newCapacity);
    return this;
  }
  
  public final ByteBufAllocator alloc()
  {
    return wrapped.alloc();
  }
  
  public final ByteOrder order()
  {
    return wrapped.order();
  }
  
  public final int numComponents()
  {
    return wrapped.numComponents();
  }
  
  public final int maxNumComponents()
  {
    return wrapped.maxNumComponents();
  }
  
  public final int toComponentIndex(int offset)
  {
    return wrapped.toComponentIndex(offset);
  }
  
  public final int toByteIndex(int cIndex)
  {
    return wrapped.toByteIndex(cIndex);
  }
  
  public byte getByte(int index)
  {
    return wrapped.getByte(index);
  }
  
  protected final byte _getByte(int index)
  {
    return wrapped._getByte(index);
  }
  
  protected final short _getShort(int index)
  {
    return wrapped._getShort(index);
  }
  
  protected final short _getShortLE(int index)
  {
    return wrapped._getShortLE(index);
  }
  
  protected final int _getUnsignedMedium(int index)
  {
    return wrapped._getUnsignedMedium(index);
  }
  
  protected final int _getUnsignedMediumLE(int index)
  {
    return wrapped._getUnsignedMediumLE(index);
  }
  
  protected final int _getInt(int index)
  {
    return wrapped._getInt(index);
  }
  
  protected final int _getIntLE(int index)
  {
    return wrapped._getIntLE(index);
  }
  
  protected final long _getLong(int index)
  {
    return wrapped._getLong(index);
  }
  
  protected final long _getLongLE(int index)
  {
    return wrapped._getLongLE(index);
  }
  
  public CompositeByteBuf getBytes(int index, byte[] dst, int dstIndex, int length)
  {
    wrapped.getBytes(index, dst, dstIndex, length);
    return this;
  }
  
  public CompositeByteBuf getBytes(int index, ByteBuffer dst)
  {
    wrapped.getBytes(index, dst);
    return this;
  }
  
  public CompositeByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length)
  {
    wrapped.getBytes(index, dst, dstIndex, length);
    return this;
  }
  
  public int getBytes(int index, GatheringByteChannel out, int length) throws IOException
  {
    return wrapped.getBytes(index, out, length);
  }
  
  public CompositeByteBuf getBytes(int index, OutputStream out, int length) throws IOException
  {
    wrapped.getBytes(index, out, length);
    return this;
  }
  
  public CompositeByteBuf setByte(int index, int value)
  {
    wrapped.setByte(index, value);
    return this;
  }
  
  protected final void _setByte(int index, int value)
  {
    wrapped._setByte(index, value);
  }
  
  public CompositeByteBuf setShort(int index, int value)
  {
    wrapped.setShort(index, value);
    return this;
  }
  
  protected final void _setShort(int index, int value)
  {
    wrapped._setShort(index, value);
  }
  
  protected final void _setShortLE(int index, int value)
  {
    wrapped._setShortLE(index, value);
  }
  
  public CompositeByteBuf setMedium(int index, int value)
  {
    wrapped.setMedium(index, value);
    return this;
  }
  
  protected final void _setMedium(int index, int value)
  {
    wrapped._setMedium(index, value);
  }
  
  protected final void _setMediumLE(int index, int value)
  {
    wrapped._setMediumLE(index, value);
  }
  
  public CompositeByteBuf setInt(int index, int value)
  {
    wrapped.setInt(index, value);
    return this;
  }
  
  protected final void _setInt(int index, int value)
  {
    wrapped._setInt(index, value);
  }
  
  protected final void _setIntLE(int index, int value)
  {
    wrapped._setIntLE(index, value);
  }
  
  public CompositeByteBuf setLong(int index, long value)
  {
    wrapped.setLong(index, value);
    return this;
  }
  
  protected final void _setLong(int index, long value)
  {
    wrapped._setLong(index, value);
  }
  
  protected final void _setLongLE(int index, long value)
  {
    wrapped._setLongLE(index, value);
  }
  
  public CompositeByteBuf setBytes(int index, byte[] src, int srcIndex, int length)
  {
    wrapped.setBytes(index, src, srcIndex, length);
    return this;
  }
  
  public CompositeByteBuf setBytes(int index, ByteBuffer src)
  {
    wrapped.setBytes(index, src);
    return this;
  }
  
  public CompositeByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length)
  {
    wrapped.setBytes(index, src, srcIndex, length);
    return this;
  }
  
  public int setBytes(int index, InputStream in, int length) throws IOException
  {
    return wrapped.setBytes(index, in, length);
  }
  
  public int setBytes(int index, ScatteringByteChannel in, int length) throws IOException
  {
    return wrapped.setBytes(index, in, length);
  }
  
  public ByteBuf copy(int index, int length)
  {
    return wrapped.copy(index, length);
  }
  
  public final ByteBuf component(int cIndex)
  {
    return wrapped.component(cIndex);
  }
  
  public final ByteBuf componentAtOffset(int offset)
  {
    return wrapped.componentAtOffset(offset);
  }
  
  public final ByteBuf internalComponent(int cIndex)
  {
    return wrapped.internalComponent(cIndex);
  }
  
  public final ByteBuf internalComponentAtOffset(int offset)
  {
    return wrapped.internalComponentAtOffset(offset);
  }
  
  public int nioBufferCount()
  {
    return wrapped.nioBufferCount();
  }
  
  public ByteBuffer internalNioBuffer(int index, int length)
  {
    return wrapped.internalNioBuffer(index, length);
  }
  
  public ByteBuffer nioBuffer(int index, int length)
  {
    return wrapped.nioBuffer(index, length);
  }
  
  public ByteBuffer[] nioBuffers(int index, int length)
  {
    return wrapped.nioBuffers(index, length);
  }
  
  public CompositeByteBuf consolidate()
  {
    wrapped.consolidate();
    return this;
  }
  
  public CompositeByteBuf consolidate(int cIndex, int numComponents)
  {
    wrapped.consolidate(cIndex, numComponents);
    return this;
  }
  
  public CompositeByteBuf discardReadComponents()
  {
    wrapped.discardReadComponents();
    return this;
  }
  
  public CompositeByteBuf discardReadBytes()
  {
    wrapped.discardReadBytes();
    return this;
  }
  
  public final String toString()
  {
    return wrapped.toString();
  }
  
  public final CompositeByteBuf readerIndex(int readerIndex)
  {
    wrapped.readerIndex(readerIndex);
    return this;
  }
  
  public final CompositeByteBuf writerIndex(int writerIndex)
  {
    wrapped.writerIndex(writerIndex);
    return this;
  }
  
  public final CompositeByteBuf setIndex(int readerIndex, int writerIndex)
  {
    wrapped.setIndex(readerIndex, writerIndex);
    return this;
  }
  
  public final CompositeByteBuf clear()
  {
    wrapped.clear();
    return this;
  }
  
  public final CompositeByteBuf markReaderIndex()
  {
    wrapped.markReaderIndex();
    return this;
  }
  
  public final CompositeByteBuf resetReaderIndex()
  {
    wrapped.resetReaderIndex();
    return this;
  }
  
  public final CompositeByteBuf markWriterIndex()
  {
    wrapped.markWriterIndex();
    return this;
  }
  
  public final CompositeByteBuf resetWriterIndex()
  {
    wrapped.resetWriterIndex();
    return this;
  }
  
  public CompositeByteBuf ensureWritable(int minWritableBytes)
  {
    wrapped.ensureWritable(minWritableBytes);
    return this;
  }
  
  public CompositeByteBuf getBytes(int index, ByteBuf dst)
  {
    wrapped.getBytes(index, dst);
    return this;
  }
  
  public CompositeByteBuf getBytes(int index, ByteBuf dst, int length)
  {
    wrapped.getBytes(index, dst, length);
    return this;
  }
  
  public CompositeByteBuf getBytes(int index, byte[] dst)
  {
    wrapped.getBytes(index, dst);
    return this;
  }
  
  public CompositeByteBuf setBoolean(int index, boolean value)
  {
    wrapped.setBoolean(index, value);
    return this;
  }
  
  public CompositeByteBuf setChar(int index, int value)
  {
    wrapped.setChar(index, value);
    return this;
  }
  
  public CompositeByteBuf setFloat(int index, float value)
  {
    wrapped.setFloat(index, value);
    return this;
  }
  
  public CompositeByteBuf setDouble(int index, double value)
  {
    wrapped.setDouble(index, value);
    return this;
  }
  
  public CompositeByteBuf setBytes(int index, ByteBuf src)
  {
    wrapped.setBytes(index, src);
    return this;
  }
  
  public CompositeByteBuf setBytes(int index, ByteBuf src, int length)
  {
    wrapped.setBytes(index, src, length);
    return this;
  }
  
  public CompositeByteBuf setBytes(int index, byte[] src)
  {
    wrapped.setBytes(index, src);
    return this;
  }
  
  public CompositeByteBuf setZero(int index, int length)
  {
    wrapped.setZero(index, length);
    return this;
  }
  
  public CompositeByteBuf readBytes(ByteBuf dst)
  {
    wrapped.readBytes(dst);
    return this;
  }
  
  public CompositeByteBuf readBytes(ByteBuf dst, int length)
  {
    wrapped.readBytes(dst, length);
    return this;
  }
  
  public CompositeByteBuf readBytes(ByteBuf dst, int dstIndex, int length)
  {
    wrapped.readBytes(dst, dstIndex, length);
    return this;
  }
  
  public CompositeByteBuf readBytes(byte[] dst)
  {
    wrapped.readBytes(dst);
    return this;
  }
  
  public CompositeByteBuf readBytes(byte[] dst, int dstIndex, int length)
  {
    wrapped.readBytes(dst, dstIndex, length);
    return this;
  }
  
  public CompositeByteBuf readBytes(ByteBuffer dst)
  {
    wrapped.readBytes(dst);
    return this;
  }
  
  public CompositeByteBuf readBytes(OutputStream out, int length) throws IOException
  {
    wrapped.readBytes(out, length);
    return this;
  }
  
  public int getBytes(int index, FileChannel out, long position, int length) throws IOException
  {
    return wrapped.getBytes(index, out, position, length);
  }
  
  public int setBytes(int index, FileChannel in, long position, int length) throws IOException
  {
    return wrapped.setBytes(index, in, position, length);
  }
  
  public boolean isReadOnly()
  {
    return wrapped.isReadOnly();
  }
  
  public ByteBuf asReadOnly()
  {
    return wrapped.asReadOnly();
  }
  
  protected SwappedByteBuf newSwappedByteBuf()
  {
    return wrapped.newSwappedByteBuf();
  }
  
  public CharSequence getCharSequence(int index, int length, Charset charset)
  {
    return wrapped.getCharSequence(index, length, charset);
  }
  
  public CharSequence readCharSequence(int length, Charset charset)
  {
    return wrapped.readCharSequence(length, charset);
  }
  
  public int setCharSequence(int index, CharSequence sequence, Charset charset)
  {
    return wrapped.setCharSequence(index, sequence, charset);
  }
  
  public int readBytes(FileChannel out, long position, int length) throws IOException
  {
    return wrapped.readBytes(out, position, length);
  }
  
  public int writeBytes(FileChannel in, long position, int length) throws IOException
  {
    return wrapped.writeBytes(in, position, length);
  }
  
  public int writeCharSequence(CharSequence sequence, Charset charset)
  {
    return wrapped.writeCharSequence(sequence, charset);
  }
  
  public CompositeByteBuf skipBytes(int length)
  {
    wrapped.skipBytes(length);
    return this;
  }
  
  public CompositeByteBuf writeBoolean(boolean value)
  {
    wrapped.writeBoolean(value);
    return this;
  }
  
  public CompositeByteBuf writeByte(int value)
  {
    wrapped.writeByte(value);
    return this;
  }
  
  public CompositeByteBuf writeShort(int value)
  {
    wrapped.writeShort(value);
    return this;
  }
  
  public CompositeByteBuf writeMedium(int value)
  {
    wrapped.writeMedium(value);
    return this;
  }
  
  public CompositeByteBuf writeInt(int value)
  {
    wrapped.writeInt(value);
    return this;
  }
  
  public CompositeByteBuf writeLong(long value)
  {
    wrapped.writeLong(value);
    return this;
  }
  
  public CompositeByteBuf writeChar(int value)
  {
    wrapped.writeChar(value);
    return this;
  }
  
  public CompositeByteBuf writeFloat(float value)
  {
    wrapped.writeFloat(value);
    return this;
  }
  
  public CompositeByteBuf writeDouble(double value)
  {
    wrapped.writeDouble(value);
    return this;
  }
  
  public CompositeByteBuf writeBytes(ByteBuf src)
  {
    wrapped.writeBytes(src);
    return this;
  }
  
  public CompositeByteBuf writeBytes(ByteBuf src, int length)
  {
    wrapped.writeBytes(src, length);
    return this;
  }
  
  public CompositeByteBuf writeBytes(ByteBuf src, int srcIndex, int length)
  {
    wrapped.writeBytes(src, srcIndex, length);
    return this;
  }
  
  public CompositeByteBuf writeBytes(byte[] src)
  {
    wrapped.writeBytes(src);
    return this;
  }
  
  public CompositeByteBuf writeBytes(byte[] src, int srcIndex, int length)
  {
    wrapped.writeBytes(src, srcIndex, length);
    return this;
  }
  
  public CompositeByteBuf writeBytes(ByteBuffer src)
  {
    wrapped.writeBytes(src);
    return this;
  }
  
  public CompositeByteBuf writeZero(int length)
  {
    wrapped.writeZero(length);
    return this;
  }
  
  public CompositeByteBuf retain(int increment)
  {
    wrapped.retain(increment);
    return this;
  }
  
  public CompositeByteBuf retain()
  {
    wrapped.retain();
    return this;
  }
  
  public CompositeByteBuf touch()
  {
    wrapped.touch();
    return this;
  }
  
  public CompositeByteBuf touch(Object hint)
  {
    wrapped.touch(hint);
    return this;
  }
  
  public ByteBuffer[] nioBuffers()
  {
    return wrapped.nioBuffers();
  }
  
  public CompositeByteBuf discardSomeReadBytes()
  {
    wrapped.discardSomeReadBytes();
    return this;
  }
  
  public final void deallocate()
  {
    wrapped.deallocate();
  }
  
  public final ByteBuf unwrap()
  {
    return wrapped;
  }
}
