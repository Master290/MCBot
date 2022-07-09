package io.netty.handler.codec.socks;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ReplayingDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;



















public class SocksInitRequestDecoder
  extends ReplayingDecoder<State>
{
  public SocksInitRequestDecoder()
  {
    super(State.CHECK_PROTOCOL_VERSION);
  }
  
  protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) throws Exception
  {
    switch (1.$SwitchMap$io$netty$handler$codec$socks$SocksInitRequestDecoder$State[((State)state()).ordinal()]) {
    case 1: 
      if (byteBuf.readByte() != SocksProtocolVersion.SOCKS5.byteValue()) {
        out.add(SocksCommonUtils.UNKNOWN_SOCKS_REQUEST);
      }
      else
        checkpoint(State.READ_AUTH_SCHEMES);
      break;
    case 2: 
      byte authSchemeNum = byteBuf.readByte();
      List<SocksAuthScheme> authSchemes;
      if (authSchemeNum > 0) {
        List<SocksAuthScheme> authSchemes = new ArrayList(authSchemeNum);
        for (int i = 0; i < authSchemeNum; i++) {
          authSchemes.add(SocksAuthScheme.valueOf(byteBuf.readByte()));
        }
      } else {
        authSchemes = Collections.emptyList();
      }
      out.add(new SocksInitRequest(authSchemes));
      break;
    
    default: 
      throw new Error();
    }
    
    ctx.pipeline().remove(this);
  }
  
  static enum State {
    CHECK_PROTOCOL_VERSION, 
    READ_AUTH_SCHEMES;
    
    private State() {}
  }
}
