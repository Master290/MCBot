package io.netty.handler.ssl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.ssl.util.LazyJavaxX509Certificate;
import io.netty.handler.ssl.util.LazyX509Certificate;
import io.netty.internal.tcnative.AsyncTask;
import io.netty.internal.tcnative.Buffer;
import io.netty.internal.tcnative.SSL;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCounted;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetectorFactory;
import io.netty.util.ResourceLeakTracker;
import io.netty.util.internal.EmptyArrays;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.SuppressJava6Requirement;
import io.netty.util.internal.ThrowableUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionBindingEvent;
import javax.net.ssl.SSLSessionBindingListener;









































public class ReferenceCountedOpenSslEngine
  extends SSLEngine
  implements ReferenceCounted, ApplicationProtocolAccessor
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(ReferenceCountedOpenSslEngine.class);
  

  private static final ResourceLeakDetector<ReferenceCountedOpenSslEngine> leakDetector = ResourceLeakDetectorFactory.instance().newResourceLeakDetector(ReferenceCountedOpenSslEngine.class);
  private static final int OPENSSL_OP_NO_PROTOCOL_INDEX_SSLV2 = 0;
  private static final int OPENSSL_OP_NO_PROTOCOL_INDEX_SSLV3 = 1;
  private static final int OPENSSL_OP_NO_PROTOCOL_INDEX_TLSv1 = 2;
  private static final int OPENSSL_OP_NO_PROTOCOL_INDEX_TLSv1_1 = 3;
  private static final int OPENSSL_OP_NO_PROTOCOL_INDEX_TLSv1_2 = 4;
  private static final int OPENSSL_OP_NO_PROTOCOL_INDEX_TLSv1_3 = 5;
  private static final int[] OPENSSL_OP_NO_PROTOCOLS = { SSL.SSL_OP_NO_SSLv2, SSL.SSL_OP_NO_SSLv3, SSL.SSL_OP_NO_TLSv1, SSL.SSL_OP_NO_TLSv1_1, SSL.SSL_OP_NO_TLSv1_2, SSL.SSL_OP_NO_TLSv1_3 };
  










  static final int MAX_PLAINTEXT_LENGTH = SSL.SSL_MAX_PLAINTEXT_LENGTH;
  


  static final int MAX_RECORD_SIZE = SSL.SSL_MAX_RECORD_LENGTH;
  
  private static final SSLEngineResult NEED_UNWRAP_OK = new SSLEngineResult(SSLEngineResult.Status.OK, SSLEngineResult.HandshakeStatus.NEED_UNWRAP, 0, 0);
  private static final SSLEngineResult NEED_UNWRAP_CLOSED = new SSLEngineResult(SSLEngineResult.Status.CLOSED, SSLEngineResult.HandshakeStatus.NEED_UNWRAP, 0, 0);
  private static final SSLEngineResult NEED_WRAP_OK = new SSLEngineResult(SSLEngineResult.Status.OK, SSLEngineResult.HandshakeStatus.NEED_WRAP, 0, 0);
  private static final SSLEngineResult NEED_WRAP_CLOSED = new SSLEngineResult(SSLEngineResult.Status.CLOSED, SSLEngineResult.HandshakeStatus.NEED_WRAP, 0, 0);
  private static final SSLEngineResult CLOSED_NOT_HANDSHAKING = new SSLEngineResult(SSLEngineResult.Status.CLOSED, SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING, 0, 0);
  

  private long ssl;
  
  private long networkBIO;
  

  private static enum HandshakeState
  {
    NOT_STARTED, 
    


    STARTED_IMPLICITLY, 
    


    STARTED_EXPLICITLY, 
    


    FINISHED;
    
    private HandshakeState() {} }
  private HandshakeState handshakeState = HandshakeState.NOT_STARTED;
  
  private boolean receivedShutdown;
  
  private volatile boolean destroyed;
  private volatile String applicationProtocol;
  private volatile boolean needTask;
  private String[] explicitlyEnabledProtocols;
  private boolean sessionSet;
  private final ResourceLeakTracker<ReferenceCountedOpenSslEngine> leak;
  private final AbstractReferenceCounted refCnt = new AbstractReferenceCounted()
  {
    public ReferenceCounted touch(Object hint) {
      if (leak != null) {
        leak.record(hint);
      }
      
      return ReferenceCountedOpenSslEngine.this;
    }
    
    protected void deallocate()
    {
      shutdown();
      if (leak != null) {
        boolean closed = leak.close(ReferenceCountedOpenSslEngine.this);
        assert (closed);
      }
      parentContext.release();
    }
  };
  
  private volatile ClientAuth clientAuth = ClientAuth.NONE;
  

  private volatile long lastAccessed = -1L;
  
  private String endPointIdentificationAlgorithm;
  
  private Object algorithmConstraints;
  
  private List<String> sniHostNames;
  
  private volatile Collection<?> matchers;
  
  private boolean isInboundDone;
  
  private boolean outboundClosed;
  
  final boolean jdkCompatibilityMode;
  
  private final boolean clientMode;
  final ByteBufAllocator alloc;
  private final OpenSslEngineMap engineMap;
  private final OpenSslApplicationProtocolNegotiator apn;
  private final ReferenceCountedOpenSslContext parentContext;
  private final OpenSslSession session;
  private final ByteBuffer[] singleSrcBuffer = new ByteBuffer[1];
  private final ByteBuffer[] singleDstBuffer = new ByteBuffer[1];
  


  private final boolean enableOcsp;
  


  private int maxWrapOverhead;
  

  private int maxWrapBufferSize;
  

  private Throwable pendingException;
  


  ReferenceCountedOpenSslEngine(ReferenceCountedOpenSslContext context, ByteBufAllocator alloc, String peerHost, int peerPort, boolean jdkCompatibilityMode, boolean leakDetection)
  {
    super(peerHost, peerPort);
    OpenSsl.ensureAvailability();
    this.alloc = ((ByteBufAllocator)ObjectUtil.checkNotNull(alloc, "alloc"));
    apn = ((OpenSslApplicationProtocolNegotiator)context.applicationProtocolNegotiator());
    clientMode = context.isClient();
    
    if (PlatformDependent.javaVersion() >= 7) {
      session = new ExtendedOpenSslSession(new DefaultOpenSslSession(context.sessionContext()))
      {
        private String[] peerSupportedSignatureAlgorithms;
        private List requestedServerNames;
        
        public List getRequestedServerNames() {
          if (clientMode) {
            return Java8SslUtils.getSniHostNames(sniHostNames);
          }
          synchronized (ReferenceCountedOpenSslEngine.this) {
            if (requestedServerNames == null) {
              if (ReferenceCountedOpenSslEngine.this.isDestroyed()) {
                requestedServerNames = Collections.emptyList();
              } else {
                String name = SSL.getSniHostname(ssl);
                if (name == null) {
                  requestedServerNames = Collections.emptyList();

                }
                else
                {
                  requestedServerNames = Java8SslUtils.getSniHostName(
                    SSL.getSniHostname(ssl).getBytes(CharsetUtil.UTF_8));
                }
              }
            }
            return requestedServerNames;
          }
        }
        

        public String[] getPeerSupportedSignatureAlgorithms()
        {
          synchronized (ReferenceCountedOpenSslEngine.this) {
            if (peerSupportedSignatureAlgorithms == null) {
              if (ReferenceCountedOpenSslEngine.this.isDestroyed()) {
                peerSupportedSignatureAlgorithms = EmptyArrays.EMPTY_STRINGS;
              } else {
                String[] algs = SSL.getSigAlgs(ssl);
                if (algs == null) {
                  peerSupportedSignatureAlgorithms = EmptyArrays.EMPTY_STRINGS;
                } else {
                  Set<String> algorithmList = new LinkedHashSet(algs.length);
                  for (String alg : algs) {
                    String converted = SignatureAlgorithmConverter.toJavaName(alg);
                    
                    if (converted != null) {
                      algorithmList.add(converted);
                    }
                  }
                  peerSupportedSignatureAlgorithms = ((String[])algorithmList.toArray(new String[0]));
                }
              }
            }
            return (String[])peerSupportedSignatureAlgorithms.clone();
          }
        }
        
        public List<byte[]> getStatusResponses()
        {
          byte[] ocspResponse = null;
          if ((enableOcsp) && (clientMode)) {
            synchronized (ReferenceCountedOpenSslEngine.this) {
              if (!ReferenceCountedOpenSslEngine.this.isDestroyed()) {
                ocspResponse = SSL.getOcspResponse(ssl);
              }
            }
          }
          return ocspResponse == null ? 
            Collections.emptyList() : Collections.singletonList(ocspResponse);
        }
      };
    } else {
      session = new DefaultOpenSslSession(context.sessionContext());
    }
    engineMap = engineMap;
    enableOcsp = enableOcsp;
    if (!context.sessionContext().useKeyManager()) {
      session.setLocalCertificate(keyCertChain);
    }
    
    this.jdkCompatibilityMode = jdkCompatibilityMode;
    Lock readerLock = ctxLock.readLock();
    readerLock.lock();
    try
    {
      finalSsl = SSL.newSSL(ctx, !context.isClient());
    } finally { long finalSsl;
      readerLock.unlock();
    }
    synchronized (this) { long finalSsl;
      ssl = finalSsl;
      try {
        networkBIO = SSL.bioNewByteBuffer(ssl, context.getBioNonApplicationBufferSize());
        


        setClientAuth(clientMode ? ClientAuth.NONE : clientAuth);
        
        if (protocols != null) {
          setEnabledProtocols0(protocols, true);
        } else {
          explicitlyEnabledProtocols = getEnabledProtocols();
        }
        


        if ((clientMode) && (SslUtils.isValidHostNameForSNI(peerHost))) {
          SSL.setTlsExtHostName(ssl, peerHost);
          sniHostNames = Collections.singletonList(peerHost);
        }
        
        if (enableOcsp) {
          SSL.enableOcsp(ssl);
        }
        
        if (!jdkCompatibilityMode) {
          SSL.setMode(ssl, SSL.getMode(ssl) | SSL.SSL_MODE_ENABLE_PARTIAL_WRITE);
        }
        
        if (isProtocolEnabled(SSL.getOptions(ssl), SSL.SSL_OP_NO_TLSv1_3, "TLSv1.3")) {
          boolean enableTickets = clientMode ? ReferenceCountedOpenSslContext.CLIENT_ENABLE_SESSION_TICKET_TLSV13 : ReferenceCountedOpenSslContext.SERVER_ENABLE_SESSION_TICKET_TLSV13;
          

          if (enableTickets)
          {






            SSL.clearOptions(ssl, SSL.SSL_OP_NO_TICKET);
          }
        }
        

        calculateMaxWrapOverhead();
      }
      catch (Throwable cause)
      {
        shutdown();
        
        PlatformDependent.throwException(cause);
      }
    }
    


    parentContext = context;
    parentContext.retain();
    


    leak = (leakDetection ? leakDetector.track(this) : null);
  }
  
  final synchronized String[] authMethods() {
    if (isDestroyed()) {
      return EmptyArrays.EMPTY_STRINGS;
    }
    return SSL.authenticationMethods(ssl);
  }
  
  final boolean setKeyMaterial(OpenSslKeyMaterial keyMaterial) throws Exception {
    synchronized (this) {
      if (isDestroyed()) {
        return false;
      }
      SSL.setKeyMaterial(ssl, keyMaterial.certificateChainAddress(), keyMaterial.privateKeyAddress());
    }
    session.setLocalCertificate(keyMaterial.certificateChain());
    return true;
  }
  
  final synchronized SecretKeySpec masterKey() {
    if (isDestroyed()) {
      return null;
    }
    return new SecretKeySpec(SSL.getMasterKey(ssl), "AES");
  }
  
  synchronized boolean isSessionReused() {
    if (isDestroyed()) {
      return false;
    }
    return SSL.isSessionReused(ssl);
  }
  



  public void setOcspResponse(byte[] response)
  {
    if (!enableOcsp) {
      throw new IllegalStateException("OCSP stapling is not enabled");
    }
    
    if (clientMode) {
      throw new IllegalStateException("Not a server SSLEngine");
    }
    
    synchronized (this) {
      if (!isDestroyed()) {
        SSL.setOcspResponse(ssl, response);
      }
    }
  }
  



  public byte[] getOcspResponse()
  {
    if (!enableOcsp) {
      throw new IllegalStateException("OCSP stapling is not enabled");
    }
    
    if (!clientMode) {
      throw new IllegalStateException("Not a client SSLEngine");
    }
    
    synchronized (this) {
      if (isDestroyed()) {
        return EmptyArrays.EMPTY_BYTES;
      }
      return SSL.getOcspResponse(ssl);
    }
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
  

  public String getApplicationProtocol()
  {
    return applicationProtocol;
  }
  

  public String getHandshakeApplicationProtocol()
  {
    return applicationProtocol;
  }
  




  public final synchronized SSLSession getHandshakeSession()
  {
    switch (handshakeState) {
    case NOT_STARTED: 
    case FINISHED: 
      return null;
    }
    return session;
  }
  





  public final synchronized long sslPointer()
  {
    return ssl;
  }
  


  public final synchronized void shutdown()
  {
    if (!destroyed) {
      destroyed = true;
      engineMap.remove(ssl);
      SSL.freeSSL(ssl);
      ssl = (this.networkBIO = 0L);
      
      isInboundDone = (this.outboundClosed = 1);
    }
    

    SSL.clearError();
  }
  




  private int writePlaintextData(ByteBuffer src, int len)
  {
    int pos = src.position();
    int limit = src.limit();
    

    if (src.isDirect()) {
      int sslWrote = SSL.writeToSSL(ssl, bufferAddress(src) + pos, len);
      if (sslWrote > 0) {
        src.position(pos + sslWrote);
      }
    } else {
      ByteBuf buf = alloc.directBuffer(len);
      try {
        src.limit(pos + len);
        
        buf.setBytes(0, src);
        src.limit(limit);
        
        int sslWrote = SSL.writeToSSL(ssl, OpenSsl.memoryAddress(buf), len);
        if (sslWrote > 0) {
          src.position(pos + sslWrote);
        } else {
          src.position(pos);
        }
      } finally {
        buf.release();
      } }
    int sslWrote;
    return sslWrote;
  }
  

  private ByteBuf writeEncryptedData(ByteBuffer src, int len)
    throws SSLException
  {
    int pos = src.position();
    if (src.isDirect()) {
      SSL.bioSetByteBuffer(networkBIO, bufferAddress(src) + pos, len, false);
    } else {
      ByteBuf buf = alloc.directBuffer(len);
      try {
        int limit = src.limit();
        src.limit(pos + len);
        buf.writeBytes(src);
        
        src.position(pos);
        src.limit(limit);
        
        SSL.bioSetByteBuffer(networkBIO, OpenSsl.memoryAddress(buf), len, false);
        return buf;
      } catch (Throwable cause) {
        buf.release();
        PlatformDependent.throwException(cause);
      }
    }
    return null;
  }
  


  private int readPlaintextData(ByteBuffer dst)
    throws SSLException
  {
    int pos = dst.position();
    if (dst.isDirect()) {
      int sslRead = SSL.readFromSSL(ssl, bufferAddress(dst) + pos, dst.limit() - pos);
      if (sslRead > 0) {
        dst.position(pos + sslRead);
      }
    } else {
      int limit = dst.limit();
      int len = Math.min(maxEncryptedPacketLength0(), limit - pos);
      ByteBuf buf = alloc.directBuffer(len);
      try {
        int sslRead = SSL.readFromSSL(ssl, OpenSsl.memoryAddress(buf), len);
        if (sslRead > 0) {
          dst.limit(pos + sslRead);
          buf.getBytes(buf.readerIndex(), dst);
          dst.limit(limit);
        }
      } finally {
        buf.release();
      }
    }
    int sslRead;
    return sslRead;
  }
  


  final synchronized int maxWrapOverhead()
  {
    return maxWrapOverhead;
  }
  


  final synchronized int maxEncryptedPacketLength()
  {
    return maxEncryptedPacketLength0();
  }
  



  final int maxEncryptedPacketLength0()
  {
    return maxWrapOverhead + MAX_PLAINTEXT_LENGTH;
  }
  




  final int calculateMaxLengthForWrap(int plaintextLength, int numComponents)
  {
    return (int)Math.min(maxWrapBufferSize, plaintextLength + maxWrapOverhead * numComponents);
  }
  
  final synchronized int sslPending() {
    return sslPending0();
  }
  


  private void calculateMaxWrapOverhead()
  {
    maxWrapOverhead = SSL.getMaxWrapOverhead(ssl);
    



    maxWrapBufferSize = (jdkCompatibilityMode ? maxEncryptedPacketLength0() : maxEncryptedPacketLength0() << 4);
  }
  



  private int sslPending0()
  {
    return handshakeState != HandshakeState.FINISHED ? 0 : SSL.sslPending(ssl);
  }
  
  private boolean isBytesAvailableEnoughForWrap(int bytesAvailable, int plaintextLength, int numComponents) {
    return bytesAvailable - maxWrapOverhead * numComponents >= plaintextLength;
  }
  

  public final SSLEngineResult wrap(ByteBuffer[] srcs, int offset, int length, ByteBuffer dst)
    throws SSLException
  {
    ObjectUtil.checkNotNullWithIAE(srcs, "srcs");
    ObjectUtil.checkNotNullWithIAE(dst, "dst");
    
    if ((offset >= srcs.length) || (offset + length > srcs.length)) {
      throw new IndexOutOfBoundsException("offset: " + offset + ", length: " + length + " (expected: offset <= offset + length <= srcs.length (" + srcs.length + "))");
    }
    


    if (dst.isReadOnly()) {
      throw new ReadOnlyBufferException();
    }
    
    synchronized (this) {
      if (isOutboundDone())
      {
        return (isInboundDone()) || (isDestroyed()) ? CLOSED_NOT_HANDSHAKING : NEED_UNWRAP_CLOSED;
      }
      
      int bytesProduced = 0;
      ByteBuf bioReadCopyBuf = null;
      try
      {
        if (dst.isDirect()) {
          SSL.bioSetByteBuffer(networkBIO, bufferAddress(dst) + dst.position(), dst.remaining(), true);
        }
        else {
          bioReadCopyBuf = alloc.directBuffer(dst.remaining());
          SSL.bioSetByteBuffer(networkBIO, OpenSsl.memoryAddress(bioReadCopyBuf), bioReadCopyBuf.writableBytes(), true);
        }
        

        int bioLengthBefore = SSL.bioLengthByteBuffer(networkBIO);
        

        if (outboundClosed)
        {




          if (!isBytesAvailableEnoughForWrap(dst.remaining(), 2, 1)) {
            localSSLEngineResult1 = new SSLEngineResult(SSLEngineResult.Status.BUFFER_OVERFLOW, getHandshakeStatus(), 0, 0);
            





































































































































































































































            SSL.bioClearByteBuffer(networkBIO);
            if (bioReadCopyBuf == null) {
              dst.position(dst.position() + bytesProduced);
            } else {
              assert (bioReadCopyBuf.readableBytes() <= dst.remaining()) : ("The destination buffer " + dst + " didn't have enough remaining space to hold the encrypted content in " + bioReadCopyBuf);
              
              dst.put(bioReadCopyBuf.internalNioBuffer(bioReadCopyBuf.readerIndex(), bytesProduced));
              bioReadCopyBuf.release();
            }
            return localSSLEngineResult1;
          }
          


          bytesProduced = SSL.bioFlushByteBuffer(networkBIO);
          if (bytesProduced <= 0) {
            localSSLEngineResult1 = newResultMayFinishHandshake(SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING, 0, 0);
            






























































































































































































































            SSL.bioClearByteBuffer(networkBIO);
            if (bioReadCopyBuf == null) {
              dst.position(dst.position() + bytesProduced);
            } else {
              assert (bioReadCopyBuf.readableBytes() <= dst.remaining()) : ("The destination buffer " + dst + " didn't have enough remaining space to hold the encrypted content in " + bioReadCopyBuf);
              
              dst.put(bioReadCopyBuf.internalNioBuffer(bioReadCopyBuf.readerIndex(), bytesProduced));
              bioReadCopyBuf.release();
            }
            return localSSLEngineResult1;
          }
          


          if (!doSSLShutdown()) {
            localSSLEngineResult1 = newResultMayFinishHandshake(SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING, 0, bytesProduced);
            
























































































































































































































            SSL.bioClearByteBuffer(networkBIO);
            if (bioReadCopyBuf == null) {
              dst.position(dst.position() + bytesProduced);
            } else {
              assert (bioReadCopyBuf.readableBytes() <= dst.remaining()) : ("The destination buffer " + dst + " didn't have enough remaining space to hold the encrypted content in " + bioReadCopyBuf);
              
              dst.put(bioReadCopyBuf.internalNioBuffer(bioReadCopyBuf.readerIndex(), bytesProduced));
              bioReadCopyBuf.release();
            }
            return localSSLEngineResult1;
          }
          bytesProduced = bioLengthBefore - SSL.bioLengthByteBuffer(networkBIO);
          SSLEngineResult localSSLEngineResult1 = newResultMayFinishHandshake(SSLEngineResult.HandshakeStatus.NEED_WRAP, 0, bytesProduced);
          





















































































































































































































          SSL.bioClearByteBuffer(networkBIO);
          if (bioReadCopyBuf == null) {
            dst.position(dst.position() + bytesProduced);
          } else {
            assert (bioReadCopyBuf.readableBytes() <= dst.remaining()) : ("The destination buffer " + dst + " didn't have enough remaining space to hold the encrypted content in " + bioReadCopyBuf);
            
            dst.put(bioReadCopyBuf.internalNioBuffer(bioReadCopyBuf.readerIndex(), bytesProduced));
            bioReadCopyBuf.release();
          }
          return localSSLEngineResult1;
        }
        

        SSLEngineResult.HandshakeStatus status = SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;
        
        if (handshakeState != HandshakeState.FINISHED) {
          if (handshakeState != HandshakeState.STARTED_EXPLICITLY)
          {
            handshakeState = HandshakeState.STARTED_IMPLICITLY;
          }
          

          bytesProduced = SSL.bioFlushByteBuffer(networkBIO);
          SSLEngineResult localSSLEngineResult2;
          if (pendingException != null)
          {










            if (bytesProduced > 0) {
              localSSLEngineResult2 = newResult(SSLEngineResult.HandshakeStatus.NEED_WRAP, 0, bytesProduced);
              

























































































































































































              SSL.bioClearByteBuffer(networkBIO);
              if (bioReadCopyBuf == null) {
                dst.position(dst.position() + bytesProduced);
              } else {
                assert (bioReadCopyBuf.readableBytes() <= dst.remaining()) : ("The destination buffer " + dst + " didn't have enough remaining space to hold the encrypted content in " + bioReadCopyBuf);
                
                dst.put(bioReadCopyBuf.internalNioBuffer(bioReadCopyBuf.readerIndex(), bytesProduced));
                bioReadCopyBuf.release();
              }
              return localSSLEngineResult2;
            }
            


            localSSLEngineResult2 = newResult(handshakeException(), 0, 0);
            




















































































































































































            SSL.bioClearByteBuffer(networkBIO);
            if (bioReadCopyBuf == null) {
              dst.position(dst.position() + bytesProduced);
            } else {
              assert (bioReadCopyBuf.readableBytes() <= dst.remaining()) : ("The destination buffer " + dst + " didn't have enough remaining space to hold the encrypted content in " + bioReadCopyBuf);
              
              dst.put(bioReadCopyBuf.internalNioBuffer(bioReadCopyBuf.readerIndex(), bytesProduced));
              bioReadCopyBuf.release();
            }
            return localSSLEngineResult2;
          }
          
          status = handshake();
          


          bytesProduced = bioLengthBefore - SSL.bioLengthByteBuffer(networkBIO);
          
          if (status == SSLEngineResult.HandshakeStatus.NEED_TASK) {
            localSSLEngineResult2 = newResult(status, 0, bytesProduced);
            










































































































































































            SSL.bioClearByteBuffer(networkBIO);
            if (bioReadCopyBuf == null) {
              dst.position(dst.position() + bytesProduced);
            } else {
              assert (bioReadCopyBuf.readableBytes() <= dst.remaining()) : ("The destination buffer " + dst + " didn't have enough remaining space to hold the encrypted content in " + bioReadCopyBuf);
              
              dst.put(bioReadCopyBuf.internalNioBuffer(bioReadCopyBuf.readerIndex(), bytesProduced));
              bioReadCopyBuf.release();
            }
            return localSSLEngineResult2;
          }
          
          if (bytesProduced > 0)
          {


            localSSLEngineResult2 = newResult(mayFinishHandshake(status != SSLEngineResult.HandshakeStatus.FINISHED ? 
            
              getHandshakeStatus(SSL.bioLengthNonApplication(networkBIO)) : bytesProduced == bioLengthBefore ? SSLEngineResult.HandshakeStatus.NEED_WRAP : SSLEngineResult.HandshakeStatus.FINISHED), 0, bytesProduced);
            

































































































































































            SSL.bioClearByteBuffer(networkBIO);
            if (bioReadCopyBuf == null) {
              dst.position(dst.position() + bytesProduced);
            } else {
              assert (bioReadCopyBuf.readableBytes() <= dst.remaining()) : ("The destination buffer " + dst + " didn't have enough remaining space to hold the encrypted content in " + bioReadCopyBuf);
              
              dst.put(bioReadCopyBuf.internalNioBuffer(bioReadCopyBuf.readerIndex(), bytesProduced));
              bioReadCopyBuf.release();
            }
            return localSSLEngineResult2;
          }
          



          if (status == SSLEngineResult.HandshakeStatus.NEED_UNWRAP)
          {
            localSSLEngineResult2 = isOutboundDone() ? NEED_UNWRAP_CLOSED : NEED_UNWRAP_OK;
            



























































































































































            SSL.bioClearByteBuffer(networkBIO);
            if (bioReadCopyBuf == null) {
              dst.position(dst.position() + bytesProduced);
            } else {
              assert (bioReadCopyBuf.readableBytes() <= dst.remaining()) : ("The destination buffer " + dst + " didn't have enough remaining space to hold the encrypted content in " + bioReadCopyBuf);
              
              dst.put(bioReadCopyBuf.internalNioBuffer(bioReadCopyBuf.readerIndex(), bytesProduced));
              bioReadCopyBuf.release();
            }
            return localSSLEngineResult2;
          }
          


          if (outboundClosed) {
            bytesProduced = SSL.bioFlushByteBuffer(networkBIO);
            localSSLEngineResult2 = newResultMayFinishHandshake(status, 0, bytesProduced);
            




















































































































































            SSL.bioClearByteBuffer(networkBIO);
            if (bioReadCopyBuf == null) {
              dst.position(dst.position() + bytesProduced);
            } else {
              assert (bioReadCopyBuf.readableBytes() <= dst.remaining()) : ("The destination buffer " + dst + " didn't have enough remaining space to hold the encrypted content in " + bioReadCopyBuf);
              
              dst.put(bioReadCopyBuf.internalNioBuffer(bioReadCopyBuf.readerIndex(), bytesProduced));
              bioReadCopyBuf.release();
            }
            return localSSLEngineResult2;
          }
        }
        
        int endOffset = offset + length;
        int i; if (jdkCompatibilityMode) {
          int srcsLen = 0;
          for (i = offset; i < endOffset; i++) {
            ByteBuffer src = srcs[i];
            if (src == null) {
              throw new IllegalArgumentException("srcs[" + i + "] is null");
            }
            if (srcsLen != MAX_PLAINTEXT_LENGTH)
            {


              srcsLen += src.remaining();
              if ((srcsLen > MAX_PLAINTEXT_LENGTH) || (srcsLen < 0))
              {


                srcsLen = MAX_PLAINTEXT_LENGTH;
              }
            }
          }
          

          if (!isBytesAvailableEnoughForWrap(dst.remaining(), srcsLen, 1)) {
            i = new SSLEngineResult(SSLEngineResult.Status.BUFFER_OVERFLOW, getHandshakeStatus(), 0, 0);
            
























































































































            SSL.bioClearByteBuffer(networkBIO);
            if (bioReadCopyBuf == null) {
              dst.position(dst.position() + bytesProduced);
            } else {
              assert (bioReadCopyBuf.readableBytes() <= dst.remaining()) : ("The destination buffer " + dst + " didn't have enough remaining space to hold the encrypted content in " + bioReadCopyBuf);
              
              dst.put(bioReadCopyBuf.internalNioBuffer(bioReadCopyBuf.readerIndex(), bytesProduced));
              bioReadCopyBuf.release();
            }
            return i;
          }
        }
        

        int bytesConsumed = 0;
        assert (bytesProduced == 0);
        

        bytesProduced = SSL.bioFlushByteBuffer(networkBIO);
        
        if (bytesProduced > 0) {
          i = newResultMayFinishHandshake(status, bytesConsumed, bytesProduced);
          












































































































          SSL.bioClearByteBuffer(networkBIO);
          if (bioReadCopyBuf == null) {
            dst.position(dst.position() + bytesProduced);
          } else {
            assert (bioReadCopyBuf.readableBytes() <= dst.remaining()) : ("The destination buffer " + dst + " didn't have enough remaining space to hold the encrypted content in " + bioReadCopyBuf);
            
            dst.put(bioReadCopyBuf.internalNioBuffer(bioReadCopyBuf.readerIndex(), bytesProduced));
            bioReadCopyBuf.release();
          }
          return i;
        }
        

        if (pendingException != null) {
          Throwable error = pendingException;
          pendingException = null;
          shutdown();
          

          throw new SSLException(error);
        }
        for (; 
            offset < endOffset; offset++) {
          src = srcs[offset];
          int remaining = src.remaining();
          if (remaining != 0)
          {
            int bytesWritten;
            SSLEngineResult localSSLEngineResult3;
            int bytesWritten;
            if (jdkCompatibilityMode)
            {


              bytesWritten = writePlaintextData(src, Math.min(remaining, MAX_PLAINTEXT_LENGTH - bytesConsumed));

            }
            else
            {
              int availableCapacityForWrap = dst.remaining() - bytesProduced - maxWrapOverhead;
              if (availableCapacityForWrap <= 0) {
                localSSLEngineResult3 = new SSLEngineResult(SSLEngineResult.Status.BUFFER_OVERFLOW, getHandshakeStatus(), bytesConsumed, bytesProduced);
                












































































                SSL.bioClearByteBuffer(networkBIO);
                if (bioReadCopyBuf == null) {
                  dst.position(dst.position() + bytesProduced);
                } else {
                  assert (bioReadCopyBuf.readableBytes() <= dst.remaining()) : ("The destination buffer " + dst + " didn't have enough remaining space to hold the encrypted content in " + bioReadCopyBuf);
                  
                  dst.put(bioReadCopyBuf.internalNioBuffer(bioReadCopyBuf.readerIndex(), bytesProduced));
                  bioReadCopyBuf.release();
                }
                return localSSLEngineResult3;
              }
              
              bytesWritten = writePlaintextData(src, Math.min(remaining, availableCapacityForWrap));
            }
            





            int pendingNow = SSL.bioLengthByteBuffer(networkBIO);
            bytesProduced += bioLengthBefore - pendingNow;
            bioLengthBefore = pendingNow;
            
            if (bytesWritten > 0) {
              bytesConsumed += bytesWritten;
              
              if ((jdkCompatibilityMode) || (bytesProduced == dst.remaining())) {
                localSSLEngineResult3 = newResultMayFinishHandshake(status, bytesConsumed, bytesProduced);
                

























































                SSL.bioClearByteBuffer(networkBIO);
                if (bioReadCopyBuf == null) {
                  dst.position(dst.position() + bytesProduced);
                } else {
                  assert (bioReadCopyBuf.readableBytes() <= dst.remaining()) : ("The destination buffer " + dst + " didn't have enough remaining space to hold the encrypted content in " + bioReadCopyBuf);
                  
                  dst.put(bioReadCopyBuf.internalNioBuffer(bioReadCopyBuf.readerIndex(), bytesProduced));
                  bioReadCopyBuf.release();
                }
                return localSSLEngineResult3;
              }
            } else {
              int sslError = SSL.getError(ssl, bytesWritten);
              SSLEngineResult.HandshakeStatus hs; if (sslError == SSL.SSL_ERROR_ZERO_RETURN)
              {
                if (!receivedShutdown) {
                  closeAll();
                  
                  bytesProduced += bioLengthBefore - SSL.bioLengthByteBuffer(networkBIO);
                  



                  hs = mayFinishHandshake(status != SSLEngineResult.HandshakeStatus.FINISHED ? 
                  
                    getHandshakeStatus(SSL.bioLengthNonApplication(networkBIO)) : bytesProduced == dst.remaining() ? SSLEngineResult.HandshakeStatus.NEED_WRAP : SSLEngineResult.HandshakeStatus.FINISHED);
                  
                  SSLEngineResult localSSLEngineResult4 = newResult(hs, bytesConsumed, bytesProduced);
                  







































                  SSL.bioClearByteBuffer(networkBIO);
                  if (bioReadCopyBuf == null) {
                    dst.position(dst.position() + bytesProduced);
                  } else {
                    assert (bioReadCopyBuf.readableBytes() <= dst.remaining()) : ("The destination buffer " + dst + " didn't have enough remaining space to hold the encrypted content in " + bioReadCopyBuf);
                    
                    dst.put(bioReadCopyBuf.internalNioBuffer(bioReadCopyBuf.readerIndex(), bytesProduced));
                    bioReadCopyBuf.release();
                  }
                  return localSSLEngineResult4;
                }
                
                hs = newResult(SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING, bytesConsumed, bytesProduced);
                




































                SSL.bioClearByteBuffer(networkBIO);
                if (bioReadCopyBuf == null) {
                  dst.position(dst.position() + bytesProduced);
                } else {
                  assert (bioReadCopyBuf.readableBytes() <= dst.remaining()) : ("The destination buffer " + dst + " didn't have enough remaining space to hold the encrypted content in " + bioReadCopyBuf);
                  
                  dst.put(bioReadCopyBuf.internalNioBuffer(bioReadCopyBuf.readerIndex(), bytesProduced));
                  bioReadCopyBuf.release();
                }
                return hs; }
              if (sslError == SSL.SSL_ERROR_WANT_READ)
              {


                hs = newResult(SSLEngineResult.HandshakeStatus.NEED_UNWRAP, bytesConsumed, bytesProduced);
                































                SSL.bioClearByteBuffer(networkBIO);
                if (bioReadCopyBuf == null) {
                  dst.position(dst.position() + bytesProduced);
                } else {
                  assert (bioReadCopyBuf.readableBytes() <= dst.remaining()) : ("The destination buffer " + dst + " didn't have enough remaining space to hold the encrypted content in " + bioReadCopyBuf);
                  
                  dst.put(bioReadCopyBuf.internalNioBuffer(bioReadCopyBuf.readerIndex(), bytesProduced));
                  bioReadCopyBuf.release();
                }
                return hs; }
              if (sslError == SSL.SSL_ERROR_WANT_WRITE)
              {











                if (bytesProduced > 0)
                {

                  hs = newResult(SSLEngineResult.HandshakeStatus.NEED_WRAP, bytesConsumed, bytesProduced);
                  














                  SSL.bioClearByteBuffer(networkBIO);
                  if (bioReadCopyBuf == null) {
                    dst.position(dst.position() + bytesProduced);
                  } else {
                    assert (bioReadCopyBuf.readableBytes() <= dst.remaining()) : ("The destination buffer " + dst + " didn't have enough remaining space to hold the encrypted content in " + bioReadCopyBuf);
                    
                    dst.put(bioReadCopyBuf.internalNioBuffer(bioReadCopyBuf.readerIndex(), bytesProduced));
                    bioReadCopyBuf.release();
                  }
                  return hs;
                }
                hs = newResult(SSLEngineResult.Status.BUFFER_OVERFLOW, status, bytesConsumed, bytesProduced);
                












                SSL.bioClearByteBuffer(networkBIO);
                if (bioReadCopyBuf == null) {
                  dst.position(dst.position() + bytesProduced);
                } else {
                  assert (bioReadCopyBuf.readableBytes() <= dst.remaining()) : ("The destination buffer " + dst + " didn't have enough remaining space to hold the encrypted content in " + bioReadCopyBuf);
                  
                  dst.put(bioReadCopyBuf.internalNioBuffer(bioReadCopyBuf.readerIndex(), bytesProduced));
                  bioReadCopyBuf.release();
                }
                return hs; }
              if ((sslError == SSL.SSL_ERROR_WANT_X509_LOOKUP) || (sslError == SSL.SSL_ERROR_WANT_CERTIFICATE_VERIFY) || (sslError == SSL.SSL_ERROR_WANT_PRIVATE_KEY_OPERATION))
              {


                hs = newResult(SSLEngineResult.HandshakeStatus.NEED_TASK, bytesConsumed, bytesProduced);
                







                SSL.bioClearByteBuffer(networkBIO);
                if (bioReadCopyBuf == null) {
                  dst.position(dst.position() + bytesProduced);
                } else {
                  assert (bioReadCopyBuf.readableBytes() <= dst.remaining()) : ("The destination buffer " + dst + " didn't have enough remaining space to hold the encrypted content in " + bioReadCopyBuf);
                  
                  dst.put(bioReadCopyBuf.internalNioBuffer(bioReadCopyBuf.readerIndex(), bytesProduced));
                  bioReadCopyBuf.release();
                }
                return hs;
              }
              
              throw shutdownWithError("SSL_write", sslError);
            }
          }
        }
        ByteBuffer src = newResultMayFinishHandshake(status, bytesConsumed, bytesProduced);
        
        SSL.bioClearByteBuffer(networkBIO);
        if (bioReadCopyBuf == null) {
          dst.position(dst.position() + bytesProduced);
        } else {
          assert (bioReadCopyBuf.readableBytes() <= dst.remaining()) : ("The destination buffer " + dst + " didn't have enough remaining space to hold the encrypted content in " + bioReadCopyBuf);
          
          dst.put(bioReadCopyBuf.internalNioBuffer(bioReadCopyBuf.readerIndex(), bytesProduced));
          bioReadCopyBuf.release();
        }
        return src;
      } finally {
        SSL.bioClearByteBuffer(networkBIO);
        if (bioReadCopyBuf == null) {
          dst.position(dst.position() + bytesProduced);
        } else {
          assert (bioReadCopyBuf.readableBytes() <= dst.remaining()) : ("The destination buffer " + dst + " didn't have enough remaining space to hold the encrypted content in " + bioReadCopyBuf);
          
          dst.put(bioReadCopyBuf.internalNioBuffer(bioReadCopyBuf.readerIndex(), bytesProduced));
          bioReadCopyBuf.release();
        }
      }
    }
  }
  
  private SSLEngineResult newResult(SSLEngineResult.HandshakeStatus hs, int bytesConsumed, int bytesProduced) {
    return newResult(SSLEngineResult.Status.OK, hs, bytesConsumed, bytesProduced);
  }
  



  private SSLEngineResult newResult(SSLEngineResult.Status status, SSLEngineResult.HandshakeStatus hs, int bytesConsumed, int bytesProduced)
  {
    if (isOutboundDone()) {
      if (isInboundDone())
      {
        hs = SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;
        

        shutdown();
      }
      return new SSLEngineResult(SSLEngineResult.Status.CLOSED, hs, bytesConsumed, bytesProduced);
    }
    if (hs == SSLEngineResult.HandshakeStatus.NEED_TASK)
    {
      needTask = true;
    }
    return new SSLEngineResult(status, hs, bytesConsumed, bytesProduced);
  }
  
  private SSLEngineResult newResultMayFinishHandshake(SSLEngineResult.HandshakeStatus hs, int bytesConsumed, int bytesProduced) throws SSLException
  {
    return newResult(mayFinishHandshake(hs, bytesConsumed, bytesProduced), bytesConsumed, bytesProduced);
  }
  
  private SSLEngineResult newResultMayFinishHandshake(SSLEngineResult.Status status, SSLEngineResult.HandshakeStatus hs, int bytesConsumed, int bytesProduced)
    throws SSLException
  {
    return newResult(status, mayFinishHandshake(hs, bytesConsumed, bytesProduced), bytesConsumed, bytesProduced);
  }
  


  private SSLException shutdownWithError(String operations, int sslError)
  {
    return shutdownWithError(operations, sslError, SSL.getLastErrorNumber());
  }
  
  private SSLException shutdownWithError(String operation, int sslError, int error) {
    String errorString = SSL.getErrorString(error);
    if (logger.isDebugEnabled()) {
      logger.debug("{} failed with {}: OpenSSL error: {} {}", new Object[] { operation, 
        Integer.valueOf(sslError), Integer.valueOf(error), errorString });
    }
    

    shutdown();
    if (handshakeState == HandshakeState.FINISHED) {
      return new SSLException(errorString);
    }
    
    SSLHandshakeException exception = new SSLHandshakeException(errorString);
    
    if (pendingException != null) {
      exception.initCause(pendingException);
      pendingException = null;
    }
    return exception;
  }
  
  private SSLEngineResult handleUnwrapException(int bytesConsumed, int bytesProduced, SSLException e) throws SSLException
  {
    int lastError = SSL.getLastErrorNumber();
    if (lastError != 0) {
      return sslReadErrorResult(SSL.SSL_ERROR_SSL, lastError, bytesConsumed, bytesProduced);
    }
    
    throw e;
  }
  


  public final SSLEngineResult unwrap(ByteBuffer[] srcs, int srcsOffset, int srcsLength, ByteBuffer[] dsts, int dstsOffset, int dstsLength)
    throws SSLException
  {
    ObjectUtil.checkNotNullWithIAE(srcs, "srcs");
    if ((srcsOffset >= srcs.length) || (srcsOffset + srcsLength > srcs.length))
    {
      throw new IndexOutOfBoundsException("offset: " + srcsOffset + ", length: " + srcsLength + " (expected: offset <= offset + length <= srcs.length (" + srcs.length + "))");
    }
    

    ObjectUtil.checkNotNullWithIAE(dsts, "dsts");
    if ((dstsOffset >= dsts.length) || (dstsOffset + dstsLength > dsts.length)) {
      throw new IndexOutOfBoundsException("offset: " + dstsOffset + ", length: " + dstsLength + " (expected: offset <= offset + length <= dsts.length (" + dsts.length + "))");
    }
    

    long capacity = 0L;
    int dstsEndOffset = dstsOffset + dstsLength;
    for (int i = dstsOffset; i < dstsEndOffset; i++) {
      ByteBuffer dst = (ByteBuffer)ObjectUtil.checkNotNullArrayParam(dsts[i], i, "dsts");
      if (dst.isReadOnly()) {
        throw new ReadOnlyBufferException();
      }
      capacity += dst.remaining();
    }
    
    int srcsEndOffset = srcsOffset + srcsLength;
    long len = 0L;
    for (int i = srcsOffset; i < srcsEndOffset; i++) {
      ByteBuffer src = (ByteBuffer)ObjectUtil.checkNotNullArrayParam(srcs[i], i, "srcs");
      len += src.remaining();
    }
    
    synchronized (this) {
      if (isInboundDone()) {
        return (isOutboundDone()) || (isDestroyed()) ? CLOSED_NOT_HANDSHAKING : NEED_WRAP_CLOSED;
      }
      
      SSLEngineResult.HandshakeStatus status = SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;
      
      if (handshakeState != HandshakeState.FINISHED) {
        if (handshakeState != HandshakeState.STARTED_EXPLICITLY)
        {
          handshakeState = HandshakeState.STARTED_IMPLICITLY;
        }
        
        status = handshake();
        
        if (status == SSLEngineResult.HandshakeStatus.NEED_TASK) {
          return newResult(status, 0, 0);
        }
        
        if (status == SSLEngineResult.HandshakeStatus.NEED_WRAP) {
          return NEED_WRAP_OK;
        }
        
        if (isInboundDone) {
          return NEED_WRAP_CLOSED;
        }
      }
      
      int sslPending = sslPending0();
      

      int packetLength;
      

      if (jdkCompatibilityMode) {
        if (len < 5L) {
          return newResultMayFinishHandshake(SSLEngineResult.Status.BUFFER_UNDERFLOW, status, 0, 0);
        }
        
        int packetLength = SslUtils.getEncryptedPacketLength(srcs, srcsOffset);
        if (packetLength == -2) {
          throw new NotSslRecordException("not an SSL/TLS record");
        }
        
        int packetLengthDataOnly = packetLength - 5;
        if (packetLengthDataOnly > capacity)
        {

          if (packetLengthDataOnly > MAX_RECORD_SIZE)
          {





            throw new SSLException("Illegal packet length: " + packetLengthDataOnly + " > " + session.getApplicationBufferSize());
          }
          session.tryExpandApplicationBufferSize(packetLengthDataOnly);
          
          return newResultMayFinishHandshake(SSLEngineResult.Status.BUFFER_OVERFLOW, status, 0, 0);
        }
        
        if (len < packetLength)
        {

          return newResultMayFinishHandshake(SSLEngineResult.Status.BUFFER_UNDERFLOW, status, 0, 0); }
      } else {
        if ((len == 0L) && (sslPending <= 0))
          return newResultMayFinishHandshake(SSLEngineResult.Status.BUFFER_UNDERFLOW, status, 0, 0);
        if (capacity == 0L) {
          return newResultMayFinishHandshake(SSLEngineResult.Status.BUFFER_OVERFLOW, status, 0, 0);
        }
        packetLength = (int)Math.min(2147483647L, len);
      }
      

      assert (srcsOffset < srcsEndOffset);
      

      assert (capacity > 0L);
      

      int bytesProduced = 0;
      int bytesConsumed = 0;
      try
      {
        for (;;) {
          ByteBuffer src = srcs[srcsOffset];
          int remaining = src.remaining();
          
          int pendingEncryptedBytes;
          if (remaining == 0) {
            if (sslPending <= 0)
            {

              srcsOffset++; if (srcsOffset < srcsEndOffset) continue;
              break;
            }
            

            ByteBuf bioWriteCopyBuf = null;
            int pendingEncryptedBytes = SSL.bioLengthByteBuffer(networkBIO);

          }
          else
          {
            pendingEncryptedBytes = Math.min(packetLength, remaining);
            try {
              bioWriteCopyBuf = writeEncryptedData(src, pendingEncryptedBytes);
            } catch (SSLException e) {
              ByteBuf bioWriteCopyBuf;
              SSLEngineResult localSSLEngineResult1 = handleUnwrapException(bytesConsumed, bytesProduced, e);
              
















































































              SSL.bioClearByteBuffer(networkBIO);
              rejectRemoteInitiatedRenegotiation();return localSSLEngineResult1;
            }
          }
          try {
            for (;;) {
              ByteBuffer dst = dsts[dstsOffset];
              if (!dst.hasRemaining())
              {
                dstsOffset++; if (dstsOffset >= dstsEndOffset)
                {


































































                  if (bioWriteCopyBuf == null) break label1361;
                  bioWriteCopyBuf.release();
                  break label1361;
                }
              }
              else
              {
                SSLEngineResult localSSLEngineResult2;
                try
                {
                  bytesRead = readPlaintextData(dst);
                } catch (SSLException e) {
                  int bytesRead;
                  localSSLEngineResult2 = handleUnwrapException(bytesConsumed, bytesProduced, e);
                  























































                  if (bioWriteCopyBuf != null) {
                    bioWriteCopyBuf.release();
                  }
                  


                  SSL.bioClearByteBuffer(networkBIO);
                  rejectRemoteInitiatedRenegotiation();return localSSLEngineResult2;
                }
                int bytesRead;
                int localBytesConsumed = pendingEncryptedBytes - SSL.bioLengthByteBuffer(networkBIO);
                bytesConsumed += localBytesConsumed;
                packetLength -= localBytesConsumed;
                pendingEncryptedBytes -= localBytesConsumed;
                src.position(src.position() + localBytesConsumed);
                
                if (bytesRead > 0) {
                  bytesProduced += bytesRead;
                  
                  if (!dst.hasRemaining()) {
                    sslPending = sslPending0();
                    
                    dstsOffset++; if (dstsOffset >= dstsEndOffset)
                    {

                      localSSLEngineResult2 = sslPending > 0 ? newResult(SSLEngineResult.Status.BUFFER_OVERFLOW, status, bytesConsumed, bytesProduced) : newResultMayFinishHandshake(isInboundDone() ? SSLEngineResult.Status.CLOSED : SSLEngineResult.Status.OK, status, bytesConsumed, bytesProduced);
                      



































                      if (bioWriteCopyBuf != null) {
                        bioWriteCopyBuf.release();
                      }
                      


                      SSL.bioClearByteBuffer(networkBIO);
                      rejectRemoteInitiatedRenegotiation();return localSSLEngineResult2;
                    }
                  }
                  else if ((packetLength == 0) || (jdkCompatibilityMode))
                  {
































                    if (bioWriteCopyBuf == null) break label1361;
                    bioWriteCopyBuf.release();
                    break label1361;
                  }
                }
                else
                {
                  int sslError = SSL.getError(ssl, bytesRead);
                  if ((sslError == SSL.SSL_ERROR_WANT_READ) || (sslError == SSL.SSL_ERROR_WANT_WRITE)) {
                    break;
                  }
                  
                  if (sslError == SSL.SSL_ERROR_ZERO_RETURN)
                  {
                    if (!receivedShutdown) {
                      closeAll();
                    }
                    localSSLEngineResult3 = newResultMayFinishHandshake(isInboundDone() ? SSLEngineResult.Status.CLOSED : SSLEngineResult.Status.OK, status, bytesConsumed, bytesProduced);
                    
















                    if (bioWriteCopyBuf != null) {
                      bioWriteCopyBuf.release();
                    }
                    


                    SSL.bioClearByteBuffer(networkBIO);
                    rejectRemoteInitiatedRenegotiation();return localSSLEngineResult3;
                  }
                  if ((sslError == SSL.SSL_ERROR_WANT_X509_LOOKUP) || (sslError == SSL.SSL_ERROR_WANT_CERTIFICATE_VERIFY) || (sslError == SSL.SSL_ERROR_WANT_PRIVATE_KEY_OPERATION))
                  {

                    localSSLEngineResult3 = newResult(isInboundDone() ? SSLEngineResult.Status.CLOSED : SSLEngineResult.Status.OK, SSLEngineResult.HandshakeStatus.NEED_TASK, bytesConsumed, bytesProduced);
                    











                    if (bioWriteCopyBuf != null) {
                      bioWriteCopyBuf.release();
                    }
                    


                    SSL.bioClearByteBuffer(networkBIO);
                    rejectRemoteInitiatedRenegotiation();return localSSLEngineResult3;
                  }
                  SSLEngineResult localSSLEngineResult3 = sslReadErrorResult(sslError, SSL.getLastErrorNumber(), bytesConsumed, bytesProduced);
                  








                  if (bioWriteCopyBuf != null) {
                    bioWriteCopyBuf.release();
                  }
                  


                  SSL.bioClearByteBuffer(networkBIO);
                  rejectRemoteInitiatedRenegotiation();return localSSLEngineResult3;
                }
              }
            }
            srcsOffset++; if (srcsOffset >= srcsEndOffset)
            {


              if (bioWriteCopyBuf == null) break;
              bioWriteCopyBuf.release(); break;
            }
          }
          finally
          {
            ByteBuf bioWriteCopyBuf;
            if (bioWriteCopyBuf != null)
              bioWriteCopyBuf.release();
          }
        }
      } finally {
        label1361:
        SSL.bioClearByteBuffer(networkBIO);
        rejectRemoteInitiatedRenegotiation();
      }
      

      if ((!receivedShutdown) && ((SSL.getShutdown(ssl) & SSL.SSL_RECEIVED_SHUTDOWN) == SSL.SSL_RECEIVED_SHUTDOWN)) {
        closeAll();
      }
      
      return newResultMayFinishHandshake(isInboundDone() ? SSLEngineResult.Status.CLOSED : SSLEngineResult.Status.OK, status, bytesConsumed, bytesProduced);
    }
  }
  



  private boolean needWrapAgain(int stackError)
  {
    if (SSL.bioLengthNonApplication(networkBIO) > 0)
    {

      String message = SSL.getErrorString(stackError);
      SSLException exception = handshakeState == HandshakeState.FINISHED ? new SSLException(message) : new SSLHandshakeException(message);
      
      if (pendingException == null) {
        pendingException = exception;
      } else {
        ThrowableUtil.addSuppressed(pendingException, exception);
      }
      

      SSL.clearError();
      return true;
    }
    return false;
  }
  
  private SSLEngineResult sslReadErrorResult(int error, int stackError, int bytesConsumed, int bytesProduced) throws SSLException
  {
    if (needWrapAgain(stackError))
    {

      return new SSLEngineResult(SSLEngineResult.Status.OK, SSLEngineResult.HandshakeStatus.NEED_WRAP, bytesConsumed, bytesProduced);
    }
    throw shutdownWithError("SSL_read", error, stackError);
  }
  
  private void closeAll() throws SSLException {
    receivedShutdown = true;
    closeOutbound();
    closeInbound();
  }
  

  private void rejectRemoteInitiatedRenegotiation()
    throws SSLHandshakeException
  {
    if ((!isDestroyed()) && (SSL.getHandshakeCount(ssl) > 1))
    {

      if ((!"TLSv1.3".equals(session.getProtocol())) && (handshakeState == HandshakeState.FINISHED))
      {

        shutdown();
        throw new SSLHandshakeException("remote-initiated renegotiation not allowed");
      } }
  }
  
  public final SSLEngineResult unwrap(ByteBuffer[] srcs, ByteBuffer[] dsts) throws SSLException {
    return unwrap(srcs, 0, srcs.length, dsts, 0, dsts.length);
  }
  
  private ByteBuffer[] singleSrcBuffer(ByteBuffer src) {
    singleSrcBuffer[0] = src;
    return singleSrcBuffer;
  }
  
  private void resetSingleSrcBuffer() {
    singleSrcBuffer[0] = null;
  }
  
  private ByteBuffer[] singleDstBuffer(ByteBuffer src) {
    singleDstBuffer[0] = src;
    return singleDstBuffer;
  }
  
  private void resetSingleDstBuffer() {
    singleDstBuffer[0] = null;
  }
  
  public final synchronized SSLEngineResult unwrap(ByteBuffer src, ByteBuffer[] dsts, int offset, int length) throws SSLException
  {
    try
    {
      return unwrap(singleSrcBuffer(src), 0, 1, dsts, offset, length);
    } finally {
      resetSingleSrcBuffer();
    }
  }
  
  public final synchronized SSLEngineResult wrap(ByteBuffer src, ByteBuffer dst) throws SSLException
  {
    try {
      return wrap(singleSrcBuffer(src), dst);
    } finally {
      resetSingleSrcBuffer();
    }
  }
  
  public final synchronized SSLEngineResult unwrap(ByteBuffer src, ByteBuffer dst) throws SSLException
  {
    try {
      return unwrap(singleSrcBuffer(src), singleDstBuffer(dst));
    } finally {
      resetSingleSrcBuffer();
      resetSingleDstBuffer();
    }
  }
  
  public final synchronized SSLEngineResult unwrap(ByteBuffer src, ByteBuffer[] dsts) throws SSLException
  {
    try {
      return unwrap(singleSrcBuffer(src), dsts);
    } finally {
      resetSingleSrcBuffer();
    }
  }
  
  private class TaskDecorator<R extends Runnable> implements Runnable {
    protected final R task;
    
    TaskDecorator() { this.task = task; }
    

    public void run()
    {
      if (ReferenceCountedOpenSslEngine.this.isDestroyed())
      {
        return;
      }
      try {
        task.run();
        

        needTask = false; } finally { needTask = false;
      }
    }
  }
  
  private final class AsyncTaskDecorator extends ReferenceCountedOpenSslEngine.TaskDecorator<AsyncTask> implements AsyncRunnable {
    AsyncTaskDecorator(AsyncTask task) {
      super(task);
    }
    
    public void run(final Runnable runnable)
    {
      if (ReferenceCountedOpenSslEngine.this.isDestroyed())
      {
        runnable.run();
        return;
      }
      ((AsyncTask)task).runAsync(new Runnable()
      {

        public void run()
        {

          needTask = false;
          runnable.run();
        }
      });
    }
  }
  
  public final synchronized Runnable getDelegatedTask()
  {
    if (isDestroyed()) {
      return null;
    }
    Runnable task = SSL.getTask(ssl);
    if (task == null) {
      return null;
    }
    if ((task instanceof AsyncTask)) {
      return new AsyncTaskDecorator((AsyncTask)task);
    }
    return new TaskDecorator(task);
  }
  
  public final synchronized void closeInbound() throws SSLException
  {
    if (isInboundDone) {
      return;
    }
    
    isInboundDone = true;
    
    if (isOutboundDone())
    {

      shutdown();
    }
    
    if ((handshakeState != HandshakeState.NOT_STARTED) && (!receivedShutdown)) {
      throw new SSLException("Inbound closed before receiving peer's close_notify: possible truncation attack?");
    }
  }
  

  public final synchronized boolean isInboundDone()
  {
    return isInboundDone;
  }
  
  public final synchronized void closeOutbound()
  {
    if (outboundClosed) {
      return;
    }
    
    outboundClosed = true;
    
    if ((handshakeState != HandshakeState.NOT_STARTED) && (!isDestroyed())) {
      int mode = SSL.getShutdown(ssl);
      if ((mode & SSL.SSL_SENT_SHUTDOWN) != SSL.SSL_SENT_SHUTDOWN) {
        doSSLShutdown();
      }
    }
    else {
      shutdown();
    }
  }
  



  private boolean doSSLShutdown()
  {
    if (SSL.isInInit(ssl) != 0)
    {



      return false;
    }
    int err = SSL.shutdownSSL(ssl);
    if (err < 0) {
      int sslErr = SSL.getError(ssl, err);
      if ((sslErr == SSL.SSL_ERROR_SYSCALL) || (sslErr == SSL.SSL_ERROR_SSL)) {
        if (logger.isDebugEnabled()) {
          int error = SSL.getLastErrorNumber();
          logger.debug("SSL_shutdown failed: OpenSSL error: {} {}", Integer.valueOf(error), SSL.getErrorString(error));
        }
        
        shutdown();
        return false;
      }
      SSL.clearError();
    }
    return true;
  }
  


  public final synchronized boolean isOutboundDone()
  {
    return (outboundClosed) && ((networkBIO == 0L) || (SSL.bioLengthNonApplication(networkBIO) == 0));
  }
  
  public final String[] getSupportedCipherSuites()
  {
    return (String[])OpenSsl.AVAILABLE_CIPHER_SUITES.toArray(new String[0]);
  }
  
  public final String[] getEnabledCipherSuites()
  {
    boolean tls13Enabled;
    String[] enabled;
    String[] extraCiphers;
    synchronized (this) { boolean tls13Enabled;
      if (!isDestroyed()) {
        String[] enabled = SSL.getCiphers(ssl);
        int opts = SSL.getOptions(ssl);
        boolean tls13Enabled; if (isProtocolEnabled(opts, SSL.SSL_OP_NO_TLSv1_3, "TLSv1.3")) {
          String[] extraCiphers = OpenSsl.EXTRA_SUPPORTED_TLS_1_3_CIPHERS;
          tls13Enabled = true;
        } else {
          String[] extraCiphers = EmptyArrays.EMPTY_STRINGS;
          tls13Enabled = false;
        }
      } else {
        return EmptyArrays.EMPTY_STRINGS; } }
    boolean tls13Enabled;
    String[] enabled;
    String[] extraCiphers; if (enabled == null) {
      return EmptyArrays.EMPTY_STRINGS;
    }
    Set<String> enabledSet = new LinkedHashSet(enabled.length + extraCiphers.length);
    synchronized (this) {
      for (int i = 0; i < enabled.length; i++) {
        String mapped = toJavaCipherSuite(enabled[i]);
        String cipher = mapped == null ? enabled[i] : mapped;
        if (((tls13Enabled) && (OpenSsl.isTlsv13Supported())) || (!SslUtils.isTLSv13Cipher(cipher)))
        {

          enabledSet.add(cipher); }
      }
      Collections.addAll(enabledSet, extraCiphers);
    }
    return (String[])enabledSet.toArray(new String[0]);
  }
  

  public final void setEnabledCipherSuites(String[] cipherSuites)
  {
    ObjectUtil.checkNotNull(cipherSuites, "cipherSuites");
    
    StringBuilder buf = new StringBuilder();
    StringBuilder bufTLSv13 = new StringBuilder();
    
    CipherSuiteConverter.convertToCipherStrings(Arrays.asList(cipherSuites), buf, bufTLSv13, OpenSsl.isBoringSSL());
    String cipherSuiteSpec = buf.toString();
    String cipherSuiteSpecTLSv13 = bufTLSv13.toString();
    
    if ((!OpenSsl.isTlsv13Supported()) && (!cipherSuiteSpecTLSv13.isEmpty())) {
      throw new IllegalArgumentException("TLSv1.3 is not supported by this java version.");
    }
    synchronized (this) {
      if (!isDestroyed()) {
        try
        {
          SSL.setCipherSuites(ssl, cipherSuiteSpec, false);
          if (OpenSsl.isTlsv13Supported())
          {
            SSL.setCipherSuites(ssl, OpenSsl.checkTls13Ciphers(logger, cipherSuiteSpecTLSv13), true);
          }
          


          Set<String> protocols = new HashSet(explicitlyEnabledProtocols.length);
          Collections.addAll(protocols, explicitlyEnabledProtocols);
          


          if (cipherSuiteSpec.isEmpty()) {
            protocols.remove("TLSv1");
            protocols.remove("TLSv1.1");
            protocols.remove("TLSv1.2");
            protocols.remove("SSLv3");
            protocols.remove("SSLv2");
            protocols.remove("SSLv2Hello");
          }
          
          if (cipherSuiteSpecTLSv13.isEmpty()) {
            protocols.remove("TLSv1.3");
          }
          

          setEnabledProtocols0((String[])protocols.toArray(EmptyArrays.EMPTY_STRINGS), false);
        } catch (Exception e) {
          throw new IllegalStateException("failed to enable cipher suites: " + cipherSuiteSpec, e);
        }
      } else {
        throw new IllegalStateException("failed to enable cipher suites: " + cipherSuiteSpec);
      }
    }
  }
  
  public final String[] getSupportedProtocols()
  {
    return (String[])OpenSsl.SUPPORTED_PROTOCOLS_SET.toArray(new String[0]);
  }
  
  public final String[] getEnabledProtocols()
  {
    List<String> enabled = new ArrayList(6);
    
    enabled.add("SSLv2Hello");
    
    int opts;
    synchronized (this) { int opts;
      if (!isDestroyed()) {
        opts = SSL.getOptions(ssl);
      } else
        return (String[])enabled.toArray(new String[0]);
    }
    int opts;
    if (isProtocolEnabled(opts, SSL.SSL_OP_NO_TLSv1, "TLSv1")) {
      enabled.add("TLSv1");
    }
    if (isProtocolEnabled(opts, SSL.SSL_OP_NO_TLSv1_1, "TLSv1.1")) {
      enabled.add("TLSv1.1");
    }
    if (isProtocolEnabled(opts, SSL.SSL_OP_NO_TLSv1_2, "TLSv1.2")) {
      enabled.add("TLSv1.2");
    }
    if (isProtocolEnabled(opts, SSL.SSL_OP_NO_TLSv1_3, "TLSv1.3")) {
      enabled.add("TLSv1.3");
    }
    if (isProtocolEnabled(opts, SSL.SSL_OP_NO_SSLv2, "SSLv2")) {
      enabled.add("SSLv2");
    }
    if (isProtocolEnabled(opts, SSL.SSL_OP_NO_SSLv3, "SSLv3")) {
      enabled.add("SSLv3");
    }
    return (String[])enabled.toArray(new String[0]);
  }
  

  private static boolean isProtocolEnabled(int opts, int disableMask, String protocolString)
  {
    return ((opts & disableMask) == 0) && (OpenSsl.SUPPORTED_PROTOCOLS_SET.contains(protocolString));
  }
  









  public final void setEnabledProtocols(String[] protocols)
  {
    setEnabledProtocols0(protocols, true);
  }
  
  private void setEnabledProtocols0(String[] protocols, boolean cache)
  {
    ObjectUtil.checkNotNullWithIAE(protocols, "protocols");
    int minProtocolIndex = OPENSSL_OP_NO_PROTOCOLS.length;
    int maxProtocolIndex = 0;
    for (String p : protocols) {
      if (!OpenSsl.SUPPORTED_PROTOCOLS_SET.contains(p)) {
        throw new IllegalArgumentException("Protocol " + p + " is not supported.");
      }
      if (p.equals("SSLv2")) {
        if (minProtocolIndex > 0) {
          minProtocolIndex = 0;
        }
        if (maxProtocolIndex < 0) {
          maxProtocolIndex = 0;
        }
      } else if (p.equals("SSLv3")) {
        if (minProtocolIndex > 1) {
          minProtocolIndex = 1;
        }
        if (maxProtocolIndex < 1) {
          maxProtocolIndex = 1;
        }
      } else if (p.equals("TLSv1")) {
        if (minProtocolIndex > 2) {
          minProtocolIndex = 2;
        }
        if (maxProtocolIndex < 2) {
          maxProtocolIndex = 2;
        }
      } else if (p.equals("TLSv1.1")) {
        if (minProtocolIndex > 3) {
          minProtocolIndex = 3;
        }
        if (maxProtocolIndex < 3) {
          maxProtocolIndex = 3;
        }
      } else if (p.equals("TLSv1.2")) {
        if (minProtocolIndex > 4) {
          minProtocolIndex = 4;
        }
        if (maxProtocolIndex < 4) {
          maxProtocolIndex = 4;
        }
      } else if (p.equals("TLSv1.3")) {
        if (minProtocolIndex > 5) {
          minProtocolIndex = 5;
        }
        if (maxProtocolIndex < 5) {
          maxProtocolIndex = 5;
        }
      }
    }
    synchronized (this) {
      if (cache) {
        explicitlyEnabledProtocols = protocols;
      }
      if (!isDestroyed())
      {
        SSL.clearOptions(ssl, SSL.SSL_OP_NO_SSLv2 | SSL.SSL_OP_NO_SSLv3 | SSL.SSL_OP_NO_TLSv1 | SSL.SSL_OP_NO_TLSv1_1 | SSL.SSL_OP_NO_TLSv1_2 | SSL.SSL_OP_NO_TLSv1_3);
        

        int opts = 0;
        for (int i = 0; i < minProtocolIndex; i++) {
          opts |= OPENSSL_OP_NO_PROTOCOLS[i];
        }
        assert (maxProtocolIndex != Integer.MAX_VALUE);
        for (int i = maxProtocolIndex + 1; i < OPENSSL_OP_NO_PROTOCOLS.length; i++) {
          opts |= OPENSSL_OP_NO_PROTOCOLS[i];
        }
        

        SSL.setOptions(ssl, opts);
      } else {
        throw new IllegalStateException("failed to enable protocols: " + Arrays.asList(protocols));
      }
    }
  }
  
  public final SSLSession getSession()
  {
    return session;
  }
  
  public final synchronized void beginHandshake() throws SSLException
  {
    switch (3.$SwitchMap$io$netty$handler$ssl$ReferenceCountedOpenSslEngine$HandshakeState[handshakeState.ordinal()]) {
    case 3: 
      checkEngineClosed();
      






      handshakeState = HandshakeState.STARTED_EXPLICITLY;
      calculateMaxWrapOverhead();
      
      break;
    case 4: 
      break;
    
    case 2: 
      throw new SSLException("renegotiation unsupported");
    case 1: 
      handshakeState = HandshakeState.STARTED_EXPLICITLY;
      if (handshake() == SSLEngineResult.HandshakeStatus.NEED_TASK)
      {
        needTask = true;
      }
      calculateMaxWrapOverhead();
      break;
    default: 
      throw new Error();
    }
  }
  
  private void checkEngineClosed() throws SSLException {
    if (isDestroyed()) {
      throw new SSLException("engine closed");
    }
  }
  
  private static SSLEngineResult.HandshakeStatus pendingStatus(int pendingStatus)
  {
    return pendingStatus > 0 ? SSLEngineResult.HandshakeStatus.NEED_WRAP : SSLEngineResult.HandshakeStatus.NEED_UNWRAP;
  }
  
  private static boolean isEmpty(Object[] arr) {
    return (arr == null) || (arr.length == 0);
  }
  
  private static boolean isEmpty(byte[] cert) {
    return (cert == null) || (cert.length == 0);
  }
  
  private SSLEngineResult.HandshakeStatus handshakeException() throws SSLException {
    if (SSL.bioLengthNonApplication(networkBIO) > 0)
    {
      return SSLEngineResult.HandshakeStatus.NEED_WRAP;
    }
    
    Throwable exception = pendingException;
    assert (exception != null);
    pendingException = null;
    shutdown();
    if ((exception instanceof SSLHandshakeException)) {
      throw ((SSLHandshakeException)exception);
    }
    SSLHandshakeException e = new SSLHandshakeException("General OpenSslEngine problem");
    e.initCause(exception);
    throw e;
  }
  



  final void initHandshakeException(Throwable cause)
  {
    if (pendingException == null) {
      pendingException = cause;
    } else {
      ThrowableUtil.addSuppressed(pendingException, cause);
    }
  }
  
  private SSLEngineResult.HandshakeStatus handshake() throws SSLException {
    if (needTask) {
      return SSLEngineResult.HandshakeStatus.NEED_TASK;
    }
    if (handshakeState == HandshakeState.FINISHED) {
      return SSLEngineResult.HandshakeStatus.FINISHED;
    }
    
    checkEngineClosed();
    
    if (pendingException != null)
    {

      if (SSL.doHandshake(ssl) <= 0)
      {
        SSL.clearError();
      }
      return handshakeException();
    }
    

    engineMap.add(this);
    
    if (!sessionSet) {
      parentContext.sessionContext().setSessionFromCache(getPeerHost(), getPeerPort(), ssl);
      sessionSet = true;
    }
    
    if (lastAccessed == -1L) {
      lastAccessed = System.currentTimeMillis();
    }
    
    int code = SSL.doHandshake(ssl);
    if (code <= 0) {
      int sslError = SSL.getError(ssl, code);
      if ((sslError == SSL.SSL_ERROR_WANT_READ) || (sslError == SSL.SSL_ERROR_WANT_WRITE)) {
        return pendingStatus(SSL.bioLengthNonApplication(networkBIO));
      }
      
      if ((sslError == SSL.SSL_ERROR_WANT_X509_LOOKUP) || (sslError == SSL.SSL_ERROR_WANT_CERTIFICATE_VERIFY) || (sslError == SSL.SSL_ERROR_WANT_PRIVATE_KEY_OPERATION))
      {

        return SSLEngineResult.HandshakeStatus.NEED_TASK;
      }
      
      if (needWrapAgain(SSL.getLastErrorNumber()))
      {

        return SSLEngineResult.HandshakeStatus.NEED_WRAP;
      }
      

      if (pendingException != null) {
        return handshakeException();
      }
      

      throw shutdownWithError("SSL_do_handshake", sslError);
    }
    
    if (SSL.bioLengthNonApplication(networkBIO) > 0) {
      return SSLEngineResult.HandshakeStatus.NEED_WRAP;
    }
    
    session.handshakeFinished(SSL.getSessionId(ssl), SSL.getCipherForSSL(ssl), SSL.getVersion(ssl), 
      SSL.getPeerCertificate(ssl), SSL.getPeerCertChain(ssl), 
      SSL.getTime(ssl) * 1000L, parentContext.sessionTimeout() * 1000L);
    selectApplicationProtocol();
    return SSLEngineResult.HandshakeStatus.FINISHED;
  }
  
  private SSLEngineResult.HandshakeStatus mayFinishHandshake(SSLEngineResult.HandshakeStatus hs, int bytesConsumed, int bytesProduced) throws SSLException
  {
    return ((hs == SSLEngineResult.HandshakeStatus.NEED_UNWRAP) && (bytesProduced > 0)) || ((hs == SSLEngineResult.HandshakeStatus.NEED_WRAP) && (bytesConsumed > 0)) ? 
      handshake() : mayFinishHandshake(hs != SSLEngineResult.HandshakeStatus.FINISHED ? getHandshakeStatus() : SSLEngineResult.HandshakeStatus.FINISHED);
  }
  
  private SSLEngineResult.HandshakeStatus mayFinishHandshake(SSLEngineResult.HandshakeStatus status) throws SSLException
  {
    if (status == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
      if (handshakeState != HandshakeState.FINISHED)
      {

        return handshake();
      }
      if ((!isDestroyed()) && (SSL.bioLengthNonApplication(networkBIO) > 0))
      {
        return SSLEngineResult.HandshakeStatus.NEED_WRAP;
      }
    }
    return status;
  }
  

  public final synchronized SSLEngineResult.HandshakeStatus getHandshakeStatus()
  {
    if (needPendingStatus()) {
      if (needTask)
      {
        return SSLEngineResult.HandshakeStatus.NEED_TASK;
      }
      return pendingStatus(SSL.bioLengthNonApplication(networkBIO));
    }
    return SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;
  }
  
  private SSLEngineResult.HandshakeStatus getHandshakeStatus(int pending)
  {
    if (needPendingStatus()) {
      if (needTask)
      {
        return SSLEngineResult.HandshakeStatus.NEED_TASK;
      }
      return pendingStatus(pending);
    }
    return SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;
  }
  
  private boolean needPendingStatus() {
    return (handshakeState != HandshakeState.NOT_STARTED) && (!isDestroyed()) && ((handshakeState != HandshakeState.FINISHED) || 
      (isInboundDone()) || (isOutboundDone()));
  }
  


  private String toJavaCipherSuite(String openSslCipherSuite)
  {
    if (openSslCipherSuite == null) {
      return null;
    }
    
    String version = SSL.getVersion(ssl);
    String prefix = toJavaCipherSuitePrefix(version);
    return CipherSuiteConverter.toJava(openSslCipherSuite, prefix);
  }
  

  private static String toJavaCipherSuitePrefix(String protocolVersion)
  {
    char c;
    char c;
    if ((protocolVersion == null) || (protocolVersion.isEmpty())) {
      c = '\000';
    } else {
      c = protocolVersion.charAt(0);
    }
    
    switch (c) {
    case 'T': 
      return "TLS";
    case 'S': 
      return "SSL";
    }
    return "UNKNOWN";
  }
  

  public final void setUseClientMode(boolean clientMode)
  {
    if (clientMode != this.clientMode) {
      throw new UnsupportedOperationException();
    }
  }
  
  public final boolean getUseClientMode()
  {
    return clientMode;
  }
  
  public final void setNeedClientAuth(boolean b)
  {
    setClientAuth(b ? ClientAuth.REQUIRE : ClientAuth.NONE);
  }
  
  public final boolean getNeedClientAuth()
  {
    return clientAuth == ClientAuth.REQUIRE;
  }
  
  public final void setWantClientAuth(boolean b)
  {
    setClientAuth(b ? ClientAuth.OPTIONAL : ClientAuth.NONE);
  }
  
  public final boolean getWantClientAuth()
  {
    return clientAuth == ClientAuth.OPTIONAL;
  }
  




  public final synchronized void setVerify(int verifyMode, int depth)
  {
    if (!isDestroyed()) {
      SSL.setVerify(ssl, verifyMode, depth);
    }
  }
  
  private void setClientAuth(ClientAuth mode) {
    if (clientMode) {
      return;
    }
    synchronized (this) {
      if (clientAuth == mode)
      {
        return;
      }
      if (!isDestroyed()) {
        switch (3.$SwitchMap$io$netty$handler$ssl$ClientAuth[mode.ordinal()]) {
        case 1: 
          SSL.setVerify(ssl, 0, 10);
          break;
        case 2: 
          SSL.setVerify(ssl, 2, 10);
          break;
        case 3: 
          SSL.setVerify(ssl, 1, 10);
          break;
        default: 
          throw new Error(mode.toString());
        }
      }
      clientAuth = mode;
    }
  }
  
  public final void setEnableSessionCreation(boolean b)
  {
    if (b) {
      throw new UnsupportedOperationException();
    }
  }
  
  public final boolean getEnableSessionCreation()
  {
    return false;
  }
  
  @SuppressJava6Requirement(reason="Usage guarded by java version check")
  public final synchronized SSLParameters getSSLParameters()
  {
    SSLParameters sslParameters = super.getSSLParameters();
    
    int version = PlatformDependent.javaVersion();
    if (version >= 7) {
      sslParameters.setEndpointIdentificationAlgorithm(endPointIdentificationAlgorithm);
      Java7SslParametersUtils.setAlgorithmConstraints(sslParameters, algorithmConstraints);
      if (version >= 8) {
        if (sniHostNames != null) {
          Java8SslUtils.setSniHostNames(sslParameters, sniHostNames);
        }
        if (!isDestroyed()) {
          Java8SslUtils.setUseCipherSuitesOrder(sslParameters, 
            (SSL.getOptions(ssl) & SSL.SSL_OP_CIPHER_SERVER_PREFERENCE) != 0);
        }
        
        Java8SslUtils.setSNIMatchers(sslParameters, matchers);
      }
    }
    return sslParameters;
  }
  
  @SuppressJava6Requirement(reason="Usage guarded by java version check")
  public final synchronized void setSSLParameters(SSLParameters sslParameters)
  {
    int version = PlatformDependent.javaVersion();
    if (version >= 7) {
      if (sslParameters.getAlgorithmConstraints() != null) {
        throw new IllegalArgumentException("AlgorithmConstraints are not supported.");
      }
      
      boolean isDestroyed = isDestroyed();
      if (version >= 8) {
        if (!isDestroyed) {
          if (clientMode) {
            List<String> sniHostNames = Java8SslUtils.getSniHostNames(sslParameters);
            for (String name : sniHostNames) {
              SSL.setTlsExtHostName(ssl, name);
            }
            this.sniHostNames = sniHostNames;
          }
          if (Java8SslUtils.getUseCipherSuitesOrder(sslParameters)) {
            SSL.setOptions(ssl, SSL.SSL_OP_CIPHER_SERVER_PREFERENCE);
          } else {
            SSL.clearOptions(ssl, SSL.SSL_OP_CIPHER_SERVER_PREFERENCE);
          }
        }
        matchers = sslParameters.getSNIMatchers();
      }
      
      String endPointIdentificationAlgorithm = sslParameters.getEndpointIdentificationAlgorithm();
      if (!isDestroyed)
      {

        if ((clientMode) && (isEndPointVerificationEnabled(endPointIdentificationAlgorithm))) {
          SSL.setVerify(ssl, 2, -1);
        }
      }
      this.endPointIdentificationAlgorithm = endPointIdentificationAlgorithm;
      algorithmConstraints = sslParameters.getAlgorithmConstraints();
    }
    super.setSSLParameters(sslParameters);
  }
  
  private static boolean isEndPointVerificationEnabled(String endPointIdentificationAlgorithm) {
    return (endPointIdentificationAlgorithm != null) && (!endPointIdentificationAlgorithm.isEmpty());
  }
  
  private boolean isDestroyed() {
    return destroyed;
  }
  
  final boolean checkSniHostnameMatch(byte[] hostname) {
    return Java8SslUtils.checkSniHostnameMatch(matchers, hostname);
  }
  
  public String getNegotiatedApplicationProtocol()
  {
    return applicationProtocol;
  }
  
  private static long bufferAddress(ByteBuffer b) {
    assert (b.isDirect());
    if (PlatformDependent.hasUnsafe()) {
      return PlatformDependent.directBufferAddress(b);
    }
    return Buffer.address(b);
  }
  

  private void selectApplicationProtocol()
    throws SSLException
  {
    ApplicationProtocolConfig.SelectedListenerFailureBehavior behavior = apn.selectedListenerFailureBehavior();
    List<String> protocols = apn.protocols();
    
    switch (3.$SwitchMap$io$netty$handler$ssl$ApplicationProtocolConfig$Protocol[apn.protocol().ordinal()])
    {
    case 1: 
      break;
    
    case 2: 
      String applicationProtocol = SSL.getAlpnSelected(ssl);
      if (applicationProtocol != null) {
        this.applicationProtocol = selectApplicationProtocol(protocols, behavior, applicationProtocol);
      }
      
      break;
    case 3: 
      String applicationProtocol = SSL.getNextProtoNegotiated(ssl);
      if (applicationProtocol != null) {
        this.applicationProtocol = selectApplicationProtocol(protocols, behavior, applicationProtocol);
      }
      
      break;
    case 4: 
      String applicationProtocol = SSL.getAlpnSelected(ssl);
      if (applicationProtocol == null) {
        applicationProtocol = SSL.getNextProtoNegotiated(ssl);
      }
      if (applicationProtocol != null) {
        this.applicationProtocol = selectApplicationProtocol(protocols, behavior, applicationProtocol);
      }
      
      break;
    default: 
      throw new Error();
    }
  }
  
  private String selectApplicationProtocol(List<String> protocols, ApplicationProtocolConfig.SelectedListenerFailureBehavior behavior, String applicationProtocol)
    throws SSLException
  {
    if (behavior == ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT) {
      return applicationProtocol;
    }
    int size = protocols.size();
    assert (size > 0);
    if (protocols.contains(applicationProtocol)) {
      return applicationProtocol;
    }
    if (behavior == ApplicationProtocolConfig.SelectedListenerFailureBehavior.CHOOSE_MY_LAST_PROTOCOL) {
      return (String)protocols.get(size - 1);
    }
    throw new SSLException("unknown protocol " + applicationProtocol);
  }
  


  final void setSessionId(OpenSslSessionId id)
  {
    session.setSessionId(id);
  }
  

  private final class DefaultOpenSslSession
    implements OpenSslSession
  {
    private final OpenSslSessionContext sessionContext;
    
    private javax.security.cert.X509Certificate[] x509PeerCerts;
    private Certificate[] peerCerts;
    private boolean valid = true;
    private String protocol;
    private String cipher;
    private OpenSslSessionId id = OpenSslSessionId.NULL_ID;
    private volatile long creationTime;
    private volatile int applicationBufferSize = ReferenceCountedOpenSslEngine.MAX_PLAINTEXT_LENGTH;
    private volatile Certificate[] localCertificateChain;
    private Map<String, Object> values;
    
    DefaultOpenSslSession(OpenSslSessionContext sessionContext)
    {
      this.sessionContext = sessionContext;
    }
    
    private SSLSessionBindingEvent newSSLSessionBindingEvent(String name) {
      return new SSLSessionBindingEvent(session, name);
    }
    
    public void setSessionId(OpenSslSessionId sessionId)
    {
      synchronized (ReferenceCountedOpenSslEngine.this) {
        if (id == OpenSslSessionId.NULL_ID) {
          id = sessionId;
          creationTime = System.currentTimeMillis();
        }
      }
    }
    
    public OpenSslSessionId sessionId()
    {
      synchronized (ReferenceCountedOpenSslEngine.this) {
        if ((id == OpenSslSessionId.NULL_ID) && (!ReferenceCountedOpenSslEngine.this.isDestroyed())) {
          byte[] sessionId = SSL.getSessionId(ssl);
          if (sessionId != null) {
            id = new OpenSslSessionId(sessionId);
          }
        }
        
        return id;
      }
    }
    
    public void setLocalCertificate(Certificate[] localCertificate)
    {
      localCertificateChain = localCertificate;
    }
    
    public byte[] getId()
    {
      return sessionId().cloneBytes();
    }
    
    public OpenSslSessionContext getSessionContext()
    {
      return sessionContext;
    }
    
    public long getCreationTime()
    {
      synchronized (ReferenceCountedOpenSslEngine.this) {
        return creationTime;
      }
    }
    
    public long getLastAccessedTime()
    {
      long lastAccessed = ReferenceCountedOpenSslEngine.this.lastAccessed;
      
      return lastAccessed == -1L ? getCreationTime() : lastAccessed;
    }
    
    public void invalidate()
    {
      synchronized (ReferenceCountedOpenSslEngine.this) {
        valid = false;
        sessionContext.removeFromCache(id);
      }
    }
    
    public boolean isValid()
    {
      synchronized (ReferenceCountedOpenSslEngine.this) {
        return (valid) || (sessionContext.isInCache(id));
      }
    }
    
    public void putValue(String name, Object value)
    {
      ObjectUtil.checkNotNull(name, "name");
      ObjectUtil.checkNotNull(value, "value");
      
      Object old;
      synchronized (this) {
        Map<String, Object> values = this.values;
        if (values == null)
        {
          values = this.values = new HashMap(2);
        }
        old = values.put(name, value);
      }
      Object old;
      if ((value instanceof SSLSessionBindingListener))
      {
        ((SSLSessionBindingListener)value).valueBound(newSSLSessionBindingEvent(name));
      }
      notifyUnbound(old, name);
    }
    
    public Object getValue(String name)
    {
      ObjectUtil.checkNotNull(name, "name");
      synchronized (this) {
        if (values == null) {
          return null;
        }
        return values.get(name);
      }
    }
    
    public void removeValue(String name)
    {
      ObjectUtil.checkNotNull(name, "name");
      
      Object old;
      synchronized (this) {
        Map<String, Object> values = this.values;
        if (values == null) {
          return;
        }
        old = values.remove(name);
      }
      Object old;
      notifyUnbound(old, name);
    }
    
    public String[] getValueNames()
    {
      synchronized (this) {
        Map<String, Object> values = this.values;
        if ((values == null) || (values.isEmpty())) {
          return EmptyArrays.EMPTY_STRINGS;
        }
        return (String[])values.keySet().toArray(new String[0]);
      }
    }
    
    private void notifyUnbound(Object value, String name) {
      if ((value instanceof SSLSessionBindingListener))
      {
        ((SSLSessionBindingListener)value).valueUnbound(newSSLSessionBindingEvent(name));
      }
    }
    





    public void handshakeFinished(byte[] id, String cipher, String protocol, byte[] peerCertificate, byte[][] peerCertificateChain, long creationTime, long timeout)
      throws SSLException
    {
      synchronized (ReferenceCountedOpenSslEngine.this) {
        if (!ReferenceCountedOpenSslEngine.this.isDestroyed()) {
          this.creationTime = creationTime;
          if (this.id == OpenSslSessionId.NULL_ID) {
            this.id = (id == null ? OpenSslSessionId.NULL_ID : new OpenSslSessionId(id));
          }
          this.cipher = ReferenceCountedOpenSslEngine.this.toJavaCipherSuite(cipher);
          this.protocol = protocol;
          
          if (clientMode) {
            if (ReferenceCountedOpenSslEngine.isEmpty(peerCertificateChain)) {
              peerCerts = EmptyArrays.EMPTY_CERTIFICATES;
              x509PeerCerts = EmptyArrays.EMPTY_JAVAX_X509_CERTIFICATES;
            } else {
              peerCerts = new Certificate[peerCertificateChain.length];
              x509PeerCerts = new javax.security.cert.X509Certificate[peerCertificateChain.length];
              initCerts(peerCertificateChain, 0);


            }
            


          }
          else if (ReferenceCountedOpenSslEngine.isEmpty(peerCertificate)) {
            peerCerts = EmptyArrays.EMPTY_CERTIFICATES;
            x509PeerCerts = EmptyArrays.EMPTY_JAVAX_X509_CERTIFICATES;
          }
          else if (ReferenceCountedOpenSslEngine.isEmpty(peerCertificateChain)) {
            peerCerts = new Certificate[] { new LazyX509Certificate(peerCertificate) };
            x509PeerCerts = new javax.security.cert.X509Certificate[] { new LazyJavaxX509Certificate(peerCertificate) };
          } else {
            peerCerts = new Certificate[peerCertificateChain.length + 1];
            x509PeerCerts = new javax.security.cert.X509Certificate[peerCertificateChain.length + 1];
            peerCerts[0] = new LazyX509Certificate(peerCertificate);
            x509PeerCerts[0] = new LazyJavaxX509Certificate(peerCertificate);
            initCerts(peerCertificateChain, 1);
          }
          


          ReferenceCountedOpenSslEngine.this.calculateMaxWrapOverhead();
          
          handshakeState = ReferenceCountedOpenSslEngine.HandshakeState.FINISHED;
        } else {
          throw new SSLException("Already closed");
        }
      }
    }
    
    private void initCerts(byte[][] chain, int startPos) {
      for (int i = 0; i < chain.length; i++) {
        int certPos = startPos + i;
        peerCerts[certPos] = new LazyX509Certificate(chain[i]);
        x509PeerCerts[certPos] = new LazyJavaxX509Certificate(chain[i]);
      }
    }
    
    public Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException
    {
      synchronized (ReferenceCountedOpenSslEngine.this) {
        if (ReferenceCountedOpenSslEngine.isEmpty(peerCerts)) {
          throw new SSLPeerUnverifiedException("peer not verified");
        }
        return (Certificate[])peerCerts.clone();
      }
    }
    
    public Certificate[] getLocalCertificates()
    {
      Certificate[] localCerts = localCertificateChain;
      if (localCerts == null) {
        return null;
      }
      return (Certificate[])localCerts.clone();
    }
    
    public javax.security.cert.X509Certificate[] getPeerCertificateChain() throws SSLPeerUnverifiedException
    {
      synchronized (ReferenceCountedOpenSslEngine.this) {
        if (ReferenceCountedOpenSslEngine.isEmpty(x509PeerCerts)) {
          throw new SSLPeerUnverifiedException("peer not verified");
        }
        return (javax.security.cert.X509Certificate[])x509PeerCerts.clone();
      }
    }
    
    public Principal getPeerPrincipal() throws SSLPeerUnverifiedException
    {
      Certificate[] peer = getPeerCertificates();
      

      return ((java.security.cert.X509Certificate)peer[0]).getSubjectX500Principal();
    }
    
    public Principal getLocalPrincipal()
    {
      Certificate[] local = localCertificateChain;
      if ((local == null) || (local.length == 0)) {
        return null;
      }
      return ((java.security.cert.X509Certificate)local[0]).getIssuerX500Principal();
    }
    
    public String getCipherSuite()
    {
      synchronized (ReferenceCountedOpenSslEngine.this) {
        if (cipher == null) {
          return "SSL_NULL_WITH_NULL_NULL";
        }
        return cipher;
      }
    }
    
    public String getProtocol()
    {
      String protocol = this.protocol;
      if (protocol == null) {
        synchronized (ReferenceCountedOpenSslEngine.this) {
          if (!ReferenceCountedOpenSslEngine.this.isDestroyed()) {
            protocol = SSL.getVersion(ssl);
          } else {
            protocol = "";
          }
        }
      }
      return protocol;
    }
    
    public String getPeerHost()
    {
      return ReferenceCountedOpenSslEngine.this.getPeerHost();
    }
    
    public int getPeerPort()
    {
      return ReferenceCountedOpenSslEngine.this.getPeerPort();
    }
    
    public int getPacketBufferSize()
    {
      return maxEncryptedPacketLength();
    }
    
    public int getApplicationBufferSize()
    {
      return applicationBufferSize;
    }
    
    public void tryExpandApplicationBufferSize(int packetLengthDataOnly)
    {
      if ((packetLengthDataOnly > ReferenceCountedOpenSslEngine.MAX_PLAINTEXT_LENGTH) && (applicationBufferSize != ReferenceCountedOpenSslEngine.MAX_RECORD_SIZE)) {
        applicationBufferSize = ReferenceCountedOpenSslEngine.MAX_RECORD_SIZE;
      }
    }
    
    public String toString()
    {
      return "DefaultOpenSslSession{sessionContext=" + sessionContext + ", id=" + id + '}';
    }
  }
}
