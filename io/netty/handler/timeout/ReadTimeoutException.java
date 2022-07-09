package io.netty.handler.timeout;

import io.netty.util.internal.PlatformDependent;




















public final class ReadTimeoutException
  extends TimeoutException
{
  private static final long serialVersionUID = 169287984113283421L;
  public static final ReadTimeoutException INSTANCE = PlatformDependent.javaVersion() >= 7 ? new ReadTimeoutException(true) : new ReadTimeoutException();
  
  ReadTimeoutException() {}
  
  private ReadTimeoutException(boolean shared)
  {
    super(shared);
  }
}
