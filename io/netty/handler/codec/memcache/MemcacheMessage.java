package io.netty.handler.codec.memcache;

import io.netty.util.ReferenceCounted;

public abstract interface MemcacheMessage
  extends MemcacheObject, ReferenceCounted
{
  public abstract MemcacheMessage retain();
  
  public abstract MemcacheMessage retain(int paramInt);
  
  public abstract MemcacheMessage touch();
  
  public abstract MemcacheMessage touch(Object paramObject);
}
