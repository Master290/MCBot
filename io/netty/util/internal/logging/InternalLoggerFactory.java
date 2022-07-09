package io.netty.util.internal.logging;

import io.netty.util.internal.ObjectUtil;






























public abstract class InternalLoggerFactory
{
  private static volatile InternalLoggerFactory defaultFactory;
  
  public InternalLoggerFactory() {}
  
  private static InternalLoggerFactory newDefaultFactory(String name)
  {
    InternalLoggerFactory f = useSlf4JLoggerFactory(name);
    if (f != null) {
      return f;
    }
    
    f = useLog4J2LoggerFactory(name);
    if (f != null) {
      return f;
    }
    
    f = useLog4JLoggerFactory(name);
    if (f != null) {
      return f;
    }
    
    return useJdkLoggerFactory(name);
  }
  
  private static InternalLoggerFactory useSlf4JLoggerFactory(String name) {
    try {
      InternalLoggerFactory f = Slf4JLoggerFactory.INSTANCE_WITH_NOP_CHECK;
      f.newInstance(name).debug("Using SLF4J as the default logging framework");
      return f;
    } catch (LinkageError ignore) {
      return null;
    }
    catch (Exception ignore) {}
    return null;
  }
  
  private static InternalLoggerFactory useLog4J2LoggerFactory(String name)
  {
    try {
      InternalLoggerFactory f = Log4J2LoggerFactory.INSTANCE;
      f.newInstance(name).debug("Using Log4J2 as the default logging framework");
      return f;
    } catch (LinkageError ignore) {
      return null;
    }
    catch (Exception ignore) {}
    return null;
  }
  
  private static InternalLoggerFactory useLog4JLoggerFactory(String name)
  {
    try {
      InternalLoggerFactory f = Log4JLoggerFactory.INSTANCE;
      f.newInstance(name).debug("Using Log4J as the default logging framework");
      return f;
    } catch (LinkageError ignore) {
      return null;
    }
    catch (Exception ignore) {}
    return null;
  }
  
  private static InternalLoggerFactory useJdkLoggerFactory(String name)
  {
    InternalLoggerFactory f = JdkLoggerFactory.INSTANCE;
    f.newInstance(name).debug("Using java.util.logging as the default logging framework");
    return f;
  }
  



  public static InternalLoggerFactory getDefaultFactory()
  {
    if (defaultFactory == null) {
      defaultFactory = newDefaultFactory(InternalLoggerFactory.class.getName());
    }
    return defaultFactory;
  }
  


  public static void setDefaultFactory(InternalLoggerFactory defaultFactory)
  {
    defaultFactory = (InternalLoggerFactory)ObjectUtil.checkNotNull(defaultFactory, "defaultFactory");
  }
  


  public static InternalLogger getInstance(Class<?> clazz)
  {
    return getInstance(clazz.getName());
  }
  


  public static InternalLogger getInstance(String name)
  {
    return getDefaultFactory().newInstance(name);
  }
  
  protected abstract InternalLogger newInstance(String paramString);
}
