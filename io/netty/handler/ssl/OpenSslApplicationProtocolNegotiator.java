package io.netty.handler.ssl;

@Deprecated
public abstract interface OpenSslApplicationProtocolNegotiator
  extends ApplicationProtocolNegotiator
{
  public abstract ApplicationProtocolConfig.Protocol protocol();
  
  public abstract ApplicationProtocolConfig.SelectorFailureBehavior selectorFailureBehavior();
  
  public abstract ApplicationProtocolConfig.SelectedListenerFailureBehavior selectedListenerFailureBehavior();
}
