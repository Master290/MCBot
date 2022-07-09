package io.netty.handler.timeout;

import io.netty.util.internal.PlatformDependent;




















public final class WriteTimeoutException
  extends TimeoutException
{
  private static final long serialVersionUID = -144786655770296065L;
  public static final WriteTimeoutException INSTANCE = PlatformDependent.javaVersion() >= 7 ? new WriteTimeoutException(true) : new WriteTimeoutException();
  
  private WriteTimeoutException() {}
  
  private WriteTimeoutException(boolean shared)
  {
    super(shared);
  }
}
