package io.netty.buffer;

import io.netty.util.internal.ObjectUtil;
import java.nio.ByteOrder;



















final class UnreleasableByteBuf
  extends WrappedByteBuf
{
  private SwappedByteBuf swappedBuf;
  
  UnreleasableByteBuf(ByteBuf buf)
  {
    super((buf instanceof UnreleasableByteBuf) ? buf.unwrap() : buf);
  }
  
  public ByteBuf order(ByteOrder endianness)
  {
    if (ObjectUtil.checkNotNull(endianness, "endianness") == order()) {
      return this;
    }
    
    SwappedByteBuf swappedBuf = this.swappedBuf;
    if (swappedBuf == null) {
      this.swappedBuf = (swappedBuf = new SwappedByteBuf(this));
    }
    return swappedBuf;
  }
  
  public ByteBuf asReadOnly()
  {
    return buf.isReadOnly() ? this : new UnreleasableByteBuf(buf.asReadOnly());
  }
  
  public ByteBuf readSlice(int length)
  {
    return new UnreleasableByteBuf(buf.readSlice(length));
  }
  



  public ByteBuf readRetainedSlice(int length)
  {
    return readSlice(length);
  }
  
  public ByteBuf slice()
  {
    return new UnreleasableByteBuf(buf.slice());
  }
  



  public ByteBuf retainedSlice()
  {
    return slice();
  }
  
  public ByteBuf slice(int index, int length)
  {
    return new UnreleasableByteBuf(buf.slice(index, length));
  }
  



  public ByteBuf retainedSlice(int index, int length)
  {
    return slice(index, length);
  }
  
  public ByteBuf duplicate()
  {
    return new UnreleasableByteBuf(buf.duplicate());
  }
  



  public ByteBuf retainedDuplicate()
  {
    return duplicate();
  }
  
  public ByteBuf retain(int increment)
  {
    return this;
  }
  
  public ByteBuf retain()
  {
    return this;
  }
  
  public ByteBuf touch()
  {
    return this;
  }
  
  public ByteBuf touch(Object hint)
  {
    return this;
  }
  
  public boolean release()
  {
    return false;
  }
  
  public boolean release(int decrement)
  {
    return false;
  }
}
