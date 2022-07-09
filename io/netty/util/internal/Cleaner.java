package io.netty.util.internal;

import java.nio.ByteBuffer;

abstract interface Cleaner
{
  public abstract void freeDirectBuffer(ByteBuffer paramByteBuffer);
}
