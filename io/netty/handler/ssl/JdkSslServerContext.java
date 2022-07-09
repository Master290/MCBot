package io.netty.handler.ssl;

import java.io.File;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.cert.X509Certificate;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.TrustManagerFactory;






























@Deprecated
public final class JdkSslServerContext
  extends JdkSslContext
{
  @Deprecated
  public JdkSslServerContext(File certChainFile, File keyFile)
    throws SSLException
  {
    this(null, certChainFile, keyFile, null, null, IdentityCipherSuiteFilter.INSTANCE, JdkDefaultApplicationProtocolNegotiator.INSTANCE, 0L, 0L, null);
  }
  








  @Deprecated
  public JdkSslServerContext(File certChainFile, File keyFile, String keyPassword)
    throws SSLException
  {
    this(certChainFile, keyFile, keyPassword, null, IdentityCipherSuiteFilter.INSTANCE, JdkDefaultApplicationProtocolNegotiator.INSTANCE, 0L, 0L);
  }
  



















  @Deprecated
  public JdkSslServerContext(File certChainFile, File keyFile, String keyPassword, Iterable<String> ciphers, Iterable<String> nextProtocols, long sessionCacheSize, long sessionTimeout)
    throws SSLException
  {
    this(null, certChainFile, keyFile, keyPassword, ciphers, IdentityCipherSuiteFilter.INSTANCE, 
      toNegotiator(toApplicationProtocolConfig(nextProtocols), true), sessionCacheSize, sessionTimeout, 
      KeyStore.getDefaultType());
  }
  


















  @Deprecated
  public JdkSslServerContext(File certChainFile, File keyFile, String keyPassword, Iterable<String> ciphers, CipherSuiteFilter cipherFilter, ApplicationProtocolConfig apn, long sessionCacheSize, long sessionTimeout)
    throws SSLException
  {
    this(null, certChainFile, keyFile, keyPassword, ciphers, cipherFilter, 
      toNegotiator(apn, true), sessionCacheSize, sessionTimeout, KeyStore.getDefaultType());
  }
  


















  @Deprecated
  public JdkSslServerContext(File certChainFile, File keyFile, String keyPassword, Iterable<String> ciphers, CipherSuiteFilter cipherFilter, JdkApplicationProtocolNegotiator apn, long sessionCacheSize, long sessionTimeout)
    throws SSLException
  {
    this(null, certChainFile, keyFile, keyPassword, ciphers, cipherFilter, apn, sessionCacheSize, sessionTimeout, 
      KeyStore.getDefaultType());
  }
  

  JdkSslServerContext(Provider provider, File certChainFile, File keyFile, String keyPassword, Iterable<String> ciphers, CipherSuiteFilter cipherFilter, JdkApplicationProtocolNegotiator apn, long sessionCacheSize, long sessionTimeout, String keyStore)
    throws SSLException
  {
    super(newSSLContext(provider, null, null, 
      toX509CertificatesInternal(certChainFile), toPrivateKeyInternal(keyFile, keyPassword), keyPassword, null, sessionCacheSize, sessionTimeout, keyStore), false, ciphers, cipherFilter, apn, ClientAuth.NONE, null, false);
  }
  
































  @Deprecated
  public JdkSslServerContext(File trustCertCollectionFile, TrustManagerFactory trustManagerFactory, File keyCertChainFile, File keyFile, String keyPassword, KeyManagerFactory keyManagerFactory, Iterable<String> ciphers, CipherSuiteFilter cipherFilter, ApplicationProtocolConfig apn, long sessionCacheSize, long sessionTimeout)
    throws SSLException
  {
    super(newSSLContext(null, toX509CertificatesInternal(trustCertCollectionFile), trustManagerFactory, 
      toX509CertificatesInternal(keyCertChainFile), toPrivateKeyInternal(keyFile, keyPassword), keyPassword, keyManagerFactory, sessionCacheSize, sessionTimeout, null), false, ciphers, cipherFilter, apn, ClientAuth.NONE, null, false);
  }
  

































  @Deprecated
  public JdkSslServerContext(File trustCertCollectionFile, TrustManagerFactory trustManagerFactory, File keyCertChainFile, File keyFile, String keyPassword, KeyManagerFactory keyManagerFactory, Iterable<String> ciphers, CipherSuiteFilter cipherFilter, JdkApplicationProtocolNegotiator apn, long sessionCacheSize, long sessionTimeout)
    throws SSLException
  {
    super(newSSLContext(null, toX509CertificatesInternal(trustCertCollectionFile), trustManagerFactory, 
      toX509CertificatesInternal(keyCertChainFile), toPrivateKeyInternal(keyFile, keyPassword), keyPassword, keyManagerFactory, sessionCacheSize, sessionTimeout, 
      KeyStore.getDefaultType()), false, ciphers, cipherFilter, apn, ClientAuth.NONE, null, false);
  }
  





  JdkSslServerContext(Provider provider, X509Certificate[] trustCertCollection, TrustManagerFactory trustManagerFactory, X509Certificate[] keyCertChain, PrivateKey key, String keyPassword, KeyManagerFactory keyManagerFactory, Iterable<String> ciphers, CipherSuiteFilter cipherFilter, ApplicationProtocolConfig apn, long sessionCacheSize, long sessionTimeout, ClientAuth clientAuth, String[] protocols, boolean startTls, String keyStore)
    throws SSLException
  {
    super(newSSLContext(provider, trustCertCollection, trustManagerFactory, keyCertChain, key, keyPassword, keyManagerFactory, sessionCacheSize, sessionTimeout, keyStore), false, ciphers, cipherFilter, 
    
      toNegotiator(apn, true), clientAuth, protocols, startTls);
  }
  


  private static SSLContext newSSLContext(Provider sslContextProvider, X509Certificate[] trustCertCollection, TrustManagerFactory trustManagerFactory, X509Certificate[] keyCertChain, PrivateKey key, String keyPassword, KeyManagerFactory keyManagerFactory, long sessionCacheSize, long sessionTimeout, String keyStore)
    throws SSLException
  {
    if ((key == null) && (keyManagerFactory == null)) {
      throw new NullPointerException("key, keyManagerFactory");
    }
    try
    {
      if (trustCertCollection != null) {
        trustManagerFactory = buildTrustManagerFactory(trustCertCollection, trustManagerFactory, keyStore);
      }
      if (key != null) {
        keyManagerFactory = buildKeyManagerFactory(keyCertChain, null, key, keyPassword, keyManagerFactory, null);
      }
      



      SSLContext ctx = sslContextProvider == null ? SSLContext.getInstance("TLS") : SSLContext.getInstance("TLS", sslContextProvider);
      ctx.init(keyManagerFactory.getKeyManagers(), trustManagerFactory == null ? null : trustManagerFactory
        .getTrustManagers(), null);
      

      SSLSessionContext sessCtx = ctx.getServerSessionContext();
      if (sessionCacheSize > 0L) {
        sessCtx.setSessionCacheSize((int)Math.min(sessionCacheSize, 2147483647L));
      }
      if (sessionTimeout > 0L) {
        sessCtx.setSessionTimeout((int)Math.min(sessionTimeout, 2147483647L));
      }
      return ctx;
    } catch (Exception e) {
      if ((e instanceof SSLException)) {
        throw ((SSLException)e);
      }
      throw new SSLException("failed to initialize the server-side SSL context", e);
    }
  }
}
