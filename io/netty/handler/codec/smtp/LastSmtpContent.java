package io.netty.handler.codec.smtp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;


























public abstract interface LastSmtpContent
  extends SmtpContent
{
  public static final LastSmtpContent EMPTY_LAST_CONTENT = new LastSmtpContent()
  {
    public LastSmtpContent copy() {
      return this;
    }
    
    public LastSmtpContent duplicate()
    {
      return this;
    }
    
    public LastSmtpContent retainedDuplicate()
    {
      return this;
    }
    
    public LastSmtpContent replace(ByteBuf content)
    {
      return new DefaultLastSmtpContent(content);
    }
    
    public LastSmtpContent retain()
    {
      return this;
    }
    
    public LastSmtpContent retain(int increment)
    {
      return this;
    }
    
    public LastSmtpContent touch()
    {
      return this;
    }
    
    public LastSmtpContent touch(Object hint)
    {
      return this;
    }
    
    public ByteBuf content()
    {
      return Unpooled.EMPTY_BUFFER;
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
  
  public abstract LastSmtpContent copy();
  
  public abstract LastSmtpContent duplicate();
  
  public abstract LastSmtpContent retainedDuplicate();
  
  public abstract LastSmtpContent replace(ByteBuf paramByteBuf);
  
  public abstract LastSmtpContent retain();
  
  public abstract LastSmtpContent retain(int paramInt);
  
  public abstract LastSmtpContent touch();
  
  public abstract LastSmtpContent touch(Object paramObject);
}
