package io.netty.handler.ssl;

import java.security.cert.Certificate;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

abstract interface OpenSslSession
  extends SSLSession
{
  public abstract OpenSslSessionId sessionId();
  
  public abstract void setLocalCertificate(Certificate[] paramArrayOfCertificate);
  
  public abstract void setSessionId(OpenSslSessionId paramOpenSslSessionId);
  
  public abstract OpenSslSessionContext getSessionContext();
  
  public abstract void tryExpandApplicationBufferSize(int paramInt);
  
  public abstract void handshakeFinished(byte[] paramArrayOfByte1, String paramString1, String paramString2, byte[] paramArrayOfByte2, byte[][] paramArrayOfByte, long paramLong1, long paramLong2)
    throws SSLException;
}
