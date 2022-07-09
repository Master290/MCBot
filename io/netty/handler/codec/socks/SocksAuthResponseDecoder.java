package io.netty.handler.codec.socks;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ReplayingDecoder;
import java.util.List;



















public class SocksAuthResponseDecoder
  extends ReplayingDecoder<State>
{
  public SocksAuthResponseDecoder()
  {
    super(State.CHECK_PROTOCOL_VERSION);
  }
  
  protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> out)
    throws Exception
  {
    switch (1.$SwitchMap$io$netty$handler$codec$socks$SocksAuthResponseDecoder$State[((State)state()).ordinal()]) {
    case 1: 
      if (byteBuf.readByte() != SocksSubnegotiationVersion.AUTH_PASSWORD.byteValue()) {
        out.add(SocksCommonUtils.UNKNOWN_SOCKS_RESPONSE);
      }
      else
        checkpoint(State.READ_AUTH_RESPONSE);
      break;
    case 2: 
      SocksAuthStatus authStatus = SocksAuthStatus.valueOf(byteBuf.readByte());
      out.add(new SocksAuthResponse(authStatus));
      break;
    
    default: 
      throw new Error();
    }
    
    channelHandlerContext.pipeline().remove(this);
  }
  
  static enum State {
    CHECK_PROTOCOL_VERSION, 
    READ_AUTH_RESPONSE;
    
    private State() {}
  }
}
