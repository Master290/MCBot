package io.netty.handler.ssl;

import io.netty.internal.tcnative.SSL;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.IllegalReferenceCountException;
import io.netty.util.internal.EmptyArrays;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;















final class OpenSslPrivateKey
  extends AbstractReferenceCounted
  implements PrivateKey
{
  private long privateKeyAddress;
  
  OpenSslPrivateKey(long privateKeyAddress)
  {
    this.privateKeyAddress = privateKeyAddress;
  }
  
  public String getAlgorithm()
  {
    return "unknown";
  }
  

  public String getFormat()
  {
    return null;
  }
  
  public byte[] getEncoded()
  {
    return null;
  }
  
  private long privateKeyAddress() {
    if (refCnt() <= 0) {
      throw new IllegalReferenceCountException();
    }
    return privateKeyAddress;
  }
  
  protected void deallocate()
  {
    SSL.freePrivateKey(privateKeyAddress);
    privateKeyAddress = 0L;
  }
  
  public OpenSslPrivateKey retain()
  {
    super.retain();
    return this;
  }
  
  public OpenSslPrivateKey retain(int increment)
  {
    super.retain(increment);
    return this;
  }
  
  public OpenSslPrivateKey touch()
  {
    super.touch();
    return this;
  }
  
  public OpenSslPrivateKey touch(Object hint)
  {
    return this;
  }
  







  public void destroy()
  {
    release(refCnt());
  }
  







  public boolean isDestroyed()
  {
    return refCnt() == 0;
  }
  





  OpenSslKeyMaterial newKeyMaterial(long certificateChain, X509Certificate[] chain)
  {
    return new OpenSslPrivateKeyMaterial(certificateChain, chain);
  }
  
  final class OpenSslPrivateKeyMaterial
    extends AbstractReferenceCounted implements OpenSslKeyMaterial
  {
    long certificateChain;
    private final X509Certificate[] x509CertificateChain;
    
    OpenSslPrivateKeyMaterial(long certificateChain, X509Certificate[] x509CertificateChain)
    {
      this.certificateChain = certificateChain;
      this.x509CertificateChain = (x509CertificateChain == null ? EmptyArrays.EMPTY_X509_CERTIFICATES : x509CertificateChain);
      
      retain();
    }
    
    public X509Certificate[] certificateChain()
    {
      return (X509Certificate[])x509CertificateChain.clone();
    }
    
    public long certificateChainAddress()
    {
      if (refCnt() <= 0) {
        throw new IllegalReferenceCountException();
      }
      return certificateChain;
    }
    
    public long privateKeyAddress()
    {
      if (refCnt() <= 0) {
        throw new IllegalReferenceCountException();
      }
      return OpenSslPrivateKey.this.privateKeyAddress();
    }
    
    public OpenSslKeyMaterial touch(Object hint)
    {
      touch(hint);
      return this;
    }
    
    public OpenSslKeyMaterial retain()
    {
      super.retain();
      return this;
    }
    
    public OpenSslKeyMaterial retain(int increment)
    {
      super.retain(increment);
      return this;
    }
    
    public OpenSslKeyMaterial touch()
    {
      touch();
      return this;
    }
    
    protected void deallocate()
    {
      releaseChain();
      release();
    }
    
    private void releaseChain() {
      SSL.freeX509Chain(certificateChain);
      certificateChain = 0L;
    }
  }
}
