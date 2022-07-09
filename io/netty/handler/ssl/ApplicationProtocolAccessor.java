package io.netty.handler.ssl;

abstract interface ApplicationProtocolAccessor
{
  public abstract String getNegotiatedApplicationProtocol();
}
