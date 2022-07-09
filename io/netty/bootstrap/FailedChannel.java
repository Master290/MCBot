package io.netty.bootstrap;

import io.netty.channel.AbstractChannel;
import io.netty.channel.AbstractChannel.AbstractUnsafe;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.EventLoop;
import java.net.SocketAddress;














final class FailedChannel
  extends AbstractChannel
{
  private static final ChannelMetadata METADATA = new ChannelMetadata(false);
  private final ChannelConfig config = new DefaultChannelConfig(this);
  
  FailedChannel() {
    super(null);
  }
  
  protected AbstractChannel.AbstractUnsafe newUnsafe()
  {
    return new FailedChannelUnsafe(null);
  }
  
  protected boolean isCompatible(EventLoop loop)
  {
    return false;
  }
  
  protected SocketAddress localAddress0()
  {
    return null;
  }
  
  protected SocketAddress remoteAddress0()
  {
    return null;
  }
  
  protected void doBind(SocketAddress localAddress)
  {
    throw new UnsupportedOperationException();
  }
  
  protected void doDisconnect()
  {
    throw new UnsupportedOperationException();
  }
  
  protected void doClose()
  {
    throw new UnsupportedOperationException();
  }
  
  protected void doBeginRead()
  {
    throw new UnsupportedOperationException();
  }
  
  protected void doWrite(ChannelOutboundBuffer in)
  {
    throw new UnsupportedOperationException();
  }
  
  public ChannelConfig config()
  {
    return config;
  }
  
  public boolean isOpen()
  {
    return false;
  }
  
  public boolean isActive()
  {
    return false;
  }
  


  public ChannelMetadata metadata() { return METADATA; }
  
  private final class FailedChannelUnsafe extends AbstractChannel.AbstractUnsafe {
    private FailedChannelUnsafe() { super(); }
    
    public void connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
      promise.setFailure(new UnsupportedOperationException());
    }
  }
}
