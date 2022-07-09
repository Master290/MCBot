package xyz.mcbot.methods;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import xyz.mcbot.Main;
import xyz.mcbot.NettyBootstrap;
import xyz.mcbot.ProxyLoader.Proxy;

public class Ping implements Method
{
  private byte[] handshakebytes;
  
  public Ping()
  {
    handshakebytes = new xyz.mcbot.minecraftutils.Handshake(Main.protcolID, Main.srvRecord, Main.port, 1).getWrappedPacket();
  }
  
  public void accept(Channel channel, ProxyLoader.Proxy proxy)
  {
    channel.writeAndFlush(Unpooled.buffer().writeBytes(handshakebytes));
    channel.writeAndFlush(Unpooled.buffer().writeBytes(new byte[] { 1 }));
    channel.writeAndFlush(Unpooled.buffer().writeBytes(new xyz.mcbot.minecraftutils.PingPacket(System.currentTimeMillis()).getWrappedPacket()));
    NettyBootstrap.integer += 1;
    NettyBootstrap.totalConnections += 1;
    channel.close();
  }
}
