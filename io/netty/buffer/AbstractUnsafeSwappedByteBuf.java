package io.netty.buffer;

import io.netty.util.internal.PlatformDependent;
import java.nio.ByteOrder;



















abstract class AbstractUnsafeSwappedByteBuf
  extends SwappedByteBuf
{
  private final boolean nativeByteOrder;
  private final AbstractByteBuf wrapped;
  
  AbstractUnsafeSwappedByteBuf(AbstractByteBuf buf)
  {
    super(buf);
    assert (PlatformDependent.isUnaligned());
    wrapped = buf;
    nativeByteOrder = (PlatformDependent.BIG_ENDIAN_NATIVE_ORDER == (order() == ByteOrder.BIG_ENDIAN));
  }
  
  public final long getLong(int index)
  {
    wrapped.checkIndex(index, 8);
    long v = _getLong(wrapped, index);
    return nativeByteOrder ? v : Long.reverseBytes(v);
  }
  
  public final float getFloat(int index)
  {
    return Float.intBitsToFloat(getInt(index));
  }
  
  public final double getDouble(int index)
  {
    return Double.longBitsToDouble(getLong(index));
  }
  
  public final char getChar(int index)
  {
    return (char)getShort(index);
  }
  
  public final long getUnsignedInt(int index)
  {
    return getInt(index) & 0xFFFFFFFF;
  }
  
  public final int getInt(int index)
  {
    wrapped.checkIndex(index, 4);
    int v = _getInt(wrapped, index);
    return nativeByteOrder ? v : Integer.reverseBytes(v);
  }
  
  public final int getUnsignedShort(int index)
  {
    return getShort(index) & 0xFFFF;
  }
  
  public final short getShort(int index)
  {
    wrapped.checkIndex(index, 2);
    short v = _getShort(wrapped, index);
    return nativeByteOrder ? v : Short.reverseBytes(v);
  }
  
  public final ByteBuf setShort(int index, int value)
  {
    wrapped.checkIndex(index, 2);
    _setShort(wrapped, index, nativeByteOrder ? (short)value : Short.reverseBytes((short)value));
    return this;
  }
  
  public final ByteBuf setInt(int index, int value)
  {
    wrapped.checkIndex(index, 4);
    _setInt(wrapped, index, nativeByteOrder ? value : Integer.reverseBytes(value));
    return this;
  }
  
  public final ByteBuf setLong(int index, long value)
  {
    wrapped.checkIndex(index, 8);
    _setLong(wrapped, index, nativeByteOrder ? value : Long.reverseBytes(value));
    return this;
  }
  
  public final ByteBuf setChar(int index, int value)
  {
    setShort(index, value);
    return this;
  }
  
  public final ByteBuf setFloat(int index, float value)
  {
    setInt(index, Float.floatToRawIntBits(value));
    return this;
  }
  
  public final ByteBuf setDouble(int index, double value)
  {
    setLong(index, Double.doubleToRawLongBits(value));
    return this;
  }
  
  public final ByteBuf writeShort(int value)
  {
    wrapped.ensureWritable0(2);
    _setShort(wrapped, wrapped.writerIndex, nativeByteOrder ? (short)value : Short.reverseBytes((short)value));
    wrapped.writerIndex += 2;
    return this;
  }
  
  public final ByteBuf writeInt(int value)
  {
    wrapped.ensureWritable0(4);
    _setInt(wrapped, wrapped.writerIndex, nativeByteOrder ? value : Integer.reverseBytes(value));
    wrapped.writerIndex += 4;
    return this;
  }
  
  public final ByteBuf writeLong(long value)
  {
    wrapped.ensureWritable0(8);
    _setLong(wrapped, wrapped.writerIndex, nativeByteOrder ? value : Long.reverseBytes(value));
    wrapped.writerIndex += 8;
    return this;
  }
  
  public final ByteBuf writeChar(int value)
  {
    writeShort(value);
    return this;
  }
  
  public final ByteBuf writeFloat(float value)
  {
    writeInt(Float.floatToRawIntBits(value));
    return this;
  }
  
  public final ByteBuf writeDouble(double value)
  {
    writeLong(Double.doubleToRawLongBits(value));
    return this;
  }
  
  protected abstract short _getShort(AbstractByteBuf paramAbstractByteBuf, int paramInt);
  
  protected abstract int _getInt(AbstractByteBuf paramAbstractByteBuf, int paramInt);
  
  protected abstract long _getLong(AbstractByteBuf paramAbstractByteBuf, int paramInt);
  
  protected abstract void _setShort(AbstractByteBuf paramAbstractByteBuf, int paramInt, short paramShort);
  
  protected abstract void _setInt(AbstractByteBuf paramAbstractByteBuf, int paramInt1, int paramInt2);
  
  protected abstract void _setLong(AbstractByteBuf paramAbstractByteBuf, int paramInt, long paramLong);
}
