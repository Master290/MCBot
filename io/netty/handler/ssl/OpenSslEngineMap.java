package io.netty.handler.ssl;

abstract interface OpenSslEngineMap
{
  public abstract ReferenceCountedOpenSslEngine remove(long paramLong);
  
  public abstract void add(ReferenceCountedOpenSslEngine paramReferenceCountedOpenSslEngine);
  
  public abstract ReferenceCountedOpenSslEngine get(long paramLong);
}
