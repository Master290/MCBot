package io.netty.handler.codec.http.multipart;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.internal.ObjectUtil;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

















final class InternalAttribute
  extends AbstractReferenceCounted
  implements InterfaceHttpData
{
  private final List<ByteBuf> value = new ArrayList();
  private final Charset charset;
  private int size;
  
  InternalAttribute(Charset charset) {
    this.charset = charset;
  }
  
  public InterfaceHttpData.HttpDataType getHttpDataType()
  {
    return InterfaceHttpData.HttpDataType.InternalAttribute;
  }
  
  public void addValue(String value) {
    ObjectUtil.checkNotNull(value, "value");
    ByteBuf buf = Unpooled.copiedBuffer(value, charset);
    this.value.add(buf);
    size += buf.readableBytes();
  }
  
  public void addValue(String value, int rank) {
    ObjectUtil.checkNotNull(value, "value");
    ByteBuf buf = Unpooled.copiedBuffer(value, charset);
    this.value.add(rank, buf);
    size += buf.readableBytes();
  }
  
  public void setValue(String value, int rank) {
    ObjectUtil.checkNotNull(value, "value");
    ByteBuf buf = Unpooled.copiedBuffer(value, charset);
    ByteBuf old = (ByteBuf)this.value.set(rank, buf);
    if (old != null) {
      size -= old.readableBytes();
      old.release();
    }
    size += buf.readableBytes();
  }
  
  public int hashCode()
  {
    return getName().hashCode();
  }
  
  public boolean equals(Object o)
  {
    if (!(o instanceof InternalAttribute)) {
      return false;
    }
    InternalAttribute attribute = (InternalAttribute)o;
    return getName().equalsIgnoreCase(attribute.getName());
  }
  
  public int compareTo(InterfaceHttpData o)
  {
    if (!(o instanceof InternalAttribute))
    {
      throw new ClassCastException("Cannot compare " + getHttpDataType() + " with " + o.getHttpDataType());
    }
    return compareTo((InternalAttribute)o);
  }
  
  public int compareTo(InternalAttribute o) {
    return getName().compareToIgnoreCase(o.getName());
  }
  
  public String toString()
  {
    StringBuilder result = new StringBuilder();
    for (ByteBuf elt : value) {
      result.append(elt.toString(charset));
    }
    return result.toString();
  }
  
  public int size() {
    return size;
  }
  
  public ByteBuf toByteBuf() {
    return Unpooled.compositeBuffer().addComponents(value).writerIndex(size()).readerIndex(0);
  }
  
  public String getName()
  {
    return "InternalAttribute";
  }
  


  protected void deallocate() {}
  

  public InterfaceHttpData retain()
  {
    for (ByteBuf buf : value) {
      buf.retain();
    }
    return this;
  }
  
  public InterfaceHttpData retain(int increment)
  {
    for (ByteBuf buf : value) {
      buf.retain(increment);
    }
    return this;
  }
  
  public InterfaceHttpData touch()
  {
    for (ByteBuf buf : value) {
      buf.touch();
    }
    return this;
  }
  
  public InterfaceHttpData touch(Object hint)
  {
    for (ByteBuf buf : value) {
      buf.touch(hint);
    }
    return this;
  }
}
