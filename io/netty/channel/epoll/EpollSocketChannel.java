package io.netty.channel.epoll;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.Channel.Unsafe;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executor;






















public final class EpollSocketChannel
  extends AbstractEpollStreamChannel
  implements SocketChannel
{
  private final EpollSocketChannelConfig config;
  private volatile Collection<InetAddress> tcpMd5SigAddresses = Collections.emptyList();
  
  public EpollSocketChannel() {
    super(LinuxSocket.newSocketStream(), false);
    config = new EpollSocketChannelConfig(this);
  }
  
  public EpollSocketChannel(int fd) {
    super(fd);
    config = new EpollSocketChannelConfig(this);
  }
  
  EpollSocketChannel(LinuxSocket fd, boolean active) {
    super(fd, active);
    config = new EpollSocketChannelConfig(this);
  }
  
  EpollSocketChannel(Channel parent, LinuxSocket fd, InetSocketAddress remoteAddress) {
    super(parent, fd, remoteAddress);
    config = new EpollSocketChannelConfig(this);
    
    if ((parent instanceof EpollServerSocketChannel)) {
      tcpMd5SigAddresses = ((EpollServerSocketChannel)parent).tcpMd5SigAddresses();
    }
  }
  



  public EpollTcpInfo tcpInfo()
  {
    return tcpInfo(new EpollTcpInfo());
  }
  


  public EpollTcpInfo tcpInfo(EpollTcpInfo info)
  {
    try
    {
      socket.getTcpInfo(info);
      return info;
    } catch (IOException e) {
      throw new ChannelException(e);
    }
  }
  
  public InetSocketAddress remoteAddress()
  {
    return (InetSocketAddress)super.remoteAddress();
  }
  
  public InetSocketAddress localAddress()
  {
    return (InetSocketAddress)super.localAddress();
  }
  
  public EpollSocketChannelConfig config()
  {
    return config;
  }
  
  public ServerSocketChannel parent()
  {
    return (ServerSocketChannel)super.parent();
  }
  
  protected AbstractEpollChannel.AbstractEpollUnsafe newUnsafe()
  {
    return new EpollSocketChannelUnsafe(null);
  }
  
  boolean doConnect0(SocketAddress remote) throws Exception
  {
    if ((Native.IS_SUPPORTING_TCP_FASTOPEN_CLIENT) && (config.isTcpFastOpenConnect())) {
      ChannelOutboundBuffer outbound = unsafe().outboundBuffer();
      outbound.addFlush();
      Object curr;
      if (((curr = outbound.current()) instanceof ByteBuf)) {
        ByteBuf initialData = (ByteBuf)curr;
        

        long localFlushedAmount = doWriteOrSendBytes(initialData, (InetSocketAddress)remote, true);
        
        if (localFlushedAmount > 0L)
        {

          outbound.removeBytes(localFlushedAmount);
          return true;
        }
      }
    }
    return super.doConnect0(remote);
  }
  
  private final class EpollSocketChannelUnsafe extends AbstractEpollStreamChannel.EpollStreamUnsafe { private EpollSocketChannelUnsafe() { super(); }
    
    protected Executor prepareToClose()
    {
      try
      {
        if ((isOpen()) && (config().getSoLinger() > 0))
        {



          ((EpollEventLoop)eventLoop()).remove(EpollSocketChannel.this);
          return GlobalEventExecutor.INSTANCE;
        }
      }
      catch (Throwable localThrowable) {}
      


      return null;
    }
  }
  
  void setTcpMd5Sig(Map<InetAddress, byte[]> keys) throws IOException {
    tcpMd5SigAddresses = TcpMd5Util.newTcpMd5Sigs(this, tcpMd5SigAddresses, keys);
  }
}
