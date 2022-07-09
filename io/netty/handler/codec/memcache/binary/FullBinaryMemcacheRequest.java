package io.netty.handler.codec.memcache.binary;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.memcache.FullMemcacheMessage;

public abstract interface FullBinaryMemcacheRequest
  extends BinaryMemcacheRequest, FullMemcacheMessage
{
  public abstract FullBinaryMemcacheRequest copy();
  
  public abstract FullBinaryMemcacheRequest duplicate();
  
  public abstract FullBinaryMemcacheRequest retainedDuplicate();
  
  public abstract FullBinaryMemcacheRequest replace(ByteBuf paramByteBuf);
  
  public abstract FullBinaryMemcacheRequest retain(int paramInt);
  
  public abstract FullBinaryMemcacheRequest retain();
  
  public abstract FullBinaryMemcacheRequest touch();
  
  public abstract FullBinaryMemcacheRequest touch(Object paramObject);
}
