package io.netty.handler.codec.socks;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ReplayingDecoder;
import java.util.List;



















public class SocksAuthRequestDecoder
  extends ReplayingDecoder<State>
{
  private String username;
  
  public SocksAuthRequestDecoder()
  {
    super(State.CHECK_PROTOCOL_VERSION);
  }
  
  protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) throws Exception
  {
    switch (1.$SwitchMap$io$netty$handler$codec$socks$SocksAuthRequestDecoder$State[((State)state()).ordinal()]) {
    case 1: 
      if (byteBuf.readByte() != SocksSubnegotiationVersion.AUTH_PASSWORD.byteValue()) {
        out.add(SocksCommonUtils.UNKNOWN_SOCKS_REQUEST);
      }
      else
        checkpoint(State.READ_USERNAME);
      break;
    case 2: 
      int fieldLength = byteBuf.readByte();
      username = SocksCommonUtils.readUsAscii(byteBuf, fieldLength);
      checkpoint(State.READ_PASSWORD);
    
    case 3: 
      int fieldLength = byteBuf.readByte();
      String password = SocksCommonUtils.readUsAscii(byteBuf, fieldLength);
      out.add(new SocksAuthRequest(username, password));
      break;
    
    default: 
      throw new Error();
    }
    
    ctx.pipeline().remove(this);
  }
  
  static enum State {
    CHECK_PROTOCOL_VERSION, 
    READ_USERNAME, 
    READ_PASSWORD;
    
    private State() {}
  }
}
