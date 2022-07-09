package io.netty.util.internal.logging;

import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;
import java.io.ObjectStreamException;
import java.io.Serializable;

























public abstract class AbstractInternalLogger
  implements InternalLogger, Serializable
{
  private static final long serialVersionUID = -6382972526573193470L;
  static final String EXCEPTION_MESSAGE = "Unexpected exception:";
  private final String name;
  
  protected AbstractInternalLogger(String name)
  {
    this.name = ((String)ObjectUtil.checkNotNull(name, "name"));
  }
  
  public String name()
  {
    return name;
  }
  
  public boolean isEnabled(InternalLogLevel level)
  {
    switch (1.$SwitchMap$io$netty$util$internal$logging$InternalLogLevel[level.ordinal()]) {
    case 1: 
      return isTraceEnabled();
    case 2: 
      return isDebugEnabled();
    case 3: 
      return isInfoEnabled();
    case 4: 
      return isWarnEnabled();
    case 5: 
      return isErrorEnabled();
    }
    throw new Error();
  }
  

  public void trace(Throwable t)
  {
    trace("Unexpected exception:", t);
  }
  
  public void debug(Throwable t)
  {
    debug("Unexpected exception:", t);
  }
  
  public void info(Throwable t)
  {
    info("Unexpected exception:", t);
  }
  
  public void warn(Throwable t)
  {
    warn("Unexpected exception:", t);
  }
  
  public void error(Throwable t)
  {
    error("Unexpected exception:", t);
  }
  
  public void log(InternalLogLevel level, String msg, Throwable cause)
  {
    switch (1.$SwitchMap$io$netty$util$internal$logging$InternalLogLevel[level.ordinal()]) {
    case 1: 
      trace(msg, cause);
      break;
    case 2: 
      debug(msg, cause);
      break;
    case 3: 
      info(msg, cause);
      break;
    case 4: 
      warn(msg, cause);
      break;
    case 5: 
      error(msg, cause);
      break;
    default: 
      throw new Error();
    }
  }
  
  public void log(InternalLogLevel level, Throwable cause)
  {
    switch (1.$SwitchMap$io$netty$util$internal$logging$InternalLogLevel[level.ordinal()]) {
    case 1: 
      trace(cause);
      break;
    case 2: 
      debug(cause);
      break;
    case 3: 
      info(cause);
      break;
    case 4: 
      warn(cause);
      break;
    case 5: 
      error(cause);
      break;
    default: 
      throw new Error();
    }
  }
  
  public void log(InternalLogLevel level, String msg)
  {
    switch (1.$SwitchMap$io$netty$util$internal$logging$InternalLogLevel[level.ordinal()]) {
    case 1: 
      trace(msg);
      break;
    case 2: 
      debug(msg);
      break;
    case 3: 
      info(msg);
      break;
    case 4: 
      warn(msg);
      break;
    case 5: 
      error(msg);
      break;
    default: 
      throw new Error();
    }
  }
  
  public void log(InternalLogLevel level, String format, Object arg)
  {
    switch (1.$SwitchMap$io$netty$util$internal$logging$InternalLogLevel[level.ordinal()]) {
    case 1: 
      trace(format, arg);
      break;
    case 2: 
      debug(format, arg);
      break;
    case 3: 
      info(format, arg);
      break;
    case 4: 
      warn(format, arg);
      break;
    case 5: 
      error(format, arg);
      break;
    default: 
      throw new Error();
    }
  }
  
  public void log(InternalLogLevel level, String format, Object argA, Object argB)
  {
    switch (1.$SwitchMap$io$netty$util$internal$logging$InternalLogLevel[level.ordinal()]) {
    case 1: 
      trace(format, argA, argB);
      break;
    case 2: 
      debug(format, argA, argB);
      break;
    case 3: 
      info(format, argA, argB);
      break;
    case 4: 
      warn(format, argA, argB);
      break;
    case 5: 
      error(format, argA, argB);
      break;
    default: 
      throw new Error();
    }
  }
  
  public void log(InternalLogLevel level, String format, Object... arguments)
  {
    switch (1.$SwitchMap$io$netty$util$internal$logging$InternalLogLevel[level.ordinal()]) {
    case 1: 
      trace(format, arguments);
      break;
    case 2: 
      debug(format, arguments);
      break;
    case 3: 
      info(format, arguments);
      break;
    case 4: 
      warn(format, arguments);
      break;
    case 5: 
      error(format, arguments);
      break;
    default: 
      throw new Error();
    }
  }
  
  protected Object readResolve() throws ObjectStreamException {
    return InternalLoggerFactory.getInstance(name());
  }
  
  public String toString()
  {
    return StringUtil.simpleClassName(this) + '(' + name() + ')';
  }
}
