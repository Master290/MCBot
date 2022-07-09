package io.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.util.IllegalReferenceCountException;
import io.netty.util.internal.ObjectUtil;





















public class DefaultFullHttpResponse
  extends DefaultHttpResponse
  implements FullHttpResponse
{
  private final ByteBuf content;
  private final HttpHeaders trailingHeaders;
  private int hash;
  
  public DefaultFullHttpResponse(HttpVersion version, HttpResponseStatus status)
  {
    this(version, status, Unpooled.buffer(0));
  }
  
  public DefaultFullHttpResponse(HttpVersion version, HttpResponseStatus status, ByteBuf content) {
    this(version, status, content, true);
  }
  
  public DefaultFullHttpResponse(HttpVersion version, HttpResponseStatus status, boolean validateHeaders) {
    this(version, status, Unpooled.buffer(0), validateHeaders, false);
  }
  
  public DefaultFullHttpResponse(HttpVersion version, HttpResponseStatus status, boolean validateHeaders, boolean singleFieldHeaders)
  {
    this(version, status, Unpooled.buffer(0), validateHeaders, singleFieldHeaders);
  }
  
  public DefaultFullHttpResponse(HttpVersion version, HttpResponseStatus status, ByteBuf content, boolean validateHeaders)
  {
    this(version, status, content, validateHeaders, false);
  }
  
  public DefaultFullHttpResponse(HttpVersion version, HttpResponseStatus status, ByteBuf content, boolean validateHeaders, boolean singleFieldHeaders)
  {
    super(version, status, validateHeaders, singleFieldHeaders);
    this.content = ((ByteBuf)ObjectUtil.checkNotNull(content, "content"));
    trailingHeaders = (singleFieldHeaders ? new CombinedHttpHeaders(validateHeaders) : new DefaultHttpHeaders(validateHeaders));
  }
  

  public DefaultFullHttpResponse(HttpVersion version, HttpResponseStatus status, ByteBuf content, HttpHeaders headers, HttpHeaders trailingHeaders)
  {
    super(version, status, headers);
    this.content = ((ByteBuf)ObjectUtil.checkNotNull(content, "content"));
    this.trailingHeaders = ((HttpHeaders)ObjectUtil.checkNotNull(trailingHeaders, "trailingHeaders"));
  }
  
  public HttpHeaders trailingHeaders()
  {
    return trailingHeaders;
  }
  
  public ByteBuf content()
  {
    return content;
  }
  
  public int refCnt()
  {
    return content.refCnt();
  }
  
  public FullHttpResponse retain()
  {
    content.retain();
    return this;
  }
  
  public FullHttpResponse retain(int increment)
  {
    content.retain(increment);
    return this;
  }
  
  public FullHttpResponse touch()
  {
    content.touch();
    return this;
  }
  
  public FullHttpResponse touch(Object hint)
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
  
  public FullHttpResponse setProtocolVersion(HttpVersion version)
  {
    super.setProtocolVersion(version);
    return this;
  }
  
  public FullHttpResponse setStatus(HttpResponseStatus status)
  {
    super.setStatus(status);
    return this;
  }
  
  public FullHttpResponse copy()
  {
    return replace(content().copy());
  }
  
  public FullHttpResponse duplicate()
  {
    return replace(content().duplicate());
  }
  
  public FullHttpResponse retainedDuplicate()
  {
    return replace(content().retainedDuplicate());
  }
  

  public FullHttpResponse replace(ByteBuf content)
  {
    FullHttpResponse response = new DefaultFullHttpResponse(protocolVersion(), status(), content, headers().copy(), trailingHeaders().copy());
    response.setDecoderResult(decoderResult());
    return response;
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
    if (!(o instanceof DefaultFullHttpResponse)) {
      return false;
    }
    
    DefaultFullHttpResponse other = (DefaultFullHttpResponse)o;
    
    return (super.equals(other)) && 
      (content().equals(other.content())) && 
      (trailingHeaders().equals(other.trailingHeaders()));
  }
  
  public String toString()
  {
    return HttpMessageUtil.appendFullResponse(new StringBuilder(256), this).toString();
  }
}
