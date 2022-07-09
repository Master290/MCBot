package io.netty.handler.codec.smtp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;

public abstract interface SmtpContent
  extends ByteBufHolder
{
  public abstract SmtpContent copy();
  
  public abstract SmtpContent duplicate();
  
  public abstract SmtpContent retainedDuplicate();
  
  public abstract SmtpContent replace(ByteBuf paramByteBuf);
  
  public abstract SmtpContent retain();
  
  public abstract SmtpContent retain(int paramInt);
  
  public abstract SmtpContent touch();
  
  public abstract SmtpContent touch(Object paramObject);
}
