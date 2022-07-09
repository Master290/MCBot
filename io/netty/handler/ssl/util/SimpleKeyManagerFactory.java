package io.netty.handler.ssl.util;

import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.SuppressJava6Requirement;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Provider;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.KeyManagerFactorySpi;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509KeyManager;




















public abstract class SimpleKeyManagerFactory
  extends KeyManagerFactory
{
  private static final Provider PROVIDER = new Provider("", 0.0D, "")
  {
    private static final long serialVersionUID = -2680540247105807895L;
  };
  







  private static final FastThreadLocal<SimpleKeyManagerFactorySpi> CURRENT_SPI = new FastThreadLocal()
  {
    protected SimpleKeyManagerFactory.SimpleKeyManagerFactorySpi initialValue()
    {
      return new SimpleKeyManagerFactory.SimpleKeyManagerFactorySpi(null);
    }
  };
  


  protected SimpleKeyManagerFactory()
  {
    this("");
  }
  




  protected SimpleKeyManagerFactory(String name)
  {
    super((KeyManagerFactorySpi)CURRENT_SPI.get(), PROVIDER, (String)ObjectUtil.checkNotNull(name, "name"));
    ((SimpleKeyManagerFactorySpi)CURRENT_SPI.get()).init(this);
    CURRENT_SPI.remove();
  }
  


  protected abstract void engineInit(KeyStore paramKeyStore, char[] paramArrayOfChar)
    throws Exception;
  


  protected abstract void engineInit(ManagerFactoryParameters paramManagerFactoryParameters)
    throws Exception;
  

  protected abstract KeyManager[] engineGetKeyManagers();
  

  private static final class SimpleKeyManagerFactorySpi
    extends KeyManagerFactorySpi
  {
    private SimpleKeyManagerFactory parent;
    
    private volatile KeyManager[] keyManagers;
    

    private SimpleKeyManagerFactorySpi() {}
    

    void init(SimpleKeyManagerFactory parent)
    {
      this.parent = parent;
    }
    
    protected void engineInit(KeyStore keyStore, char[] pwd) throws KeyStoreException
    {
      try {
        parent.engineInit(keyStore, pwd);
      } catch (KeyStoreException e) {
        throw e;
      } catch (Exception e) {
        throw new KeyStoreException(e);
      }
    }
    
    protected void engineInit(ManagerFactoryParameters managerFactoryParameters) throws InvalidAlgorithmParameterException
    {
      try
      {
        parent.engineInit(managerFactoryParameters);
      } catch (InvalidAlgorithmParameterException e) {
        throw e;
      } catch (Exception e) {
        throw new InvalidAlgorithmParameterException(e);
      }
    }
    
    protected KeyManager[] engineGetKeyManagers()
    {
      KeyManager[] keyManagers = this.keyManagers;
      if (keyManagers == null) {
        keyManagers = parent.engineGetKeyManagers();
        if (PlatformDependent.javaVersion() >= 7) {
          wrapIfNeeded(keyManagers);
        }
        this.keyManagers = keyManagers;
      }
      return (KeyManager[])keyManagers.clone();
    }
    
    @SuppressJava6Requirement(reason="Usage guarded by java version check")
    private static void wrapIfNeeded(KeyManager[] keyManagers) {
      for (int i = 0; i < keyManagers.length; i++) {
        KeyManager tm = keyManagers[i];
        if (((tm instanceof X509KeyManager)) && (!(tm instanceof X509ExtendedKeyManager))) {
          keyManagers[i] = new X509KeyManagerWrapper((X509KeyManager)tm);
        }
      }
    }
  }
}
