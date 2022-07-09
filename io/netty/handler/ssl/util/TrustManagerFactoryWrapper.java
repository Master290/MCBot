package io.netty.handler.ssl.util;

import io.netty.util.internal.ObjectUtil;
import java.security.KeyStore;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;















public final class TrustManagerFactoryWrapper
  extends SimpleTrustManagerFactory
{
  private final TrustManager tm;
  
  public TrustManagerFactoryWrapper(TrustManager tm)
  {
    this.tm = ((TrustManager)ObjectUtil.checkNotNull(tm, "tm"));
  }
  
  protected void engineInit(KeyStore keyStore)
    throws Exception
  {}
  
  protected void engineInit(ManagerFactoryParameters managerFactoryParameters) throws Exception
  {}
  
  protected TrustManager[] engineGetTrustManagers()
  {
    return new TrustManager[] { tm };
  }
}
