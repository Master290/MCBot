package io.netty.handler.ssl;

import java.util.List;

/**
 * @deprecated
 */
public abstract interface ApplicationProtocolNegotiator
{
  public abstract List<String> protocols();
}
