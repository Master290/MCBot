package io.netty.handler.ssl;

import io.netty.util.internal.EmptyArrays;
import io.netty.util.internal.SuppressJava6Requirement;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.List;
import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSessionBindingEvent;
import javax.net.ssl.SSLSessionBindingListener;
import javax.security.cert.X509Certificate;






















@SuppressJava6Requirement(reason="Usage guarded by java version check")
abstract class ExtendedOpenSslSession
  extends ExtendedSSLSession
  implements OpenSslSession
{
  private static final String[] LOCAL_SUPPORTED_SIGNATURE_ALGORITHMS = { "SHA512withRSA", "SHA512withECDSA", "SHA384withRSA", "SHA384withECDSA", "SHA256withRSA", "SHA256withECDSA", "SHA224withRSA", "SHA224withECDSA", "SHA1withRSA", "SHA1withECDSA" };
  

  private final OpenSslSession wrapped;
  

  ExtendedOpenSslSession(OpenSslSession wrapped)
  {
    this.wrapped = wrapped;
  }
  



  public abstract List getRequestedServerNames();
  


  public List<byte[]> getStatusResponses()
  {
    return Collections.emptyList();
  }
  
  public OpenSslSessionId sessionId()
  {
    return wrapped.sessionId();
  }
  
  public void setSessionId(OpenSslSessionId id)
  {
    wrapped.setSessionId(id);
  }
  
  public final void setLocalCertificate(Certificate[] localCertificate)
  {
    wrapped.setLocalCertificate(localCertificate);
  }
  
  public String[] getPeerSupportedSignatureAlgorithms()
  {
    return EmptyArrays.EMPTY_STRINGS;
  }
  
  public final void tryExpandApplicationBufferSize(int packetLengthDataOnly)
  {
    wrapped.tryExpandApplicationBufferSize(packetLengthDataOnly);
  }
  
  public final String[] getLocalSupportedSignatureAlgorithms()
  {
    return (String[])LOCAL_SUPPORTED_SIGNATURE_ALGORITHMS.clone();
  }
  
  public final byte[] getId()
  {
    return wrapped.getId();
  }
  
  public final OpenSslSessionContext getSessionContext()
  {
    return wrapped.getSessionContext();
  }
  
  public final long getCreationTime()
  {
    return wrapped.getCreationTime();
  }
  
  public final long getLastAccessedTime()
  {
    return wrapped.getLastAccessedTime();
  }
  
  public final void invalidate()
  {
    wrapped.invalidate();
  }
  
  public final boolean isValid()
  {
    return wrapped.isValid();
  }
  
  public final void putValue(String name, Object value)
  {
    if ((value instanceof SSLSessionBindingListener))
    {
      value = new SSLSessionBindingListenerDecorator((SSLSessionBindingListener)value);
    }
    wrapped.putValue(name, value);
  }
  
  public final Object getValue(String s)
  {
    Object value = wrapped.getValue(s);
    if ((value instanceof SSLSessionBindingListenerDecorator))
    {
      return delegate;
    }
    return value;
  }
  
  public final void removeValue(String s)
  {
    wrapped.removeValue(s);
  }
  
  public final String[] getValueNames()
  {
    return wrapped.getValueNames();
  }
  
  public final Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException
  {
    return wrapped.getPeerCertificates();
  }
  
  public final Certificate[] getLocalCertificates()
  {
    return wrapped.getLocalCertificates();
  }
  
  public final X509Certificate[] getPeerCertificateChain() throws SSLPeerUnverifiedException
  {
    return wrapped.getPeerCertificateChain();
  }
  
  public final Principal getPeerPrincipal() throws SSLPeerUnverifiedException
  {
    return wrapped.getPeerPrincipal();
  }
  
  public final Principal getLocalPrincipal()
  {
    return wrapped.getLocalPrincipal();
  }
  
  public final String getCipherSuite()
  {
    return wrapped.getCipherSuite();
  }
  
  public String getProtocol()
  {
    return wrapped.getProtocol();
  }
  
  public final String getPeerHost()
  {
    return wrapped.getPeerHost();
  }
  
  public final int getPeerPort()
  {
    return wrapped.getPeerPort();
  }
  
  public final int getPacketBufferSize()
  {
    return wrapped.getPacketBufferSize();
  }
  
  public final int getApplicationBufferSize()
  {
    return wrapped.getApplicationBufferSize();
  }
  
  private final class SSLSessionBindingListenerDecorator implements SSLSessionBindingListener
  {
    final SSLSessionBindingListener delegate;
    
    SSLSessionBindingListenerDecorator(SSLSessionBindingListener delegate) {
      this.delegate = delegate;
    }
    
    public void valueBound(SSLSessionBindingEvent event)
    {
      delegate.valueBound(new SSLSessionBindingEvent(ExtendedOpenSslSession.this, event.getName()));
    }
    
    public void valueUnbound(SSLSessionBindingEvent event)
    {
      delegate.valueUnbound(new SSLSessionBindingEvent(ExtendedOpenSslSession.this, event.getName()));
    }
  }
  
  public void handshakeFinished(byte[] id, String cipher, String protocol, byte[] peerCertificate, byte[][] peerCertificateChain, long creationTime, long timeout)
    throws SSLException
  {
    wrapped.handshakeFinished(id, cipher, protocol, peerCertificate, peerCertificateChain, creationTime, timeout);
  }
  
  public String toString()
  {
    return "ExtendedOpenSslSession{wrapped=" + wrapped + '}';
  }
}
