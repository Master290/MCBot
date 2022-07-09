package io.netty.handler.codec.smtp;

import java.util.Collections;
import java.util.List;























public final class DefaultSmtpResponse
  implements SmtpResponse
{
  private final int code;
  private final List<CharSequence> details;
  
  public DefaultSmtpResponse(int code)
  {
    this(code, (List)null);
  }
  


  public DefaultSmtpResponse(int code, CharSequence... details)
  {
    this(code, SmtpUtils.toUnmodifiableList(details));
  }
  
  DefaultSmtpResponse(int code, List<CharSequence> details) {
    if ((code < 100) || (code > 599)) {
      throw new IllegalArgumentException("code must be 100 <= code <= 599");
    }
    this.code = code;
    if (details == null) {
      this.details = Collections.emptyList();
    } else {
      this.details = Collections.unmodifiableList(details);
    }
  }
  
  public int code()
  {
    return code;
  }
  
  public List<CharSequence> details()
  {
    return details;
  }
  
  public int hashCode()
  {
    return code * 31 + details.hashCode();
  }
  
  public boolean equals(Object o)
  {
    if (!(o instanceof DefaultSmtpResponse)) {
      return false;
    }
    
    if (o == this) {
      return true;
    }
    
    DefaultSmtpResponse other = (DefaultSmtpResponse)o;
    
    return (code() == other.code()) && 
      (details().equals(other.details()));
  }
  
  public String toString()
  {
    return "DefaultSmtpResponse{code=" + code + ", details=" + details + '}';
  }
}
