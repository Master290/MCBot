package io.netty.handler.codec.socksx.v5;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;


















@ChannelHandler.Sharable
public class Socks5ServerEncoder
  extends MessageToByteEncoder<Socks5Message>
{
  public static final Socks5ServerEncoder DEFAULT = new Socks5ServerEncoder(Socks5AddressEncoder.DEFAULT);
  

  private final Socks5AddressEncoder addressEncoder;
  

  protected Socks5ServerEncoder()
  {
    this(Socks5AddressEncoder.DEFAULT);
  }
  


  public Socks5ServerEncoder(Socks5AddressEncoder addressEncoder)
  {
    this.addressEncoder = ((Socks5AddressEncoder)ObjectUtil.checkNotNull(addressEncoder, "addressEncoder"));
  }
  


  protected final Socks5AddressEncoder addressEncoder()
  {
    return addressEncoder;
  }
  
  protected void encode(ChannelHandlerContext ctx, Socks5Message msg, ByteBuf out) throws Exception
  {
    if ((msg instanceof Socks5InitialResponse)) {
      encodeAuthMethodResponse((Socks5InitialResponse)msg, out);
    } else if ((msg instanceof Socks5PasswordAuthResponse)) {
      encodePasswordAuthResponse((Socks5PasswordAuthResponse)msg, out);
    } else if ((msg instanceof Socks5CommandResponse)) {
      encodeCommandResponse((Socks5CommandResponse)msg, out);
    } else {
      throw new EncoderException("unsupported message type: " + StringUtil.simpleClassName(msg));
    }
  }
  
  private static void encodeAuthMethodResponse(Socks5InitialResponse msg, ByteBuf out) {
    out.writeByte(msg.version().byteValue());
    out.writeByte(msg.authMethod().byteValue());
  }
  
  private static void encodePasswordAuthResponse(Socks5PasswordAuthResponse msg, ByteBuf out) {
    out.writeByte(1);
    out.writeByte(msg.status().byteValue());
  }
  
  private void encodeCommandResponse(Socks5CommandResponse msg, ByteBuf out) throws Exception {
    out.writeByte(msg.version().byteValue());
    out.writeByte(msg.status().byteValue());
    out.writeByte(0);
    
    Socks5AddressType bndAddrType = msg.bndAddrType();
    out.writeByte(bndAddrType.byteValue());
    addressEncoder.encodeAddress(bndAddrType, msg.bndAddr(), out);
    
    out.writeShort(msg.bndPort());
  }
}
