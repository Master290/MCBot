package io.netty.handler.ssl;

import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.EmptyArrays;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSessionContext;
























public class JdkSslContext
  extends SslContext
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(JdkSslContext.class);
  static final String PROTOCOL = "TLS";
  private static final String[] DEFAULT_PROTOCOLS;
  private static final List<String> DEFAULT_CIPHERS;
  private static final List<String> DEFAULT_CIPHERS_NON_TLSV13;
  private static final Set<String> SUPPORTED_CIPHERS;
  private static final Set<String> SUPPORTED_CIPHERS_NON_TLSV13;
  private static final Provider DEFAULT_PROVIDER;
  
  static
  {
    try
    {
      SSLContext context = SSLContext.getInstance("TLS");
      context.init(null, null, null);
    } catch (Exception e) {
      throw new Error("failed to initialize the default SSL context", e);
    }
    SSLContext context;
    DEFAULT_PROVIDER = context.getProvider();
    
    SSLEngine engine = context.createSSLEngine();
    DEFAULT_PROTOCOLS = defaultProtocols(context, engine);
    
    SUPPORTED_CIPHERS = Collections.unmodifiableSet(supportedCiphers(engine));
    DEFAULT_CIPHERS = Collections.unmodifiableList(defaultCiphers(engine, SUPPORTED_CIPHERS));
    
    List<String> ciphersNonTLSv13 = new ArrayList(DEFAULT_CIPHERS);
    ciphersNonTLSv13.removeAll(Arrays.asList(SslUtils.DEFAULT_TLSV13_CIPHER_SUITES));
    DEFAULT_CIPHERS_NON_TLSV13 = Collections.unmodifiableList(ciphersNonTLSv13);
    
    Set<String> suppertedCiphersNonTLSv13 = new LinkedHashSet(SUPPORTED_CIPHERS);
    suppertedCiphersNonTLSv13.removeAll(Arrays.asList(SslUtils.DEFAULT_TLSV13_CIPHER_SUITES));
    SUPPORTED_CIPHERS_NON_TLSV13 = Collections.unmodifiableSet(suppertedCiphersNonTLSv13);
    
    if (logger.isDebugEnabled()) {
      logger.debug("Default protocols (JDK): {} ", Arrays.asList(DEFAULT_PROTOCOLS));
      logger.debug("Default cipher suites (JDK): {}", DEFAULT_CIPHERS);
    }
  }
  
  private static String[] defaultProtocols(SSLContext context, SSLEngine engine)
  {
    String[] supportedProtocols = context.getDefaultSSLParameters().getProtocols();
    Set<String> supportedProtocolsSet = new HashSet(supportedProtocols.length);
    Collections.addAll(supportedProtocolsSet, supportedProtocols);
    List<String> protocols = new ArrayList();
    SslUtils.addIfSupported(supportedProtocolsSet, protocols, new String[] { "TLSv1.3", "TLSv1.2", "TLSv1.1", "TLSv1" });
    



    if (!protocols.isEmpty()) {
      return (String[])protocols.toArray(EmptyArrays.EMPTY_STRINGS);
    }
    return engine.getEnabledProtocols();
  }
  
  private static Set<String> supportedCiphers(SSLEngine engine)
  {
    String[] supportedCiphers = engine.getSupportedCipherSuites();
    Set<String> supportedCiphersSet = new LinkedHashSet(supportedCiphers.length);
    for (int i = 0; i < supportedCiphers.length; i++) {
      String supportedCipher = supportedCiphers[i];
      supportedCiphersSet.add(supportedCipher);
      








      if (supportedCipher.startsWith("SSL_")) {
        String tlsPrefixedCipherName = "TLS_" + supportedCipher.substring("SSL_".length());
        try {
          engine.setEnabledCipherSuites(new String[] { tlsPrefixedCipherName });
          supportedCiphersSet.add(tlsPrefixedCipherName);
        }
        catch (IllegalArgumentException localIllegalArgumentException) {}
      }
    }
    
    return supportedCiphersSet;
  }
  
  private static List<String> defaultCiphers(SSLEngine engine, Set<String> supportedCiphers) {
    List<String> ciphers = new ArrayList();
    SslUtils.addIfSupported(supportedCiphers, ciphers, SslUtils.DEFAULT_CIPHER_SUITES);
    SslUtils.useFallbackCiphersIfDefaultIsEmpty(ciphers, engine.getEnabledCipherSuites());
    return ciphers;
  }
  
  private static boolean isTlsV13Supported(String[] protocols) {
    for (String protocol : protocols) {
      if ("TLSv1.3".equals(protocol)) {
        return true;
      }
    }
    return false;
  }
  


  private final String[] protocols;
  

  private final String[] cipherSuites;
  

  private final List<String> unmodifiableCipherSuites;
  
  private final JdkApplicationProtocolNegotiator apn;
  
  private final ClientAuth clientAuth;
  
  private final SSLContext sslContext;
  
  private final boolean isClient;
  
  @Deprecated
  public JdkSslContext(SSLContext sslContext, boolean isClient, ClientAuth clientAuth)
  {
    this(sslContext, isClient, null, IdentityCipherSuiteFilter.INSTANCE, JdkDefaultApplicationProtocolNegotiator.INSTANCE, clientAuth, null, false);
  }
  














  @Deprecated
  public JdkSslContext(SSLContext sslContext, boolean isClient, Iterable<String> ciphers, CipherSuiteFilter cipherFilter, ApplicationProtocolConfig apn, ClientAuth clientAuth)
  {
    this(sslContext, isClient, ciphers, cipherFilter, apn, clientAuth, null, false);
  }
  


















  public JdkSslContext(SSLContext sslContext, boolean isClient, Iterable<String> ciphers, CipherSuiteFilter cipherFilter, ApplicationProtocolConfig apn, ClientAuth clientAuth, String[] protocols, boolean startTls)
  {
    this(sslContext, isClient, ciphers, cipherFilter, 
    


      toNegotiator(apn, !isClient), clientAuth, protocols == null ? null : 
      
      (String[])protocols.clone(), startTls);
  }
  


  JdkSslContext(SSLContext sslContext, boolean isClient, Iterable<String> ciphers, CipherSuiteFilter cipherFilter, JdkApplicationProtocolNegotiator apn, ClientAuth clientAuth, String[] protocols, boolean startTls)
  {
    super(startTls);
    this.apn = ((JdkApplicationProtocolNegotiator)ObjectUtil.checkNotNull(apn, "apn"));
    this.clientAuth = ((ClientAuth)ObjectUtil.checkNotNull(clientAuth, "clientAuth"));
    this.sslContext = ((SSLContext)ObjectUtil.checkNotNull(sslContext, "sslContext"));
    
    List<String> defaultCiphers;
    
    if (DEFAULT_PROVIDER.equals(sslContext.getProvider())) {
      this.protocols = (protocols == null ? DEFAULT_PROTOCOLS : protocols);
      List<String> defaultCiphers; if (isTlsV13Supported(this.protocols)) {
        Set<String> supportedCiphers = SUPPORTED_CIPHERS;
        defaultCiphers = DEFAULT_CIPHERS;
      }
      else {
        Set<String> supportedCiphers = SUPPORTED_CIPHERS_NON_TLSV13;
        defaultCiphers = DEFAULT_CIPHERS_NON_TLSV13;
      }
      
    }
    else
    {
      SSLEngine engine = sslContext.createSSLEngine();
      try {
        if (protocols == null) {
          this.protocols = defaultProtocols(sslContext, engine);
        } else {
          this.protocols = protocols;
        }
        Set<String> supportedCiphers = supportedCiphers(engine);
        List<String> defaultCiphers = defaultCiphers(engine, supportedCiphers);
        if (!isTlsV13Supported(this.protocols))
        {
          for (String cipher : SslUtils.DEFAULT_TLSV13_CIPHER_SUITES) {
            supportedCiphers.remove(cipher);
            defaultCiphers.remove(cipher);
          }
        }
      } finally {
        ReferenceCountUtil.release(engine);
      } }
    Set<String> supportedCiphers;
    List<String> defaultCiphers;
    cipherSuites = ((CipherSuiteFilter)ObjectUtil.checkNotNull(cipherFilter, "cipherFilter")).filterCipherSuites(ciphers, defaultCiphers, supportedCiphers);
    

    unmodifiableCipherSuites = Collections.unmodifiableList(Arrays.asList(cipherSuites));
    this.isClient = isClient;
  }
  


  public final SSLContext context()
  {
    return sslContext;
  }
  
  public final boolean isClient()
  {
    return isClient;
  }
  



  public final SSLSessionContext sessionContext()
  {
    if (isServer()) {
      return context().getServerSessionContext();
    }
    return context().getClientSessionContext();
  }
  

  public final List<String> cipherSuites()
  {
    return unmodifiableCipherSuites;
  }
  
  public final SSLEngine newEngine(ByteBufAllocator alloc)
  {
    return configureAndWrapEngine(context().createSSLEngine(), alloc);
  }
  
  public final SSLEngine newEngine(ByteBufAllocator alloc, String peerHost, int peerPort)
  {
    return configureAndWrapEngine(context().createSSLEngine(peerHost, peerPort), alloc);
  }
  
  private SSLEngine configureAndWrapEngine(SSLEngine engine, ByteBufAllocator alloc)
  {
    engine.setEnabledCipherSuites(cipherSuites);
    engine.setEnabledProtocols(protocols);
    engine.setUseClientMode(isClient());
    if (isServer()) {
      switch (1.$SwitchMap$io$netty$handler$ssl$ClientAuth[clientAuth.ordinal()]) {
      case 1: 
        engine.setWantClientAuth(true);
        break;
      case 2: 
        engine.setNeedClientAuth(true);
        break;
      case 3: 
        break;
      default: 
        throw new Error("Unknown auth " + clientAuth);
      }
    }
    JdkApplicationProtocolNegotiator.SslEngineWrapperFactory factory = apn.wrapperFactory();
    if ((factory instanceof JdkApplicationProtocolNegotiator.AllocatorAwareSslEngineWrapperFactory)) {
      return 
        ((JdkApplicationProtocolNegotiator.AllocatorAwareSslEngineWrapperFactory)factory).wrapSslEngine(engine, alloc, apn, isServer());
    }
    return factory.wrapSslEngine(engine, apn, isServer());
  }
  
  public final JdkApplicationProtocolNegotiator applicationProtocolNegotiator()
  {
    return apn;
  }
  






  static JdkApplicationProtocolNegotiator toNegotiator(ApplicationProtocolConfig config, boolean isServer)
  {
    if (config == null) {
      return JdkDefaultApplicationProtocolNegotiator.INSTANCE;
    }
    
    switch (1.$SwitchMap$io$netty$handler$ssl$ApplicationProtocolConfig$Protocol[config.protocol().ordinal()]) {
    case 1: 
      return JdkDefaultApplicationProtocolNegotiator.INSTANCE;
    case 2: 
      if (isServer) {
        switch (config.selectorFailureBehavior()) {
        case FATAL_ALERT: 
          return new JdkAlpnApplicationProtocolNegotiator(true, config.supportedProtocols());
        case NO_ADVERTISE: 
          return new JdkAlpnApplicationProtocolNegotiator(false, config.supportedProtocols());
        }
        
        throw new UnsupportedOperationException("JDK provider does not support " + config.selectorFailureBehavior() + " failure behavior");
      }
      
      switch (config.selectedListenerFailureBehavior()) {
      case ACCEPT: 
        return new JdkAlpnApplicationProtocolNegotiator(false, config.supportedProtocols());
      case FATAL_ALERT: 
        return new JdkAlpnApplicationProtocolNegotiator(true, config.supportedProtocols());
      }
      
      throw new UnsupportedOperationException("JDK provider does not support " + config.selectedListenerFailureBehavior() + " failure behavior");
    

    case 3: 
      if (isServer) {
        switch (config.selectedListenerFailureBehavior()) {
        case ACCEPT: 
          return new JdkNpnApplicationProtocolNegotiator(false, config.supportedProtocols());
        case FATAL_ALERT: 
          return new JdkNpnApplicationProtocolNegotiator(true, config.supportedProtocols());
        }
        
        throw new UnsupportedOperationException("JDK provider does not support " + config.selectedListenerFailureBehavior() + " failure behavior");
      }
      
      switch (config.selectorFailureBehavior()) {
      case FATAL_ALERT: 
        return new JdkNpnApplicationProtocolNegotiator(true, config.supportedProtocols());
      case NO_ADVERTISE: 
        return new JdkNpnApplicationProtocolNegotiator(false, config.supportedProtocols());
      }
      
      throw new UnsupportedOperationException("JDK provider does not support " + config.selectorFailureBehavior() + " failure behavior");
    }
    
    

    throw new UnsupportedOperationException("JDK provider does not support " + config.protocol() + " protocol");
  }
  













  static KeyManagerFactory buildKeyManagerFactory(File certChainFile, File keyFile, String keyPassword, KeyManagerFactory kmf, String keyStore)
    throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, InvalidAlgorithmParameterException, CertificateException, KeyException, IOException
  {
    String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
    if (algorithm == null) {
      algorithm = "SunX509";
    }
    return buildKeyManagerFactory(certChainFile, algorithm, keyFile, keyPassword, kmf, keyStore);
  }
  












  @Deprecated
  protected static KeyManagerFactory buildKeyManagerFactory(File certChainFile, File keyFile, String keyPassword, KeyManagerFactory kmf)
    throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, InvalidAlgorithmParameterException, CertificateException, KeyException, IOException
  {
    return buildKeyManagerFactory(certChainFile, keyFile, keyPassword, kmf, KeyStore.getDefaultType());
  }
  

















  static KeyManagerFactory buildKeyManagerFactory(File certChainFile, String keyAlgorithm, File keyFile, String keyPassword, KeyManagerFactory kmf, String keyStore)
    throws KeyStoreException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, InvalidAlgorithmParameterException, IOException, CertificateException, KeyException, UnrecoverableKeyException
  {
    return buildKeyManagerFactory(toX509Certificates(certChainFile), keyAlgorithm, 
      toPrivateKey(keyFile, keyPassword), keyPassword, kmf, keyStore);
  }
  

















  @Deprecated
  protected static KeyManagerFactory buildKeyManagerFactory(File certChainFile, String keyAlgorithm, File keyFile, String keyPassword, KeyManagerFactory kmf)
    throws KeyStoreException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, InvalidAlgorithmParameterException, IOException, CertificateException, KeyException, UnrecoverableKeyException
  {
    return buildKeyManagerFactory(toX509Certificates(certChainFile), keyAlgorithm, 
      toPrivateKey(keyFile, keyPassword), keyPassword, kmf, KeyStore.getDefaultType());
  }
}
