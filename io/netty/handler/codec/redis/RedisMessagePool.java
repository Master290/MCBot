package io.netty.handler.codec.redis;

import io.netty.buffer.ByteBuf;

public abstract interface RedisMessagePool
{
  public abstract SimpleStringRedisMessage getSimpleString(String paramString);
  
  public abstract SimpleStringRedisMessage getSimpleString(ByteBuf paramByteBuf);
  
  public abstract ErrorRedisMessage getError(String paramString);
  
  public abstract ErrorRedisMessage getError(ByteBuf paramByteBuf);
  
  public abstract IntegerRedisMessage getInteger(long paramLong);
  
  public abstract IntegerRedisMessage getInteger(ByteBuf paramByteBuf);
  
  public abstract byte[] getByteBufOfInteger(long paramLong);
}
