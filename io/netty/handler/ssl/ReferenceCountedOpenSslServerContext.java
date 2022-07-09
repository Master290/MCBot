package io.netty.handler.ssl;

import io.netty.buffer.ByteBufAllocator;
import io.netty.internal.tcnative.CertificateCallback;
import io.netty.internal.tcnative.SSLContext;
import io.netty.internal.tcnative.SniHostNameMatcher;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.SuppressJava6Requirement;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Map.Entry;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;

























public final class ReferenceCountedOpenSslServerContext
  extends ReferenceCountedOpenSslContext
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(ReferenceCountedOpenSslServerContext.class);
  private static final byte[] ID = { 110, 101, 116, 116, 121 };
  

  private final OpenSslServerSessionContext sessionContext;
  


  ReferenceCountedOpenSslServerContext(X509Certificate[] trustCertCollection, TrustManagerFactory trustManagerFactory, X509Certificate[] keyCertChain, PrivateKey key, String keyPassword, KeyManagerFactory keyManagerFactory, Iterable<String> ciphers, CipherSuiteFilter cipherFilter, ApplicationProtocolConfig apn, long sessionCacheSize, long sessionTimeout, ClientAuth clientAuth, String[] protocols, boolean startTls, boolean enableOcsp, String keyStore, Map.Entry<SslContextOption<?>, Object>... options)
    throws SSLException
  {
    this(trustCertCollection, trustManagerFactory, keyCertChain, key, keyPassword, keyManagerFactory, ciphers, cipherFilter, 
      toNegotiator(apn), sessionCacheSize, sessionTimeout, clientAuth, protocols, startTls, enableOcsp, keyStore, options);
  }
  





  ReferenceCountedOpenSslServerContext(X509Certificate[] trustCertCollection, TrustManagerFactory trustManagerFactory, X509Certificate[] keyCertChain, PrivateKey key, String keyPassword, KeyManagerFactory keyManagerFactory, Iterable<String> ciphers, CipherSuiteFilter cipherFilter, OpenSslApplicationProtocolNegotiator apn, long sessionCacheSize, long sessionTimeout, ClientAuth clientAuth, String[] protocols, boolean startTls, boolean enableOcsp, String keyStore, Map.Entry<SslContextOption<?>, Object>... options)
    throws SSLException
  {
    super(ciphers, cipherFilter, apn, 1, keyCertChain, clientAuth, protocols, startTls, enableOcsp, true, options);
    

    boolean success = false;
    try {
      sessionContext = newSessionContext(this, ctx, engineMap, trustCertCollection, trustManagerFactory, keyCertChain, key, keyPassword, keyManagerFactory, keyStore, sessionCacheSize, sessionTimeout);
      

      if (SERVER_ENABLE_SESSION_TICKET) {
        sessionContext.setTicketKeys(new OpenSslSessionTicketKey[0]);
      }
      success = true;
    } finally {
      if (!success) {
        release();
      }
    }
  }
  
  public OpenSslServerSessionContext sessionContext()
  {
    return sessionContext;
  }
  





  static OpenSslServerSessionContext newSessionContext(ReferenceCountedOpenSslContext thiz, long ctx, OpenSslEngineMap engineMap, X509Certificate[] trustCertCollection, TrustManagerFactory trustManagerFactory, X509Certificate[] keyCertChain, PrivateKey key, String keyPassword, KeyManagerFactory keyManagerFactory, String keyStore, long sessionCacheSize, long sessionTimeout)
    throws SSLException
  {
    OpenSslKeyMaterialProvider keyMaterialProvider = null;
    try {
      try {
        SSLContext.setVerify(ctx, 0, 10);
        if (!OpenSsl.useKeyManagerFactory()) {
          if (keyManagerFactory != null) {
            throw new IllegalArgumentException("KeyManagerFactory not supported");
          }
          
          ObjectUtil.checkNotNull(keyCertChain, "keyCertChain");
          
          setKeyMaterial(ctx, keyCertChain, key, keyPassword);
        }
        else
        {
          if (keyManagerFactory == null) {
            char[] keyPasswordChars = keyStorePassword(keyPassword);
            KeyStore ks = buildKeyStore(keyCertChain, key, keyPasswordChars, keyStore);
            if (ks.aliases().hasMoreElements()) {
              keyManagerFactory = new OpenSslX509KeyManagerFactory();
            }
            else {
              keyManagerFactory = new OpenSslCachingX509KeyManagerFactory(KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()));
            }
            keyManagerFactory.init(ks, keyPasswordChars);
          }
          keyMaterialProvider = providerFor(keyManagerFactory, keyPassword);
          
          SSLContext.setCertificateCallback(ctx, new OpenSslServerCertificateCallback(engineMap, new OpenSslKeyMaterialManager(keyMaterialProvider)));
        }
      }
      catch (Exception e) {
        throw new SSLException("failed to set certificate and key", e);
      }
      X509Certificate[] issuers;
      try { if (trustCertCollection != null) {
          trustManagerFactory = buildTrustManagerFactory(trustCertCollection, trustManagerFactory, keyStore);
        } else if (trustManagerFactory == null)
        {
          trustManagerFactory = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm());
          trustManagerFactory.init((KeyStore)null);
        }
        
        X509TrustManager manager = chooseTrustManager(trustManagerFactory.getTrustManagers());
        






        setVerifyCallback(ctx, engineMap, manager);
        
        issuers = manager.getAcceptedIssuers();
        if ((issuers != null) && (issuers.length > 0)) {
          long bio = 0L;
          try {
            bio = toBIO(ByteBufAllocator.DEFAULT, issuers);
            if (!SSLContext.setCACertificateBio(ctx, bio)) {
              throw new SSLException("unable to setup accepted issuers for trustmanager " + manager);
            }
          } finally {
            freeBio(bio);
          }
        }
        
        if (PlatformDependent.javaVersion() >= 8)
        {



          SSLContext.setSniHostnameMatcher(ctx, new OpenSslSniHostnameMatcher(engineMap));
        }
      } catch (SSLException e) {
        throw e;
      } catch (Exception e) {
        throw new SSLException("unable to setup trustmanager", e);
      }
      
      OpenSslServerSessionContext sessionContext = new OpenSslServerSessionContext(thiz, keyMaterialProvider);
      sessionContext.setSessionIdContext(ID);
      
      sessionContext.setSessionCacheEnabled(SERVER_ENABLE_SESSION_CACHE);
      if (sessionCacheSize > 0L) {
        sessionContext.setSessionCacheSize((int)Math.min(sessionCacheSize, 2147483647L));
      }
      if (sessionTimeout > 0L) {
        sessionContext.setSessionTimeout((int)Math.min(sessionTimeout, 2147483647L));
      }
      
      keyMaterialProvider = null;
      
      return sessionContext;
    } finally {
      if (keyMaterialProvider != null) {
        keyMaterialProvider.destroy();
      }
    }
  }
  
  @SuppressJava6Requirement(reason="Guarded by java version check")
  private static void setVerifyCallback(long ctx, OpenSslEngineMap engineMap, X509TrustManager manager)
  {
    if (useExtendedTrustManager(manager)) {
      SSLContext.setCertVerifyCallback(ctx, new ExtendedTrustManagerVerifyCallback(engineMap, (X509ExtendedTrustManager)manager));
    }
    else {
      SSLContext.setCertVerifyCallback(ctx, new TrustManagerVerifyCallback(engineMap, manager));
    }
  }
  
  private static final class OpenSslServerCertificateCallback implements CertificateCallback {
    private final OpenSslEngineMap engineMap;
    private final OpenSslKeyMaterialManager keyManagerHolder;
    
    OpenSslServerCertificateCallback(OpenSslEngineMap engineMap, OpenSslKeyMaterialManager keyManagerHolder) {
      this.engineMap = engineMap;
      this.keyManagerHolder = keyManagerHolder;
    }
    
    public void handle(long ssl, byte[] keyTypeBytes, byte[][] asn1DerEncodedPrincipals) throws Exception
    {
      ReferenceCountedOpenSslEngine engine = engineMap.get(ssl);
      if (engine == null)
      {
        return;
      }
      
      try
      {
        keyManagerHolder.setKeyMaterialServerSide(engine);
      } catch (Throwable cause) {
        engine.initHandshakeException(cause);
        
        if ((cause instanceof Exception)) {
          throw ((Exception)cause);
        }
        throw new SSLException(cause);
      }
    }
  }
  
  private static final class TrustManagerVerifyCallback extends ReferenceCountedOpenSslContext.AbstractCertificateVerifier {
    private final X509TrustManager manager;
    
    TrustManagerVerifyCallback(OpenSslEngineMap engineMap, X509TrustManager manager) {
      super();
      this.manager = manager;
    }
    
    void verify(ReferenceCountedOpenSslEngine engine, X509Certificate[] peerCerts, String auth)
      throws Exception
    {
      manager.checkClientTrusted(peerCerts, auth);
    }
  }
  
  @SuppressJava6Requirement(reason="Usage guarded by java version check")
  private static final class ExtendedTrustManagerVerifyCallback extends ReferenceCountedOpenSslContext.AbstractCertificateVerifier {
    private final X509ExtendedTrustManager manager;
    
    ExtendedTrustManagerVerifyCallback(OpenSslEngineMap engineMap, X509ExtendedTrustManager manager) {
      super();
      this.manager = OpenSslTlsv13X509ExtendedTrustManager.wrap(manager);
    }
    
    void verify(ReferenceCountedOpenSslEngine engine, X509Certificate[] peerCerts, String auth)
      throws Exception
    {
      manager.checkClientTrusted(peerCerts, auth, engine);
    }
  }
  
  private static final class OpenSslSniHostnameMatcher implements SniHostNameMatcher {
    private final OpenSslEngineMap engineMap;
    
    OpenSslSniHostnameMatcher(OpenSslEngineMap engineMap) {
      this.engineMap = engineMap;
    }
    
    public boolean match(long ssl, String hostname)
    {
      ReferenceCountedOpenSslEngine engine = engineMap.get(ssl);
      if (engine != null)
      {
        return engine.checkSniHostnameMatch(hostname.getBytes(CharsetUtil.UTF_8));
      }
      ReferenceCountedOpenSslServerContext.logger.warn("No ReferenceCountedOpenSslEngine found for SSL pointer: {}", Long.valueOf(ssl));
      return false;
    }
  }
}
