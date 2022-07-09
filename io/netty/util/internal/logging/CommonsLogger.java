package io.netty.util.internal.logging;

import io.netty.util.internal.ObjectUtil;
import org.apache.commons.logging.Log;














































@Deprecated
class CommonsLogger
  extends AbstractInternalLogger
{
  private static final long serialVersionUID = 8647838678388394885L;
  private final transient Log logger;
  
  CommonsLogger(Log logger, String name)
  {
    super(name);
    this.logger = ((Log)ObjectUtil.checkNotNull(logger, "logger"));
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
    if (logger.isTraceEnabled()) {
      FormattingTuple ft = MessageFormatter.format(format, arg);
      logger.trace(ft.getMessage(), ft.getThrowable());
    }
  }
  
















  public void trace(String format, Object argA, Object argB)
  {
    if (logger.isTraceEnabled()) {
      FormattingTuple ft = MessageFormatter.format(format, argA, argB);
      logger.trace(ft.getMessage(), ft.getThrowable());
    }
  }
  












  public void trace(String format, Object... arguments)
  {
    if (logger.isTraceEnabled()) {
      FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
      logger.trace(ft.getMessage(), ft.getThrowable());
    }
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
    if (logger.isDebugEnabled()) {
      FormattingTuple ft = MessageFormatter.format(format, arg);
      logger.debug(ft.getMessage(), ft.getThrowable());
    }
  }
  
















  public void debug(String format, Object argA, Object argB)
  {
    if (logger.isDebugEnabled()) {
      FormattingTuple ft = MessageFormatter.format(format, argA, argB);
      logger.debug(ft.getMessage(), ft.getThrowable());
    }
  }
  












  public void debug(String format, Object... arguments)
  {
    if (logger.isDebugEnabled()) {
      FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
      logger.debug(ft.getMessage(), ft.getThrowable());
    }
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
    if (logger.isInfoEnabled()) {
      FormattingTuple ft = MessageFormatter.format(format, arg);
      logger.info(ft.getMessage(), ft.getThrowable());
    }
  }
  















  public void info(String format, Object argA, Object argB)
  {
    if (logger.isInfoEnabled()) {
      FormattingTuple ft = MessageFormatter.format(format, argA, argB);
      logger.info(ft.getMessage(), ft.getThrowable());
    }
  }
  












  public void info(String format, Object... arguments)
  {
    if (logger.isInfoEnabled()) {
      FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
      logger.info(ft.getMessage(), ft.getThrowable());
    }
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
    if (logger.isWarnEnabled()) {
      FormattingTuple ft = MessageFormatter.format(format, arg);
      logger.warn(ft.getMessage(), ft.getThrowable());
    }
  }
  
















  public void warn(String format, Object argA, Object argB)
  {
    if (logger.isWarnEnabled()) {
      FormattingTuple ft = MessageFormatter.format(format, argA, argB);
      logger.warn(ft.getMessage(), ft.getThrowable());
    }
  }
  












  public void warn(String format, Object... arguments)
  {
    if (logger.isWarnEnabled()) {
      FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
      logger.warn(ft.getMessage(), ft.getThrowable());
    }
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
    if (logger.isErrorEnabled()) {
      FormattingTuple ft = MessageFormatter.format(format, arg);
      logger.error(ft.getMessage(), ft.getThrowable());
    }
  }
  
















  public void error(String format, Object argA, Object argB)
  {
    if (logger.isErrorEnabled()) {
      FormattingTuple ft = MessageFormatter.format(format, argA, argB);
      logger.error(ft.getMessage(), ft.getThrowable());
    }
  }
  












  public void error(String format, Object... arguments)
  {
    if (logger.isErrorEnabled()) {
      FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
      logger.error(ft.getMessage(), ft.getThrowable());
    }
  }
  









  public void error(String msg, Throwable t)
  {
    logger.error(msg, t);
  }
}
