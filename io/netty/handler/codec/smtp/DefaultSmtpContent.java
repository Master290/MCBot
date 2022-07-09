package io.netty.handler.codec.smtp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.DefaultByteBufHolder;





















public class DefaultSmtpContent
  extends DefaultByteBufHolder
  implements SmtpContent
{
  public DefaultSmtpContent(ByteBuf data)
  {
    super(data);
  }
  
  public SmtpContent copy()
  {
    return (SmtpContent)super.copy();
  }
  
  public SmtpContent duplicate()
  {
    return (SmtpContent)super.duplicate();
  }
  
  public SmtpContent retainedDuplicate()
  {
    return (SmtpContent)super.retainedDuplicate();
  }
  
  public SmtpContent replace(ByteBuf content)
  {
    return new DefaultSmtpContent(content);
  }
  
  public SmtpContent retain()
  {
    super.retain();
    return this;
  }
  
  public SmtpContent retain(int increment)
  {
    super.retain(increment);
    return this;
  }
  
  public SmtpContent touch()
  {
    super.touch();
    return this;
  }
  
  public SmtpContent touch(Object hint)
  {
    super.touch(hint);
    return this;
  }
}
