package io.netty.handler.codec.memcache;

import io.netty.buffer.ByteBuf;

public abstract interface FullMemcacheMessage
  extends MemcacheMessage, LastMemcacheContent
{
  public abstract FullMemcacheMessage copy();
  
  public abstract FullMemcacheMessage duplicate();
  
  public abstract FullMemcacheMessage retainedDuplicate();
  
  public abstract FullMemcacheMessage replace(ByteBuf paramByteBuf);
  
  public abstract FullMemcacheMessage retain(int paramInt);
  
  public abstract FullMemcacheMessage retain();
  
  public abstract FullMemcacheMessage touch();
  
  public abstract FullMemcacheMessage touch(Object paramObject);
}
