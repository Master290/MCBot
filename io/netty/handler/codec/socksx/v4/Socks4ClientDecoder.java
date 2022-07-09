package io.netty.handler.codec.socksx.v4;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.util.NetUtil;
import java.util.List;






















public class Socks4ClientDecoder
  extends ReplayingDecoder<State>
{
  static enum State
  {
    START, 
    SUCCESS, 
    FAILURE;
    
    private State() {} }
  
  public Socks4ClientDecoder() { super(State.START);
    setSingleDecode(true);
  }
  
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
  {
    try {
      switch (1.$SwitchMap$io$netty$handler$codec$socksx$v4$Socks4ClientDecoder$State[((State)state()).ordinal()]) {
      case 1: 
        int version = in.readUnsignedByte();
        if (version != 0) {
          throw new DecoderException("unsupported reply version: " + version + " (expected: 0)");
        }
        
        Socks4CommandStatus status = Socks4CommandStatus.valueOf(in.readByte());
        int dstPort = in.readUnsignedShort();
        String dstAddr = NetUtil.intToIpAddress(in.readInt());
        
        out.add(new DefaultSocks4CommandResponse(status, dstAddr, dstPort));
        checkpoint(State.SUCCESS);
      
      case 2: 
        int readableBytes = actualReadableBytes();
        if (readableBytes > 0) {
          out.add(in.readRetainedSlice(readableBytes));
        }
        
        break;
      case 3: 
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
    
    Socks4CommandResponse m = new DefaultSocks4CommandResponse(Socks4CommandStatus.REJECTED_OR_FAILED);
    m.setDecoderResult(DecoderResult.failure(cause));
    out.add(m);
    
    checkpoint(State.FAILURE);
  }
}
