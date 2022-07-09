package io.netty.handler.ssl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;

abstract interface PemEncoded
  extends ByteBufHolder
{
  public abstract boolean isSensitive();
  
  public abstract PemEncoded copy();
  
  public abstract PemEncoded duplicate();
  
  public abstract PemEncoded retainedDuplicate();
  
  public abstract PemEncoded replace(ByteBuf paramByteBuf);
  
  public abstract PemEncoded retain();
  
  public abstract PemEncoded retain(int paramInt);
  
  public abstract PemEncoded touch();
  
  public abstract PemEncoded touch(Object paramObject);
}
