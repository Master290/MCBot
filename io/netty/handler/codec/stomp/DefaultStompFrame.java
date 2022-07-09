package io.netty.handler.codec.stomp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.ObjectUtil;
















public class DefaultStompFrame
  extends DefaultStompHeadersSubframe
  implements StompFrame
{
  private final ByteBuf content;
  
  public DefaultStompFrame(StompCommand command)
  {
    this(command, Unpooled.buffer(0));
  }
  
  public DefaultStompFrame(StompCommand command, ByteBuf content) {
    this(command, content, null);
  }
  
  DefaultStompFrame(StompCommand command, ByteBuf content, DefaultStompHeaders headers) {
    super(command, headers);
    this.content = ((ByteBuf)ObjectUtil.checkNotNull(content, "content"));
  }
  
  public ByteBuf content()
  {
    return content;
  }
  
  public StompFrame copy()
  {
    return replace(content.copy());
  }
  
  public StompFrame duplicate()
  {
    return replace(content.duplicate());
  }
  
  public StompFrame retainedDuplicate()
  {
    return replace(content.retainedDuplicate());
  }
  
  public StompFrame replace(ByteBuf content)
  {
    return new DefaultStompFrame(command, content, headers.copy());
  }
  
  public int refCnt()
  {
    return content.refCnt();
  }
  
  public StompFrame retain()
  {
    content.retain();
    return this;
  }
  
  public StompFrame retain(int increment)
  {
    content.retain(increment);
    return this;
  }
  
  public StompFrame touch()
  {
    content.touch();
    return this;
  }
  
  public StompFrame touch(Object hint)
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
    

      "DefaultStompFrame{command=" + command + ", headers=" + headers + ", content=" + content.toString(CharsetUtil.UTF_8) + '}';
  }
}
