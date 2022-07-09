package io.netty.handler.codec.http;

import io.netty.handler.codec.DecoderResult;
import io.netty.util.internal.ObjectUtil;















public class DefaultHttpObject
  implements HttpObject
{
  private static final int HASH_CODE_PRIME = 31;
  private DecoderResult decoderResult = DecoderResult.SUCCESS;
  

  protected DefaultHttpObject() {}
  

  public DecoderResult decoderResult()
  {
    return decoderResult;
  }
  
  @Deprecated
  public DecoderResult getDecoderResult()
  {
    return decoderResult();
  }
  
  public void setDecoderResult(DecoderResult decoderResult)
  {
    this.decoderResult = ((DecoderResult)ObjectUtil.checkNotNull(decoderResult, "decoderResult"));
  }
  
  public int hashCode()
  {
    int result = 1;
    result = 31 * result + decoderResult.hashCode();
    return result;
  }
  
  public boolean equals(Object o)
  {
    if (!(o instanceof DefaultHttpObject)) {
      return false;
    }
    
    DefaultHttpObject other = (DefaultHttpObject)o;
    
    return decoderResult().equals(other.decoderResult());
  }
}
