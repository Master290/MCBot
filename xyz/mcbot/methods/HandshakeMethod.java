package xyz.mcbot.methods;

import io.netty.channel.Channel;
import xyz.mcbot.Main;
import xyz.mcbot.NettyBootstrap;
import xyz.mcbot.ProxyLoader.Proxy;
import xyz.mcbot.minecraftutils.Handshake;

public class HandshakeMethod implements Method
{
  private Handshake handshake;
  private byte[] bytes;
  
  public HandshakeMethod()
  {
    handshake = new Handshake(Main.protcolID, Main.srvRecord, Main.port, 2);
    bytes = handshake.getWrappedPacket();
  }
  
  public void accept(Channel channel, ProxyLoader.Proxy proxy)
  {
    channel.writeAndFlush(io.netty.buffer.Unpooled.buffer().writeBytes(bytes));
    NettyBootstrap.integer += 1;
    NettyBootstrap.totalConnections += 1;
  }
}
