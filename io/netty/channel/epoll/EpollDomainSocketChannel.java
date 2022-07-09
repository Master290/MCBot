package io.netty.channel.epoll;

import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.channel.unix.DomainSocketChannel;
import io.netty.channel.unix.FileDescriptor;
import io.netty.channel.unix.PeerCredentials;
import java.io.IOException;
import java.net.SocketAddress;

















public final class EpollDomainSocketChannel
  extends AbstractEpollStreamChannel
  implements DomainSocketChannel
{
  private final EpollDomainSocketChannelConfig config = new EpollDomainSocketChannelConfig(this);
  private volatile DomainSocketAddress local;
  private volatile DomainSocketAddress remote;
  
  public EpollDomainSocketChannel()
  {
    super(LinuxSocket.newSocketDomain(), false);
  }
  
  EpollDomainSocketChannel(Channel parent, FileDescriptor fd) {
    super(parent, new LinuxSocket(fd.intValue()));
  }
  
  public EpollDomainSocketChannel(int fd) {
    super(fd);
  }
  
  public EpollDomainSocketChannel(Channel parent, LinuxSocket fd) {
    super(parent, fd);
  }
  
  public EpollDomainSocketChannel(int fd, boolean active) {
    super(new LinuxSocket(fd), active);
  }
  
  protected AbstractEpollChannel.AbstractEpollUnsafe newUnsafe()
  {
    return new EpollDomainUnsafe(null);
  }
  
  protected DomainSocketAddress localAddress0()
  {
    return local;
  }
  
  protected DomainSocketAddress remoteAddress0()
  {
    return remote;
  }
  
  protected void doBind(SocketAddress localAddress) throws Exception
  {
    socket.bind(localAddress);
    local = ((DomainSocketAddress)localAddress);
  }
  
  public EpollDomainSocketChannelConfig config()
  {
    return config;
  }
  
  protected boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception
  {
    if (super.doConnect(remoteAddress, localAddress)) {
      local = ((DomainSocketAddress)localAddress);
      remote = ((DomainSocketAddress)remoteAddress);
      return true;
    }
    return false;
  }
  
  public DomainSocketAddress remoteAddress()
  {
    return (DomainSocketAddress)super.remoteAddress();
  }
  
  public DomainSocketAddress localAddress()
  {
    return (DomainSocketAddress)super.localAddress();
  }
  
  protected int doWriteSingle(ChannelOutboundBuffer in) throws Exception
  {
    Object msg = in.current();
    if (((msg instanceof FileDescriptor)) && (socket.sendFd(((FileDescriptor)msg).intValue()) > 0))
    {
      in.remove();
      return 1;
    }
    return super.doWriteSingle(in);
  }
  
  protected Object filterOutboundMessage(Object msg)
  {
    if ((msg instanceof FileDescriptor)) {
      return msg;
    }
    return super.filterOutboundMessage(msg);
  }
  





  public PeerCredentials peerCredentials()
    throws IOException { return socket.getPeerCredentials(); }
  
  private final class EpollDomainUnsafe extends AbstractEpollStreamChannel.EpollStreamUnsafe {
    private EpollDomainUnsafe() { super(); }
    
    void epollInReady() {
      switch (EpollDomainSocketChannel.1.$SwitchMap$io$netty$channel$unix$DomainSocketReadMode[config().getReadMode().ordinal()]) {
      case 1: 
        super.epollInReady();
        break;
      case 2: 
        epollInReadFd();
        break;
      default: 
        throw new Error();
      }
    }
    
    private void epollInReadFd() {
      if (socket.isInputShutdown()) {
        clearEpollIn0();
        return;
      }
      ChannelConfig config = config();
      EpollRecvByteAllocatorHandle allocHandle = recvBufAllocHandle();
      allocHandle.edgeTriggered(isFlagSet(Native.EPOLLET));
      
      ChannelPipeline pipeline = pipeline();
      allocHandle.reset(config);
      epollInBefore();
      

      try
      {
        do
        {
          allocHandle.lastBytesRead(socket.recvFd());
          switch (allocHandle.lastBytesRead()) {
          case 0: 
            break;
          case -1: 
            close(voidPromise());
            return;
          default: 
            allocHandle.incMessagesRead(1);
            readPending = false;
            pipeline.fireChannelRead(new FileDescriptor(allocHandle.lastBytesRead()));
          }
          
        } while (allocHandle.continueReading());
        
        allocHandle.readComplete();
        pipeline.fireChannelReadComplete();
      } catch (Throwable t) {
        allocHandle.readComplete();
        pipeline.fireChannelReadComplete();
        pipeline.fireExceptionCaught(t);
      } finally {
        epollInFinally(config);
      }
    }
  }
}
