package io.netty.handler.ssl;

import io.netty.util.internal.SuppressJava6Requirement;
import java.nio.ByteBuffer;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;














class JdkSslEngine
  extends SSLEngine
  implements ApplicationProtocolAccessor
{
  private final SSLEngine engine;
  private volatile String applicationProtocol;
  
  JdkSslEngine(SSLEngine engine)
  {
    this.engine = engine;
  }
  
  public String getNegotiatedApplicationProtocol()
  {
    return applicationProtocol;
  }
  
  void setNegotiatedApplicationProtocol(String applicationProtocol) {
    this.applicationProtocol = applicationProtocol;
  }
  
  public SSLSession getSession()
  {
    return engine.getSession();
  }
  
  public SSLEngine getWrappedEngine() {
    return engine;
  }
  
  public void closeInbound() throws SSLException
  {
    engine.closeInbound();
  }
  
  public void closeOutbound()
  {
    engine.closeOutbound();
  }
  
  public String getPeerHost()
  {
    return engine.getPeerHost();
  }
  
  public int getPeerPort()
  {
    return engine.getPeerPort();
  }
  
  public SSLEngineResult wrap(ByteBuffer byteBuffer, ByteBuffer byteBuffer2) throws SSLException
  {
    return engine.wrap(byteBuffer, byteBuffer2);
  }
  
  public SSLEngineResult wrap(ByteBuffer[] byteBuffers, ByteBuffer byteBuffer) throws SSLException
  {
    return engine.wrap(byteBuffers, byteBuffer);
  }
  
  public SSLEngineResult wrap(ByteBuffer[] byteBuffers, int i, int i2, ByteBuffer byteBuffer) throws SSLException
  {
    return engine.wrap(byteBuffers, i, i2, byteBuffer);
  }
  
  public SSLEngineResult unwrap(ByteBuffer byteBuffer, ByteBuffer byteBuffer2) throws SSLException
  {
    return engine.unwrap(byteBuffer, byteBuffer2);
  }
  
  public SSLEngineResult unwrap(ByteBuffer byteBuffer, ByteBuffer[] byteBuffers) throws SSLException
  {
    return engine.unwrap(byteBuffer, byteBuffers);
  }
  
  public SSLEngineResult unwrap(ByteBuffer byteBuffer, ByteBuffer[] byteBuffers, int i, int i2) throws SSLException
  {
    return engine.unwrap(byteBuffer, byteBuffers, i, i2);
  }
  
  public Runnable getDelegatedTask()
  {
    return engine.getDelegatedTask();
  }
  
  public boolean isInboundDone()
  {
    return engine.isInboundDone();
  }
  
  public boolean isOutboundDone()
  {
    return engine.isOutboundDone();
  }
  
  public String[] getSupportedCipherSuites()
  {
    return engine.getSupportedCipherSuites();
  }
  
  public String[] getEnabledCipherSuites()
  {
    return engine.getEnabledCipherSuites();
  }
  
  public void setEnabledCipherSuites(String[] strings)
  {
    engine.setEnabledCipherSuites(strings);
  }
  
  public String[] getSupportedProtocols()
  {
    return engine.getSupportedProtocols();
  }
  
  public String[] getEnabledProtocols()
  {
    return engine.getEnabledProtocols();
  }
  
  public void setEnabledProtocols(String[] strings)
  {
    engine.setEnabledProtocols(strings);
  }
  
  @SuppressJava6Requirement(reason="Can only be called when running on JDK7+")
  public SSLSession getHandshakeSession()
  {
    return engine.getHandshakeSession();
  }
  
  public void beginHandshake() throws SSLException
  {
    engine.beginHandshake();
  }
  
  public SSLEngineResult.HandshakeStatus getHandshakeStatus()
  {
    return engine.getHandshakeStatus();
  }
  
  public void setUseClientMode(boolean b)
  {
    engine.setUseClientMode(b);
  }
  
  public boolean getUseClientMode()
  {
    return engine.getUseClientMode();
  }
  
  public void setNeedClientAuth(boolean b)
  {
    engine.setNeedClientAuth(b);
  }
  
  public boolean getNeedClientAuth()
  {
    return engine.getNeedClientAuth();
  }
  
  public void setWantClientAuth(boolean b)
  {
    engine.setWantClientAuth(b);
  }
  
  public boolean getWantClientAuth()
  {
    return engine.getWantClientAuth();
  }
  
  public void setEnableSessionCreation(boolean b)
  {
    engine.setEnableSessionCreation(b);
  }
  
  public boolean getEnableSessionCreation()
  {
    return engine.getEnableSessionCreation();
  }
  
  public SSLParameters getSSLParameters()
  {
    return engine.getSSLParameters();
  }
  
  public void setSSLParameters(SSLParameters sslParameters)
  {
    engine.setSSLParameters(sslParameters);
  }
}
