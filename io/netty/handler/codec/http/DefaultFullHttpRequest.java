package io.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.util.IllegalReferenceCountException;
import io.netty.util.internal.ObjectUtil;



















public class DefaultFullHttpRequest
  extends DefaultHttpRequest
  implements FullHttpRequest
{
  private final ByteBuf content;
  private final HttpHeaders trailingHeader;
  private int hash;
  
  public DefaultFullHttpRequest(HttpVersion httpVersion, HttpMethod method, String uri)
  {
    this(httpVersion, method, uri, Unpooled.buffer(0));
  }
  
  public DefaultFullHttpRequest(HttpVersion httpVersion, HttpMethod method, String uri, ByteBuf content) {
    this(httpVersion, method, uri, content, true);
  }
  
  public DefaultFullHttpRequest(HttpVersion httpVersion, HttpMethod method, String uri, boolean validateHeaders) {
    this(httpVersion, method, uri, Unpooled.buffer(0), validateHeaders);
  }
  
  public DefaultFullHttpRequest(HttpVersion httpVersion, HttpMethod method, String uri, ByteBuf content, boolean validateHeaders)
  {
    super(httpVersion, method, uri, validateHeaders);
    this.content = ((ByteBuf)ObjectUtil.checkNotNull(content, "content"));
    trailingHeader = new DefaultHttpHeaders(validateHeaders);
  }
  
  public DefaultFullHttpRequest(HttpVersion httpVersion, HttpMethod method, String uri, ByteBuf content, HttpHeaders headers, HttpHeaders trailingHeader)
  {
    super(httpVersion, method, uri, headers);
    this.content = ((ByteBuf)ObjectUtil.checkNotNull(content, "content"));
    this.trailingHeader = ((HttpHeaders)ObjectUtil.checkNotNull(trailingHeader, "trailingHeader"));
  }
  
  public HttpHeaders trailingHeaders()
  {
    return trailingHeader;
  }
  
  public ByteBuf content()
  {
    return content;
  }
  
  public int refCnt()
  {
    return content.refCnt();
  }
  
  public FullHttpRequest retain()
  {
    content.retain();
    return this;
  }
  
  public FullHttpRequest retain(int increment)
  {
    content.retain(increment);
    return this;
  }
  
  public FullHttpRequest touch()
  {
    content.touch();
    return this;
  }
  
  public FullHttpRequest touch(Object hint)
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
  
  public FullHttpRequest setProtocolVersion(HttpVersion version)
  {
    super.setProtocolVersion(version);
    return this;
  }
  
  public FullHttpRequest setMethod(HttpMethod method)
  {
    super.setMethod(method);
    return this;
  }
  
  public FullHttpRequest setUri(String uri)
  {
    super.setUri(uri);
    return this;
  }
  
  public FullHttpRequest copy()
  {
    return replace(content().copy());
  }
  
  public FullHttpRequest duplicate()
  {
    return replace(content().duplicate());
  }
  
  public FullHttpRequest retainedDuplicate()
  {
    return replace(content().retainedDuplicate());
  }
  

  public FullHttpRequest replace(ByteBuf content)
  {
    FullHttpRequest request = new DefaultFullHttpRequest(protocolVersion(), method(), uri(), content, headers().copy(), trailingHeaders().copy());
    request.setDecoderResult(decoderResult());
    return request;
  }
  
  public int hashCode()
  {
    int hash = this.hash;
    if (hash == 0) {
      if (ByteBufUtil.isAccessible(content())) {
        try {
          hash = 31 + content().hashCode();
        }
        catch (IllegalReferenceCountException ignored) {
          hash = 31;
        }
      } else {
        hash = 31;
      }
      hash = 31 * hash + trailingHeaders().hashCode();
      hash = 31 * hash + super.hashCode();
      this.hash = hash;
    }
    return hash;
  }
  
  public boolean equals(Object o)
  {
    if (!(o instanceof DefaultFullHttpRequest)) {
      return false;
    }
    
    DefaultFullHttpRequest other = (DefaultFullHttpRequest)o;
    
    return (super.equals(other)) && 
      (content().equals(other.content())) && 
      (trailingHeaders().equals(other.trailingHeaders()));
  }
  
  public String toString()
  {
    return HttpMessageUtil.appendFullRequest(new StringBuilder(256), this).toString();
  }
}
