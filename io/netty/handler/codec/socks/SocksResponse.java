package io.netty.handler.codec.socks;

import io.netty.util.internal.ObjectUtil;






















public abstract class SocksResponse
  extends SocksMessage
{
  private final SocksResponseType responseType;
  
  protected SocksResponse(SocksResponseType responseType)
  {
    super(SocksMessageType.RESPONSE);
    this.responseType = ((SocksResponseType)ObjectUtil.checkNotNull(responseType, "responseType"));
  }
  




  public SocksResponseType responseType()
  {
    return responseType;
  }
}
