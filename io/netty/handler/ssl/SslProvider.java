package io.netty.handler.ssl;

import java.security.Provider;

























public enum SslProvider
{
  JDK, 
  


  OPENSSL, 
  


  OPENSSL_REFCNT;
  


  private SslProvider() {}
  

  public static boolean isAlpnSupported(SslProvider provider)
  {
    switch (1.$SwitchMap$io$netty$handler$ssl$SslProvider[provider.ordinal()]) {
    case 1: 
      return JdkAlpnApplicationProtocolNegotiator.isAlpnSupported();
    case 2: 
    case 3: 
      return OpenSsl.isAlpnSupported();
    }
    throw new Error("Unknown SslProvider: " + provider);
  }
  




  public static boolean isTlsv13Supported(SslProvider sslProvider)
  {
    return isTlsv13Supported(sslProvider, null);
  }
  



  public static boolean isTlsv13Supported(SslProvider sslProvider, Provider provider)
  {
    switch (1.$SwitchMap$io$netty$handler$ssl$SslProvider[sslProvider.ordinal()]) {
    case 1: 
      return SslUtils.isTLSv13SupportedByJDK(provider);
    case 2: 
    case 3: 
      return OpenSsl.isTlsv13Supported();
    }
    throw new Error("Unknown SslProvider: " + sslProvider);
  }
  




  static boolean isTlsv13EnabledByDefault(SslProvider sslProvider, Provider provider)
  {
    switch (1.$SwitchMap$io$netty$handler$ssl$SslProvider[sslProvider.ordinal()]) {
    case 1: 
      return SslUtils.isTLSv13EnabledByJDK(provider);
    case 2: 
    case 3: 
      return OpenSsl.isTlsv13Supported();
    }
    throw new Error("Unknown SslProvider: " + sslProvider);
  }
}
