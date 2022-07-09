package io.netty.handler.codec.socks;

import io.netty.buffer.ByteBuf;
import io.netty.util.internal.ObjectUtil;
import java.util.Collections;
import java.util.List;




















public final class SocksInitRequest
  extends SocksRequest
{
  private final List<SocksAuthScheme> authSchemes;
  
  public SocksInitRequest(List<SocksAuthScheme> authSchemes)
  {
    super(SocksRequestType.INIT);
    this.authSchemes = ((List)ObjectUtil.checkNotNull(authSchemes, "authSchemes"));
  }
  




  public List<SocksAuthScheme> authSchemes()
  {
    return Collections.unmodifiableList(authSchemes);
  }
  
  public void encodeAsByteBuf(ByteBuf byteBuf)
  {
    byteBuf.writeByte(protocolVersion().byteValue());
    byteBuf.writeByte(authSchemes.size());
    for (SocksAuthScheme authScheme : authSchemes) {
      byteBuf.writeByte(authScheme.byteValue());
    }
  }
}
