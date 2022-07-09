package io.netty.handler.ssl;

import io.netty.util.ReferenceCounted;
import java.security.cert.X509Certificate;

abstract interface OpenSslKeyMaterial
  extends ReferenceCounted
{
  public abstract X509Certificate[] certificateChain();
  
  public abstract long certificateChainAddress();
  
  public abstract long privateKeyAddress();
  
  public abstract OpenSslKeyMaterial retain();
  
  public abstract OpenSslKeyMaterial retain(int paramInt);
  
  public abstract OpenSslKeyMaterial touch();
  
  public abstract OpenSslKeyMaterial touch(Object paramObject);
  
  public abstract boolean release();
  
  public abstract boolean release(int paramInt);
}
