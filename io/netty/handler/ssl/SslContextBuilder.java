package io.netty.handler.ssl;

import io.netty.handler.ssl.util.KeyManagerFactoryWrapper;
import io.netty.handler.ssl.util.TrustManagerFactoryWrapper;
import io.netty.util.internal.EmptyArrays;
import io.netty.util.internal.ObjectUtil;
import java.io.File;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;


























public final class SslContextBuilder
{
  private static final Map.Entry[] EMPTY_ENTRIES = new Map.Entry[0];
  private final boolean forServer;
  private SslProvider provider;
  
  public static SslContextBuilder forClient()
  {
    return new SslContextBuilder(false);
  }
  






  public static SslContextBuilder forServer(File keyCertChainFile, File keyFile)
  {
    return new SslContextBuilder(true).keyManager(keyCertChainFile, keyFile);
  }
  


  private Provider sslContextProvider;
  

  private X509Certificate[] trustCertCollection;
  

  private TrustManagerFactory trustManagerFactory;
  

  public static SslContextBuilder forServer(InputStream keyCertChainInputStream, InputStream keyInputStream)
  {
    return new SslContextBuilder(true).keyManager(keyCertChainInputStream, keyInputStream);
  }
  






  public static SslContextBuilder forServer(PrivateKey key, X509Certificate... keyCertChain)
  {
    return new SslContextBuilder(true).keyManager(key, keyCertChain);
  }
  






  public static SslContextBuilder forServer(PrivateKey key, Iterable<? extends X509Certificate> keyCertChain)
  {
    return forServer(key, (X509Certificate[])toArray(keyCertChain, EmptyArrays.EMPTY_X509_CERTIFICATES));
  }
  









  public static SslContextBuilder forServer(File keyCertChainFile, File keyFile, String keyPassword)
  {
    return new SslContextBuilder(true).keyManager(keyCertChainFile, keyFile, keyPassword);
  }
  


  private X509Certificate[] keyCertChain;
  

  private PrivateKey key;
  
  private String keyPassword;
  
  private KeyManagerFactory keyManagerFactory;
  
  private Iterable<String> ciphers;
  
  public static SslContextBuilder forServer(InputStream keyCertChainInputStream, InputStream keyInputStream, String keyPassword)
  {
    return new SslContextBuilder(true).keyManager(keyCertChainInputStream, keyInputStream, keyPassword);
  }
  









  public static SslContextBuilder forServer(PrivateKey key, String keyPassword, X509Certificate... keyCertChain)
  {
    return new SslContextBuilder(true).keyManager(key, keyPassword, keyCertChain);
  }
  









  public static SslContextBuilder forServer(PrivateKey key, String keyPassword, Iterable<? extends X509Certificate> keyCertChain)
  {
    return forServer(key, keyPassword, (X509Certificate[])toArray(keyCertChain, EmptyArrays.EMPTY_X509_CERTIFICATES));
  }
  








  public static SslContextBuilder forServer(KeyManagerFactory keyManagerFactory)
  {
    return new SslContextBuilder(true).keyManager(keyManagerFactory);
  }
  




  public static SslContextBuilder forServer(KeyManager keyManager)
  {
    return new SslContextBuilder(true).keyManager(keyManager);
  }
  










  private CipherSuiteFilter cipherFilter = IdentityCipherSuiteFilter.INSTANCE;
  private ApplicationProtocolConfig apn;
  private long sessionCacheSize;
  private long sessionTimeout;
  private ClientAuth clientAuth = ClientAuth.NONE;
  private String[] protocols;
  private boolean startTls;
  private boolean enableOcsp;
  private String keyStoreType = KeyStore.getDefaultType();
  private final Map<SslContextOption<?>, Object> options = new HashMap();
  
  private SslContextBuilder(boolean forServer) {
    this.forServer = forServer;
  }
  


  public <T> SslContextBuilder option(SslContextOption<T> option, T value)
  {
    if (value == null) {
      options.remove(option);
    } else {
      options.put(option, value);
    }
    return this;
  }
  


  public SslContextBuilder sslProvider(SslProvider provider)
  {
    this.provider = provider;
    return this;
  }
  


  public SslContextBuilder keyStoreType(String keyStoreType)
  {
    this.keyStoreType = keyStoreType;
    return this;
  }
  



  public SslContextBuilder sslContextProvider(Provider sslContextProvider)
  {
    this.sslContextProvider = sslContextProvider;
    return this;
  }
  


  public SslContextBuilder trustManager(File trustCertCollectionFile)
  {
    try
    {
      return trustManager(SslContext.toX509Certificates(trustCertCollectionFile));
    } catch (Exception e) {
      throw new IllegalArgumentException("File does not contain valid certificates: " + trustCertCollectionFile, e);
    }
  }
  





  public SslContextBuilder trustManager(InputStream trustCertCollectionInputStream)
  {
    try
    {
      return trustManager(SslContext.toX509Certificates(trustCertCollectionInputStream));
    } catch (Exception e) {
      throw new IllegalArgumentException("Input stream does not contain valid certificates.", e);
    }
  }
  


  public SslContextBuilder trustManager(X509Certificate... trustCertCollection)
  {
    this.trustCertCollection = (trustCertCollection != null ? (X509Certificate[])trustCertCollection.clone() : null);
    trustManagerFactory = null;
    return this;
  }
  


  public SslContextBuilder trustManager(Iterable<? extends X509Certificate> trustCertCollection)
  {
    return trustManager((X509Certificate[])toArray(trustCertCollection, EmptyArrays.EMPTY_X509_CERTIFICATES));
  }
  


  public SslContextBuilder trustManager(TrustManagerFactory trustManagerFactory)
  {
    trustCertCollection = null;
    this.trustManagerFactory = trustManagerFactory;
    return this;
  }
  






  public SslContextBuilder trustManager(TrustManager trustManager)
  {
    trustManagerFactory = new TrustManagerFactoryWrapper(trustManager);
    trustCertCollection = null;
    return this;
  }
  






  public SslContextBuilder keyManager(File keyCertChainFile, File keyFile)
  {
    return keyManager(keyCertChainFile, keyFile, null);
  }
  










  public SslContextBuilder keyManager(InputStream keyCertChainInputStream, InputStream keyInputStream)
  {
    return keyManager(keyCertChainInputStream, keyInputStream, null);
  }
  






  public SslContextBuilder keyManager(PrivateKey key, X509Certificate... keyCertChain)
  {
    return keyManager(key, null, keyCertChain);
  }
  






  public SslContextBuilder keyManager(PrivateKey key, Iterable<? extends X509Certificate> keyCertChain)
  {
    return keyManager(key, (X509Certificate[])toArray(keyCertChain, EmptyArrays.EMPTY_X509_CERTIFICATES));
  }
  









  public SslContextBuilder keyManager(File keyCertChainFile, File keyFile, String keyPassword)
  {
    try
    {
      keyCertChain = SslContext.toX509Certificates(keyCertChainFile);
    } catch (Exception e) { X509Certificate[] keyCertChain;
      throw new IllegalArgumentException("File does not contain valid certificates: " + keyCertChainFile, e);
    }
    X509Certificate[] keyCertChain;
    try { key = SslContext.toPrivateKey(keyFile, keyPassword);
    } catch (Exception e) { PrivateKey key;
      throw new IllegalArgumentException("File does not contain valid private key: " + keyFile, e); }
    PrivateKey key;
    return keyManager(key, keyPassword, keyCertChain);
  }
  














  public SslContextBuilder keyManager(InputStream keyCertChainInputStream, InputStream keyInputStream, String keyPassword)
  {
    try
    {
      keyCertChain = SslContext.toX509Certificates(keyCertChainInputStream);
    } catch (Exception e) { X509Certificate[] keyCertChain;
      throw new IllegalArgumentException("Input stream not contain valid certificates.", e);
    }
    X509Certificate[] keyCertChain;
    try { key = SslContext.toPrivateKey(keyInputStream, keyPassword);
    } catch (Exception e) { PrivateKey key;
      throw new IllegalArgumentException("Input stream does not contain valid private key.", e); }
    PrivateKey key;
    return keyManager(key, keyPassword, keyCertChain);
  }
  








  public SslContextBuilder keyManager(PrivateKey key, String keyPassword, X509Certificate... keyCertChain)
  {
    if (forServer) {
      ObjectUtil.checkNonEmpty(keyCertChain, "keyCertChain");
      ObjectUtil.checkNotNull(key, "key required for servers");
    }
    if ((keyCertChain == null) || (keyCertChain.length == 0)) {
      this.keyCertChain = null;
    } else {
      for (X509Certificate cert : keyCertChain) {
        ObjectUtil.checkNotNullWithIAE(cert, "cert");
      }
      this.keyCertChain = ((X509Certificate[])keyCertChain.clone());
    }
    this.key = key;
    this.keyPassword = keyPassword;
    keyManagerFactory = null;
    return this;
  }
  









  public SslContextBuilder keyManager(PrivateKey key, String keyPassword, Iterable<? extends X509Certificate> keyCertChain)
  {
    return keyManager(key, keyPassword, (X509Certificate[])toArray(keyCertChain, EmptyArrays.EMPTY_X509_CERTIFICATES));
  }
  










  public SslContextBuilder keyManager(KeyManagerFactory keyManagerFactory)
  {
    if (forServer) {
      ObjectUtil.checkNotNull(keyManagerFactory, "keyManagerFactory required for servers");
    }
    keyCertChain = null;
    key = null;
    keyPassword = null;
    this.keyManagerFactory = keyManagerFactory;
    return this;
  }
  






  public SslContextBuilder keyManager(KeyManager keyManager)
  {
    if (forServer) {
      ObjectUtil.checkNotNull(keyManager, "keyManager required for servers");
    }
    if (keyManager != null) {
      keyManagerFactory = new KeyManagerFactoryWrapper(keyManager);
    } else {
      keyManagerFactory = null;
    }
    keyCertChain = null;
    key = null;
    keyPassword = null;
    return this;
  }
  



  public SslContextBuilder ciphers(Iterable<String> ciphers)
  {
    return ciphers(ciphers, IdentityCipherSuiteFilter.INSTANCE);
  }
  




  public SslContextBuilder ciphers(Iterable<String> ciphers, CipherSuiteFilter cipherFilter)
  {
    this.cipherFilter = ((CipherSuiteFilter)ObjectUtil.checkNotNull(cipherFilter, "cipherFilter"));
    this.ciphers = ciphers;
    return this;
  }
  


  public SslContextBuilder applicationProtocolConfig(ApplicationProtocolConfig apn)
  {
    this.apn = apn;
    return this;
  }
  



  public SslContextBuilder sessionCacheSize(long sessionCacheSize)
  {
    this.sessionCacheSize = sessionCacheSize;
    return this;
  }
  



  public SslContextBuilder sessionTimeout(long sessionTimeout)
  {
    this.sessionTimeout = sessionTimeout;
    return this;
  }
  


  public SslContextBuilder clientAuth(ClientAuth clientAuth)
  {
    this.clientAuth = ((ClientAuth)ObjectUtil.checkNotNull(clientAuth, "clientAuth"));
    return this;
  }
  




  public SslContextBuilder protocols(String... protocols)
  {
    this.protocols = (protocols == null ? null : (String[])protocols.clone());
    return this;
  }
  




  public SslContextBuilder protocols(Iterable<String> protocols)
  {
    return protocols((String[])toArray(protocols, EmptyArrays.EMPTY_STRINGS));
  }
  


  public SslContextBuilder startTls(boolean startTls)
  {
    this.startTls = startTls;
    return this;
  }
  






  public SslContextBuilder enableOcsp(boolean enableOcsp)
  {
    this.enableOcsp = enableOcsp;
    return this;
  }
  



  public SslContext build()
    throws SSLException
  {
    if (forServer) {
      return SslContext.newServerContextInternal(provider, sslContextProvider, trustCertCollection, trustManagerFactory, keyCertChain, key, keyPassword, keyManagerFactory, ciphers, cipherFilter, apn, sessionCacheSize, sessionTimeout, clientAuth, protocols, startTls, enableOcsp, keyStoreType, 
      

        (Map.Entry[])toArray(options.entrySet(), EMPTY_ENTRIES));
    }
    return SslContext.newClientContextInternal(provider, sslContextProvider, trustCertCollection, trustManagerFactory, keyCertChain, key, keyPassword, keyManagerFactory, ciphers, cipherFilter, apn, protocols, sessionCacheSize, sessionTimeout, enableOcsp, keyStoreType, 
    

      (Map.Entry[])toArray(options.entrySet(), EMPTY_ENTRIES));
  }
  
  private static <T> T[] toArray(Iterable<? extends T> iterable, T[] prototype)
  {
    if (iterable == null) {
      return null;
    }
    List<T> list = new ArrayList();
    for (T element : iterable) {
      list.add(element);
    }
    return list.toArray(prototype);
  }
}
