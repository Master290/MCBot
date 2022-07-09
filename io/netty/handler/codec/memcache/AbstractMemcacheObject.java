package io.netty.handler.codec.memcache;

import io.netty.handler.codec.DecoderResult;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.internal.ObjectUtil;



















public abstract class AbstractMemcacheObject
  extends AbstractReferenceCounted
  implements MemcacheObject
{
  private DecoderResult decoderResult = DecoderResult.SUCCESS;
  

  protected AbstractMemcacheObject() {}
  

  public DecoderResult decoderResult()
  {
    return decoderResult;
  }
  
  public void setDecoderResult(DecoderResult result)
  {
    decoderResult = ((DecoderResult)ObjectUtil.checkNotNull(result, "DecoderResult should not be null."));
  }
}
