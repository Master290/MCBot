package io.netty.handler.codec;

public abstract interface DecoderResultProvider
{
  public abstract DecoderResult decoderResult();
  
  public abstract void setDecoderResult(DecoderResult paramDecoderResult);
}
