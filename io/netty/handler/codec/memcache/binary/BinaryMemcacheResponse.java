package io.netty.handler.codec.memcache.binary;

public abstract interface BinaryMemcacheResponse
  extends BinaryMemcacheMessage
{
  public abstract short status();
  
  public abstract BinaryMemcacheResponse setStatus(short paramShort);
  
  public abstract BinaryMemcacheResponse retain();
  
  public abstract BinaryMemcacheResponse retain(int paramInt);
  
  public abstract BinaryMemcacheResponse touch();
  
  public abstract BinaryMemcacheResponse touch(Object paramObject);
}
