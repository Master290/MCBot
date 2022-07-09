package io.netty.handler.ssl;

import javax.net.ssl.SSLHandshakeException;

















public final class SslHandshakeTimeoutException
  extends SSLHandshakeException
{
  SslHandshakeTimeoutException(String reason)
  {
    super(reason);
  }
}
