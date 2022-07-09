package io.netty.handler.ssl;

import io.netty.util.internal.PlatformDependent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.net.ssl.SSLEngine;




















final class Conscrypt
{
  private static final Method IS_CONSCRYPT_SSLENGINE;
  
  static
  {
    Method isConscryptSSLEngine = null;
    
    if (((PlatformDependent.javaVersion() >= 8) && 
    

      (PlatformDependent.javaVersion() < 15)) || (PlatformDependent.isAndroid())) {
      try {
        Class<?> providerClass = Class.forName("org.conscrypt.OpenSSLProvider", true, 
          PlatformDependent.getClassLoader(ConscryptAlpnSslEngine.class));
        providerClass.newInstance();
        
        Class<?> conscryptClass = Class.forName("org.conscrypt.Conscrypt", true, 
          PlatformDependent.getClassLoader(ConscryptAlpnSslEngine.class));
        isConscryptSSLEngine = conscryptClass.getMethod("isConscrypt", new Class[] { SSLEngine.class });
      }
      catch (Throwable localThrowable) {}
    }
    
    IS_CONSCRYPT_SSLENGINE = isConscryptSSLEngine;
  }
  


  static boolean isAvailable()
  {
    return IS_CONSCRYPT_SSLENGINE != null;
  }
  

  static boolean isEngineSupported(SSLEngine engine)
  {
    try
    {
      if (IS_CONSCRYPT_SSLENGINE != null) {} return ((Boolean)IS_CONSCRYPT_SSLENGINE.invoke(null, new Object[] { engine })).booleanValue();
    } catch (IllegalAccessException ignore) {
      return false;
    } catch (InvocationTargetException ex) {
      throw new RuntimeException(ex);
    }
  }
  
  private Conscrypt() {}
}
