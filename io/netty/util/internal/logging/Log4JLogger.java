package io.netty.util.internal.logging;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

















































class Log4JLogger
  extends AbstractInternalLogger
{
  private static final long serialVersionUID = 2851357342488183058L;
  final transient Logger logger;
  static final String FQCN = Log4JLogger.class.getName();
  
  final boolean traceCapable;
  

  Log4JLogger(Logger logger)
  {
    super(logger.getName());
    this.logger = logger;
    traceCapable = isTraceCapable();
  }
  
  private boolean isTraceCapable() {
    try {
      logger.isTraceEnabled();
      return true;
    } catch (NoSuchMethodError ignored) {}
    return false;
  }
  






  public boolean isTraceEnabled()
  {
    if (traceCapable) {
      return logger.isTraceEnabled();
    }
    return logger.isDebugEnabled();
  }
  







  public void trace(String msg)
  {
    logger.log(FQCN, traceCapable ? Level.TRACE : Level.DEBUG, msg, null);
  }
  














  public void trace(String format, Object arg)
  {
    if (isTraceEnabled()) {
      FormattingTuple ft = MessageFormatter.format(format, arg);
      logger.log(FQCN, traceCapable ? Level.TRACE : Level.DEBUG, ft
        .getMessage(), ft.getThrowable());
    }
  }
  
















  public void trace(String format, Object argA, Object argB)
  {
    if (isTraceEnabled()) {
      FormattingTuple ft = MessageFormatter.format(format, argA, argB);
      logger.log(FQCN, traceCapable ? Level.TRACE : Level.DEBUG, ft
        .getMessage(), ft.getThrowable());
    }
  }
  














  public void trace(String format, Object... arguments)
  {
    if (isTraceEnabled()) {
      FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
      logger.log(FQCN, traceCapable ? Level.TRACE : Level.DEBUG, ft
        .getMessage(), ft.getThrowable());
    }
  }
  








  public void trace(String msg, Throwable t)
  {
    logger.log(FQCN, traceCapable ? Level.TRACE : Level.DEBUG, msg, t);
  }
  





  public boolean isDebugEnabled()
  {
    return logger.isDebugEnabled();
  }
  






  public void debug(String msg)
  {
    logger.log(FQCN, Level.DEBUG, msg, null);
  }
  














  public void debug(String format, Object arg)
  {
    if (logger.isDebugEnabled()) {
      FormattingTuple ft = MessageFormatter.format(format, arg);
      logger.log(FQCN, Level.DEBUG, ft.getMessage(), ft.getThrowable());
    }
  }
  
















  public void debug(String format, Object argA, Object argB)
  {
    if (logger.isDebugEnabled()) {
      FormattingTuple ft = MessageFormatter.format(format, argA, argB);
      logger.log(FQCN, Level.DEBUG, ft.getMessage(), ft.getThrowable());
    }
  }
  













  public void debug(String format, Object... arguments)
  {
    if (logger.isDebugEnabled()) {
      FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
      logger.log(FQCN, Level.DEBUG, ft.getMessage(), ft.getThrowable());
    }
  }
  








  public void debug(String msg, Throwable t)
  {
    logger.log(FQCN, Level.DEBUG, msg, t);
  }
  





  public boolean isInfoEnabled()
  {
    return logger.isInfoEnabled();
  }
  






  public void info(String msg)
  {
    logger.log(FQCN, Level.INFO, msg, null);
  }
  













  public void info(String format, Object arg)
  {
    if (logger.isInfoEnabled()) {
      FormattingTuple ft = MessageFormatter.format(format, arg);
      logger.log(FQCN, Level.INFO, ft.getMessage(), ft.getThrowable());
    }
  }
  
















  public void info(String format, Object argA, Object argB)
  {
    if (logger.isInfoEnabled()) {
      FormattingTuple ft = MessageFormatter.format(format, argA, argB);
      logger.log(FQCN, Level.INFO, ft.getMessage(), ft.getThrowable());
    }
  }
  














  public void info(String format, Object... argArray)
  {
    if (logger.isInfoEnabled()) {
      FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
      logger.log(FQCN, Level.INFO, ft.getMessage(), ft.getThrowable());
    }
  }
  









  public void info(String msg, Throwable t)
  {
    logger.log(FQCN, Level.INFO, msg, t);
  }
  





  public boolean isWarnEnabled()
  {
    return logger.isEnabledFor(Level.WARN);
  }
  






  public void warn(String msg)
  {
    logger.log(FQCN, Level.WARN, msg, null);
  }
  














  public void warn(String format, Object arg)
  {
    if (logger.isEnabledFor(Level.WARN)) {
      FormattingTuple ft = MessageFormatter.format(format, arg);
      logger.log(FQCN, Level.WARN, ft.getMessage(), ft.getThrowable());
    }
  }
  
















  public void warn(String format, Object argA, Object argB)
  {
    if (logger.isEnabledFor(Level.WARN)) {
      FormattingTuple ft = MessageFormatter.format(format, argA, argB);
      logger.log(FQCN, Level.WARN, ft.getMessage(), ft.getThrowable());
    }
  }
  














  public void warn(String format, Object... argArray)
  {
    if (logger.isEnabledFor(Level.WARN)) {
      FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
      logger.log(FQCN, Level.WARN, ft.getMessage(), ft.getThrowable());
    }
  }
  









  public void warn(String msg, Throwable t)
  {
    logger.log(FQCN, Level.WARN, msg, t);
  }
  





  public boolean isErrorEnabled()
  {
    return logger.isEnabledFor(Level.ERROR);
  }
  






  public void error(String msg)
  {
    logger.log(FQCN, Level.ERROR, msg, null);
  }
  














  public void error(String format, Object arg)
  {
    if (logger.isEnabledFor(Level.ERROR)) {
      FormattingTuple ft = MessageFormatter.format(format, arg);
      logger.log(FQCN, Level.ERROR, ft.getMessage(), ft.getThrowable());
    }
  }
  
















  public void error(String format, Object argA, Object argB)
  {
    if (logger.isEnabledFor(Level.ERROR)) {
      FormattingTuple ft = MessageFormatter.format(format, argA, argB);
      logger.log(FQCN, Level.ERROR, ft.getMessage(), ft.getThrowable());
    }
  }
  














  public void error(String format, Object... argArray)
  {
    if (logger.isEnabledFor(Level.ERROR)) {
      FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
      logger.log(FQCN, Level.ERROR, ft.getMessage(), ft.getThrowable());
    }
  }
  









  public void error(String msg, Throwable t)
  {
    logger.log(FQCN, Level.ERROR, msg, t);
  }
}
