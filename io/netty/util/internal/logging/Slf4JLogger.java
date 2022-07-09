package io.netty.util.internal.logging;

import org.slf4j.Logger;


















final class Slf4JLogger
  extends AbstractInternalLogger
{
  private static final long serialVersionUID = 108038972685130825L;
  private final transient Logger logger;
  
  Slf4JLogger(Logger logger)
  {
    super(logger.getName());
    this.logger = logger;
  }
  
  public boolean isTraceEnabled()
  {
    return logger.isTraceEnabled();
  }
  
  public void trace(String msg)
  {
    logger.trace(msg);
  }
  
  public void trace(String format, Object arg)
  {
    logger.trace(format, arg);
  }
  
  public void trace(String format, Object argA, Object argB)
  {
    logger.trace(format, argA, argB);
  }
  
  public void trace(String format, Object... argArray)
  {
    logger.trace(format, argArray);
  }
  
  public void trace(String msg, Throwable t)
  {
    logger.trace(msg, t);
  }
  
  public boolean isDebugEnabled()
  {
    return logger.isDebugEnabled();
  }
  
  public void debug(String msg)
  {
    logger.debug(msg);
  }
  
  public void debug(String format, Object arg)
  {
    logger.debug(format, arg);
  }
  
  public void debug(String format, Object argA, Object argB)
  {
    logger.debug(format, argA, argB);
  }
  
  public void debug(String format, Object... argArray)
  {
    logger.debug(format, argArray);
  }
  
  public void debug(String msg, Throwable t)
  {
    logger.debug(msg, t);
  }
  
  public boolean isInfoEnabled()
  {
    return logger.isInfoEnabled();
  }
  
  public void info(String msg)
  {
    logger.info(msg);
  }
  
  public void info(String format, Object arg)
  {
    logger.info(format, arg);
  }
  
  public void info(String format, Object argA, Object argB)
  {
    logger.info(format, argA, argB);
  }
  
  public void info(String format, Object... argArray)
  {
    logger.info(format, argArray);
  }
  
  public void info(String msg, Throwable t)
  {
    logger.info(msg, t);
  }
  
  public boolean isWarnEnabled()
  {
    return logger.isWarnEnabled();
  }
  
  public void warn(String msg)
  {
    logger.warn(msg);
  }
  
  public void warn(String format, Object arg)
  {
    logger.warn(format, arg);
  }
  
  public void warn(String format, Object... argArray)
  {
    logger.warn(format, argArray);
  }
  
  public void warn(String format, Object argA, Object argB)
  {
    logger.warn(format, argA, argB);
  }
  
  public void warn(String msg, Throwable t)
  {
    logger.warn(msg, t);
  }
  
  public boolean isErrorEnabled()
  {
    return logger.isErrorEnabled();
  }
  
  public void error(String msg)
  {
    logger.error(msg);
  }
  
  public void error(String format, Object arg)
  {
    logger.error(format, arg);
  }
  
  public void error(String format, Object argA, Object argB)
  {
    logger.error(format, argA, argB);
  }
  
  public void error(String format, Object... argArray)
  {
    logger.error(format, argArray);
  }
  
  public void error(String msg, Throwable t)
  {
    logger.error(msg, t);
  }
}
