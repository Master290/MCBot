package io.netty.handler.codec.memcache.binary;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.memcache.FullMemcacheMessage;

public abstract interface FullBinaryMemcacheResponse
  extends BinaryMemcacheResponse, FullMemcacheMessage
{
  public abstract FullBinaryMemcacheResponse copy();
  
  public abstract FullBinaryMemcacheResponse duplicate();
  
  public abstract FullBinaryMemcacheResponse retainedDuplicate();
  
  public abstract FullBinaryMemcacheResponse replace(ByteBuf paramByteBuf);
  
  public abstract FullBinaryMemcacheResponse retain(int paramInt);
  
  public abstract FullBinaryMemcacheResponse retain();
  
  public abstract FullBinaryMemcacheResponse touch();
  
  public abstract FullBinaryMemcacheResponse touch(Object paramObject);
}
