package io.netty.buffer;

import io.netty.util.AsciiString;
import io.netty.util.ByteProcessor;
import io.netty.util.CharsetUtil;
import io.netty.util.IllegalReferenceCountException;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetectorFactory;
import io.netty.util.internal.MathUtil;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;




















public abstract class AbstractByteBuf
  extends ByteBuf
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractByteBuf.class);
  private static final String LEGACY_PROP_CHECK_ACCESSIBLE = "io.netty.buffer.bytebuf.checkAccessible";
  private static final String PROP_CHECK_ACCESSIBLE = "io.netty.buffer.checkAccessible";
  static final boolean checkAccessible;
  private static final String PROP_CHECK_BOUNDS = "io.netty.buffer.checkBounds";
  private static final boolean checkBounds;
  
  static {
    if (SystemPropertyUtil.contains("io.netty.buffer.checkAccessible")) {
      checkAccessible = SystemPropertyUtil.getBoolean("io.netty.buffer.checkAccessible", true);
    } else {
      checkAccessible = SystemPropertyUtil.getBoolean("io.netty.buffer.bytebuf.checkAccessible", true);
    }
    checkBounds = SystemPropertyUtil.getBoolean("io.netty.buffer.checkBounds", true);
    if (logger.isDebugEnabled()) {
      logger.debug("-D{}: {}", "io.netty.buffer.checkAccessible", Boolean.valueOf(checkAccessible));
      logger.debug("-D{}: {}", "io.netty.buffer.checkBounds", Boolean.valueOf(checkBounds));
    }
  }
  

  static final ResourceLeakDetector<ByteBuf> leakDetector = ResourceLeakDetectorFactory.instance().newResourceLeakDetector(ByteBuf.class);
  int readerIndex;
  int writerIndex;
  private int markedReaderIndex;
  private int markedWriterIndex;
  private int maxCapacity;
  
  protected AbstractByteBuf(int maxCapacity)
  {
    ObjectUtil.checkPositiveOrZero(maxCapacity, "maxCapacity");
    this.maxCapacity = maxCapacity;
  }
  
  public boolean isReadOnly()
  {
    return false;
  }
  

  public ByteBuf asReadOnly()
  {
    if (isReadOnly()) {
      return this;
    }
    return Unpooled.unmodifiableBuffer(this);
  }
  
  public int maxCapacity()
  {
    return maxCapacity;
  }
  
  protected final void maxCapacity(int maxCapacity) {
    this.maxCapacity = maxCapacity;
  }
  
  public int readerIndex()
  {
    return readerIndex;
  }
  
  private static void checkIndexBounds(int readerIndex, int writerIndex, int capacity) {
    if ((readerIndex < 0) || (readerIndex > writerIndex) || (writerIndex > capacity)) {
      throw new IndexOutOfBoundsException(String.format("readerIndex: %d, writerIndex: %d (expected: 0 <= readerIndex <= writerIndex <= capacity(%d))", new Object[] {
      
        Integer.valueOf(readerIndex), Integer.valueOf(writerIndex), Integer.valueOf(capacity) }));
    }
  }
  
  public ByteBuf readerIndex(int readerIndex)
  {
    if (checkBounds) {
      checkIndexBounds(readerIndex, writerIndex, capacity());
    }
    this.readerIndex = readerIndex;
    return this;
  }
  
  public int writerIndex()
  {
    return writerIndex;
  }
  
  public ByteBuf writerIndex(int writerIndex)
  {
    if (checkBounds) {
      checkIndexBounds(readerIndex, writerIndex, capacity());
    }
    this.writerIndex = writerIndex;
    return this;
  }
  
  public ByteBuf setIndex(int readerIndex, int writerIndex)
  {
    if (checkBounds) {
      checkIndexBounds(readerIndex, writerIndex, capacity());
    }
    setIndex0(readerIndex, writerIndex);
    return this;
  }
  
  public ByteBuf clear()
  {
    readerIndex = (this.writerIndex = 0);
    return this;
  }
  
  public boolean isReadable()
  {
    return writerIndex > readerIndex;
  }
  
  public boolean isReadable(int numBytes)
  {
    return writerIndex - readerIndex >= numBytes;
  }
  
  public boolean isWritable()
  {
    return capacity() > writerIndex;
  }
  
  public boolean isWritable(int numBytes)
  {
    return capacity() - writerIndex >= numBytes;
  }
  
  public int readableBytes()
  {
    return writerIndex - readerIndex;
  }
  
  public int writableBytes()
  {
    return capacity() - writerIndex;
  }
  
  public int maxWritableBytes()
  {
    return maxCapacity() - writerIndex;
  }
  
  public ByteBuf markReaderIndex()
  {
    markedReaderIndex = readerIndex;
    return this;
  }
  
  public ByteBuf resetReaderIndex()
  {
    readerIndex(markedReaderIndex);
    return this;
  }
  
  public ByteBuf markWriterIndex()
  {
    markedWriterIndex = writerIndex;
    return this;
  }
  
  public ByteBuf resetWriterIndex()
  {
    writerIndex(markedWriterIndex);
    return this;
  }
  
  public ByteBuf discardReadBytes()
  {
    if (readerIndex == 0) {
      ensureAccessible();
      return this;
    }
    
    if (readerIndex != writerIndex) {
      setBytes(0, this, readerIndex, writerIndex - readerIndex);
      writerIndex -= readerIndex;
      adjustMarkers(readerIndex);
      readerIndex = 0;
    } else {
      ensureAccessible();
      adjustMarkers(readerIndex);
      writerIndex = (this.readerIndex = 0);
    }
    return this;
  }
  
  public ByteBuf discardSomeReadBytes()
  {
    if (readerIndex > 0) {
      if (readerIndex == writerIndex) {
        ensureAccessible();
        adjustMarkers(readerIndex);
        writerIndex = (this.readerIndex = 0);
        return this;
      }
      
      if (readerIndex >= capacity() >>> 1) {
        setBytes(0, this, readerIndex, writerIndex - readerIndex);
        writerIndex -= readerIndex;
        adjustMarkers(readerIndex);
        readerIndex = 0;
        return this;
      }
    }
    ensureAccessible();
    return this;
  }
  
  protected final void adjustMarkers(int decrement) {
    if (markedReaderIndex <= decrement) {
      markedReaderIndex = 0;
      if (markedWriterIndex <= decrement) {
        markedWriterIndex = 0;
      } else {
        markedWriterIndex -= decrement;
      }
    } else {
      markedReaderIndex -= decrement;
      markedWriterIndex -= decrement;
    }
  }
  
  protected final void trimIndicesToCapacity(int newCapacity)
  {
    if (writerIndex() > newCapacity) {
      setIndex0(Math.min(readerIndex(), newCapacity), newCapacity);
    }
  }
  
  public ByteBuf ensureWritable(int minWritableBytes)
  {
    ensureWritable0(ObjectUtil.checkPositiveOrZero(minWritableBytes, "minWritableBytes"));
    return this;
  }
  
  final void ensureWritable0(int minWritableBytes) {
    int writerIndex = writerIndex();
    int targetCapacity = writerIndex + minWritableBytes;
    
    if (((targetCapacity >= 0 ? 1 : 0) & (targetCapacity <= capacity() ? 1 : 0)) != 0) {
      ensureAccessible();
      return;
    }
    if ((checkBounds) && ((targetCapacity < 0) || (targetCapacity > maxCapacity))) {
      ensureAccessible();
      throw new IndexOutOfBoundsException(String.format("writerIndex(%d) + minWritableBytes(%d) exceeds maxCapacity(%d): %s", new Object[] {
      
        Integer.valueOf(writerIndex), Integer.valueOf(minWritableBytes), Integer.valueOf(maxCapacity), this }));
    }
    

    int fastWritable = maxFastWritableBytes();
    
    int newCapacity = fastWritable >= minWritableBytes ? writerIndex + fastWritable : alloc().calculateNewCapacity(targetCapacity, maxCapacity);
    

    capacity(newCapacity);
  }
  
  public int ensureWritable(int minWritableBytes, boolean force)
  {
    ensureAccessible();
    ObjectUtil.checkPositiveOrZero(minWritableBytes, "minWritableBytes");
    
    if (minWritableBytes <= writableBytes()) {
      return 0;
    }
    
    int maxCapacity = maxCapacity();
    int writerIndex = writerIndex();
    if (minWritableBytes > maxCapacity - writerIndex) {
      if ((!force) || (capacity() == maxCapacity)) {
        return 1;
      }
      
      capacity(maxCapacity);
      return 3;
    }
    
    int fastWritable = maxFastWritableBytes();
    
    int newCapacity = fastWritable >= minWritableBytes ? writerIndex + fastWritable : alloc().calculateNewCapacity(writerIndex + minWritableBytes, maxCapacity);
    

    capacity(newCapacity);
    return 2;
  }
  
  public ByteBuf order(ByteOrder endianness)
  {
    if (endianness == order()) {
      return this;
    }
    ObjectUtil.checkNotNull(endianness, "endianness");
    return newSwappedByteBuf();
  }
  


  protected SwappedByteBuf newSwappedByteBuf()
  {
    return new SwappedByteBuf(this);
  }
  
  public byte getByte(int index)
  {
    checkIndex(index);
    return _getByte(index);
  }
  


  public boolean getBoolean(int index)
  {
    return getByte(index) != 0;
  }
  
  public short getUnsignedByte(int index)
  {
    return (short)(getByte(index) & 0xFF);
  }
  
  public short getShort(int index)
  {
    checkIndex(index, 2);
    return _getShort(index);
  }
  


  public short getShortLE(int index)
  {
    checkIndex(index, 2);
    return _getShortLE(index);
  }
  


  public int getUnsignedShort(int index)
  {
    return getShort(index) & 0xFFFF;
  }
  
  public int getUnsignedShortLE(int index)
  {
    return getShortLE(index) & 0xFFFF;
  }
  
  public int getUnsignedMedium(int index)
  {
    checkIndex(index, 3);
    return _getUnsignedMedium(index);
  }
  


  public int getUnsignedMediumLE(int index)
  {
    checkIndex(index, 3);
    return _getUnsignedMediumLE(index);
  }
  


  public int getMedium(int index)
  {
    int value = getUnsignedMedium(index);
    if ((value & 0x800000) != 0) {
      value |= 0xFF000000;
    }
    return value;
  }
  
  public int getMediumLE(int index)
  {
    int value = getUnsignedMediumLE(index);
    if ((value & 0x800000) != 0) {
      value |= 0xFF000000;
    }
    return value;
  }
  
  public int getInt(int index)
  {
    checkIndex(index, 4);
    return _getInt(index);
  }
  


  public int getIntLE(int index)
  {
    checkIndex(index, 4);
    return _getIntLE(index);
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
    checkIndex(index, 8);
    return _getLong(index);
  }
  


  public long getLongLE(int index)
  {
    checkIndex(index, 8);
    return _getLongLE(index);
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
  
  public ByteBuf getBytes(int index, byte[] dst)
  {
    getBytes(index, dst, 0, dst.length);
    return this;
  }
  
  public ByteBuf getBytes(int index, ByteBuf dst)
  {
    getBytes(index, dst, dst.writableBytes());
    return this;
  }
  
  public ByteBuf getBytes(int index, ByteBuf dst, int length)
  {
    getBytes(index, dst, dst.writerIndex(), length);
    dst.writerIndex(dst.writerIndex() + length);
    return this;
  }
  
  public CharSequence getCharSequence(int index, int length, Charset charset)
  {
    if ((CharsetUtil.US_ASCII.equals(charset)) || (CharsetUtil.ISO_8859_1.equals(charset)))
    {
      return new AsciiString(ByteBufUtil.getBytes(this, index, length, true), false);
    }
    return toString(index, length, charset);
  }
  
  public CharSequence readCharSequence(int length, Charset charset)
  {
    CharSequence sequence = getCharSequence(readerIndex, length, charset);
    readerIndex += length;
    return sequence;
  }
  
  public ByteBuf setByte(int index, int value)
  {
    checkIndex(index);
    _setByte(index, value);
    return this;
  }
  


  public ByteBuf setBoolean(int index, boolean value)
  {
    setByte(index, value ? 1 : 0);
    return this;
  }
  
  public ByteBuf setShort(int index, int value)
  {
    checkIndex(index, 2);
    _setShort(index, value);
    return this;
  }
  


  public ByteBuf setShortLE(int index, int value)
  {
    checkIndex(index, 2);
    _setShortLE(index, value);
    return this;
  }
  


  public ByteBuf setChar(int index, int value)
  {
    setShort(index, value);
    return this;
  }
  
  public ByteBuf setMedium(int index, int value)
  {
    checkIndex(index, 3);
    _setMedium(index, value);
    return this;
  }
  


  public ByteBuf setMediumLE(int index, int value)
  {
    checkIndex(index, 3);
    _setMediumLE(index, value);
    return this;
  }
  


  public ByteBuf setInt(int index, int value)
  {
    checkIndex(index, 4);
    _setInt(index, value);
    return this;
  }
  


  public ByteBuf setIntLE(int index, int value)
  {
    checkIndex(index, 4);
    _setIntLE(index, value);
    return this;
  }
  


  public ByteBuf setFloat(int index, float value)
  {
    setInt(index, Float.floatToRawIntBits(value));
    return this;
  }
  
  public ByteBuf setLong(int index, long value)
  {
    checkIndex(index, 8);
    _setLong(index, value);
    return this;
  }
  


  public ByteBuf setLongLE(int index, long value)
  {
    checkIndex(index, 8);
    _setLongLE(index, value);
    return this;
  }
  


  public ByteBuf setDouble(int index, double value)
  {
    setLong(index, Double.doubleToRawLongBits(value));
    return this;
  }
  
  public ByteBuf setBytes(int index, byte[] src)
  {
    setBytes(index, src, 0, src.length);
    return this;
  }
  
  public ByteBuf setBytes(int index, ByteBuf src)
  {
    setBytes(index, src, src.readableBytes());
    return this;
  }
  
  private static void checkReadableBounds(ByteBuf src, int length) {
    if (length > src.readableBytes()) {
      throw new IndexOutOfBoundsException(String.format("length(%d) exceeds src.readableBytes(%d) where src is: %s", new Object[] {
        Integer.valueOf(length), Integer.valueOf(src.readableBytes()), src }));
    }
  }
  
  public ByteBuf setBytes(int index, ByteBuf src, int length)
  {
    checkIndex(index, length);
    ObjectUtil.checkNotNull(src, "src");
    if (checkBounds) {
      checkReadableBounds(src, length);
    }
    
    setBytes(index, src, src.readerIndex(), length);
    src.readerIndex(src.readerIndex() + length);
    return this;
  }
  
  public ByteBuf setZero(int index, int length)
  {
    if (length == 0) {
      return this;
    }
    
    checkIndex(index, length);
    
    int nLong = length >>> 3;
    int nBytes = length & 0x7;
    for (int i = nLong; i > 0; i--) {
      _setLong(index, 0L);
      index += 8;
    }
    if (nBytes == 4) {
      _setInt(index, 0);
    }
    else if (nBytes < 4) {
      for (int i = nBytes; i > 0; i--) {
        _setByte(index, 0);
        index++;
      }
    } else {
      _setInt(index, 0);
      index += 4;
      for (int i = nBytes - 4; i > 0; i--) {
        _setByte(index, 0);
        index++;
      }
    }
    return this;
  }
  
  public int setCharSequence(int index, CharSequence sequence, Charset charset)
  {
    return setCharSequence0(index, sequence, charset, false);
  }
  
  private int setCharSequence0(int index, CharSequence sequence, Charset charset, boolean expand) {
    if (charset.equals(CharsetUtil.UTF_8)) {
      int length = ByteBufUtil.utf8MaxBytes(sequence);
      if (expand) {
        ensureWritable0(length);
        checkIndex0(index, length);
      } else {
        checkIndex(index, length);
      }
      return ByteBufUtil.writeUtf8(this, index, length, sequence, sequence.length());
    }
    if ((charset.equals(CharsetUtil.US_ASCII)) || (charset.equals(CharsetUtil.ISO_8859_1))) {
      int length = sequence.length();
      if (expand) {
        ensureWritable0(length);
        checkIndex0(index, length);
      } else {
        checkIndex(index, length);
      }
      return ByteBufUtil.writeAscii(this, index, sequence, length);
    }
    byte[] bytes = sequence.toString().getBytes(charset);
    if (expand) {
      ensureWritable0(bytes.length);
    }
    
    setBytes(index, bytes);
    return bytes.length;
  }
  
  public byte readByte()
  {
    checkReadableBytes0(1);
    int i = readerIndex;
    byte b = _getByte(i);
    readerIndex = (i + 1);
    return b;
  }
  
  public boolean readBoolean()
  {
    return readByte() != 0;
  }
  
  public short readUnsignedByte()
  {
    return (short)(readByte() & 0xFF);
  }
  
  public short readShort()
  {
    checkReadableBytes0(2);
    short v = _getShort(readerIndex);
    readerIndex += 2;
    return v;
  }
  
  public short readShortLE()
  {
    checkReadableBytes0(2);
    short v = _getShortLE(readerIndex);
    readerIndex += 2;
    return v;
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
    int value = readUnsignedMedium();
    if ((value & 0x800000) != 0) {
      value |= 0xFF000000;
    }
    return value;
  }
  
  public int readMediumLE()
  {
    int value = readUnsignedMediumLE();
    if ((value & 0x800000) != 0) {
      value |= 0xFF000000;
    }
    return value;
  }
  
  public int readUnsignedMedium()
  {
    checkReadableBytes0(3);
    int v = _getUnsignedMedium(readerIndex);
    readerIndex += 3;
    return v;
  }
  
  public int readUnsignedMediumLE()
  {
    checkReadableBytes0(3);
    int v = _getUnsignedMediumLE(readerIndex);
    readerIndex += 3;
    return v;
  }
  
  public int readInt()
  {
    checkReadableBytes0(4);
    int v = _getInt(readerIndex);
    readerIndex += 4;
    return v;
  }
  
  public int readIntLE()
  {
    checkReadableBytes0(4);
    int v = _getIntLE(readerIndex);
    readerIndex += 4;
    return v;
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
    checkReadableBytes0(8);
    long v = _getLong(readerIndex);
    readerIndex += 8;
    return v;
  }
  
  public long readLongLE()
  {
    checkReadableBytes0(8);
    long v = _getLongLE(readerIndex);
    readerIndex += 8;
    return v;
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
    checkReadableBytes(length);
    if (length == 0) {
      return Unpooled.EMPTY_BUFFER;
    }
    
    ByteBuf buf = alloc().buffer(length, maxCapacity);
    buf.writeBytes(this, readerIndex, length);
    readerIndex += length;
    return buf;
  }
  
  public ByteBuf readSlice(int length)
  {
    checkReadableBytes(length);
    ByteBuf slice = slice(readerIndex, length);
    readerIndex += length;
    return slice;
  }
  
  public ByteBuf readRetainedSlice(int length)
  {
    checkReadableBytes(length);
    ByteBuf slice = retainedSlice(readerIndex, length);
    readerIndex += length;
    return slice;
  }
  
  public ByteBuf readBytes(byte[] dst, int dstIndex, int length)
  {
    checkReadableBytes(length);
    getBytes(readerIndex, dst, dstIndex, length);
    readerIndex += length;
    return this;
  }
  
  public ByteBuf readBytes(byte[] dst)
  {
    readBytes(dst, 0, dst.length);
    return this;
  }
  
  public ByteBuf readBytes(ByteBuf dst)
  {
    readBytes(dst, dst.writableBytes());
    return this;
  }
  
  public ByteBuf readBytes(ByteBuf dst, int length)
  {
    if ((checkBounds) && 
      (length > dst.writableBytes())) {
      throw new IndexOutOfBoundsException(String.format("length(%d) exceeds dst.writableBytes(%d) where dst is: %s", new Object[] {
        Integer.valueOf(length), Integer.valueOf(dst.writableBytes()), dst }));
    }
    
    readBytes(dst, dst.writerIndex(), length);
    dst.writerIndex(dst.writerIndex() + length);
    return this;
  }
  
  public ByteBuf readBytes(ByteBuf dst, int dstIndex, int length)
  {
    checkReadableBytes(length);
    getBytes(readerIndex, dst, dstIndex, length);
    readerIndex += length;
    return this;
  }
  
  public ByteBuf readBytes(ByteBuffer dst)
  {
    int length = dst.remaining();
    checkReadableBytes(length);
    getBytes(readerIndex, dst);
    readerIndex += length;
    return this;
  }
  
  public int readBytes(GatheringByteChannel out, int length)
    throws IOException
  {
    checkReadableBytes(length);
    int readBytes = getBytes(readerIndex, out, length);
    readerIndex += readBytes;
    return readBytes;
  }
  
  public int readBytes(FileChannel out, long position, int length)
    throws IOException
  {
    checkReadableBytes(length);
    int readBytes = getBytes(readerIndex, out, position, length);
    readerIndex += readBytes;
    return readBytes;
  }
  
  public ByteBuf readBytes(OutputStream out, int length) throws IOException
  {
    checkReadableBytes(length);
    getBytes(readerIndex, out, length);
    readerIndex += length;
    return this;
  }
  
  public ByteBuf skipBytes(int length)
  {
    checkReadableBytes(length);
    readerIndex += length;
    return this;
  }
  
  public ByteBuf writeBoolean(boolean value)
  {
    writeByte(value ? 1 : 0);
    return this;
  }
  
  public ByteBuf writeByte(int value)
  {
    ensureWritable0(1);
    _setByte(writerIndex++, value);
    return this;
  }
  
  public ByteBuf writeShort(int value)
  {
    ensureWritable0(2);
    _setShort(writerIndex, value);
    writerIndex += 2;
    return this;
  }
  
  public ByteBuf writeShortLE(int value)
  {
    ensureWritable0(2);
    _setShortLE(writerIndex, value);
    writerIndex += 2;
    return this;
  }
  
  public ByteBuf writeMedium(int value)
  {
    ensureWritable0(3);
    _setMedium(writerIndex, value);
    writerIndex += 3;
    return this;
  }
  
  public ByteBuf writeMediumLE(int value)
  {
    ensureWritable0(3);
    _setMediumLE(writerIndex, value);
    writerIndex += 3;
    return this;
  }
  
  public ByteBuf writeInt(int value)
  {
    ensureWritable0(4);
    _setInt(writerIndex, value);
    writerIndex += 4;
    return this;
  }
  
  public ByteBuf writeIntLE(int value)
  {
    ensureWritable0(4);
    _setIntLE(writerIndex, value);
    writerIndex += 4;
    return this;
  }
  
  public ByteBuf writeLong(long value)
  {
    ensureWritable0(8);
    _setLong(writerIndex, value);
    writerIndex += 8;
    return this;
  }
  
  public ByteBuf writeLongLE(long value)
  {
    ensureWritable0(8);
    _setLongLE(writerIndex, value);
    writerIndex += 8;
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
  
  public ByteBuf writeBytes(byte[] src, int srcIndex, int length)
  {
    ensureWritable(length);
    setBytes(writerIndex, src, srcIndex, length);
    writerIndex += length;
    return this;
  }
  
  public ByteBuf writeBytes(byte[] src)
  {
    writeBytes(src, 0, src.length);
    return this;
  }
  
  public ByteBuf writeBytes(ByteBuf src)
  {
    writeBytes(src, src.readableBytes());
    return this;
  }
  
  public ByteBuf writeBytes(ByteBuf src, int length)
  {
    if (checkBounds) {
      checkReadableBounds(src, length);
    }
    writeBytes(src, src.readerIndex(), length);
    src.readerIndex(src.readerIndex() + length);
    return this;
  }
  
  public ByteBuf writeBytes(ByteBuf src, int srcIndex, int length)
  {
    ensureWritable(length);
    setBytes(writerIndex, src, srcIndex, length);
    writerIndex += length;
    return this;
  }
  
  public ByteBuf writeBytes(ByteBuffer src)
  {
    int length = src.remaining();
    ensureWritable0(length);
    setBytes(writerIndex, src);
    writerIndex += length;
    return this;
  }
  
  public int writeBytes(InputStream in, int length)
    throws IOException
  {
    ensureWritable(length);
    int writtenBytes = setBytes(writerIndex, in, length);
    if (writtenBytes > 0) {
      writerIndex += writtenBytes;
    }
    return writtenBytes;
  }
  
  public int writeBytes(ScatteringByteChannel in, int length) throws IOException
  {
    ensureWritable(length);
    int writtenBytes = setBytes(writerIndex, in, length);
    if (writtenBytes > 0) {
      writerIndex += writtenBytes;
    }
    return writtenBytes;
  }
  
  public int writeBytes(FileChannel in, long position, int length) throws IOException
  {
    ensureWritable(length);
    int writtenBytes = setBytes(writerIndex, in, position, length);
    if (writtenBytes > 0) {
      writerIndex += writtenBytes;
    }
    return writtenBytes;
  }
  
  public ByteBuf writeZero(int length)
  {
    if (length == 0) {
      return this;
    }
    
    ensureWritable(length);
    int wIndex = writerIndex;
    checkIndex0(wIndex, length);
    
    int nLong = length >>> 3;
    int nBytes = length & 0x7;
    for (int i = nLong; i > 0; i--) {
      _setLong(wIndex, 0L);
      wIndex += 8;
    }
    if (nBytes == 4) {
      _setInt(wIndex, 0);
      wIndex += 4;
    } else if (nBytes < 4) {
      for (int i = nBytes; i > 0; i--) {
        _setByte(wIndex, 0);
        wIndex++;
      }
    } else {
      _setInt(wIndex, 0);
      wIndex += 4;
      for (int i = nBytes - 4; i > 0; i--) {
        _setByte(wIndex, 0);
        wIndex++;
      }
    }
    writerIndex = wIndex;
    return this;
  }
  
  public int writeCharSequence(CharSequence sequence, Charset charset)
  {
    int written = setCharSequence0(writerIndex, sequence, charset, true);
    writerIndex += written;
    return written;
  }
  
  public ByteBuf copy()
  {
    return copy(readerIndex, readableBytes());
  }
  
  public ByteBuf duplicate()
  {
    ensureAccessible();
    return new UnpooledDuplicatedByteBuf(this);
  }
  
  public ByteBuf retainedDuplicate()
  {
    return duplicate().retain();
  }
  
  public ByteBuf slice()
  {
    return slice(readerIndex, readableBytes());
  }
  
  public ByteBuf retainedSlice()
  {
    return slice().retain();
  }
  
  public ByteBuf slice(int index, int length)
  {
    ensureAccessible();
    return new UnpooledSlicedByteBuf(this, index, length);
  }
  
  public ByteBuf retainedSlice(int index, int length)
  {
    return slice(index, length).retain();
  }
  
  public ByteBuffer nioBuffer()
  {
    return nioBuffer(readerIndex, readableBytes());
  }
  
  public ByteBuffer[] nioBuffers()
  {
    return nioBuffers(readerIndex, readableBytes());
  }
  
  public String toString(Charset charset)
  {
    return toString(readerIndex, readableBytes(), charset);
  }
  
  public String toString(int index, int length, Charset charset)
  {
    return ByteBufUtil.decodeString(this, index, length, charset);
  }
  
  public int indexOf(int fromIndex, int toIndex, byte value)
  {
    if (fromIndex <= toIndex) {
      return ByteBufUtil.firstIndexOf(this, fromIndex, toIndex, value);
    }
    return ByteBufUtil.lastIndexOf(this, fromIndex, toIndex, value);
  }
  
  public int bytesBefore(byte value)
  {
    return bytesBefore(readerIndex(), readableBytes(), value);
  }
  
  public int bytesBefore(int length, byte value)
  {
    checkReadableBytes(length);
    return bytesBefore(readerIndex(), length, value);
  }
  
  public int bytesBefore(int index, int length, byte value)
  {
    int endIndex = indexOf(index, index + length, value);
    if (endIndex < 0) {
      return -1;
    }
    return endIndex - index;
  }
  
  public int forEachByte(ByteProcessor processor)
  {
    ensureAccessible();
    try {
      return forEachByteAsc0(readerIndex, writerIndex, processor);
    } catch (Exception e) {
      PlatformDependent.throwException(e); }
    return -1;
  }
  

  public int forEachByte(int index, int length, ByteProcessor processor)
  {
    checkIndex(index, length);
    try {
      return forEachByteAsc0(index, index + length, processor);
    } catch (Exception e) {
      PlatformDependent.throwException(e); }
    return -1;
  }
  
  int forEachByteAsc0(int start, int end, ByteProcessor processor) throws Exception
  {
    for (; start < end; start++) {
      if (!processor.process(_getByte(start))) {
        return start;
      }
    }
    
    return -1;
  }
  
  public int forEachByteDesc(ByteProcessor processor)
  {
    ensureAccessible();
    try {
      return forEachByteDesc0(writerIndex - 1, readerIndex, processor);
    } catch (Exception e) {
      PlatformDependent.throwException(e); }
    return -1;
  }
  

  public int forEachByteDesc(int index, int length, ByteProcessor processor)
  {
    checkIndex(index, length);
    try {
      return forEachByteDesc0(index + length - 1, index, processor);
    } catch (Exception e) {
      PlatformDependent.throwException(e); }
    return -1;
  }
  
  int forEachByteDesc0(int rStart, int rEnd, ByteProcessor processor) throws Exception
  {
    for (; rStart >= rEnd; rStart--) {
      if (!processor.process(_getByte(rStart))) {
        return rStart;
      }
    }
    return -1;
  }
  
  public int hashCode()
  {
    return ByteBufUtil.hashCode(this);
  }
  
  public boolean equals(Object o)
  {
    return ((o instanceof ByteBuf)) && (ByteBufUtil.equals(this, (ByteBuf)o));
  }
  
  public int compareTo(ByteBuf that)
  {
    return ByteBufUtil.compare(this, that);
  }
  
  public String toString()
  {
    if (refCnt() == 0) {
      return StringUtil.simpleClassName(this) + "(freed)";
    }
    




    StringBuilder buf = new StringBuilder().append(StringUtil.simpleClassName(this)).append("(ridx: ").append(readerIndex).append(", widx: ").append(writerIndex).append(", cap: ").append(capacity());
    if (maxCapacity != Integer.MAX_VALUE) {
      buf.append('/').append(maxCapacity);
    }
    
    ByteBuf unwrapped = unwrap();
    if (unwrapped != null) {
      buf.append(", unwrapped: ").append(unwrapped);
    }
    buf.append(')');
    return buf.toString();
  }
  
  protected final void checkIndex(int index) {
    checkIndex(index, 1);
  }
  
  protected final void checkIndex(int index, int fieldLength) {
    ensureAccessible();
    checkIndex0(index, fieldLength);
  }
  
  private static void checkRangeBounds(String indexName, int index, int fieldLength, int capacity)
  {
    if (MathUtil.isOutOfBounds(index, fieldLength, capacity)) {
      throw new IndexOutOfBoundsException(String.format("%s: %d, length: %d (expected: range(0, %d))", new Object[] { indexName, 
        Integer.valueOf(index), Integer.valueOf(fieldLength), Integer.valueOf(capacity) }));
    }
  }
  
  final void checkIndex0(int index, int fieldLength) {
    if (checkBounds) {
      checkRangeBounds("index", index, fieldLength, capacity());
    }
  }
  
  protected final void checkSrcIndex(int index, int length, int srcIndex, int srcCapacity) {
    checkIndex(index, length);
    if (checkBounds) {
      checkRangeBounds("srcIndex", srcIndex, length, srcCapacity);
    }
  }
  
  protected final void checkDstIndex(int index, int length, int dstIndex, int dstCapacity) {
    checkIndex(index, length);
    if (checkBounds) {
      checkRangeBounds("dstIndex", dstIndex, length, dstCapacity);
    }
  }
  
  protected final void checkDstIndex(int length, int dstIndex, int dstCapacity) {
    checkReadableBytes(length);
    if (checkBounds) {
      checkRangeBounds("dstIndex", dstIndex, length, dstCapacity);
    }
  }
  




  protected final void checkReadableBytes(int minimumReadableBytes)
  {
    checkReadableBytes0(ObjectUtil.checkPositiveOrZero(minimumReadableBytes, "minimumReadableBytes"));
  }
  
  protected final void checkNewCapacity(int newCapacity) {
    ensureAccessible();
    if ((checkBounds) && ((newCapacity < 0) || (newCapacity > maxCapacity())))
    {
      throw new IllegalArgumentException("newCapacity: " + newCapacity + " (expected: 0-" + maxCapacity() + ')');
    }
  }
  
  private void checkReadableBytes0(int minimumReadableBytes) {
    ensureAccessible();
    if ((checkBounds) && (readerIndex > writerIndex - minimumReadableBytes)) {
      throw new IndexOutOfBoundsException(String.format("readerIndex(%d) + length(%d) exceeds writerIndex(%d): %s", new Object[] {
      
        Integer.valueOf(readerIndex), Integer.valueOf(minimumReadableBytes), Integer.valueOf(writerIndex), this }));
    }
  }
  



  protected final void ensureAccessible()
  {
    if ((checkAccessible) && (!isAccessible())) {
      throw new IllegalReferenceCountException(0);
    }
  }
  
  final void setIndex0(int readerIndex, int writerIndex) {
    this.readerIndex = readerIndex;
    this.writerIndex = writerIndex;
  }
  
  final void discardMarks() {
    markedReaderIndex = (this.markedWriterIndex = 0);
  }
  
  protected abstract byte _getByte(int paramInt);
  
  protected abstract short _getShort(int paramInt);
  
  protected abstract short _getShortLE(int paramInt);
  
  protected abstract int _getUnsignedMedium(int paramInt);
  
  protected abstract int _getUnsignedMediumLE(int paramInt);
  
  protected abstract int _getInt(int paramInt);
  
  protected abstract int _getIntLE(int paramInt);
  
  protected abstract long _getLong(int paramInt);
  
  protected abstract long _getLongLE(int paramInt);
  
  protected abstract void _setByte(int paramInt1, int paramInt2);
  
  protected abstract void _setShort(int paramInt1, int paramInt2);
  
  protected abstract void _setShortLE(int paramInt1, int paramInt2);
  
  protected abstract void _setMedium(int paramInt1, int paramInt2);
  
  protected abstract void _setMediumLE(int paramInt1, int paramInt2);
  
  protected abstract void _setInt(int paramInt1, int paramInt2);
  
  protected abstract void _setIntLE(int paramInt1, int paramInt2);
  
  protected abstract void _setLong(int paramInt, long paramLong);
  
  protected abstract void _setLongLE(int paramInt, long paramLong);
}
