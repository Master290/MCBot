package io.netty.util.internal.shaded.org.jctools.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import sun.misc.Unsafe;




































public class UnsafeAccess
{
  public static final Unsafe UNSAFE = ;
  public static final boolean SUPPORTS_GET_AND_SET_REF = hasGetAndSetSupport();
  public static final boolean SUPPORTS_GET_AND_ADD_LONG = hasGetAndAddLongSupport();
  
  public UnsafeAccess() {}
  
  private static Unsafe getUnsafe()
  {
    try
    {
      Field field = Unsafe.class.getDeclaredField("theUnsafe");
      field.setAccessible(true);
      instance = (Unsafe)field.get(null);
    }
    catch (Exception ignored)
    {
      try
      {
        Unsafe instance;
        

        Constructor<Unsafe> c = Unsafe.class.getDeclaredConstructor(new Class[0]);
        c.setAccessible(true);
        instance = (Unsafe)c.newInstance(new Object[0]);
      }
      catch (Exception e) {
        Unsafe instance;
        throw new RuntimeException(e);
      } }
    Unsafe instance;
    return instance;
  }
  
  private static boolean hasGetAndSetSupport()
  {
    try
    {
      Unsafe.class.getMethod("getAndSetObject", new Class[] { Object.class, Long.TYPE, Object.class });
      return true;
    }
    catch (Exception localException) {}
    

    return false;
  }
  
  private static boolean hasGetAndAddLongSupport()
  {
    try
    {
      Unsafe.class.getMethod("getAndAddLong", new Class[] { Object.class, Long.TYPE, Long.TYPE });
      return true;
    }
    catch (Exception localException) {}
    

    return false;
  }
  
  public static long fieldOffset(Class clz, String fieldName) throws RuntimeException
  {
    try
    {
      return UNSAFE.objectFieldOffset(clz.getDeclaredField(fieldName));
    }
    catch (NoSuchFieldException e)
    {
      throw new RuntimeException(e);
    }
  }
}
