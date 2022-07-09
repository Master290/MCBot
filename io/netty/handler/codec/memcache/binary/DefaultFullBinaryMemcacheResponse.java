package io.netty.handler.codec.memcache.binary;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.internal.ObjectUtil;

























public class DefaultFullBinaryMemcacheResponse
  extends DefaultBinaryMemcacheResponse
  implements FullBinaryMemcacheResponse
{
  private final ByteBuf content;
  
  public DefaultFullBinaryMemcacheResponse(ByteBuf key, ByteBuf extras)
  {
    this(key, extras, Unpooled.buffer(0));
  }
  







  public DefaultFullBinaryMemcacheResponse(ByteBuf key, ByteBuf extras, ByteBuf content)
  {
    super(key, extras);
    this.content = ((ByteBuf)ObjectUtil.checkNotNull(content, "content"));
    setTotalBodyLength(keyLength() + extrasLength() + content.readableBytes());
  }
  
  public ByteBuf content()
  {
    return content;
  }
  
  public FullBinaryMemcacheResponse retain()
  {
    super.retain();
    return this;
  }
  
  public FullBinaryMemcacheResponse retain(int increment)
  {
    super.retain(increment);
    return this;
  }
  
  public FullBinaryMemcacheResponse touch()
  {
    super.touch();
    return this;
  }
  
  public FullBinaryMemcacheResponse touch(Object hint)
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
  
  public FullBinaryMemcacheResponse copy()
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
  
  public FullBinaryMemcacheResponse duplicate()
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
  
  public FullBinaryMemcacheResponse retainedDuplicate()
  {
    return replace(content().retainedDuplicate());
  }
  
  public FullBinaryMemcacheResponse replace(ByteBuf content)
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
  
  private FullBinaryMemcacheResponse newInstance(ByteBuf key, ByteBuf extras, ByteBuf content) {
    DefaultFullBinaryMemcacheResponse newInstance = new DefaultFullBinaryMemcacheResponse(key, extras, content);
    copyMeta(newInstance);
    return newInstance;
  }
}
