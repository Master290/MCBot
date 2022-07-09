package io.netty.handler.ssl;

import java.io.File;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Map.Entry;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;



























public final class OpenSslClientContext
  extends OpenSslContext
{
  private final OpenSslSessionContext sessionContext;
  
  @Deprecated
  public OpenSslClientContext()
    throws SSLException
  {
    this(null, null, null, null, null, null, null, IdentityCipherSuiteFilter.INSTANCE, null, 0L, 0L);
  }
  





  @Deprecated
  public OpenSslClientContext(File certChainFile)
    throws SSLException
  {
    this(certChainFile, null);
  }
  






  @Deprecated
  public OpenSslClientContext(TrustManagerFactory trustManagerFactory)
    throws SSLException
  {
    this(null, trustManagerFactory);
  }
  








  @Deprecated
  public OpenSslClientContext(File certChainFile, TrustManagerFactory trustManagerFactory)
    throws SSLException
  {
    this(certChainFile, trustManagerFactory, null, null, null, null, null, IdentityCipherSuiteFilter.INSTANCE, null, 0L, 0L);
  }
  

















  @Deprecated
  public OpenSslClientContext(File certChainFile, TrustManagerFactory trustManagerFactory, Iterable<String> ciphers, ApplicationProtocolConfig apn, long sessionCacheSize, long sessionTimeout)
    throws SSLException
  {
    this(certChainFile, trustManagerFactory, null, null, null, null, ciphers, IdentityCipherSuiteFilter.INSTANCE, apn, sessionCacheSize, sessionTimeout);
  }
  


















  @Deprecated
  public OpenSslClientContext(File certChainFile, TrustManagerFactory trustManagerFactory, Iterable<String> ciphers, CipherSuiteFilter cipherFilter, ApplicationProtocolConfig apn, long sessionCacheSize, long sessionTimeout)
    throws SSLException
  {
    this(certChainFile, trustManagerFactory, null, null, null, null, ciphers, cipherFilter, apn, sessionCacheSize, sessionTimeout);
  }
  



































  @Deprecated
  public OpenSslClientContext(File trustCertCollectionFile, TrustManagerFactory trustManagerFactory, File keyCertChainFile, File keyFile, String keyPassword, KeyManagerFactory keyManagerFactory, Iterable<String> ciphers, CipherSuiteFilter cipherFilter, ApplicationProtocolConfig apn, long sessionCacheSize, long sessionTimeout)
    throws SSLException
  {
    this(toX509CertificatesInternal(trustCertCollectionFile), trustManagerFactory, 
      toX509CertificatesInternal(keyCertChainFile), toPrivateKeyInternal(keyFile, keyPassword), keyPassword, keyManagerFactory, ciphers, cipherFilter, apn, null, sessionCacheSize, sessionTimeout, false, 
      
      KeyStore.getDefaultType(), new Map.Entry[0]);
  }
  




  OpenSslClientContext(X509Certificate[] trustCertCollection, TrustManagerFactory trustManagerFactory, X509Certificate[] keyCertChain, PrivateKey key, String keyPassword, KeyManagerFactory keyManagerFactory, Iterable<String> ciphers, CipherSuiteFilter cipherFilter, ApplicationProtocolConfig apn, String[] protocols, long sessionCacheSize, long sessionTimeout, boolean enableOcsp, String keyStore, Map.Entry<SslContextOption<?>, Object>... options)
    throws SSLException
  {
    super(ciphers, cipherFilter, apn, 0, keyCertChain, ClientAuth.NONE, protocols, false, enableOcsp, options);
    
    boolean success = false;
    try {
      OpenSslKeyMaterialProvider.validateKeyMaterialSupported(keyCertChain, key, keyPassword);
      sessionContext = ReferenceCountedOpenSslClientContext.newSessionContext(this, ctx, engineMap, trustCertCollection, trustManagerFactory, keyCertChain, key, keyPassword, keyManagerFactory, keyStore, sessionCacheSize, sessionTimeout);
      

      success = true;
    } finally {
      if (!success) {
        release();
      }
    }
  }
  
  public OpenSslSessionContext sessionContext()
  {
    return sessionContext;
  }
}
