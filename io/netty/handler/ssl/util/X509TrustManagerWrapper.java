package io.netty.handler.ssl.util;

import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.SuppressJava6Requirement;
import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;
















@SuppressJava6Requirement(reason="Usage guarded by java version check")
final class X509TrustManagerWrapper
  extends X509ExtendedTrustManager
{
  private final X509TrustManager delegate;
  
  X509TrustManagerWrapper(X509TrustManager delegate)
  {
    this.delegate = ((X509TrustManager)ObjectUtil.checkNotNull(delegate, "delegate"));
  }
  
  public void checkClientTrusted(X509Certificate[] chain, String s) throws CertificateException
  {
    delegate.checkClientTrusted(chain, s);
  }
  
  public void checkClientTrusted(X509Certificate[] chain, String s, Socket socket)
    throws CertificateException
  {
    delegate.checkClientTrusted(chain, s);
  }
  
  public void checkClientTrusted(X509Certificate[] chain, String s, SSLEngine sslEngine)
    throws CertificateException
  {
    delegate.checkClientTrusted(chain, s);
  }
  
  public void checkServerTrusted(X509Certificate[] chain, String s) throws CertificateException
  {
    delegate.checkServerTrusted(chain, s);
  }
  
  public void checkServerTrusted(X509Certificate[] chain, String s, Socket socket)
    throws CertificateException
  {
    delegate.checkServerTrusted(chain, s);
  }
  
  public void checkServerTrusted(X509Certificate[] chain, String s, SSLEngine sslEngine)
    throws CertificateException
  {
    delegate.checkServerTrusted(chain, s);
  }
  
  public X509Certificate[] getAcceptedIssuers()
  {
    return delegate.getAcceptedIssuers();
  }
}
