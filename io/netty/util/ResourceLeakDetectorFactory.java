package io.netty.util;

import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.lang.reflect.Constructor;




















public abstract class ResourceLeakDetectorFactory
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(ResourceLeakDetectorFactory.class);
  
  private static volatile ResourceLeakDetectorFactory factoryInstance = new DefaultResourceLeakDetectorFactory();
  

  public ResourceLeakDetectorFactory() {}
  

  public static ResourceLeakDetectorFactory instance()
  {
    return factoryInstance;
  }
  






  public static void setResourceLeakDetectorFactory(ResourceLeakDetectorFactory factory)
  {
    factoryInstance = (ResourceLeakDetectorFactory)ObjectUtil.checkNotNull(factory, "factory");
  }
  






  public final <T> ResourceLeakDetector<T> newResourceLeakDetector(Class<T> resource)
  {
    return newResourceLeakDetector(resource, ResourceLeakDetector.SAMPLING_INTERVAL);
  }
  










  @Deprecated
  public abstract <T> ResourceLeakDetector<T> newResourceLeakDetector(Class<T> paramClass, int paramInt, long paramLong);
  










  public <T> ResourceLeakDetector<T> newResourceLeakDetector(Class<T> resource, int samplingInterval)
  {
    ObjectUtil.checkPositive(samplingInterval, "samplingInterval");
    return newResourceLeakDetector(resource, samplingInterval, Long.MAX_VALUE);
  }
  
  private static final class DefaultResourceLeakDetectorFactory extends ResourceLeakDetectorFactory
  {
    private final Constructor<?> obsoleteCustomClassConstructor;
    private final Constructor<?> customClassConstructor;
    
    DefaultResourceLeakDetectorFactory()
    {
      String customLeakDetector;
      try
      {
        customLeakDetector = SystemPropertyUtil.get("io.netty.customResourceLeakDetector");
      } catch (Throwable cause) { String customLeakDetector;
        ResourceLeakDetectorFactory.logger.error("Could not access System property: io.netty.customResourceLeakDetector", cause);
        customLeakDetector = null;
      }
      if (customLeakDetector == null) {
        obsoleteCustomClassConstructor = (this.customClassConstructor = null);
      } else {
        obsoleteCustomClassConstructor = obsoleteCustomClassConstructor(customLeakDetector);
        customClassConstructor = customClassConstructor(customLeakDetector);
      }
    }
    
    private static Constructor<?> obsoleteCustomClassConstructor(String customLeakDetector) {
      try {
        Class<?> detectorClass = Class.forName(customLeakDetector, true, 
          PlatformDependent.getSystemClassLoader());
        
        if (ResourceLeakDetector.class.isAssignableFrom(detectorClass)) {
          return detectorClass.getConstructor(new Class[] { Class.class, Integer.TYPE, Long.TYPE });
        }
        ResourceLeakDetectorFactory.logger.error("Class {} does not inherit from ResourceLeakDetector.", customLeakDetector);
      }
      catch (Throwable t) {
        ResourceLeakDetectorFactory.logger.error("Could not load custom resource leak detector class provided: {}", customLeakDetector, t);
      }
      
      return null;
    }
    
    private static Constructor<?> customClassConstructor(String customLeakDetector) {
      try {
        Class<?> detectorClass = Class.forName(customLeakDetector, true, 
          PlatformDependent.getSystemClassLoader());
        
        if (ResourceLeakDetector.class.isAssignableFrom(detectorClass)) {
          return detectorClass.getConstructor(new Class[] { Class.class, Integer.TYPE });
        }
        ResourceLeakDetectorFactory.logger.error("Class {} does not inherit from ResourceLeakDetector.", customLeakDetector);
      }
      catch (Throwable t) {
        ResourceLeakDetectorFactory.logger.error("Could not load custom resource leak detector class provided: {}", customLeakDetector, t);
      }
      
      return null;
    }
    


    public <T> ResourceLeakDetector<T> newResourceLeakDetector(Class<T> resource, int samplingInterval, long maxActive)
    {
      if (obsoleteCustomClassConstructor != null)
      {
        try
        {
          ResourceLeakDetector<T> leakDetector = (ResourceLeakDetector)obsoleteCustomClassConstructor.newInstance(new Object[] { resource, 
            Integer.valueOf(samplingInterval), Long.valueOf(maxActive) });
          ResourceLeakDetectorFactory.logger.debug("Loaded custom ResourceLeakDetector: {}", obsoleteCustomClassConstructor
            .getDeclaringClass().getName());
          return leakDetector;
        } catch (Throwable t) {
          ResourceLeakDetectorFactory.logger.error("Could not load custom resource leak detector provided: {} with the given resource: {}", new Object[] {obsoleteCustomClassConstructor
          
            .getDeclaringClass().getName(), resource, t });
        }
      }
      
      ResourceLeakDetector<T> resourceLeakDetector = new ResourceLeakDetector(resource, samplingInterval, maxActive);
      
      ResourceLeakDetectorFactory.logger.debug("Loaded default ResourceLeakDetector: {}", resourceLeakDetector);
      return resourceLeakDetector;
    }
    
    public <T> ResourceLeakDetector<T> newResourceLeakDetector(Class<T> resource, int samplingInterval)
    {
      if (customClassConstructor != null)
      {
        try
        {
          ResourceLeakDetector<T> leakDetector = (ResourceLeakDetector)customClassConstructor.newInstance(new Object[] { resource, Integer.valueOf(samplingInterval) });
          ResourceLeakDetectorFactory.logger.debug("Loaded custom ResourceLeakDetector: {}", customClassConstructor
            .getDeclaringClass().getName());
          return leakDetector;
        } catch (Throwable t) {
          ResourceLeakDetectorFactory.logger.error("Could not load custom resource leak detector provided: {} with the given resource: {}", new Object[] {customClassConstructor
          
            .getDeclaringClass().getName(), resource, t });
        }
      }
      
      ResourceLeakDetector<T> resourceLeakDetector = new ResourceLeakDetector(resource, samplingInterval);
      ResourceLeakDetectorFactory.logger.debug("Loaded default ResourceLeakDetector: {}", resourceLeakDetector);
      return resourceLeakDetector;
    }
  }
}
