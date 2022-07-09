package io.netty.handler.codec.redis;

import io.netty.util.internal.StringUtil;






















public class ArrayHeaderRedisMessage
  implements RedisMessage
{
  private final long length;
  
  public ArrayHeaderRedisMessage(long length)
  {
    if (length < -1L) {
      throw new RedisCodecException("length: " + length + " (expected: >= " + -1 + ")");
    }
    this.length = length;
  }
  


  public final long length()
  {
    return length;
  }
  




  public boolean isNull()
  {
    return length == -1L;
  }
  
  public String toString()
  {
    return 
    


      StringUtil.simpleClassName(this) + '[' + "length=" + length + ']';
  }
}
