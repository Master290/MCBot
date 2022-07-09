package io.netty.handler.codec.socksx.v4;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.util.CharsetUtil;
import io.netty.util.NetUtil;
import java.util.List;


















public class Socks4ServerDecoder
  extends ReplayingDecoder<State>
{
  private static final int MAX_FIELD_LENGTH = 255;
  private Socks4CommandType type;
  private String dstAddr;
  private int dstPort;
  private String userId;
  
  static enum State
  {
    START, 
    READ_USERID, 
    READ_DOMAIN, 
    SUCCESS, 
    FAILURE;
    

    private State() {}
  }
  

  public Socks4ServerDecoder()
  {
    super(State.START);
    setSingleDecode(true);
  }
  
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
  {
    try {
      switch (1.$SwitchMap$io$netty$handler$codec$socksx$v4$Socks4ServerDecoder$State[((State)state()).ordinal()]) {
      case 1: 
        int version = in.readUnsignedByte();
        if (version != SocksVersion.SOCKS4a.byteValue()) {
          throw new DecoderException("unsupported protocol version: " + version);
        }
        
        type = Socks4CommandType.valueOf(in.readByte());
        dstPort = in.readUnsignedShort();
        dstAddr = NetUtil.intToIpAddress(in.readInt());
        checkpoint(State.READ_USERID);
      
      case 2: 
        userId = readString("userid", in);
        checkpoint(State.READ_DOMAIN);
      

      case 3: 
        if ((!"0.0.0.0".equals(dstAddr)) && (dstAddr.startsWith("0.0.0."))) {
          dstAddr = readString("dstAddr", in);
        }
        out.add(new DefaultSocks4CommandRequest(type, dstAddr, dstPort, userId));
        checkpoint(State.SUCCESS);
      
      case 4: 
        int readableBytes = actualReadableBytes();
        if (readableBytes > 0) {
          out.add(in.readRetainedSlice(readableBytes));
        }
        
        break;
      case 5: 
        in.skipBytes(actualReadableBytes());
      }
    }
    catch (Exception e)
    {
      fail(out, e);
    }
  }
  
  private void fail(List<Object> out, Exception cause) {
    if (!(cause instanceof DecoderException)) {
      cause = new DecoderException(cause);
    }
    
    Socks4CommandRequest m = new DefaultSocks4CommandRequest(type != null ? type : Socks4CommandType.CONNECT, dstAddr != null ? dstAddr : "", dstPort != 0 ? dstPort : 65535, userId != null ? userId : "");
    




    m.setDecoderResult(DecoderResult.failure(cause));
    out.add(m);
    
    checkpoint(State.FAILURE);
  }
  


  private static String readString(String fieldName, ByteBuf in)
  {
    int length = in.bytesBefore(256, (byte)0);
    if (length < 0) {
      throw new DecoderException("field '" + fieldName + "' longer than " + 255 + " chars");
    }
    
    String value = in.readSlice(length).toString(CharsetUtil.US_ASCII);
    in.skipBytes(1);
    
    return value;
  }
}
