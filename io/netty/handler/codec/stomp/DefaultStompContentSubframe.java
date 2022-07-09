package io.netty.handler.codec.stomp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.DefaultByteBufHolder;
import io.netty.handler.codec.DecoderResult;

















public class DefaultStompContentSubframe
  extends DefaultByteBufHolder
  implements StompContentSubframe
{
  private DecoderResult decoderResult = DecoderResult.SUCCESS;
  
  public DefaultStompContentSubframe(ByteBuf content) {
    super(content);
  }
  
  public StompContentSubframe copy()
  {
    return (StompContentSubframe)super.copy();
  }
  
  public StompContentSubframe duplicate()
  {
    return (StompContentSubframe)super.duplicate();
  }
  
  public StompContentSubframe retainedDuplicate()
  {
    return (StompContentSubframe)super.retainedDuplicate();
  }
  
  public StompContentSubframe replace(ByteBuf content)
  {
    return new DefaultStompContentSubframe(content);
  }
  
  public StompContentSubframe retain()
  {
    super.retain();
    return this;
  }
  
  public StompContentSubframe retain(int increment)
  {
    super.retain(increment);
    return this;
  }
  
  public StompContentSubframe touch()
  {
    super.touch();
    return this;
  }
  
  public StompContentSubframe touch(Object hint)
  {
    super.touch(hint);
    return this;
  }
  
  public DecoderResult decoderResult()
  {
    return decoderResult;
  }
  
  public void setDecoderResult(DecoderResult decoderResult)
  {
    this.decoderResult = decoderResult;
  }
  
  public String toString()
  {
    return "DefaultStompContent{decoderResult=" + decoderResult + '}';
  }
}
