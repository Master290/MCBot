package io.netty.handler.codec.socksx;

import io.netty.handler.codec.DecoderResultProvider;

public abstract interface SocksMessage
  extends DecoderResultProvider
{
  public abstract SocksVersion version();
}
