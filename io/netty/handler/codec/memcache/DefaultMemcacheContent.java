package io.netty.handler.codec.memcache;

import io.netty.buffer.ByteBuf;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;





















public class DefaultMemcacheContent
  extends AbstractMemcacheObject
  implements MemcacheContent
{
  private final ByteBuf content;
  
  public DefaultMemcacheContent(ByteBuf content)
  {
    this.content = ((ByteBuf)ObjectUtil.checkNotNull(content, "content"));
  }
  
  public ByteBuf content()
  {
    return content;
  }
  
  public MemcacheContent copy()
  {
    return replace(content.copy());
  }
  
  public MemcacheContent duplicate()
  {
    return replace(content.duplicate());
  }
  
  public MemcacheContent retainedDuplicate()
  {
    return replace(content.retainedDuplicate());
  }
  
  public MemcacheContent replace(ByteBuf content)
  {
    return new DefaultMemcacheContent(content);
  }
  
  public MemcacheContent retain()
  {
    super.retain();
    return this;
  }
  
  public MemcacheContent retain(int increment)
  {
    super.retain(increment);
    return this;
  }
  
  public MemcacheContent touch()
  {
    super.touch();
    return this;
  }
  
  public MemcacheContent touch(Object hint)
  {
    content.touch(hint);
    return this;
  }
  
  protected void deallocate()
  {
    content.release();
  }
  
  public String toString()
  {
    return 
      StringUtil.simpleClassName(this) + "(data: " + content() + ", decoderResult: " + decoderResult() + ')';
  }
}
