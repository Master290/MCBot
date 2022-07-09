package io.netty.handler.ssl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.ssl.util.LazyX509Certificate;
import io.netty.internal.tcnative.AsyncSSLPrivateKeyMethod;
import io.netty.internal.tcnative.CertificateVerifier;
import io.netty.internal.tcnative.ResultCallback;
import io.netty.internal.tcnative.SSL;
import io.netty.internal.tcnative.SSLContext;
import io.netty.internal.tcnative.SSLPrivateKeyMethod;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ReferenceCounted;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetectorFactory;
import io.netty.util.ResourceLeakTracker;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.SuppressJava6Requirement;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertPathValidatorException.BasicReason;
import java.security.cert.CertPathValidatorException.Reason;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateRevokedException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;





























public abstract class ReferenceCountedOpenSslContext
  extends SslContext
  implements ReferenceCounted
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(ReferenceCountedOpenSslContext.class);
  
  private static final int DEFAULT_BIO_NON_APPLICATION_BUFFER_SIZE = Math.max(1, 
    SystemPropertyUtil.getInt("io.netty.handler.ssl.openssl.bioNonApplicationBufferSize", 2048));
  


  static final boolean USE_TASKS = SystemPropertyUtil.getBoolean("io.netty.handler.ssl.openssl.useTasks", true);
  
  private static final Integer DH_KEY_LENGTH;
  private static final ResourceLeakDetector<ReferenceCountedOpenSslContext> leakDetector = ResourceLeakDetectorFactory.instance().newResourceLeakDetector(ReferenceCountedOpenSslContext.class);
  

  protected static final int VERIFY_DEPTH = 10;
  

  static final boolean CLIENT_ENABLE_SESSION_TICKET = SystemPropertyUtil.getBoolean("jdk.tls.client.enableSessionTicketExtension", false);
  

  static final boolean CLIENT_ENABLE_SESSION_TICKET_TLSV13 = SystemPropertyUtil.getBoolean("jdk.tls.client.enableSessionTicketExtension", true);
  

  static final boolean SERVER_ENABLE_SESSION_TICKET = SystemPropertyUtil.getBoolean("jdk.tls.server.enableSessionTicketExtension", false);
  

  static final boolean SERVER_ENABLE_SESSION_TICKET_TLSV13 = SystemPropertyUtil.getBoolean("jdk.tls.server.enableSessionTicketExtension", true);
  

  static final boolean SERVER_ENABLE_SESSION_CACHE = SystemPropertyUtil.getBoolean("io.netty.handler.ssl.openssl.sessionCacheServer", true);
  


  static final boolean CLIENT_ENABLE_SESSION_CACHE = SystemPropertyUtil.getBoolean("io.netty.handler.ssl.openssl.sessionCacheClient", false);
  

  protected long ctx;
  
  private final List<String> unmodifiableCiphers;
  
  private final OpenSslApplicationProtocolNegotiator apn;
  
  private final int mode;
  
  private final ResourceLeakTracker<ReferenceCountedOpenSslContext> leak;
  
  private final AbstractReferenceCounted refCnt = new AbstractReferenceCounted()
  {
    public ReferenceCounted touch(Object hint) {
      if (leak != null) {
        leak.record(hint);
      }
      
      return ReferenceCountedOpenSslContext.this;
    }
    
    protected void deallocate()
    {
      ReferenceCountedOpenSslContext.this.destroy();
      if (leak != null) {
        boolean closed = leak.close(ReferenceCountedOpenSslContext.this);
        assert (closed);
      }
    }
  };
  
  final Certificate[] keyCertChain;
  final ClientAuth clientAuth;
  final String[] protocols;
  final boolean enableOcsp;
  final OpenSslEngineMap engineMap = new DefaultOpenSslEngineMap(null);
  final ReadWriteLock ctxLock = new ReentrantReadWriteLock();
  
  private volatile int bioNonApplicationBufferSize = DEFAULT_BIO_NON_APPLICATION_BUFFER_SIZE;
  

  static final OpenSslApplicationProtocolNegotiator NONE_PROTOCOL_NEGOTIATOR = new OpenSslApplicationProtocolNegotiator()
  {
    public ApplicationProtocolConfig.Protocol protocol()
    {
      return ApplicationProtocolConfig.Protocol.NONE;
    }
    
    public List<String> protocols()
    {
      return Collections.emptyList();
    }
    
    public ApplicationProtocolConfig.SelectorFailureBehavior selectorFailureBehavior()
    {
      return ApplicationProtocolConfig.SelectorFailureBehavior.CHOOSE_MY_LAST_PROTOCOL;
    }
    


    public ApplicationProtocolConfig.SelectedListenerFailureBehavior selectedListenerFailureBehavior() { return ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT; }
  };
  final boolean tlsFalseStart;
  
  static {
    Integer dhLen = null;
    try
    {
      String dhKeySize = SystemPropertyUtil.get("jdk.tls.ephemeralDHKeySize");
      if (dhKeySize != null) {
        try {
          dhLen = Integer.valueOf(dhKeySize);
        } catch (NumberFormatException e) {
          logger.debug("ReferenceCountedOpenSslContext supports -Djdk.tls.ephemeralDHKeySize={int}, but got: " + dhKeySize);
        }
      }
    }
    catch (Throwable localThrowable) {}
    

    DH_KEY_LENGTH = dhLen;
  }
  




  ReferenceCountedOpenSslContext(Iterable<String> ciphers, CipherSuiteFilter cipherFilter, OpenSslApplicationProtocolNegotiator apn, int mode, Certificate[] keyCertChain, ClientAuth clientAuth, String[] protocols, boolean startTls, boolean enableOcsp, boolean leakDetection, Map.Entry<SslContextOption<?>, Object>... ctxOptions)
    throws SSLException
  {
    super(startTls);
    
    OpenSsl.ensureAvailability();
    
    if ((enableOcsp) && (!OpenSsl.isOcspSupported())) {
      throw new IllegalStateException("OCSP is not supported.");
    }
    
    if ((mode != 1) && (mode != 0)) {
      throw new IllegalArgumentException("mode most be either SSL.SSL_MODE_SERVER or SSL.SSL_MODE_CLIENT");
    }
    
    boolean tlsFalseStart = false;
    boolean useTasks = USE_TASKS;
    OpenSslPrivateKeyMethod privateKeyMethod = null;
    OpenSslAsyncPrivateKeyMethod asyncPrivateKeyMethod = null;
    
    if (ctxOptions != null) {
      for (Map.Entry<SslContextOption<?>, Object> ctxOpt : ctxOptions) {
        SslContextOption<?> option = (SslContextOption)ctxOpt.getKey();
        
        if (option == OpenSslContextOption.TLS_FALSE_START) {
          tlsFalseStart = ((Boolean)ctxOpt.getValue()).booleanValue();
        } else if (option == OpenSslContextOption.USE_TASKS) {
          useTasks = ((Boolean)ctxOpt.getValue()).booleanValue();
        } else if (option == OpenSslContextOption.PRIVATE_KEY_METHOD) {
          privateKeyMethod = (OpenSslPrivateKeyMethod)ctxOpt.getValue();
        } else if (option == OpenSslContextOption.ASYNC_PRIVATE_KEY_METHOD) {
          asyncPrivateKeyMethod = (OpenSslAsyncPrivateKeyMethod)ctxOpt.getValue();
        } else {
          logger.debug("Skipping unsupported " + SslContextOption.class.getSimpleName() + ": " + ctxOpt
            .getKey());
        }
      }
    }
    if ((privateKeyMethod != null) && (asyncPrivateKeyMethod != null))
    {

      throw new IllegalArgumentException("You can either only use " + OpenSslAsyncPrivateKeyMethod.class.getSimpleName() + " or " + OpenSslPrivateKeyMethod.class.getSimpleName());
    }
    
    this.tlsFalseStart = tlsFalseStart;
    
    leak = (leakDetection ? leakDetector.track(this) : null);
    this.mode = mode;
    this.clientAuth = (isServer() ? (ClientAuth)ObjectUtil.checkNotNull(clientAuth, "clientAuth") : ClientAuth.NONE);
    this.protocols = protocols;
    this.enableOcsp = enableOcsp;
    
    this.keyCertChain = (keyCertChain == null ? null : (Certificate[])keyCertChain.clone());
    
    unmodifiableCiphers = Arrays.asList(((CipherSuiteFilter)ObjectUtil.checkNotNull(cipherFilter, "cipherFilter")).filterCipherSuites(ciphers, OpenSsl.DEFAULT_CIPHERS, 
      OpenSsl.availableJavaCipherSuites()));
    
    this.apn = ((OpenSslApplicationProtocolNegotiator)ObjectUtil.checkNotNull(apn, "apn"));
    

    boolean success = false;
    try {
      boolean tlsv13Supported = OpenSsl.isTlsv13Supported();
      try
      {
        int protocolOpts = 30;
        
        if (tlsv13Supported) {
          protocolOpts |= 0x20;
        }
        ctx = SSLContext.make(protocolOpts, mode);
      } catch (Exception e) {
        throw new SSLException("failed to create an SSL_CTX", e);
      }
      
      StringBuilder cipherBuilder = new StringBuilder();
      StringBuilder cipherTLSv13Builder = new StringBuilder();
      
      try
      {
        if (unmodifiableCiphers.isEmpty())
        {
          SSLContext.setCipherSuite(ctx, "", false);
          if (tlsv13Supported)
          {
            SSLContext.setCipherSuite(ctx, "", true);
          }
        } else {
          CipherSuiteConverter.convertToCipherStrings(unmodifiableCiphers, cipherBuilder, cipherTLSv13Builder, 
            OpenSsl.isBoringSSL());
          

          SSLContext.setCipherSuite(ctx, cipherBuilder.toString(), false);
          if (tlsv13Supported)
          {
            SSLContext.setCipherSuite(ctx, 
              OpenSsl.checkTls13Ciphers(logger, cipherTLSv13Builder.toString()), true);
          }
        }
      } catch (SSLException e) {
        throw e;
      } catch (Exception e) {
        throw new SSLException("failed to set cipher suite: " + unmodifiableCiphers, e);
      }
      
      int options = SSLContext.getOptions(ctx) | SSL.SSL_OP_NO_SSLv2 | SSL.SSL_OP_NO_SSLv3 | SSL.SSL_OP_NO_TLSv1 | SSL.SSL_OP_NO_TLSv1_1 | SSL.SSL_OP_CIPHER_SERVER_PREFERENCE | SSL.SSL_OP_NO_COMPRESSION | SSL.SSL_OP_NO_TICKET;
      


















      if (cipherBuilder.length() == 0)
      {
        options |= SSL.SSL_OP_NO_SSLv2 | SSL.SSL_OP_NO_SSLv3 | SSL.SSL_OP_NO_TLSv1 | SSL.SSL_OP_NO_TLSv1_1 | SSL.SSL_OP_NO_TLSv1_2;
      }
      

      SSLContext.setOptions(ctx, options);
      



      SSLContext.setMode(ctx, SSLContext.getMode(ctx) | SSL.SSL_MODE_ACCEPT_MOVING_WRITE_BUFFER);
      
      if (DH_KEY_LENGTH != null) {
        SSLContext.setTmpDHLength(ctx, DH_KEY_LENGTH.intValue());
      }
      
      List<String> nextProtoList = apn.protocols();
      
      if (!nextProtoList.isEmpty()) {
        String[] appProtocols = (String[])nextProtoList.toArray(new String[0]);
        int selectorBehavior = opensslSelectorFailureBehavior(apn.selectorFailureBehavior());
        
        switch (3.$SwitchMap$io$netty$handler$ssl$ApplicationProtocolConfig$Protocol[apn.protocol().ordinal()]) {
        case 1: 
          SSLContext.setNpnProtos(ctx, appProtocols, selectorBehavior);
          break;
        case 2: 
          SSLContext.setAlpnProtos(ctx, appProtocols, selectorBehavior);
          break;
        case 3: 
          SSLContext.setNpnProtos(ctx, appProtocols, selectorBehavior);
          SSLContext.setAlpnProtos(ctx, appProtocols, selectorBehavior);
          break;
        default: 
          throw new Error();
        }
        
      }
      if (enableOcsp) {
        SSLContext.enableOcsp(ctx, isClient());
      }
      
      SSLContext.setUseTasks(ctx, useTasks);
      if (privateKeyMethod != null) {
        SSLContext.setPrivateKeyMethod(ctx, new PrivateKeyMethod(engineMap, privateKeyMethod));
      }
      if (asyncPrivateKeyMethod != null) {
        SSLContext.setPrivateKeyMethod(ctx, new AsyncPrivateKeyMethod(engineMap, asyncPrivateKeyMethod));
      }
      success = true;
    } finally {
      if (!success) {
        release();
      }
    }
  }
  
  private static int opensslSelectorFailureBehavior(ApplicationProtocolConfig.SelectorFailureBehavior behavior) {
    switch (behavior) {
    case NO_ADVERTISE: 
      return 0;
    case CHOOSE_MY_LAST_PROTOCOL: 
      return 1;
    }
    throw new Error();
  }
  

  public final List<String> cipherSuites()
  {
    return unmodifiableCiphers;
  }
  
  public ApplicationProtocolNegotiator applicationProtocolNegotiator()
  {
    return apn;
  }
  
  public final boolean isClient()
  {
    return mode == 0;
  }
  
  public final SSLEngine newEngine(ByteBufAllocator alloc, String peerHost, int peerPort)
  {
    return newEngine0(alloc, peerHost, peerPort, true);
  }
  
  protected final SslHandler newHandler(ByteBufAllocator alloc, boolean startTls)
  {
    return new SslHandler(newEngine0(alloc, null, -1, false), startTls);
  }
  
  protected final SslHandler newHandler(ByteBufAllocator alloc, String peerHost, int peerPort, boolean startTls)
  {
    return new SslHandler(newEngine0(alloc, peerHost, peerPort, false), startTls);
  }
  
  protected SslHandler newHandler(ByteBufAllocator alloc, boolean startTls, Executor executor)
  {
    return new SslHandler(newEngine0(alloc, null, -1, false), startTls, executor);
  }
  

  protected SslHandler newHandler(ByteBufAllocator alloc, String peerHost, int peerPort, boolean startTls, Executor executor)
  {
    return new SslHandler(newEngine0(alloc, peerHost, peerPort, false), executor);
  }
  
  SSLEngine newEngine0(ByteBufAllocator alloc, String peerHost, int peerPort, boolean jdkCompatibilityMode) {
    return new ReferenceCountedOpenSslEngine(this, alloc, peerHost, peerPort, jdkCompatibilityMode, true);
  }
  



  public final SSLEngine newEngine(ByteBufAllocator alloc)
  {
    return newEngine(alloc, null, -1);
  }
  






  @Deprecated
  public final long context()
  {
    return sslCtxPointer();
  }
  




  @Deprecated
  public final OpenSslSessionStats stats()
  {
    return sessionContext().stats();
  }
  




  @Deprecated
  public void setRejectRemoteInitiatedRenegotiation(boolean rejectRemoteInitiatedRenegotiation)
  {
    if (!rejectRemoteInitiatedRenegotiation) {
      throw new UnsupportedOperationException("Renegotiation is not supported");
    }
  }
  



  @Deprecated
  public boolean getRejectRemoteInitiatedRenegotiation()
  {
    return true;
  }
  




  public void setBioNonApplicationBufferSize(int bioNonApplicationBufferSize)
  {
    this.bioNonApplicationBufferSize = ObjectUtil.checkPositiveOrZero(bioNonApplicationBufferSize, "bioNonApplicationBufferSize");
  }
  


  public int getBioNonApplicationBufferSize()
  {
    return bioNonApplicationBufferSize;
  }
  




  @Deprecated
  public final void setTicketKeys(byte[] keys)
  {
    sessionContext().setTicketKeys(keys);
  }
  









  @Deprecated
  public final long sslCtxPointer()
  {
    Lock readerLock = ctxLock.readLock();
    readerLock.lock();
    try {
      return SSLContext.getSslCtx(ctx);
    } finally {
      readerLock.unlock();
    }
  }
  










  @Deprecated
  public final void setPrivateKeyMethod(OpenSslPrivateKeyMethod method)
  {
    ObjectUtil.checkNotNull(method, "method");
    Lock writerLock = ctxLock.writeLock();
    writerLock.lock();
    try {
      SSLContext.setPrivateKeyMethod(ctx, new PrivateKeyMethod(engineMap, method));
      
      writerLock.unlock(); } finally { writerLock.unlock();
    }
  }
  



  @Deprecated
  public final void setUseTasks(boolean useTasks)
  {
    Lock writerLock = ctxLock.writeLock();
    writerLock.lock();
    try {
      SSLContext.setUseTasks(ctx, useTasks);
      
      writerLock.unlock(); } finally { writerLock.unlock();
    }
  }
  


  private void destroy()
  {
    Lock writerLock = ctxLock.writeLock();
    writerLock.lock();
    try {
      if (ctx != 0L) {
        if (enableOcsp) {
          SSLContext.disableOcsp(ctx);
        }
        
        SSLContext.free(ctx);
        ctx = 0L;
        
        OpenSslSessionContext context = sessionContext();
        if (context != null) {
          context.destroy();
        }
      }
      
      writerLock.unlock(); } finally { writerLock.unlock();
    }
  }
  
  protected static X509Certificate[] certificates(byte[][] chain) {
    X509Certificate[] peerCerts = new X509Certificate[chain.length];
    for (int i = 0; i < peerCerts.length; i++) {
      peerCerts[i] = new LazyX509Certificate(chain[i]);
    }
    return peerCerts;
  }
  
  protected static X509TrustManager chooseTrustManager(TrustManager[] managers) {
    for (TrustManager m : managers) {
      if ((m instanceof X509TrustManager)) {
        if (PlatformDependent.javaVersion() >= 7) {
          return OpenSslX509TrustManagerWrapper.wrapIfNeeded((X509TrustManager)m);
        }
        return (X509TrustManager)m;
      }
    }
    throw new IllegalStateException("no X509TrustManager found");
  }
  
  protected static X509KeyManager chooseX509KeyManager(KeyManager[] kms) {
    for (KeyManager km : kms) {
      if ((km instanceof X509KeyManager)) {
        return (X509KeyManager)km;
      }
    }
    throw new IllegalStateException("no X509KeyManager found");
  }
  







  static OpenSslApplicationProtocolNegotiator toNegotiator(ApplicationProtocolConfig config)
  {
    if (config == null) {
      return NONE_PROTOCOL_NEGOTIATOR;
    }
    
    switch (3.$SwitchMap$io$netty$handler$ssl$ApplicationProtocolConfig$Protocol[config.protocol().ordinal()]) {
    case 4: 
      return NONE_PROTOCOL_NEGOTIATOR;
    case 1: 
    case 2: 
    case 3: 
      switch (config.selectedListenerFailureBehavior()) {
      case CHOOSE_MY_LAST_PROTOCOL: 
      case ACCEPT: 
        switch (config.selectorFailureBehavior()) {
        case NO_ADVERTISE: 
        case CHOOSE_MY_LAST_PROTOCOL: 
          return new OpenSslDefaultApplicationProtocolNegotiator(config);
        }
        
        


        throw new UnsupportedOperationException("OpenSSL provider does not support " + config.selectorFailureBehavior() + " behavior");
      }
      
      


      throw new UnsupportedOperationException("OpenSSL provider does not support " + config.selectedListenerFailureBehavior() + " behavior");
    }
    
    throw new Error();
  }
  
  @SuppressJava6Requirement(reason="Guarded by java version check")
  static boolean useExtendedTrustManager(X509TrustManager trustManager)
  {
    return (PlatformDependent.javaVersion() >= 7) && ((trustManager instanceof X509ExtendedTrustManager));
  }
  
  public final int refCnt()
  {
    return refCnt.refCnt();
  }
  
  public final ReferenceCounted retain()
  {
    refCnt.retain();
    return this;
  }
  
  public final ReferenceCounted retain(int increment)
  {
    refCnt.retain(increment);
    return this;
  }
  
  public final ReferenceCounted touch()
  {
    refCnt.touch();
    return this;
  }
  
  public final ReferenceCounted touch(Object hint)
  {
    refCnt.touch(hint);
    return this;
  }
  
  public final boolean release()
  {
    return refCnt.release();
  }
  
  public final boolean release(int decrement)
  {
    return refCnt.release(decrement);
  }
  
  static abstract class AbstractCertificateVerifier extends CertificateVerifier {
    private final OpenSslEngineMap engineMap;
    
    AbstractCertificateVerifier(OpenSslEngineMap engineMap) {
      this.engineMap = engineMap;
    }
    
    public final int verify(long ssl, byte[][] chain, String auth)
    {
      ReferenceCountedOpenSslEngine engine = engineMap.get(ssl);
      if (engine == null)
      {
        return CertificateVerifier.X509_V_ERR_UNSPECIFIED;
      }
      X509Certificate[] peerCerts = ReferenceCountedOpenSslContext.certificates(chain);
      try {
        verify(engine, peerCerts, auth);
        return CertificateVerifier.X509_V_OK;
      } catch (Throwable cause) {
        ReferenceCountedOpenSslContext.logger.debug("verification of certificate failed", cause);
        engine.initHandshakeException(cause);
        

        if ((cause instanceof OpenSslCertificateException))
        {

          return ((OpenSslCertificateException)cause).errorCode();
        }
        if ((cause instanceof CertificateExpiredException)) {
          return CertificateVerifier.X509_V_ERR_CERT_HAS_EXPIRED;
        }
        if ((cause instanceof CertificateNotYetValidException)) {
          return CertificateVerifier.X509_V_ERR_CERT_NOT_YET_VALID;
        }
        if (PlatformDependent.javaVersion() >= 7) {
          return translateToError(cause);
        }
      }
      
      return CertificateVerifier.X509_V_ERR_UNSPECIFIED;
    }
    
    @SuppressJava6Requirement(reason="Usage guarded by java version check")
    private static int translateToError(Throwable cause)
    {
      if ((cause instanceof CertificateRevokedException)) {
        return CertificateVerifier.X509_V_ERR_CERT_REVOKED;
      }
      



      Throwable wrapped = cause.getCause();
      while (wrapped != null) {
        if ((wrapped instanceof CertPathValidatorException)) {
          CertPathValidatorException ex = (CertPathValidatorException)wrapped;
          CertPathValidatorException.Reason reason = ex.getReason();
          if (reason == CertPathValidatorException.BasicReason.EXPIRED) {
            return CertificateVerifier.X509_V_ERR_CERT_HAS_EXPIRED;
          }
          if (reason == CertPathValidatorException.BasicReason.NOT_YET_VALID) {
            return CertificateVerifier.X509_V_ERR_CERT_NOT_YET_VALID;
          }
          if (reason == CertPathValidatorException.BasicReason.REVOKED) {
            return CertificateVerifier.X509_V_ERR_CERT_REVOKED;
          }
        }
        wrapped = wrapped.getCause();
      }
      return CertificateVerifier.X509_V_ERR_UNSPECIFIED;
    }
    
    abstract void verify(ReferenceCountedOpenSslEngine paramReferenceCountedOpenSslEngine, X509Certificate[] paramArrayOfX509Certificate, String paramString) throws Exception;
  }
  
  private static final class DefaultOpenSslEngineMap implements OpenSslEngineMap
  {
    private final Map<Long, ReferenceCountedOpenSslEngine> engines = PlatformDependent.newConcurrentHashMap();
    
    private DefaultOpenSslEngineMap() {}
    
    public ReferenceCountedOpenSslEngine remove(long ssl) { return (ReferenceCountedOpenSslEngine)engines.remove(Long.valueOf(ssl)); }
    

    public void add(ReferenceCountedOpenSslEngine engine)
    {
      engines.put(Long.valueOf(engine.sslPointer()), engine);
    }
    
    public ReferenceCountedOpenSslEngine get(long ssl)
    {
      return (ReferenceCountedOpenSslEngine)engines.get(Long.valueOf(ssl));
    }
  }
  
  static void setKeyMaterial(long ctx, X509Certificate[] keyCertChain, PrivateKey key, String keyPassword)
    throws SSLException
  {
    long keyBio = 0L;
    long keyCertChainBio = 0L;
    long keyCertChainBio2 = 0L;
    PemEncoded encoded = null;
    try
    {
      encoded = PemX509Certificate.toPEM(ByteBufAllocator.DEFAULT, true, keyCertChain);
      keyCertChainBio = toBIO(ByteBufAllocator.DEFAULT, encoded.retain());
      keyCertChainBio2 = toBIO(ByteBufAllocator.DEFAULT, encoded.retain());
      
      if (key != null) {
        keyBio = toBIO(ByteBufAllocator.DEFAULT, key);
      }
      
      SSLContext.setCertificateBio(ctx, keyCertChainBio, keyBio, keyPassword == null ? "" : keyPassword);
      


      SSLContext.setCertificateChainBio(ctx, keyCertChainBio2, true);
    } catch (SSLException e) {
      throw e;
    } catch (Exception e) {
      throw new SSLException("failed to set certificate and key", e);
    } finally {
      freeBio(keyBio);
      freeBio(keyCertChainBio);
      freeBio(keyCertChainBio2);
      if (encoded != null) {
        encoded.release();
      }
    }
  }
  
  static void freeBio(long bio) {
    if (bio != 0L) {
      SSL.freeBIO(bio);
    }
  }
  


  static long toBIO(ByteBufAllocator allocator, PrivateKey key)
    throws Exception
  {
    if (key == null) {
      return 0L;
    }
    
    PemEncoded pem = PemPrivateKey.toPEM(allocator, true, key);
    try {
      return toBIO(allocator, pem.retain());
    } finally {
      pem.release();
    }
  }
  


  static long toBIO(ByteBufAllocator allocator, X509Certificate... certChain)
    throws Exception
  {
    if (certChain == null) {
      return 0L;
    }
    
    ObjectUtil.checkNonEmpty(certChain, "certChain");
    
    PemEncoded pem = PemX509Certificate.toPEM(allocator, true, certChain);
    try {
      return toBIO(allocator, pem.retain());
    } finally {
      pem.release();
    }
  }
  



























  private static long newBIO(ByteBuf buffer)
    throws Exception
  {
    try
    {
      long bio = SSL.newMemBIO();
      int readable = buffer.readableBytes();
      if (SSL.bioWrite(bio, OpenSsl.memoryAddress(buffer) + buffer.readerIndex(), readable) != readable) {
        SSL.freeBIO(bio);
        throw new IllegalStateException("Could not write data to memory BIO");
      }
      return bio;
    } finally {
      buffer.release();
    }
  }
  




  static OpenSslKeyMaterialProvider providerFor(KeyManagerFactory factory, String password)
  {
    if ((factory instanceof OpenSslX509KeyManagerFactory)) {
      return ((OpenSslX509KeyManagerFactory)factory).newProvider();
    }
    
    if ((factory instanceof OpenSslCachingX509KeyManagerFactory))
    {
      return ((OpenSslCachingX509KeyManagerFactory)factory).newProvider(password);
    }
    
    return new OpenSslKeyMaterialProvider(chooseX509KeyManager(factory.getKeyManagers()), password);
  }
  
  private static final class PrivateKeyMethod implements SSLPrivateKeyMethod {
    private final OpenSslEngineMap engineMap;
    private final OpenSslPrivateKeyMethod keyMethod;
    
    PrivateKeyMethod(OpenSslEngineMap engineMap, OpenSslPrivateKeyMethod keyMethod) {
      this.engineMap = engineMap;
      this.keyMethod = keyMethod;
    }
    
    private ReferenceCountedOpenSslEngine retrieveEngine(long ssl) throws SSLException {
      ReferenceCountedOpenSslEngine engine = engineMap.get(ssl);
      if (engine == null)
      {
        throw new SSLException("Could not find a " + StringUtil.simpleClassName(ReferenceCountedOpenSslEngine.class) + " for sslPointer " + ssl);
      }
      return engine;
    }
    
    public byte[] sign(long ssl, int signatureAlgorithm, byte[] digest) throws Exception
    {
      ReferenceCountedOpenSslEngine engine = retrieveEngine(ssl);
      try {
        return ReferenceCountedOpenSslContext.verifyResult(keyMethod.sign(engine, signatureAlgorithm, digest));
      } catch (Exception e) {
        engine.initHandshakeException(e);
        throw e;
      }
    }
    
    public byte[] decrypt(long ssl, byte[] input) throws Exception
    {
      ReferenceCountedOpenSslEngine engine = retrieveEngine(ssl);
      try {
        return ReferenceCountedOpenSslContext.verifyResult(keyMethod.decrypt(engine, input));
      } catch (Exception e) {
        engine.initHandshakeException(e);
        throw e;
      }
    }
  }
  
  private static final class AsyncPrivateKeyMethod implements AsyncSSLPrivateKeyMethod
  {
    private final OpenSslEngineMap engineMap;
    private final OpenSslAsyncPrivateKeyMethod keyMethod;
    
    AsyncPrivateKeyMethod(OpenSslEngineMap engineMap, OpenSslAsyncPrivateKeyMethod keyMethod) {
      this.engineMap = engineMap;
      this.keyMethod = keyMethod;
    }
    
    private ReferenceCountedOpenSslEngine retrieveEngine(long ssl) throws SSLException {
      ReferenceCountedOpenSslEngine engine = engineMap.get(ssl);
      if (engine == null)
      {
        throw new SSLException("Could not find a " + StringUtil.simpleClassName(ReferenceCountedOpenSslEngine.class) + " for sslPointer " + ssl);
      }
      return engine;
    }
    
    public void sign(long ssl, int signatureAlgorithm, byte[] bytes, ResultCallback<byte[]> resultCallback)
    {
      try {
        ReferenceCountedOpenSslEngine engine = retrieveEngine(ssl);
        keyMethod.sign(engine, signatureAlgorithm, bytes)
          .addListener(new ResultCallbackListener(engine, ssl, resultCallback));
      } catch (SSLException e) {
        resultCallback.onError(ssl, e);
      }
    }
    
    public void decrypt(long ssl, byte[] bytes, ResultCallback<byte[]> resultCallback)
    {
      try {
        ReferenceCountedOpenSslEngine engine = retrieveEngine(ssl);
        keyMethod.decrypt(engine, bytes)
          .addListener(new ResultCallbackListener(engine, ssl, resultCallback));
      } catch (SSLException e) {
        resultCallback.onError(ssl, e);
      }
    }
    
    private static final class ResultCallbackListener implements FutureListener<byte[]>
    {
      private final ReferenceCountedOpenSslEngine engine;
      private final long ssl;
      private final ResultCallback<byte[]> resultCallback;
      
      ResultCallbackListener(ReferenceCountedOpenSslEngine engine, long ssl, ResultCallback<byte[]> resultCallback) {
        this.engine = engine;
        this.ssl = ssl;
        this.resultCallback = resultCallback;
      }
      
      public void operationComplete(Future<byte[]> future)
      {
        Throwable cause = future.cause();
        if (cause == null) {
          try {
            byte[] result = ReferenceCountedOpenSslContext.verifyResult((byte[])future.getNow());
            resultCallback.onSuccess(ssl, result);
            return;
          } catch (SignatureException e) {
            cause = e;
            engine.initHandshakeException(e);
          }
        }
        resultCallback.onError(ssl, cause);
      }
    }
  }
  
  private static byte[] verifyResult(byte[] result) throws SignatureException {
    if (result == null) {
      throw new SignatureException();
    }
    return result;
  }
  
  public abstract OpenSslSessionContext sessionContext();
  
  /* Error */
  static long toBIO(ByteBufAllocator allocator, PemEncoded pem)
    throws Exception
  {
    // Byte code:
    //   0: aload_1
    //   1: invokeinterface 199 1 0
    //   6: astore_2
    //   7: aload_2
    //   8: invokevirtual 200	io/netty/buffer/ByteBuf:isDirect	()Z
    //   11: ifeq +20 -> 31
    //   14: aload_2
    //   15: invokevirtual 201	io/netty/buffer/ByteBuf:retainedSlice	()Lio/netty/buffer/ByteBuf;
    //   18: invokestatic 202	io/netty/handler/ssl/ReferenceCountedOpenSslContext:newBIO	(Lio/netty/buffer/ByteBuf;)J
    //   21: lstore_3
    //   22: aload_1
    //   23: invokeinterface 193 1 0
    //   28: pop
    //   29: lload_3
    //   30: lreturn
    //   31: aload_0
    //   32: aload_2
    //   33: invokevirtual 203	io/netty/buffer/ByteBuf:readableBytes	()I
    //   36: invokeinterface 204 2 0
    //   41: astore_3
    //   42: aload_3
    //   43: aload_2
    //   44: aload_2
    //   45: invokevirtual 205	io/netty/buffer/ByteBuf:readerIndex	()I
    //   48: aload_2
    //   49: invokevirtual 203	io/netty/buffer/ByteBuf:readableBytes	()I
    //   52: invokevirtual 206	io/netty/buffer/ByteBuf:writeBytes	(Lio/netty/buffer/ByteBuf;II)Lio/netty/buffer/ByteBuf;
    //   55: pop
    //   56: aload_3
    //   57: invokevirtual 201	io/netty/buffer/ByteBuf:retainedSlice	()Lio/netty/buffer/ByteBuf;
    //   60: invokestatic 202	io/netty/handler/ssl/ReferenceCountedOpenSslContext:newBIO	(Lio/netty/buffer/ByteBuf;)J
    //   63: lstore 4
    //   65: aload_1
    //   66: invokeinterface 207 1 0
    //   71: ifeq +7 -> 78
    //   74: aload_3
    //   75: invokestatic 208	io/netty/handler/ssl/SslUtils:zeroout	(Lio/netty/buffer/ByteBuf;)V
    //   78: aload_3
    //   79: invokevirtual 209	io/netty/buffer/ByteBuf:release	()Z
    //   82: pop
    //   83: goto +13 -> 96
    //   86: astore 6
    //   88: aload_3
    //   89: invokevirtual 209	io/netty/buffer/ByteBuf:release	()Z
    //   92: pop
    //   93: aload 6
    //   95: athrow
    //   96: aload_1
    //   97: invokeinterface 193 1 0
    //   102: pop
    //   103: lload 4
    //   105: lreturn
    //   106: astore 7
    //   108: aload_1
    //   109: invokeinterface 207 1 0
    //   114: ifeq +7 -> 121
    //   117: aload_3
    //   118: invokestatic 208	io/netty/handler/ssl/SslUtils:zeroout	(Lio/netty/buffer/ByteBuf;)V
    //   121: aload_3
    //   122: invokevirtual 209	io/netty/buffer/ByteBuf:release	()Z
    //   125: pop
    //   126: goto +13 -> 139
    //   129: astore 8
    //   131: aload_3
    //   132: invokevirtual 209	io/netty/buffer/ByteBuf:release	()Z
    //   135: pop
    //   136: aload 8
    //   138: athrow
    //   139: aload 7
    //   141: athrow
    //   142: astore 9
    //   144: aload_1
    //   145: invokeinterface 193 1 0
    //   150: pop
    //   151: aload 9
    //   153: athrow
    // Line number table:
    //   Java source line #901	-> byte code offset #0
    //   Java source line #903	-> byte code offset #7
    //   Java source line #904	-> byte code offset #14
    //   Java source line #923	-> byte code offset #22
    //   Java source line #904	-> byte code offset #29
    //   Java source line #907	-> byte code offset #31
    //   Java source line #909	-> byte code offset #42
    //   Java source line #910	-> byte code offset #56
    //   Java source line #915	-> byte code offset #65
    //   Java source line #916	-> byte code offset #74
    //   Java source line #919	-> byte code offset #78
    //   Java source line #920	-> byte code offset #83
    //   Java source line #919	-> byte code offset #86
    //   Java source line #920	-> byte code offset #93
    //   Java source line #923	-> byte code offset #96
    //   Java source line #910	-> byte code offset #103
    //   Java source line #912	-> byte code offset #106
    //   Java source line #915	-> byte code offset #108
    //   Java source line #916	-> byte code offset #117
    //   Java source line #919	-> byte code offset #121
    //   Java source line #920	-> byte code offset #126
    //   Java source line #919	-> byte code offset #129
    //   Java source line #920	-> byte code offset #136
    //   Java source line #921	-> byte code offset #139
    //   Java source line #923	-> byte code offset #142
    //   Java source line #924	-> byte code offset #151
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	154	0	allocator	ByteBufAllocator
    //   0	154	1	pem	PemEncoded
    //   6	43	2	content	ByteBuf
    //   21	9	3	l1	long
    //   41	91	3	buffer	ByteBuf
    //   63	41	4	l2	long
    //   86	8	6	localObject1	Object
    //   106	34	7	localObject2	Object
    //   129	8	8	localObject3	Object
    //   142	10	9	localObject4	Object
    // Exception table:
    //   from	to	target	type
    //   65	78	86	finally
    //   86	88	86	finally
    //   42	65	106	finally
    //   106	108	106	finally
    //   108	121	129	finally
    //   129	131	129	finally
    //   0	22	142	finally
    //   31	96	142	finally
    //   106	144	142	finally
  }
}
