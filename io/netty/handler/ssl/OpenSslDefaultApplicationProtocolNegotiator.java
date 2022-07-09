package io.netty.handler.ssl;

import io.netty.util.internal.ObjectUtil;
import java.util.List;


















@Deprecated
public final class OpenSslDefaultApplicationProtocolNegotiator
  implements OpenSslApplicationProtocolNegotiator
{
  private final ApplicationProtocolConfig config;
  
  public OpenSslDefaultApplicationProtocolNegotiator(ApplicationProtocolConfig config)
  {
    this.config = ((ApplicationProtocolConfig)ObjectUtil.checkNotNull(config, "config"));
  }
  
  public List<String> protocols()
  {
    return config.supportedProtocols();
  }
  
  public ApplicationProtocolConfig.Protocol protocol()
  {
    return config.protocol();
  }
  
  public ApplicationProtocolConfig.SelectorFailureBehavior selectorFailureBehavior()
  {
    return config.selectorFailureBehavior();
  }
  
  public ApplicationProtocolConfig.SelectedListenerFailureBehavior selectedListenerFailureBehavior()
  {
    return config.selectedListenerFailureBehavior();
  }
}
