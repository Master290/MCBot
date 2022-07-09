package io.netty.handler.codec.redis;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;























public abstract interface LastBulkStringRedisContent
  extends BulkStringRedisContent
{
  public static final LastBulkStringRedisContent EMPTY_LAST_CONTENT = new LastBulkStringRedisContent()
  {
    public ByteBuf content()
    {
      return Unpooled.EMPTY_BUFFER;
    }
    
    public LastBulkStringRedisContent copy()
    {
      return this;
    }
    
    public LastBulkStringRedisContent duplicate()
    {
      return this;
    }
    
    public LastBulkStringRedisContent retainedDuplicate()
    {
      return this;
    }
    
    public LastBulkStringRedisContent replace(ByteBuf content)
    {
      return new DefaultLastBulkStringRedisContent(content);
    }
    
    public LastBulkStringRedisContent retain(int increment)
    {
      return this;
    }
    
    public LastBulkStringRedisContent retain()
    {
      return this;
    }
    
    public int refCnt()
    {
      return 1;
    }
    
    public LastBulkStringRedisContent touch()
    {
      return this;
    }
    
    public LastBulkStringRedisContent touch(Object hint)
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
  };
  
  public abstract LastBulkStringRedisContent copy();
  
  public abstract LastBulkStringRedisContent duplicate();
  
  public abstract LastBulkStringRedisContent retainedDuplicate();
  
  public abstract LastBulkStringRedisContent replace(ByteBuf paramByteBuf);
  
  public abstract LastBulkStringRedisContent retain();
  
  public abstract LastBulkStringRedisContent retain(int paramInt);
  
  public abstract LastBulkStringRedisContent touch();
  
  public abstract LastBulkStringRedisContent touch(Object paramObject);
}
