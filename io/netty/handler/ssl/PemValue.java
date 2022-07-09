package io.netty.handler.ssl;

import io.netty.buffer.ByteBuf;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.IllegalReferenceCountException;
import io.netty.util.internal.ObjectUtil;






















class PemValue
  extends AbstractReferenceCounted
  implements PemEncoded
{
  private final ByteBuf content;
  private final boolean sensitive;
  
  PemValue(ByteBuf content, boolean sensitive)
  {
    this.content = ((ByteBuf)ObjectUtil.checkNotNull(content, "content"));
    this.sensitive = sensitive;
  }
  
  public boolean isSensitive()
  {
    return sensitive;
  }
  
  public ByteBuf content()
  {
    int count = refCnt();
    if (count <= 0) {
      throw new IllegalReferenceCountException(count);
    }
    
    return content;
  }
  
  public PemValue copy()
  {
    return replace(content.copy());
  }
  
  public PemValue duplicate()
  {
    return replace(content.duplicate());
  }
  
  public PemValue retainedDuplicate()
  {
    return replace(content.retainedDuplicate());
  }
  
  public PemValue replace(ByteBuf content)
  {
    return new PemValue(content, sensitive);
  }
  
  public PemValue touch()
  {
    return (PemValue)super.touch();
  }
  
  public PemValue touch(Object hint)
  {
    content.touch(hint);
    return this;
  }
  
  public PemValue retain()
  {
    return (PemValue)super.retain();
  }
  
  public PemValue retain(int increment)
  {
    return (PemValue)super.retain(increment);
  }
  
  protected void deallocate()
  {
    if (sensitive) {
      SslUtils.zeroout(content);
    }
    content.release();
  }
}
