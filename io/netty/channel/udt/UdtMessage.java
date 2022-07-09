package io.netty.channel.udt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.DefaultByteBufHolder;























@Deprecated
public final class UdtMessage
  extends DefaultByteBufHolder
{
  public UdtMessage(ByteBuf data)
  {
    super(data);
  }
  
  public UdtMessage copy()
  {
    return (UdtMessage)super.copy();
  }
  
  public UdtMessage duplicate()
  {
    return (UdtMessage)super.duplicate();
  }
  
  public UdtMessage retainedDuplicate()
  {
    return (UdtMessage)super.retainedDuplicate();
  }
  
  public UdtMessage replace(ByteBuf content)
  {
    return new UdtMessage(content);
  }
  
  public UdtMessage retain()
  {
    super.retain();
    return this;
  }
  
  public UdtMessage retain(int increment)
  {
    super.retain(increment);
    return this;
  }
  
  public UdtMessage touch()
  {
    super.touch();
    return this;
  }
  
  public UdtMessage touch(Object hint)
  {
    super.touch(hint);
    return this;
  }
}
