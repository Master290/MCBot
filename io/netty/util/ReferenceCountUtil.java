package io.netty.util;

import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;



















public final class ReferenceCountUtil
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(ReferenceCountUtil.class);
  
  static {
    ResourceLeakDetector.addExclusions(ReferenceCountUtil.class, new String[] { "touch" });
  }
  




  public static <T> T retain(T msg)
  {
    if ((msg instanceof ReferenceCounted)) {
      return ((ReferenceCounted)msg).retain();
    }
    return msg;
  }
  




  public static <T> T retain(T msg, int increment)
  {
    ObjectUtil.checkPositive(increment, "increment");
    if ((msg instanceof ReferenceCounted)) {
      return ((ReferenceCounted)msg).retain(increment);
    }
    return msg;
  }
  




  public static <T> T touch(T msg)
  {
    if ((msg instanceof ReferenceCounted)) {
      return ((ReferenceCounted)msg).touch();
    }
    return msg;
  }
  





  public static <T> T touch(T msg, Object hint)
  {
    if ((msg instanceof ReferenceCounted)) {
      return ((ReferenceCounted)msg).touch(hint);
    }
    return msg;
  }
  



  public static boolean release(Object msg)
  {
    if ((msg instanceof ReferenceCounted)) {
      return ((ReferenceCounted)msg).release();
    }
    return false;
  }
  



  public static boolean release(Object msg, int decrement)
  {
    ObjectUtil.checkPositive(decrement, "decrement");
    if ((msg instanceof ReferenceCounted)) {
      return ((ReferenceCounted)msg).release(decrement);
    }
    return false;
  }
  





  public static void safeRelease(Object msg)
  {
    try
    {
      release(msg);
    } catch (Throwable t) {
      logger.warn("Failed to release a message: {}", msg, t);
    }
  }
  





  public static void safeRelease(Object msg, int decrement)
  {
    try
    {
      ObjectUtil.checkPositive(decrement, "decrement");
      release(msg, decrement);
    } catch (Throwable t) {
      if (logger.isWarnEnabled()) {
        logger.warn("Failed to release a message: {} (decrement: {})", new Object[] { msg, Integer.valueOf(decrement), t });
      }
    }
  }
  






  @Deprecated
  public static <T> T releaseLater(T msg)
  {
    return releaseLater(msg, 1);
  }
  






  @Deprecated
  public static <T> T releaseLater(T msg, int decrement)
  {
    ObjectUtil.checkPositive(decrement, "decrement");
    if ((msg instanceof ReferenceCounted)) {
      ThreadDeathWatcher.watch(Thread.currentThread(), new ReleasingTask((ReferenceCounted)msg, decrement));
    }
    return msg;
  }
  



  public static int refCnt(Object msg)
  {
    return (msg instanceof ReferenceCounted) ? ((ReferenceCounted)msg).refCnt() : -1;
  }
  
  private ReferenceCountUtil() {}
  
  private static final class ReleasingTask implements Runnable
  {
    private final ReferenceCounted obj;
    private final int decrement;
    
    ReleasingTask(ReferenceCounted obj, int decrement)
    {
      this.obj = obj;
      this.decrement = decrement;
    }
    
    public void run()
    {
      try {
        if (!obj.release(decrement)) {
          ReferenceCountUtil.logger.warn("Non-zero refCnt: {}", this);
        } else {
          ReferenceCountUtil.logger.debug("Released: {}", this);
        }
      } catch (Exception ex) {
        ReferenceCountUtil.logger.warn("Failed to release an object: {}", obj, ex);
      }
    }
    
    public String toString()
    {
      return StringUtil.simpleClassName(obj) + ".release(" + decrement + ") refCnt: " + obj.refCnt();
    }
  }
}
