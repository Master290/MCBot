package io.netty.handler.codec.socksx.v5;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.codec.socksx.SocksVersion;
import java.util.List;























public class Socks5InitialResponseDecoder
  extends ReplayingDecoder<State>
{
  static enum State
  {
    INIT, 
    SUCCESS, 
    FAILURE;
    
    private State() {} }
  
  public Socks5InitialResponseDecoder() { super(State.INIT); }
  
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
  {
    try
    {
      switch (1.$SwitchMap$io$netty$handler$codec$socksx$v5$Socks5InitialResponseDecoder$State[((State)state()).ordinal()]) {
      case 1: 
        byte version = in.readByte();
        if (version != SocksVersion.SOCKS5.byteValue())
        {
          throw new DecoderException("unsupported version: " + version + " (expected: " + SocksVersion.SOCKS5.byteValue() + ')');
        }
        
        Socks5AuthMethod authMethod = Socks5AuthMethod.valueOf(in.readByte());
        out.add(new DefaultSocks5InitialResponse(authMethod));
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
    
    checkpoint(State.FAILURE);
    
    Socks5Message m = new DefaultSocks5InitialResponse(Socks5AuthMethod.UNACCEPTED);
    m.setDecoderResult(DecoderResult.failure(cause));
    out.add(m);
  }
}
