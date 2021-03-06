package io.netty.channel.kqueue;

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


















public final class KQueueDomainSocketChannel
  extends AbstractKQueueStreamChannel
  implements DomainSocketChannel
{
  private final KQueueDomainSocketChannelConfig config = new KQueueDomainSocketChannelConfig(this);
  private volatile DomainSocketAddress local;
  private volatile DomainSocketAddress remote;
  
  public KQueueDomainSocketChannel()
  {
    super(null, BsdSocket.newSocketDomain(), false);
  }
  
  public KQueueDomainSocketChannel(int fd) {
    this(null, new BsdSocket(fd));
  }
  
  KQueueDomainSocketChannel(Channel parent, BsdSocket fd) {
    super(parent, fd, true);
  }
  
  protected AbstractKQueueChannel.AbstractKQueueUnsafe newUnsafe()
  {
    return new KQueueDomainUnsafe(null);
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
  
  public KQueueDomainSocketChannelConfig config()
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
  
  private final class KQueueDomainUnsafe extends AbstractKQueueStreamChannel.KQueueStreamUnsafe {
    private KQueueDomainUnsafe() { super(); }
    
    void readReady(KQueueRecvByteAllocatorHandle allocHandle) {
      switch (KQueueDomainSocketChannel.1.$SwitchMap$io$netty$channel$unix$DomainSocketReadMode[config().getReadMode().ordinal()]) {
      case 1: 
        super.readReady(allocHandle);
        break;
      case 2: 
        readReadyFd();
        break;
      default: 
        throw new Error();
      }
    }
    
    private void readReadyFd() {
      if (socket.isInputShutdown()) {
        super.clearReadFilter0();
        return;
      }
      ChannelConfig config = config();
      KQueueRecvByteAllocatorHandle allocHandle = recvBufAllocHandle();
      
      ChannelPipeline pipeline = pipeline();
      allocHandle.reset(config);
      readReadyBefore();
      

      try
      {
        do
        {
          int recvFd = socket.recvFd();
          switch (recvFd) {
          case 0: 
            allocHandle.lastBytesRead(0);
            break;
          case -1: 
            allocHandle.lastBytesRead(-1);
            close(voidPromise());
            return;
          default: 
            allocHandle.lastBytesRead(1);
            allocHandle.incMessagesRead(1);
            readPending = false;
            pipeline.fireChannelRead(new FileDescriptor(recvFd));
          }
          
        } while (allocHandle.continueReading());
        
        allocHandle.readComplete();
        pipeline.fireChannelReadComplete();
      } catch (Throwable t) {
        allocHandle.readComplete();
        pipeline.fireChannelReadComplete();
        pipeline.fireExceptionCaught(t);
      } finally {
        readReadyFinally(config);
      }
    }
  }
}
