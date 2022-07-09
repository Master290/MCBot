package io.netty.channel.kqueue;

import io.netty.channel.Channel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;















public final class KQueueSocketChannel
  extends AbstractKQueueStreamChannel
  implements SocketChannel
{
  private final KQueueSocketChannelConfig config;
  
  public KQueueSocketChannel()
  {
    super(null, BsdSocket.newSocketStream(), false);
    config = new KQueueSocketChannelConfig(this);
  }
  
  public KQueueSocketChannel(int fd) {
    super(new BsdSocket(fd));
    config = new KQueueSocketChannelConfig(this);
  }
  
  KQueueSocketChannel(Channel parent, BsdSocket fd, InetSocketAddress remoteAddress) {
    super(parent, fd, remoteAddress);
    config = new KQueueSocketChannelConfig(this);
  }
  
  public InetSocketAddress remoteAddress()
  {
    return (InetSocketAddress)super.remoteAddress();
  }
  
  public InetSocketAddress localAddress()
  {
    return (InetSocketAddress)super.localAddress();
  }
  
  public KQueueSocketChannelConfig config()
  {
    return config;
  }
  
  public ServerSocketChannel parent()
  {
    return (ServerSocketChannel)super.parent();
  }
  


  protected AbstractKQueueChannel.AbstractKQueueUnsafe newUnsafe() { return new KQueueSocketChannelUnsafe(null); }
  
  private final class KQueueSocketChannelUnsafe extends AbstractKQueueStreamChannel.KQueueStreamUnsafe {
    private KQueueSocketChannelUnsafe() { super(); }
    
    protected Executor prepareToClose()
    {
      try
      {
        if ((isOpen()) && (config().getSoLinger() > 0))
        {



          ((KQueueEventLoop)eventLoop()).remove(KQueueSocketChannel.this);
          return GlobalEventExecutor.INSTANCE;
        }
      }
      catch (Throwable localThrowable) {}
      


      return null;
    }
  }
}
