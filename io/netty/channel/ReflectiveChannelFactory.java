package io.netty.channel;

import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;
import java.lang.reflect.Constructor;



















public class ReflectiveChannelFactory<T extends Channel>
  implements ChannelFactory<T>
{
  private final Constructor<? extends T> constructor;
  
  public ReflectiveChannelFactory(Class<? extends T> clazz)
  {
    ObjectUtil.checkNotNull(clazz, "clazz");
    try {
      constructor = clazz.getConstructor(new Class[0]);
    } catch (NoSuchMethodException e) {
      throw new IllegalArgumentException("Class " + StringUtil.simpleClassName(clazz) + " does not have a public non-arg constructor", e);
    }
  }
  
  public T newChannel()
  {
    try
    {
      return (Channel)constructor.newInstance(new Object[0]);
    } catch (Throwable t) {
      throw new ChannelException("Unable to create Channel from class " + constructor.getDeclaringClass(), t);
    }
  }
  
  public String toString()
  {
    return 
      StringUtil.simpleClassName(ReflectiveChannelFactory.class) + '(' + StringUtil.simpleClassName(constructor.getDeclaringClass()) + ".class)";
  }
}
