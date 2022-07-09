package io.netty.channel.socket;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;

public abstract interface DuplexChannel
  extends Channel
{
  public abstract boolean isInputShutdown();
  
  public abstract ChannelFuture shutdownInput();
  
  public abstract ChannelFuture shutdownInput(ChannelPromise paramChannelPromise);
  
  public abstract boolean isOutputShutdown();
  
  public abstract ChannelFuture shutdownOutput();
  
  public abstract ChannelFuture shutdownOutput(ChannelPromise paramChannelPromise);
  
  public abstract boolean isShutdown();
  
  public abstract ChannelFuture shutdown();
  
  public abstract ChannelFuture shutdown(ChannelPromise paramChannelPromise);
}
