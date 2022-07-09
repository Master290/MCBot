package io.netty.channel.epoll;

import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.unix.NativeInetAddress;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;






















public final class EpollServerSocketChannel
  extends AbstractEpollServerChannel
  implements ServerSocketChannel
{
  private final EpollServerSocketChannelConfig config;
  private volatile Collection<InetAddress> tcpMd5SigAddresses = Collections.emptyList();
  
  public EpollServerSocketChannel() {
    super(LinuxSocket.newSocketStream(), false);
    config = new EpollServerSocketChannelConfig(this);
  }
  

  public EpollServerSocketChannel(int fd)
  {
    this(new LinuxSocket(fd));
  }
  
  EpollServerSocketChannel(LinuxSocket fd) {
    super(fd);
    config = new EpollServerSocketChannelConfig(this);
  }
  
  EpollServerSocketChannel(LinuxSocket fd, boolean active) {
    super(fd, active);
    config = new EpollServerSocketChannelConfig(this);
  }
  
  protected boolean isCompatible(EventLoop loop)
  {
    return loop instanceof EpollEventLoop;
  }
  
  protected void doBind(SocketAddress localAddress) throws Exception
  {
    super.doBind(localAddress);
    int tcpFastopen;
    if ((Native.IS_SUPPORTING_TCP_FASTOPEN_SERVER) && ((tcpFastopen = config.getTcpFastopen()) > 0)) {
      socket.setTcpFastOpen(tcpFastopen);
    }
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
  
  public EpollServerSocketChannelConfig config()
  {
    return config;
  }
  
  protected Channel newChildChannel(int fd, byte[] address, int offset, int len) throws Exception
  {
    return new EpollSocketChannel(this, new LinuxSocket(fd), NativeInetAddress.address(address, offset, len));
  }
  
  Collection<InetAddress> tcpMd5SigAddresses() {
    return tcpMd5SigAddresses;
  }
  
  void setTcpMd5Sig(Map<InetAddress, byte[]> keys) throws IOException {
    tcpMd5SigAddresses = TcpMd5Util.newTcpMd5Sigs(this, tcpMd5SigAddresses, keys);
  }
}
