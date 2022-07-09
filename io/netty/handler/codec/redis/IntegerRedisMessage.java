package io.netty.handler.codec.redis;

import io.netty.util.internal.StringUtil;
























public final class IntegerRedisMessage
  implements RedisMessage
{
  private final long value;
  
  public IntegerRedisMessage(long value)
  {
    this.value = value;
  }
  




  public long value()
  {
    return value;
  }
  
  public String toString()
  {
    return 
    


      StringUtil.simpleClassName(this) + '[' + "value=" + value + ']';
  }
}
