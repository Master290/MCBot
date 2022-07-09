package io.netty.channel.kqueue;

import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.channel.ServerChannel;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
















public abstract class AbstractKQueueServerChannel
  extends AbstractKQueueChannel
  implements ServerChannel
{
  private static final ChannelMetadata METADATA = new ChannelMetadata(false, 16);
  
  AbstractKQueueServerChannel(BsdSocket fd) {
    this(fd, isSoErrorZero(fd));
  }
  
  AbstractKQueueServerChannel(BsdSocket fd, boolean active) {
    super(null, fd, active);
  }
  
  public ChannelMetadata metadata()
  {
    return METADATA;
  }
  
  protected boolean isCompatible(EventLoop loop)
  {
    return loop instanceof KQueueEventLoop;
  }
  
  protected InetSocketAddress remoteAddress0()
  {
    return null;
  }
  
  protected AbstractKQueueChannel.AbstractKQueueUnsafe newUnsafe()
  {
    return new KQueueServerSocketUnsafe();
  }
  
  protected void doWrite(ChannelOutboundBuffer in) throws Exception
  {
    throw new UnsupportedOperationException();
  }
  
  protected Object filterOutboundMessage(Object msg) throws Exception
  {
    throw new UnsupportedOperationException();
  }
  

  abstract Channel newChildChannel(int paramInt1, byte[] paramArrayOfByte, int paramInt2, int paramInt3) throws Exception;
  

  protected boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception { throw new UnsupportedOperationException(); }
  
  final class KQueueServerSocketUnsafe extends AbstractKQueueChannel.AbstractKQueueUnsafe {
    KQueueServerSocketUnsafe() { super(); }
    


    private final byte[] acceptedAddress = new byte[26];
    
    void readReady(KQueueRecvByteAllocatorHandle allocHandle)
    {
      assert (eventLoop().inEventLoop());
      ChannelConfig config = config();
      if (shouldBreakReadReady(config)) {
        clearReadFilter0();
        return;
      }
      ChannelPipeline pipeline = pipeline();
      allocHandle.reset(config);
      allocHandle.attemptedBytesRead(1);
      readReadyBefore();
      
      Throwable exception = null;
      try {
        try {
          do {
            int acceptFd = socket.accept(acceptedAddress);
            if (acceptFd == -1)
            {
              allocHandle.lastBytesRead(-1);
              break;
            }
            allocHandle.lastBytesRead(1);
            allocHandle.incMessagesRead(1);
            
            readPending = false;
            pipeline.fireChannelRead(newChildChannel(acceptFd, acceptedAddress, 1, acceptedAddress[0]));
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
        readReadyFinally(config);
      }
    }
  }
}
