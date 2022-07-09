package xyz.mcbot.methods;

import io.netty.channel.Channel;
import xyz.mcbot.Main;
import xyz.mcbot.NettyBootstrap;
import xyz.mcbot.ProxyLoader.Proxy;

public class InvalidData implements Method
{
  private xyz.mcbot.minecraftutils.Handshake handshake;
  
  public InvalidData()
  {
    handshake = new xyz.mcbot.minecraftutils.Handshake(Main.protcolID, Main.srvRecord, Main.port, 2);
  }
  

  public void accept(Channel channel, ProxyLoader.Proxy proxy)
  {
    channel.writeAndFlush(handshake);
    NettyBootstrap.integer += 1;
    NettyBootstrap.totalConnections += 1;
    channel.close();
  }
}
