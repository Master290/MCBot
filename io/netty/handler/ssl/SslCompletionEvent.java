package io.netty.handler.ssl;

import io.netty.util.internal.ObjectUtil;















public abstract class SslCompletionEvent
{
  private final Throwable cause;
  
  SslCompletionEvent()
  {
    cause = null;
  }
  
  SslCompletionEvent(Throwable cause) {
    this.cause = ((Throwable)ObjectUtil.checkNotNull(cause, "cause"));
  }
  


  public final boolean isSuccess()
  {
    return cause == null;
  }
  



  public final Throwable cause()
  {
    return cause;
  }
  
  public String toString()
  {
    Throwable cause = cause();
    return 
      getClass().getSimpleName() + '(' + cause + ')';
  }
}
