package io.netty.channel;

import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.SuppressJava6Requirement;
import io.netty.util.internal.ThrowableUtil;

























public class ChannelException
  extends RuntimeException
{
  private static final long serialVersionUID = 2908618315971075004L;
  
  public ChannelException() {}
  
  public ChannelException(String message, Throwable cause)
  {
    super(message, cause);
  }
  


  public ChannelException(String message)
  {
    super(message);
  }
  


  public ChannelException(Throwable cause)
  {
    super(cause);
  }
  

  @SuppressJava6Requirement(reason="uses Java 7+ RuntimeException.<init>(String, Throwable, boolean, boolean) but is guarded by version checks")
  protected ChannelException(String message, Throwable cause, boolean shared)
  {
    super(message, cause, false, true);
    assert (shared);
  }
  
  static ChannelException newStatic(String message, Class<?> clazz, String method) { ChannelException exception;
    ChannelException exception;
    if (PlatformDependent.javaVersion() >= 7) {
      exception = new StacklessChannelException(message, null, true);
    } else {
      exception = new StacklessChannelException(message, null);
    }
    return (ChannelException)ThrowableUtil.unknownStackTrace(exception, clazz, method);
  }
  
  private static final class StacklessChannelException extends ChannelException {
    private static final long serialVersionUID = -6384642137753538579L;
    
    StacklessChannelException(String message, Throwable cause) {
      super(cause);
    }
    
    StacklessChannelException(String message, Throwable cause, boolean shared) {
      super(cause, shared);
    }
    



    public Throwable fillInStackTrace()
    {
      return this;
    }
  }
}
