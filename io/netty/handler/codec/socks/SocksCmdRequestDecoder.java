package io.netty.handler.codec.socks;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.util.NetUtil;
import java.util.List;



















public class SocksCmdRequestDecoder
  extends ReplayingDecoder<State>
{
  private SocksCmdType cmdType;
  private SocksAddressType addressType;
  
  public SocksCmdRequestDecoder()
  {
    super(State.CHECK_PROTOCOL_VERSION);
  }
  
  protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) throws Exception
  {
    switch (1.$SwitchMap$io$netty$handler$codec$socks$SocksCmdRequestDecoder$State[((State)state()).ordinal()]) {
    case 1: 
      if (byteBuf.readByte() != SocksProtocolVersion.SOCKS5.byteValue()) {
        out.add(SocksCommonUtils.UNKNOWN_SOCKS_REQUEST);
      }
      else
        checkpoint(State.READ_CMD_HEADER);
      break;
    case 2: 
      cmdType = SocksCmdType.valueOf(byteBuf.readByte());
      byteBuf.skipBytes(1);
      addressType = SocksAddressType.valueOf(byteBuf.readByte());
      checkpoint(State.READ_CMD_ADDRESS);
    
    case 3: 
      switch (1.$SwitchMap$io$netty$handler$codec$socks$SocksAddressType[addressType.ordinal()]) {
      case 1: 
        String host = NetUtil.intToIpAddress(byteBuf.readInt());
        int port = byteBuf.readUnsignedShort();
        out.add(new SocksCmdRequest(cmdType, addressType, host, port));
        break;
      
      case 2: 
        int fieldLength = byteBuf.readByte();
        String host = SocksCommonUtils.readUsAscii(byteBuf, fieldLength);
        int port = byteBuf.readUnsignedShort();
        out.add(new SocksCmdRequest(cmdType, addressType, host, port));
        break;
      
      case 3: 
        byte[] bytes = new byte[16];
        byteBuf.readBytes(bytes);
        String host = SocksCommonUtils.ipv6toStr(bytes);
        int port = byteBuf.readUnsignedShort();
        out.add(new SocksCmdRequest(cmdType, addressType, host, port));
        break;
      
      case 4: 
        out.add(SocksCommonUtils.UNKNOWN_SOCKS_REQUEST);
        break;
      
      default: 
        throw new Error();
      }
      
      
      break;
    default: 
      throw new Error();
    }
    
    ctx.pipeline().remove(this);
  }
  
  static enum State {
    CHECK_PROTOCOL_VERSION, 
    READ_CMD_HEADER, 
    READ_CMD_ADDRESS;
    
    private State() {}
  }
}
