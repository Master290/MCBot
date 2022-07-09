package io.netty.handler.codec.smtp;

import java.util.List;

public abstract interface SmtpResponse
{
  public abstract int code();
  
  public abstract List<CharSequence> details();
}
