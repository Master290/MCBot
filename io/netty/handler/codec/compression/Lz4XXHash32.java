package io.netty.handler.codec.compression;

import io.netty.buffer.ByteBuf;
import net.jpountz.xxhash.XXHash32;
import net.jpountz.xxhash.XXHashFactory;










































public final class Lz4XXHash32
  extends ByteBufChecksum
{
  private static final XXHash32 XXHASH32 = XXHashFactory.fastestInstance().hash32();
  
  private final int seed;
  private boolean used;
  private int value;
  
  public Lz4XXHash32(int seed)
  {
    this.seed = seed;
  }
  
  public void update(int b)
  {
    throw new UnsupportedOperationException();
  }
  
  public void update(byte[] b, int off, int len)
  {
    if (used) {
      throw new IllegalStateException();
    }
    value = XXHASH32.hash(b, off, len, seed);
    used = true;
  }
  
  public void update(ByteBuf b, int off, int len)
  {
    if (used) {
      throw new IllegalStateException();
    }
    if (b.hasArray()) {
      value = XXHASH32.hash(b.array(), b.arrayOffset() + off, len, seed);
    } else {
      value = XXHASH32.hash(CompressionUtil.safeNioBuffer(b, off, len), seed);
    }
    used = true;
  }
  
  public long getValue()
  {
    if (!used) {
      throw new IllegalStateException();
    }
    





    return value & 0xFFFFFFF;
  }
  
  public void reset()
  {
    used = false;
  }
}
