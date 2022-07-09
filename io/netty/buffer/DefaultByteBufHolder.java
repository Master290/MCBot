package io.netty.buffer;

import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;


















public class DefaultByteBufHolder
  implements ByteBufHolder
{
  private final ByteBuf data;
  
  public DefaultByteBufHolder(ByteBuf data)
  {
    this.data = ((ByteBuf)ObjectUtil.checkNotNull(data, "data"));
  }
  
  public ByteBuf content()
  {
    return ByteBufUtil.ensureAccessible(data);
  }
  





  public ByteBufHolder copy()
  {
    return replace(data.copy());
  }
  





  public ByteBufHolder duplicate()
  {
    return replace(data.duplicate());
  }
  





  public ByteBufHolder retainedDuplicate()
  {
    return replace(data.retainedDuplicate());
  }
  







  public ByteBufHolder replace(ByteBuf content)
  {
    return new DefaultByteBufHolder(content);
  }
  
  public int refCnt()
  {
    return data.refCnt();
  }
  
  public ByteBufHolder retain()
  {
    data.retain();
    return this;
  }
  
  public ByteBufHolder retain(int increment)
  {
    data.retain(increment);
    return this;
  }
  
  public ByteBufHolder touch()
  {
    data.touch();
    return this;
  }
  
  public ByteBufHolder touch(Object hint)
  {
    data.touch(hint);
    return this;
  }
  
  public boolean release()
  {
    return data.release();
  }
  
  public boolean release(int decrement)
  {
    return data.release(decrement);
  }
  



  protected final String contentToString()
  {
    return data.toString();
  }
  
  public String toString()
  {
    return StringUtil.simpleClassName(this) + '(' + contentToString() + ')';
  }
  











  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if ((o != null) && (getClass() == o.getClass())) {
      return data.equals(data);
    }
    return false;
  }
  
  public int hashCode()
  {
    return data.hashCode();
  }
}
