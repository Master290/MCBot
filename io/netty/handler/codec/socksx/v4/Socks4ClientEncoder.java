package io.netty.handler.codec.socksx.v4;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.util.NetUtil;





















@ChannelHandler.Sharable
public final class Socks4ClientEncoder
  extends MessageToByteEncoder<Socks4CommandRequest>
{
  public static final Socks4ClientEncoder INSTANCE = new Socks4ClientEncoder();
  
  private static final byte[] IPv4_DOMAIN_MARKER = { 0, 0, 0, 1 };
  
  private Socks4ClientEncoder() {}
  
  protected void encode(ChannelHandlerContext ctx, Socks4CommandRequest msg, ByteBuf out) throws Exception
  {
    out.writeByte(msg.version().byteValue());
    out.writeByte(msg.type().byteValue());
    out.writeShort(msg.dstPort());
    if (NetUtil.isValidIpV4Address(msg.dstAddr())) {
      out.writeBytes(NetUtil.createByteArrayFromIpAddressString(msg.dstAddr()));
      ByteBufUtil.writeAscii(out, msg.userId());
      out.writeByte(0);
    } else {
      out.writeBytes(IPv4_DOMAIN_MARKER);
      ByteBufUtil.writeAscii(out, msg.userId());
      out.writeByte(0);
      ByteBufUtil.writeAscii(out, msg.dstAddr());
      out.writeByte(0);
    }
  }
}
