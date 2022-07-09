package io.netty.handler.codec.socksx.v5;

import io.netty.handler.codec.DecoderResult;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;
















public class DefaultSocks5PasswordAuthResponse
  extends AbstractSocks5Message
  implements Socks5PasswordAuthResponse
{
  private final Socks5PasswordAuthStatus status;
  
  public DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus status)
  {
    this.status = ((Socks5PasswordAuthStatus)ObjectUtil.checkNotNull(status, "status"));
  }
  
  public Socks5PasswordAuthStatus status()
  {
    return status;
  }
  
  public String toString()
  {
    StringBuilder buf = new StringBuilder(StringUtil.simpleClassName(this));
    
    DecoderResult decoderResult = decoderResult();
    if (!decoderResult.isSuccess()) {
      buf.append("(decoderResult: ");
      buf.append(decoderResult);
      buf.append(", status: ");
    } else {
      buf.append("(status: ");
    }
    buf.append(status());
    buf.append(')');
    
    return buf.toString();
  }
}
