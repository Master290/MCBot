package xyz.mcbot.methods;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import xyz.mcbot.NettyBootstrap;
import xyz.mcbot.ProxyLoader.Proxy;


public class EmptyPacket
  implements Method
{
  public EmptyPacket() {}
  
  public void accept(Channel channel, ProxyLoader.Proxy proxy)
  {
    channel.writeAndFlush(Unpooled.buffer().writeBytes(new byte[1]));
    
    NettyBootstrap.integer += 1;
    NettyBootstrap.totalConnections += 1;
    channel.close();
  }
}
