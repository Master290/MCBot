package io.netty.handler.codec.stomp;

import io.netty.buffer.ByteBuf;
















public class DefaultLastStompContentSubframe
  extends DefaultStompContentSubframe
  implements LastStompContentSubframe
{
  public DefaultLastStompContentSubframe(ByteBuf content)
  {
    super(content);
  }
  
  public LastStompContentSubframe copy()
  {
    return (LastStompContentSubframe)super.copy();
  }
  
  public LastStompContentSubframe duplicate()
  {
    return (LastStompContentSubframe)super.duplicate();
  }
  
  public LastStompContentSubframe retainedDuplicate()
  {
    return (LastStompContentSubframe)super.retainedDuplicate();
  }
  
  public LastStompContentSubframe replace(ByteBuf content)
  {
    return new DefaultLastStompContentSubframe(content);
  }
  
  public DefaultLastStompContentSubframe retain()
  {
    super.retain();
    return this;
  }
  
  public LastStompContentSubframe retain(int increment)
  {
    super.retain(increment);
    return this;
  }
  
  public LastStompContentSubframe touch()
  {
    super.touch();
    return this;
  }
  
  public LastStompContentSubframe touch(Object hint)
  {
    super.touch(hint);
    return this;
  }
  
  public String toString()
  {
    return 
      "DefaultLastStompContent{decoderResult=" + decoderResult() + '}';
  }
}
