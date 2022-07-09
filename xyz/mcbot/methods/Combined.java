package xyz.mcbot.methods;

import io.netty.channel.Channel;
import java.util.Random;
import xyz.mcbot.NettyBootstrap;
import xyz.mcbot.ProxyLoader.Proxy;







public class Combined
  implements Method
{
  private int at = 0;
  
  private String randomString(int len) {
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
  

  public Combined() {}
  

  public void accept(Channel channel, ProxyLoader.Proxy proxy)
  {
    switch (at)
    {
    }
    
    NettyBootstrap.integer = NettyBootstrap.integer + 1;
    NettyBootstrap.totalConnections += 1;
    channel.close();
  }
}
