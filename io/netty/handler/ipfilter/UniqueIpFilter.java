package io.netty.handler.ipfilter;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.ConcurrentSet;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Set;




















@ChannelHandler.Sharable
public class UniqueIpFilter
  extends AbstractRemoteAddressFilter<InetSocketAddress>
{
  private final Set<InetAddress> connected = new ConcurrentSet();
  
  public UniqueIpFilter() {}
  
  protected boolean accept(ChannelHandlerContext ctx, InetSocketAddress remoteAddress) throws Exception { final InetAddress remoteIp = remoteAddress.getAddress();
    if (!connected.add(remoteIp)) {
      return false;
    }
    ctx.channel().closeFuture().addListener(new ChannelFutureListener()
    {
      public void operationComplete(ChannelFuture future) throws Exception {
        connected.remove(remoteIp);
      }
    });
    return true;
  }
}
