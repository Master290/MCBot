package io.netty.channel.sctp;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ServerChannel;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Set;

public abstract interface SctpServerChannel
  extends ServerChannel
{
  public abstract SctpServerChannelConfig config();
  
  public abstract InetSocketAddress localAddress();
  
  public abstract Set<InetSocketAddress> allLocalAddresses();
  
  public abstract ChannelFuture bindAddress(InetAddress paramInetAddress);
  
  public abstract ChannelFuture bindAddress(InetAddress paramInetAddress, ChannelPromise paramChannelPromise);
  
  public abstract ChannelFuture unbindAddress(InetAddress paramInetAddress);
  
  public abstract ChannelFuture unbindAddress(InetAddress paramInetAddress, ChannelPromise paramChannelPromise);
}
