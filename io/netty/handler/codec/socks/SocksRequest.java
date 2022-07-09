package io.netty.handler.codec.socks;

import io.netty.util.internal.ObjectUtil;






















public abstract class SocksRequest
  extends SocksMessage
{
  private final SocksRequestType requestType;
  
  protected SocksRequest(SocksRequestType requestType)
  {
    super(SocksMessageType.REQUEST);
    this.requestType = ((SocksRequestType)ObjectUtil.checkNotNull(requestType, "requestType"));
  }
  




  public SocksRequestType requestType()
  {
    return requestType;
  }
}
