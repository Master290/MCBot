package io.netty.buffer.search;

import io.netty.util.ByteProcessor;

public abstract interface SearchProcessor
  extends ByteProcessor
{
  public abstract void reset();
}
