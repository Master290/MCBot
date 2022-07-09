package io.netty.handler.ssl;

import io.netty.internal.tcnative.SSL;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.IllegalReferenceCountException;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetectorFactory;
import io.netty.util.ResourceLeakTracker;
import java.security.cert.X509Certificate;
















final class DefaultOpenSslKeyMaterial
  extends AbstractReferenceCounted
  implements OpenSslKeyMaterial
{
  private static final ResourceLeakDetector<DefaultOpenSslKeyMaterial> leakDetector = ResourceLeakDetectorFactory.instance().newResourceLeakDetector(DefaultOpenSslKeyMaterial.class);
  private final ResourceLeakTracker<DefaultOpenSslKeyMaterial> leak;
  private final X509Certificate[] x509CertificateChain;
  private long chain;
  private long privateKey;
  
  DefaultOpenSslKeyMaterial(long chain, long privateKey, X509Certificate[] x509CertificateChain) {
    this.chain = chain;
    this.privateKey = privateKey;
    this.x509CertificateChain = x509CertificateChain;
    leak = leakDetector.track(this);
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
    return chain;
  }
  
  public long privateKeyAddress()
  {
    if (refCnt() <= 0) {
      throw new IllegalReferenceCountException();
    }
    return privateKey;
  }
  
  protected void deallocate()
  {
    SSL.freeX509Chain(chain);
    chain = 0L;
    SSL.freePrivateKey(privateKey);
    privateKey = 0L;
    if (leak != null) {
      boolean closed = leak.close(this);
      assert (closed);
    }
  }
  
  public DefaultOpenSslKeyMaterial retain()
  {
    if (leak != null) {
      leak.record();
    }
    super.retain();
    return this;
  }
  
  public DefaultOpenSslKeyMaterial retain(int increment)
  {
    if (leak != null) {
      leak.record();
    }
    super.retain(increment);
    return this;
  }
  
  public DefaultOpenSslKeyMaterial touch()
  {
    if (leak != null) {
      leak.record();
    }
    super.touch();
    return this;
  }
  
  public DefaultOpenSslKeyMaterial touch(Object hint)
  {
    if (leak != null) {
      leak.record(hint);
    }
    return this;
  }
  
  public boolean release()
  {
    if (leak != null) {
      leak.record();
    }
    return super.release();
  }
  
  public boolean release(int decrement)
  {
    if (leak != null) {
      leak.record();
    }
    return super.release(decrement);
  }
}
