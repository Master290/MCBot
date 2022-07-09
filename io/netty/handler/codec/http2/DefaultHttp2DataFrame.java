package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;
























public final class DefaultHttp2DataFrame
  extends AbstractHttp2StreamFrame
  implements Http2DataFrame
{
  private final ByteBuf content;
  private final boolean endStream;
  private final int padding;
  private final int initialFlowControlledBytes;
  
  public DefaultHttp2DataFrame(ByteBuf content)
  {
    this(content, false);
  }
  




  public DefaultHttp2DataFrame(boolean endStream)
  {
    this(Unpooled.EMPTY_BUFFER, endStream);
  }
  





  public DefaultHttp2DataFrame(ByteBuf content, boolean endStream)
  {
    this(content, endStream, 0);
  }
  







  public DefaultHttp2DataFrame(ByteBuf content, boolean endStream, int padding)
  {
    this.content = ((ByteBuf)ObjectUtil.checkNotNull(content, "content"));
    this.endStream = endStream;
    Http2CodecUtil.verifyPadding(padding);
    this.padding = padding;
    if (content().readableBytes() + padding > 2147483647L) {
      throw new IllegalArgumentException("content + padding must be <= Integer.MAX_VALUE");
    }
    initialFlowControlledBytes = (content().readableBytes() + padding);
  }
  
  public DefaultHttp2DataFrame stream(Http2FrameStream stream)
  {
    super.stream(stream);
    return this;
  }
  
  public String name()
  {
    return "DATA";
  }
  
  public boolean isEndStream()
  {
    return endStream;
  }
  
  public int padding()
  {
    return padding;
  }
  
  public ByteBuf content()
  {
    return ByteBufUtil.ensureAccessible(content);
  }
  
  public int initialFlowControlledBytes()
  {
    return initialFlowControlledBytes;
  }
  
  public DefaultHttp2DataFrame copy()
  {
    return replace(content().copy());
  }
  
  public DefaultHttp2DataFrame duplicate()
  {
    return replace(content().duplicate());
  }
  
  public DefaultHttp2DataFrame retainedDuplicate()
  {
    return replace(content().retainedDuplicate());
  }
  
  public DefaultHttp2DataFrame replace(ByteBuf content)
  {
    return new DefaultHttp2DataFrame(content, endStream, padding);
  }
  
  public int refCnt()
  {
    return content.refCnt();
  }
  
  public boolean release()
  {
    return content.release();
  }
  
  public boolean release(int decrement)
  {
    return content.release(decrement);
  }
  
  public DefaultHttp2DataFrame retain()
  {
    content.retain();
    return this;
  }
  
  public DefaultHttp2DataFrame retain(int increment)
  {
    content.retain(increment);
    return this;
  }
  
  public String toString()
  {
    return StringUtil.simpleClassName(this) + "(stream=" + stream() + ", content=" + content + ", endStream=" + endStream + ", padding=" + padding + ')';
  }
  

  public DefaultHttp2DataFrame touch()
  {
    content.touch();
    return this;
  }
  
  public DefaultHttp2DataFrame touch(Object hint)
  {
    content.touch(hint);
    return this;
  }
  
  public boolean equals(Object o)
  {
    if (!(o instanceof DefaultHttp2DataFrame)) {
      return false;
    }
    DefaultHttp2DataFrame other = (DefaultHttp2DataFrame)o;
    return (super.equals(other)) && (content.equals(other.content())) && (endStream == endStream) && (padding == padding);
  }
  

  public int hashCode()
  {
    int hash = super.hashCode();
    hash = hash * 31 + content.hashCode();
    hash = hash * 31 + (endStream ? 0 : 1);
    hash = hash * 31 + padding;
    return hash;
  }
}
