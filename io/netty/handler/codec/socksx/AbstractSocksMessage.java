package io.netty.handler.codec.socksx;

import io.netty.handler.codec.DecoderResult;
import io.netty.util.internal.ObjectUtil;



















public abstract class AbstractSocksMessage
  implements SocksMessage
{
  private DecoderResult decoderResult = DecoderResult.SUCCESS;
  
  public AbstractSocksMessage() {}
  
  public DecoderResult decoderResult() { return decoderResult; }
  

  public void setDecoderResult(DecoderResult decoderResult)
  {
    this.decoderResult = ((DecoderResult)ObjectUtil.checkNotNull(decoderResult, "decoderResult"));
  }
}
