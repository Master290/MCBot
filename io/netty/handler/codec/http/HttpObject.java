package io.netty.handler.codec.http;

import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.DecoderResultProvider;

public abstract interface HttpObject
  extends DecoderResultProvider
{
  @Deprecated
  public abstract DecoderResult getDecoderResult();
}
