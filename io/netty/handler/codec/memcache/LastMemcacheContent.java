package io.netty.handler.codec.memcache;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderResult;
























public abstract interface LastMemcacheContent
  extends MemcacheContent
{
  public static final LastMemcacheContent EMPTY_LAST_CONTENT = new LastMemcacheContent()
  {
    public LastMemcacheContent copy()
    {
      return EMPTY_LAST_CONTENT;
    }
    
    public LastMemcacheContent duplicate()
    {
      return this;
    }
    
    public LastMemcacheContent retainedDuplicate()
    {
      return this;
    }
    
    public LastMemcacheContent replace(ByteBuf content)
    {
      return new DefaultLastMemcacheContent(content);
    }
    
    public LastMemcacheContent retain(int increment)
    {
      return this;
    }
    
    public LastMemcacheContent retain()
    {
      return this;
    }
    
    public LastMemcacheContent touch()
    {
      return this;
    }
    
    public LastMemcacheContent touch(Object hint)
    {
      return this;
    }
    
    public ByteBuf content()
    {
      return Unpooled.EMPTY_BUFFER;
    }
    
    public DecoderResult decoderResult()
    {
      return DecoderResult.SUCCESS;
    }
    
    public void setDecoderResult(DecoderResult result)
    {
      throw new UnsupportedOperationException("read only");
    }
    
    public int refCnt()
    {
      return 1;
    }
    
    public boolean release()
    {
      return false;
    }
    
    public boolean release(int decrement)
    {
      return false;
    }
  };
  
  public abstract LastMemcacheContent copy();
  
  public abstract LastMemcacheContent duplicate();
  
  public abstract LastMemcacheContent retainedDuplicate();
  
  public abstract LastMemcacheContent replace(ByteBuf paramByteBuf);
  
  public abstract LastMemcacheContent retain(int paramInt);
  
  public abstract LastMemcacheContent retain();
  
  public abstract LastMemcacheContent touch();
  
  public abstract LastMemcacheContent touch(Object paramObject);
}
