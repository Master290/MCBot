package io.netty.handler.codec.socksx.v5;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.util.internal.ObjectUtil;
import java.util.List;





















public class Socks5CommandResponseDecoder
  extends ReplayingDecoder<State>
{
  private final Socks5AddressDecoder addressDecoder;
  
  static enum State
  {
    INIT, 
    SUCCESS, 
    FAILURE;
    
    private State() {}
  }
  
  public Socks5CommandResponseDecoder() {
    this(Socks5AddressDecoder.DEFAULT);
  }
  
  public Socks5CommandResponseDecoder(Socks5AddressDecoder addressDecoder) {
    super(State.INIT);
    this.addressDecoder = ((Socks5AddressDecoder)ObjectUtil.checkNotNull(addressDecoder, "addressDecoder"));
  }
  
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
  {
    try {
      switch (1.$SwitchMap$io$netty$handler$codec$socksx$v5$Socks5CommandResponseDecoder$State[((State)state()).ordinal()]) {
      case 1: 
        byte version = in.readByte();
        if (version != SocksVersion.SOCKS5.byteValue())
        {
          throw new DecoderException("unsupported version: " + version + " (expected: " + SocksVersion.SOCKS5.byteValue() + ')');
        }
        Socks5CommandStatus status = Socks5CommandStatus.valueOf(in.readByte());
        in.skipBytes(1);
        Socks5AddressType addrType = Socks5AddressType.valueOf(in.readByte());
        String addr = addressDecoder.decodeAddress(addrType, in);
        int port = in.readUnsignedShort();
        
        out.add(new DefaultSocks5CommandResponse(status, addrType, addr, port));
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
    
    Socks5Message m = new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, Socks5AddressType.IPv4, null, 0);
    
    m.setDecoderResult(DecoderResult.failure(cause));
    out.add(m);
  }
}
