package io.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderResult;





















public abstract interface LastHttpContent
  extends HttpContent
{
  public static final LastHttpContent EMPTY_LAST_CONTENT = new LastHttpContent()
  {
    public ByteBuf content()
    {
      return Unpooled.EMPTY_BUFFER;
    }
    
    public LastHttpContent copy()
    {
      return EMPTY_LAST_CONTENT;
    }
    
    public LastHttpContent duplicate()
    {
      return this;
    }
    
    public LastHttpContent replace(ByteBuf content)
    {
      return new DefaultLastHttpContent(content);
    }
    
    public LastHttpContent retainedDuplicate()
    {
      return this;
    }
    
    public HttpHeaders trailingHeaders()
    {
      return EmptyHttpHeaders.INSTANCE;
    }
    
    public DecoderResult decoderResult()
    {
      return DecoderResult.SUCCESS;
    }
    
    @Deprecated
    public DecoderResult getDecoderResult()
    {
      return decoderResult();
    }
    
    public void setDecoderResult(DecoderResult result)
    {
      throw new UnsupportedOperationException("read only");
    }
    
    public int refCnt()
    {
      return 1;
    }
    
    public LastHttpContent retain()
    {
      return this;
    }
    
    public LastHttpContent retain(int increment)
    {
      return this;
    }
    
    public LastHttpContent touch()
    {
      return this;
    }
    
    public LastHttpContent touch(Object hint)
    {
      return this;
    }
    
    public boolean release()
    {
      return false;
    }
    
    public boolean release(int decrement)
    {
      return false;
    }
    
    public String toString()
    {
      return "EmptyLastHttpContent";
    }
  };
  
  public abstract HttpHeaders trailingHeaders();
  
  public abstract LastHttpContent copy();
  
  public abstract LastHttpContent duplicate();
  
  public abstract LastHttpContent retainedDuplicate();
  
  public abstract LastHttpContent replace(ByteBuf paramByteBuf);
  
  public abstract LastHttpContent retain(int paramInt);
  
  public abstract LastHttpContent retain();
  
  public abstract LastHttpContent touch();
  
  public abstract LastHttpContent touch(Object paramObject);
}
