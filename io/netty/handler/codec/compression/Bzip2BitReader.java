package io.netty.handler.codec.compression;

import io.netty.buffer.ByteBuf;



































class Bzip2BitReader
{
  private static final int MAX_COUNT_OF_READABLE_BYTES = 268435455;
  private ByteBuf in;
  private long bitBuffer;
  private int bitCount;
  
  Bzip2BitReader() {}
  
  void setByteBuf(ByteBuf in)
  {
    this.in = in;
  }
  




  int readBits(int count)
  {
    if ((count < 0) || (count > 32)) {
      throw new IllegalArgumentException("count: " + count + " (expected: 0-32 )");
    }
    int bitCount = this.bitCount;
    long bitBuffer = this.bitBuffer;
    
    if (bitCount < count) { int offset;
      int offset;
      int offset;
      long readData; int offset; switch (in.readableBytes()) {
      case 1: 
        long readData = in.readUnsignedByte();
        offset = 8;
        break;
      
      case 2: 
        long readData = in.readUnsignedShort();
        offset = 16;
        break;
      
      case 3: 
        long readData = in.readUnsignedMedium();
        offset = 24;
        break;
      
      default: 
        readData = in.readUnsignedInt();
        offset = 32;
      }
      
      

      bitBuffer = bitBuffer << offset | readData;
      bitCount += offset;
      this.bitBuffer = bitBuffer;
    }
    
    this.bitCount = (bitCount -= count);
    return (int)(bitBuffer >>> bitCount & (count != 32 ? (1 << count) - 1 : 4294967295L));
  }
  



  boolean readBoolean()
  {
    return readBits(1) != 0;
  }
  



  int readInt()
  {
    return readBits(32);
  }
  


  void refill()
  {
    int readData = in.readUnsignedByte();
    bitBuffer = (bitBuffer << 8 | readData);
    bitCount += 8;
  }
  



  boolean isReadable()
  {
    return (bitCount > 0) || (in.isReadable());
  }
  




  boolean hasReadableBits(int count)
  {
    if (count < 0) {
      throw new IllegalArgumentException("count: " + count + " (expected value greater than 0)");
    }
    return (bitCount >= count) || ((in.readableBytes() << 3 & 0x7FFFFFFF) >= count - bitCount);
  }
  




  boolean hasReadableBytes(int count)
  {
    if ((count < 0) || (count > 268435455)) {
      throw new IllegalArgumentException("count: " + count + " (expected: 0-" + 268435455 + ')');
    }
    
    return hasReadableBits(count << 3);
  }
}
