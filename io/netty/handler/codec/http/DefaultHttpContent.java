package io.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;



















public class DefaultHttpContent
  extends DefaultHttpObject
  implements HttpContent
{
  private final ByteBuf content;
  
  public DefaultHttpContent(ByteBuf content)
  {
    this.content = ((ByteBuf)ObjectUtil.checkNotNull(content, "content"));
  }
  
  public ByteBuf content()
  {
    return content;
  }
  
  public HttpContent copy()
  {
    return replace(content.copy());
  }
  
  public HttpContent duplicate()
  {
    return replace(content.duplicate());
  }
  
  public HttpContent retainedDuplicate()
  {
    return replace(content.retainedDuplicate());
  }
  
  public HttpContent replace(ByteBuf content)
  {
    return new DefaultHttpContent(content);
  }
  
  public int refCnt()
  {
    return content.refCnt();
  }
  
  public HttpContent retain()
  {
    content.retain();
    return this;
  }
  
  public HttpContent retain(int increment)
  {
    content.retain(increment);
    return this;
  }
  
  public HttpContent touch()
  {
    content.touch();
    return this;
  }
  
  public HttpContent touch(Object hint)
  {
    content.touch(hint);
    return this;
  }
  
  public boolean release()
  {
    return content.release();
  }
  
  public boolean release(int decrement)
  {
    return content.release(decrement);
  }
  
  public String toString()
  {
    return 
      StringUtil.simpleClassName(this) + "(data: " + content() + ", decoderResult: " + decoderResult() + ')';
  }
}
