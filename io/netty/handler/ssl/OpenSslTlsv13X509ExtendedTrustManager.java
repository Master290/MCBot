package io.netty.handler.ssl;

import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.SuppressJava6Requirement;
import java.net.Socket;
import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.List;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.X509ExtendedTrustManager;






















@SuppressJava6Requirement(reason="Usage guarded by java version check")
final class OpenSslTlsv13X509ExtendedTrustManager
  extends X509ExtendedTrustManager
{
  private final X509ExtendedTrustManager tm;
  
  private OpenSslTlsv13X509ExtendedTrustManager(X509ExtendedTrustManager tm)
  {
    this.tm = tm;
  }
  
  static X509ExtendedTrustManager wrap(X509ExtendedTrustManager tm) {
    if ((!SslProvider.isTlsv13Supported(SslProvider.JDK)) && (SslProvider.isTlsv13Supported(SslProvider.OPENSSL))) {
      return new OpenSslTlsv13X509ExtendedTrustManager(tm);
    }
    return tm;
  }
  
  public void checkClientTrusted(java.security.cert.X509Certificate[] x509Certificates, String s, Socket socket)
    throws CertificateException
  {
    tm.checkClientTrusted(x509Certificates, s, socket);
  }
  
  public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates, String s, Socket socket)
    throws CertificateException
  {
    tm.checkServerTrusted(x509Certificates, s, socket);
  }
  
  private static SSLEngine wrapEngine(final SSLEngine engine) {
    final SSLSession session = engine.getHandshakeSession();
    if ((session != null) && ("TLSv1.3".equals(session.getProtocol()))) {
      new JdkSslEngine(engine)
      {
        public String getNegotiatedApplicationProtocol() {
          if ((engine instanceof ApplicationProtocolAccessor)) {
            return ((ApplicationProtocolAccessor)engine).getNegotiatedApplicationProtocol();
          }
          return super.getNegotiatedApplicationProtocol();
        }
        
        public SSLSession getHandshakeSession()
        {
          if ((PlatformDependent.javaVersion() >= 7) && ((session instanceof ExtendedOpenSslSession))) {
            final ExtendedOpenSslSession extendedOpenSslSession = (ExtendedOpenSslSession)session;
            new ExtendedOpenSslSession(extendedOpenSslSession)
            {
              public List getRequestedServerNames() {
                return extendedOpenSslSession.getRequestedServerNames();
              }
              
              public String[] getPeerSupportedSignatureAlgorithms()
              {
                return extendedOpenSslSession.getPeerSupportedSignatureAlgorithms();
              }
              
              public String getProtocol()
              {
                return "TLSv1.2";
              }
            };
          }
          new SSLSession()
          {
            public byte[] getId() {
              return val$session.getId();
            }
            
            public SSLSessionContext getSessionContext()
            {
              return val$session.getSessionContext();
            }
            
            public long getCreationTime()
            {
              return val$session.getCreationTime();
            }
            
            public long getLastAccessedTime()
            {
              return val$session.getLastAccessedTime();
            }
            
            public void invalidate()
            {
              val$session.invalidate();
            }
            
            public boolean isValid()
            {
              return val$session.isValid();
            }
            
            public void putValue(String s, Object o)
            {
              val$session.putValue(s, o);
            }
            
            public Object getValue(String s)
            {
              return val$session.getValue(s);
            }
            
            public void removeValue(String s)
            {
              val$session.removeValue(s);
            }
            
            public String[] getValueNames()
            {
              return val$session.getValueNames();
            }
            
            public Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException
            {
              return val$session.getPeerCertificates();
            }
            
            public Certificate[] getLocalCertificates()
            {
              return val$session.getLocalCertificates();
            }
            
            public javax.security.cert.X509Certificate[] getPeerCertificateChain()
              throws SSLPeerUnverifiedException
            {
              return val$session.getPeerCertificateChain();
            }
            
            public Principal getPeerPrincipal() throws SSLPeerUnverifiedException
            {
              return val$session.getPeerPrincipal();
            }
            
            public Principal getLocalPrincipal()
            {
              return val$session.getLocalPrincipal();
            }
            
            public String getCipherSuite()
            {
              return val$session.getCipherSuite();
            }
            
            public String getProtocol()
            {
              return "TLSv1.2";
            }
            
            public String getPeerHost()
            {
              return val$session.getPeerHost();
            }
            
            public int getPeerPort()
            {
              return val$session.getPeerPort();
            }
            
            public int getPacketBufferSize()
            {
              return val$session.getPacketBufferSize();
            }
            
            public int getApplicationBufferSize()
            {
              return val$session.getApplicationBufferSize();
            }
          };
        }
      };
    }
    
    return engine;
  }
  
  public void checkClientTrusted(java.security.cert.X509Certificate[] x509Certificates, String s, SSLEngine sslEngine)
    throws CertificateException
  {
    tm.checkClientTrusted(x509Certificates, s, wrapEngine(sslEngine));
  }
  
  public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates, String s, SSLEngine sslEngine)
    throws CertificateException
  {
    tm.checkServerTrusted(x509Certificates, s, wrapEngine(sslEngine));
  }
  
  public void checkClientTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException
  {
    tm.checkClientTrusted(x509Certificates, s);
  }
  
  public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException
  {
    tm.checkServerTrusted(x509Certificates, s);
  }
  
  public java.security.cert.X509Certificate[] getAcceptedIssuers()
  {
    return tm.getAcceptedIssuers();
  }
}
