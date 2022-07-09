package io.netty.handler.codec.smtp;

import java.util.List;

public abstract interface SmtpRequest
{
  public abstract SmtpCommand command();
  
  public abstract List<CharSequence> parameters();
}
