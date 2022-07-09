package io.netty.handler.codec.memcache.binary;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.internal.ObjectUtil;

























public class DefaultFullBinaryMemcacheRequest
  extends DefaultBinaryMemcacheRequest
  implements FullBinaryMemcacheRequest
{
  private final ByteBuf content;
  
  public DefaultFullBinaryMemcacheRequest(ByteBuf key, ByteBuf extras)
  {
    this(key, extras, Unpooled.buffer(0));
  }
  







  public DefaultFullBinaryMemcacheRequest(ByteBuf key, ByteBuf extras, ByteBuf content)
  {
    super(key, extras);
    this.content = ((ByteBuf)ObjectUtil.checkNotNull(content, "content"));
    setTotalBodyLength(keyLength() + extrasLength() + content.readableBytes());
  }
  
  public ByteBuf content()
  {
    return content;
  }
  
  public FullBinaryMemcacheRequest retain()
  {
    super.retain();
    return this;
  }
  
  public FullBinaryMemcacheRequest retain(int increment)
  {
    super.retain(increment);
    return this;
  }
  
  public FullBinaryMemcacheRequest touch()
  {
    super.touch();
    return this;
  }
  
  public FullBinaryMemcacheRequest touch(Object hint)
  {
    super.touch(hint);
    content.touch(hint);
    return this;
  }
  
  protected void deallocate()
  {
    super.deallocate();
    content.release();
  }
  
  public FullBinaryMemcacheRequest copy()
  {
    ByteBuf key = key();
    if (key != null) {
      key = key.copy();
    }
    ByteBuf extras = extras();
    if (extras != null) {
      extras = extras.copy();
    }
    return newInstance(key, extras, content().copy());
  }
  
  public FullBinaryMemcacheRequest duplicate()
  {
    ByteBuf key = key();
    if (key != null) {
      key = key.duplicate();
    }
    ByteBuf extras = extras();
    if (extras != null) {
      extras = extras.duplicate();
    }
    return newInstance(key, extras, content().duplicate());
  }
  
  public FullBinaryMemcacheRequest retainedDuplicate()
  {
    return replace(content().retainedDuplicate());
  }
  
  public FullBinaryMemcacheRequest replace(ByteBuf content)
  {
    ByteBuf key = key();
    if (key != null) {
      key = key.retainedDuplicate();
    }
    ByteBuf extras = extras();
    if (extras != null) {
      extras = extras.retainedDuplicate();
    }
    return newInstance(key, extras, content);
  }
  
  private DefaultFullBinaryMemcacheRequest newInstance(ByteBuf key, ByteBuf extras, ByteBuf content) {
    DefaultFullBinaryMemcacheRequest newInstance = new DefaultFullBinaryMemcacheRequest(key, extras, content);
    copyMeta(newInstance);
    return newInstance;
  }
}
