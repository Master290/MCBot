package xyz.mcbot.methods;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import java.io.IOException;
import xyz.mcbot.NettyBootstrap;
import xyz.mcbot.ProxyLoader.Proxy;





public class LegacyPing
  implements Method
{
  public LegacyPing() {}
  
  public void accept(Channel channel, ProxyLoader.Proxy proxy)
  {
    ByteBuf b = Unpooled.buffer();
    ByteBufOutputStream out = new ByteBufOutputStream(b);
    try {
      out.write(new byte[] { -2, 1, -6, 0, 11, 0, 77, 0, 67, 0, 124, 0, 80, 0, 105, 0, 110, 0, 103, 0, 72, 0, 111, 0, 115, 0, 116, 0, 25, 73, 0, 9, 0, 108, 0, 111, 0, 99, 0, 97, 0, 108, 0, 104, 0, 111, 0, 115, 0, 116, 0, 0, 99, -35 });
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
