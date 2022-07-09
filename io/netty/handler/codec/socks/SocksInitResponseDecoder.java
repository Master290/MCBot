package io.netty.handler.codec.socks;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ReplayingDecoder;
import java.util.List;



















public class SocksInitResponseDecoder
  extends ReplayingDecoder<State>
{
  public SocksInitResponseDecoder()
  {
    super(State.CHECK_PROTOCOL_VERSION);
  }
  
  protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) throws Exception
  {
    switch (1.$SwitchMap$io$netty$handler$codec$socks$SocksInitResponseDecoder$State[((State)state()).ordinal()]) {
    case 1: 
      if (byteBuf.readByte() != SocksProtocolVersion.SOCKS5.byteValue()) {
        out.add(SocksCommonUtils.UNKNOWN_SOCKS_RESPONSE);
      }
      else
        checkpoint(State.READ_PREFERRED_AUTH_TYPE);
      break;
    case 2: 
      SocksAuthScheme authScheme = SocksAuthScheme.valueOf(byteBuf.readByte());
      out.add(new SocksInitResponse(authScheme));
      break;
    
    default: 
      throw new Error();
    }
    
    ctx.pipeline().remove(this);
  }
  
  static enum State {
    CHECK_PROTOCOL_VERSION, 
    READ_PREFERRED_AUTH_TYPE;
    
    private State() {}
  }
}
