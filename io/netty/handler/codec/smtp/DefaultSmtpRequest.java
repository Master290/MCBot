package io.netty.handler.codec.smtp;

import io.netty.util.internal.ObjectUtil;
import java.util.Collections;
import java.util.List;























public final class DefaultSmtpRequest
  implements SmtpRequest
{
  private final SmtpCommand command;
  private final List<CharSequence> parameters;
  
  public DefaultSmtpRequest(SmtpCommand command)
  {
    this.command = ((SmtpCommand)ObjectUtil.checkNotNull(command, "command"));
    parameters = Collections.emptyList();
  }
  


  public DefaultSmtpRequest(SmtpCommand command, CharSequence... parameters)
  {
    this.command = ((SmtpCommand)ObjectUtil.checkNotNull(command, "command"));
    this.parameters = SmtpUtils.toUnmodifiableList(parameters);
  }
  


  public DefaultSmtpRequest(CharSequence command, CharSequence... parameters)
  {
    this(SmtpCommand.valueOf(command), parameters);
  }
  
  DefaultSmtpRequest(SmtpCommand command, List<CharSequence> parameters) {
    this.command = ((SmtpCommand)ObjectUtil.checkNotNull(command, "command"));
    this.parameters = (parameters != null ? 
      Collections.unmodifiableList(parameters) : Collections.emptyList());
  }
  
  public SmtpCommand command()
  {
    return command;
  }
  
  public List<CharSequence> parameters()
  {
    return parameters;
  }
  
  public int hashCode()
  {
    return command.hashCode() * 31 + parameters.hashCode();
  }
  
  public boolean equals(Object o)
  {
    if (!(o instanceof DefaultSmtpRequest)) {
      return false;
    }
    
    if (o == this) {
      return true;
    }
    
    DefaultSmtpRequest other = (DefaultSmtpRequest)o;
    
    return (command().equals(other.command())) && 
      (parameters().equals(other.parameters()));
  }
  
  public String toString()
  {
    return "DefaultSmtpRequest{command=" + command + ", parameters=" + parameters + '}';
  }
}
