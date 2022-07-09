package io.netty.handler.codec.http2;

import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;
























public final class DefaultHttp2ResetFrame
  extends AbstractHttp2StreamFrame
  implements Http2ResetFrame
{
  private final long errorCode;
  
  public DefaultHttp2ResetFrame(Http2Error error)
  {
    errorCode = ((Http2Error)ObjectUtil.checkNotNull(error, "error")).code();
  }
  




  public DefaultHttp2ResetFrame(long errorCode)
  {
    this.errorCode = errorCode;
  }
  
  public DefaultHttp2ResetFrame stream(Http2FrameStream stream)
  {
    super.stream(stream);
    return this;
  }
  
  public String name()
  {
    return "RST_STREAM";
  }
  
  public long errorCode()
  {
    return errorCode;
  }
  
  public String toString()
  {
    return StringUtil.simpleClassName(this) + "(stream=" + stream() + ", errorCode=" + errorCode + ')';
  }
  
  public boolean equals(Object o)
  {
    if (!(o instanceof DefaultHttp2ResetFrame)) {
      return false;
    }
    DefaultHttp2ResetFrame other = (DefaultHttp2ResetFrame)o;
    return (super.equals(o)) && (errorCode == errorCode);
  }
  
  public int hashCode()
  {
    int hash = super.hashCode();
    hash = hash * 31 + (int)(errorCode ^ errorCode >>> 32);
    return hash;
  }
}
