package io.netty.util.internal.logging;

import java.security.AccessController;
import java.security.PrivilegedAction;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.spi.ExtendedLoggerWrapper;
























class Log4J2Logger
  extends ExtendedLoggerWrapper
  implements InternalLogger
{
  private static final long serialVersionUID = 5485418394879791397L;
  private static final boolean VARARGS_ONLY = ((Boolean)AccessController.doPrivileged(new PrivilegedAction()
  {
    public Boolean run() {
      try {
        Logger.class.getMethod("debug", new Class[] { String.class, Object.class });
        return Boolean.valueOf(false);
      }
      catch (NoSuchMethodException ignore) {
        return Boolean.valueOf(true);
      }
      catch (SecurityException ignore) {}
      return Boolean.valueOf(false);
    }
  })).booleanValue();
  














  Log4J2Logger(Logger logger)
  {
    super((ExtendedLogger)logger, logger.getName(), logger.getMessageFactory());
    if (VARARGS_ONLY) {
      throw new UnsupportedOperationException("Log4J2 version mismatch");
    }
  }
  
  public String name()
  {
    return getName();
  }
  
  public void trace(Throwable t)
  {
    log(Level.TRACE, "Unexpected exception:", t);
  }
  
  public void debug(Throwable t)
  {
    log(Level.DEBUG, "Unexpected exception:", t);
  }
  
  public void info(Throwable t)
  {
    log(Level.INFO, "Unexpected exception:", t);
  }
  
  public void warn(Throwable t)
  {
    log(Level.WARN, "Unexpected exception:", t);
  }
  
  public void error(Throwable t)
  {
    log(Level.ERROR, "Unexpected exception:", t);
  }
  
  public boolean isEnabled(InternalLogLevel level)
  {
    return isEnabled(toLevel(level));
  }
  
  public void log(InternalLogLevel level, String msg)
  {
    log(toLevel(level), msg);
  }
  
  public void log(InternalLogLevel level, String format, Object arg)
  {
    log(toLevel(level), format, arg);
  }
  
  public void log(InternalLogLevel level, String format, Object argA, Object argB)
  {
    log(toLevel(level), format, argA, argB);
  }
  
  public void log(InternalLogLevel level, String format, Object... arguments)
  {
    log(toLevel(level), format, arguments);
  }
  
  public void log(InternalLogLevel level, String msg, Throwable t)
  {
    log(toLevel(level), msg, t);
  }
  
  public void log(InternalLogLevel level, Throwable t)
  {
    log(toLevel(level), "Unexpected exception:", t);
  }
  
  private static Level toLevel(InternalLogLevel level) {
    switch (2.$SwitchMap$io$netty$util$internal$logging$InternalLogLevel[level.ordinal()]) {
    case 1: 
      return Level.INFO;
    case 2: 
      return Level.DEBUG;
    case 3: 
      return Level.WARN;
    case 4: 
      return Level.ERROR;
    case 5: 
      return Level.TRACE;
    }
    throw new Error();
  }
}
