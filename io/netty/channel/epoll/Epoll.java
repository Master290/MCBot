package io.netty.channel.epoll;

import io.netty.channel.unix.FileDescriptor;
import io.netty.util.internal.SystemPropertyUtil;



















public final class Epoll
{
  private static final Throwable UNAVAILABILITY_CAUSE;
  
  static
  {
    Throwable cause = null;
    
    if (SystemPropertyUtil.getBoolean("io.netty.transport.noNative", false)) {
      cause = new UnsupportedOperationException("Native transport was explicit disabled with -Dio.netty.transport.noNative=true");
    }
    else {
      FileDescriptor epollFd = null;
      FileDescriptor eventFd = null;
      try {
        epollFd = Native.newEpollCreate();
        eventFd = Native.newEventFd();
        


        if (epollFd != null) {
          try {
            epollFd.close();
          }
          catch (Exception localException) {}
        }
        
        if (eventFd != null) {
          try {
            eventFd.close();
          }
          catch (Exception localException1) {}
        }
        



        UNAVAILABILITY_CAUSE = cause;
      }
      catch (Throwable t)
      {
        cause = t;
      } finally {
        if (epollFd != null) {
          try {
            epollFd.close();
          }
          catch (Exception localException4) {}
        }
        
        if (eventFd != null) {
          try {
            eventFd.close();
          }
          catch (Exception localException5) {}
        }
      }
    }
  }
  






  public static boolean isAvailable()
  {
    return UNAVAILABILITY_CAUSE == null;
  }
  





  public static void ensureAvailability()
  {
    if (UNAVAILABILITY_CAUSE != null)
    {
      throw ((Error)new UnsatisfiedLinkError("failed to load the required native library").initCause(UNAVAILABILITY_CAUSE));
    }
  }
  





  public static Throwable unavailabilityCause()
  {
    return UNAVAILABILITY_CAUSE;
  }
  
  private Epoll() {}
}
