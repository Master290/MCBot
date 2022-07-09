package io.netty.handler.codec.memcache.binary;

public abstract interface BinaryMemcacheRequest
  extends BinaryMemcacheMessage
{
  public abstract short reserved();
  
  public abstract BinaryMemcacheRequest setReserved(short paramShort);
  
  public abstract BinaryMemcacheRequest retain();
  
  public abstract BinaryMemcacheRequest retain(int paramInt);
  
  public abstract BinaryMemcacheRequest touch();
  
  public abstract BinaryMemcacheRequest touch(Object paramObject);
}
