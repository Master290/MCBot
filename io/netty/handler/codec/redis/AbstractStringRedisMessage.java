package io.netty.handler.codec.redis;

import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;



















public abstract class AbstractStringRedisMessage
  implements RedisMessage
{
  private final String content;
  
  AbstractStringRedisMessage(String content)
  {
    this.content = ((String)ObjectUtil.checkNotNull(content, "content"));
  }
  




  public final String content()
  {
    return content;
  }
  
  public String toString()
  {
    return 
    


      StringUtil.simpleClassName(this) + '[' + "content=" + content + ']';
  }
}
