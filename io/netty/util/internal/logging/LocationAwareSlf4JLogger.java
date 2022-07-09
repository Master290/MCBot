package io.netty.util.internal.logging;

import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.spi.LocationAwareLogger;






















final class LocationAwareSlf4JLogger
  extends AbstractInternalLogger
{
  static final String FQCN = LocationAwareSlf4JLogger.class.getName();
  private static final long serialVersionUID = -8292030083201538180L;
  private final transient LocationAwareLogger logger;
  
  LocationAwareSlf4JLogger(LocationAwareLogger logger)
  {
    super(logger.getName());
    this.logger = logger;
  }
  
  private void log(int level, String message) {
    logger.log(null, FQCN, level, message, null, null);
  }
  
  private void log(int level, String message, Throwable cause) {
    logger.log(null, FQCN, level, message, null, cause);
  }
  
  private void log(int level, FormattingTuple tuple) {
    logger.log(null, FQCN, level, tuple.getMessage(), tuple.getArgArray(), tuple.getThrowable());
  }
  
  public boolean isTraceEnabled()
  {
    return logger.isTraceEnabled();
  }
  
  public void trace(String msg)
  {
    if (isTraceEnabled()) {
      log(0, msg);
    }
  }
  
  public void trace(String format, Object arg)
  {
    if (isTraceEnabled()) {
      log(0, MessageFormatter.format(format, arg));
    }
  }
  
  public void trace(String format, Object argA, Object argB)
  {
    if (isTraceEnabled()) {
      log(0, MessageFormatter.format(format, argA, argB));
    }
  }
  
  public void trace(String format, Object... argArray)
  {
    if (isTraceEnabled()) {
      log(0, MessageFormatter.arrayFormat(format, argArray));
    }
  }
  
  public void trace(String msg, Throwable t)
  {
    if (isTraceEnabled()) {
      log(0, msg, t);
    }
  }
  
  public boolean isDebugEnabled()
  {
    return logger.isDebugEnabled();
  }
  
  public void debug(String msg)
  {
    if (isDebugEnabled()) {
      log(10, msg);
    }
  }
  
  public void debug(String format, Object arg)
  {
    if (isDebugEnabled()) {
      log(10, MessageFormatter.format(format, arg));
    }
  }
  
  public void debug(String format, Object argA, Object argB)
  {
    if (isDebugEnabled()) {
      log(10, MessageFormatter.format(format, argA, argB));
    }
  }
  
  public void debug(String format, Object... argArray)
  {
    if (isDebugEnabled()) {
      log(10, MessageFormatter.arrayFormat(format, argArray));
    }
  }
  
  public void debug(String msg, Throwable t)
  {
    if (isDebugEnabled()) {
      log(10, msg, t);
    }
  }
  
  public boolean isInfoEnabled()
  {
    return logger.isInfoEnabled();
  }
  
  public void info(String msg)
  {
    if (isInfoEnabled()) {
      log(20, msg);
    }
  }
  
  public void info(String format, Object arg)
  {
    if (isInfoEnabled()) {
      log(20, MessageFormatter.format(format, arg));
    }
  }
  
  public void info(String format, Object argA, Object argB)
  {
    if (isInfoEnabled()) {
      log(20, MessageFormatter.format(format, argA, argB));
    }
  }
  
  public void info(String format, Object... argArray)
  {
    if (isInfoEnabled()) {
      log(20, MessageFormatter.arrayFormat(format, argArray));
    }
  }
  
  public void info(String msg, Throwable t)
  {
    if (isInfoEnabled()) {
      log(20, msg, t);
    }
  }
  
  public boolean isWarnEnabled()
  {
    return logger.isWarnEnabled();
  }
  
  public void warn(String msg)
  {
    if (isWarnEnabled()) {
      log(30, msg);
    }
  }
  
  public void warn(String format, Object arg)
  {
    if (isWarnEnabled()) {
      log(30, MessageFormatter.format(format, arg));
    }
  }
  
  public void warn(String format, Object... argArray)
  {
    if (isWarnEnabled()) {
      log(30, MessageFormatter.arrayFormat(format, argArray));
    }
  }
  
  public void warn(String format, Object argA, Object argB)
  {
    if (isWarnEnabled()) {
      log(30, MessageFormatter.format(format, argA, argB));
    }
  }
  
  public void warn(String msg, Throwable t)
  {
    if (isWarnEnabled()) {
      log(30, msg, t);
    }
  }
  
  public boolean isErrorEnabled()
  {
    return logger.isErrorEnabled();
  }
  
  public void error(String msg)
  {
    if (isErrorEnabled()) {
      log(40, msg);
    }
  }
  
  public void error(String format, Object arg)
  {
    if (isErrorEnabled()) {
      log(40, MessageFormatter.format(format, arg));
    }
  }
  
  public void error(String format, Object argA, Object argB)
  {
    if (isErrorEnabled()) {
      log(40, MessageFormatter.format(format, argA, argB));
    }
  }
  
  public void error(String format, Object... argArray)
  {
    if (isErrorEnabled()) {
      log(40, MessageFormatter.arrayFormat(format, argArray));
    }
  }
  
  public void error(String msg, Throwable t)
  {
    if (isErrorEnabled()) {
      log(40, msg, t);
    }
  }
}
