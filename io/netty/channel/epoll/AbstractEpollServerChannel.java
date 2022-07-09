package io.netty.channel.epoll;

import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.ServerChannel;
import java.net.InetSocketAddress;
import java.net.SocketAddress;














public abstract class AbstractEpollServerChannel
  extends AbstractEpollChannel
  implements ServerChannel
{
  private static final ChannelMetadata METADATA = new ChannelMetadata(false, 16);
  
  protected AbstractEpollServerChannel(int fd) {
    this(new LinuxSocket(fd), false);
  }
  
  AbstractEpollServerChannel(LinuxSocket fd) {
    this(fd, isSoErrorZero(fd));
  }
  
  AbstractEpollServerChannel(LinuxSocket fd, boolean active) {
    super(null, fd, active);
  }
  
  public ChannelMetadata metadata()
  {
    return METADATA;
  }
  
  protected boolean isCompatible(EventLoop loop)
  {
    return loop instanceof EpollEventLoop;
  }
  
  protected InetSocketAddress remoteAddress0()
  {
    return null;
  }
  
  protected AbstractEpollChannel.AbstractEpollUnsafe newUnsafe()
  {
    return new EpollServerSocketUnsafe();
  }
  
  protected void doWrite(ChannelOutboundBuffer in) throws Exception
  {
    throw new UnsupportedOperationException();
  }
  


  protected Object filterOutboundMessage(Object msg) throws Exception { throw new UnsupportedOperationException(); }
  
  abstract Channel newChildChannel(int paramInt1, byte[] paramArrayOfByte, int paramInt2, int paramInt3) throws Exception;
  
  final class EpollServerSocketUnsafe extends AbstractEpollChannel.AbstractEpollUnsafe {
    EpollServerSocketUnsafe() { super(); }
    


    private final byte[] acceptedAddress = new byte[26];
    

    public void connect(SocketAddress socketAddress, SocketAddress socketAddress2, ChannelPromise channelPromise)
    {
      channelPromise.setFailure(new UnsupportedOperationException());
    }
    
    void epollInReady()
    {
      assert (eventLoop().inEventLoop());
      ChannelConfig config = config();
      if (shouldBreakEpollInReady(config)) {
        clearEpollIn0();
        return;
      }
      EpollRecvByteAllocatorHandle allocHandle = recvBufAllocHandle();
      allocHandle.edgeTriggered(isFlagSet(Native.EPOLLET));
      
      ChannelPipeline pipeline = pipeline();
      allocHandle.reset(config);
      allocHandle.attemptedBytesRead(1);
      epollInBefore();
      
      Throwable exception = null;
      try
      {
        try
        {
          do
          {
            allocHandle.lastBytesRead(socket.accept(acceptedAddress));
            if (allocHandle.lastBytesRead() == -1) {
              break;
            }
            
            allocHandle.incMessagesRead(1);
            
            readPending = false;
            pipeline.fireChannelRead(newChildChannel(allocHandle.lastBytesRead(), acceptedAddress, 1, acceptedAddress[0]));
          }
          while (allocHandle.continueReading());
        } catch (Throwable t) {
          exception = t;
        }
        allocHandle.readComplete();
        pipeline.fireChannelReadComplete();
        
        if (exception != null) {
          pipeline.fireExceptionCaught(exception);
        }
      } finally {
        epollInFinally(config);
      }
    }
  }
  
  protected boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception
  {
    throw new UnsupportedOperationException();
  }
}
