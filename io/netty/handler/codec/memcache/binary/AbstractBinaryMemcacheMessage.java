package io.netty.handler.codec.memcache.binary;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.memcache.AbstractMemcacheObject;


































public abstract class AbstractBinaryMemcacheMessage
  extends AbstractMemcacheObject
  implements BinaryMemcacheMessage
{
  private ByteBuf key;
  private ByteBuf extras;
  private byte magic;
  private byte opcode;
  private short keyLength;
  private byte extrasLength;
  private byte dataType;
  private int totalBodyLength;
  private int opaque;
  private long cas;
  
  protected AbstractBinaryMemcacheMessage(ByteBuf key, ByteBuf extras)
  {
    this.key = key;
    keyLength = (key == null ? 0 : (short)key.readableBytes());
    this.extras = extras;
    extrasLength = (extras == null ? 0 : (byte)extras.readableBytes());
    totalBodyLength = (keyLength + extrasLength);
  }
  
  public ByteBuf key()
  {
    return key;
  }
  
  public ByteBuf extras()
  {
    return extras;
  }
  
  public BinaryMemcacheMessage setKey(ByteBuf key)
  {
    if (this.key != null) {
      this.key.release();
    }
    this.key = key;
    short oldKeyLength = keyLength;
    keyLength = (key == null ? 0 : (short)key.readableBytes());
    totalBodyLength = (totalBodyLength + keyLength - oldKeyLength);
    return this;
  }
  
  public BinaryMemcacheMessage setExtras(ByteBuf extras)
  {
    if (this.extras != null) {
      this.extras.release();
    }
    this.extras = extras;
    short oldExtrasLength = (short)extrasLength;
    extrasLength = (extras == null ? 0 : (byte)extras.readableBytes());
    totalBodyLength = (totalBodyLength + extrasLength - oldExtrasLength);
    return this;
  }
  
  public byte magic()
  {
    return magic;
  }
  
  public BinaryMemcacheMessage setMagic(byte magic)
  {
    this.magic = magic;
    return this;
  }
  
  public long cas()
  {
    return cas;
  }
  
  public BinaryMemcacheMessage setCas(long cas)
  {
    this.cas = cas;
    return this;
  }
  
  public int opaque()
  {
    return opaque;
  }
  
  public BinaryMemcacheMessage setOpaque(int opaque)
  {
    this.opaque = opaque;
    return this;
  }
  
  public int totalBodyLength()
  {
    return totalBodyLength;
  }
  
  public BinaryMemcacheMessage setTotalBodyLength(int totalBodyLength)
  {
    this.totalBodyLength = totalBodyLength;
    return this;
  }
  
  public byte dataType()
  {
    return dataType;
  }
  
  public BinaryMemcacheMessage setDataType(byte dataType)
  {
    this.dataType = dataType;
    return this;
  }
  
  public byte extrasLength()
  {
    return extrasLength;
  }
  






  BinaryMemcacheMessage setExtrasLength(byte extrasLength)
  {
    this.extrasLength = extrasLength;
    return this;
  }
  
  public short keyLength()
  {
    return keyLength;
  }
  






  BinaryMemcacheMessage setKeyLength(short keyLength)
  {
    this.keyLength = keyLength;
    return this;
  }
  
  public byte opcode()
  {
    return opcode;
  }
  
  public BinaryMemcacheMessage setOpcode(byte opcode)
  {
    this.opcode = opcode;
    return this;
  }
  
  public BinaryMemcacheMessage retain()
  {
    super.retain();
    return this;
  }
  
  public BinaryMemcacheMessage retain(int increment)
  {
    super.retain(increment);
    return this;
  }
  
  protected void deallocate()
  {
    if (key != null) {
      key.release();
    }
    if (extras != null) {
      extras.release();
    }
  }
  
  public BinaryMemcacheMessage touch()
  {
    super.touch();
    return this;
  }
  
  public BinaryMemcacheMessage touch(Object hint)
  {
    if (key != null) {
      key.touch(hint);
    }
    if (extras != null) {
      extras.touch(hint);
    }
    return this;
  }
  




  void copyMeta(AbstractBinaryMemcacheMessage dst)
  {
    magic = magic;
    opcode = opcode;
    keyLength = keyLength;
    extrasLength = extrasLength;
    dataType = dataType;
    totalBodyLength = totalBodyLength;
    opaque = opaque;
    cas = cas;
  }
}
