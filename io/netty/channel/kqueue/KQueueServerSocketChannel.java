package io.netty.channel.kqueue;

import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.unix.NativeInetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

















public final class KQueueServerSocketChannel
  extends AbstractKQueueServerChannel
  implements ServerSocketChannel
{
  private final KQueueServerSocketChannelConfig config;
  
  public KQueueServerSocketChannel()
  {
    super(BsdSocket.newSocketStream(), false);
    config = new KQueueServerSocketChannelConfig(this);
  }
  

  public KQueueServerSocketChannel(int fd)
  {
    this(new BsdSocket(fd));
  }
  
  KQueueServerSocketChannel(BsdSocket fd) {
    super(fd);
    config = new KQueueServerSocketChannelConfig(this);
  }
  
  KQueueServerSocketChannel(BsdSocket fd, boolean active) {
    super(fd, active);
    config = new KQueueServerSocketChannelConfig(this);
  }
  
  protected boolean isCompatible(EventLoop loop)
  {
    return loop instanceof KQueueEventLoop;
  }
  
  protected void doBind(SocketAddress localAddress) throws Exception
  {
    super.doBind(localAddress);
    

    socket.listen(config.getBacklog());
    active = true;
  }
  
  public InetSocketAddress remoteAddress()
  {
    return (InetSocketAddress)super.remoteAddress();
  }
  
  public InetSocketAddress localAddress()
  {
    return (InetSocketAddress)super.localAddress();
  }
  
  public KQueueServerSocketChannelConfig config()
  {
    return config;
  }
  
  protected Channel newChildChannel(int fd, byte[] address, int offset, int len) throws Exception
  {
    return new KQueueSocketChannel(this, new BsdSocket(fd), NativeInetAddress.address(address, offset, len));
  }
}
