package io.netty.handler.codec.serialization;

import java.util.Map;














class CachingClassResolver
  implements ClassResolver
{
  private final Map<String, Class<?>> classCache;
  private final ClassResolver delegate;
  
  CachingClassResolver(ClassResolver delegate, Map<String, Class<?>> classCache)
  {
    this.delegate = delegate;
    this.classCache = classCache;
  }
  

  public Class<?> resolve(String className)
    throws ClassNotFoundException
  {
    Class<?> clazz = (Class)classCache.get(className);
    if (clazz != null) {
      return clazz;
    }
    

    clazz = delegate.resolve(className);
    
    classCache.put(className, clazz);
    return clazz;
  }
}
