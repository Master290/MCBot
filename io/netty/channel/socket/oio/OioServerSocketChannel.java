package io.netty.channel.socket.oio;

import io.netty.channel.ChannelException;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.oio.AbstractOioMessageChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.SocketUtils;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.List;
























@Deprecated
public class OioServerSocketChannel
  extends AbstractOioMessageChannel
  implements ServerSocketChannel
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(OioServerSocketChannel.class);
  
  private static final ChannelMetadata METADATA = new ChannelMetadata(false, 1);
  final ServerSocket socket;
  private final OioServerSocketChannelConfig config;
  
  private static ServerSocket newServerSocket() { try { return new ServerSocket();
    } catch (IOException e) {
      throw new ChannelException("failed to create a server socket", e);
    }
  }
  





  public OioServerSocketChannel()
  {
    this(newServerSocket());
  }
  




  public OioServerSocketChannel(ServerSocket socket)
  {
    super(null);
    ObjectUtil.checkNotNull(socket, "socket");
    
    boolean success = false;
    try {
      socket.setSoTimeout(1000);
      success = true;
      



      if (!success) {
        try {
          socket.close();
        } catch (IOException e) {
          if (logger.isWarnEnabled()) {
            logger.warn("Failed to close a partially initialized socket.", e);
          }
        }
      }
      

      this.socket = socket;
    }
    catch (IOException e)
    {
      throw new ChannelException("Failed to set the server socket timeout.", e);
    }
    finally {
      if (!success) {
        try {
          socket.close();
        } catch (IOException e) {
          if (logger.isWarnEnabled()) {
            logger.warn("Failed to close a partially initialized socket.", e);
          }
        }
      }
    }
    

    config = new DefaultOioServerSocketChannelConfig(this, socket);
  }
  
  public InetSocketAddress localAddress()
  {
    return (InetSocketAddress)super.localAddress();
  }
  
  public ChannelMetadata metadata()
  {
    return METADATA;
  }
  
  public OioServerSocketChannelConfig config()
  {
    return config;
  }
  
  public InetSocketAddress remoteAddress()
  {
    return null;
  }
  
  public boolean isOpen()
  {
    return !socket.isClosed();
  }
  
  public boolean isActive()
  {
    return (isOpen()) && (socket.isBound());
  }
  
  protected SocketAddress localAddress0()
  {
    return SocketUtils.localSocketAddress(socket);
  }
  
  protected void doBind(SocketAddress localAddress) throws Exception
  {
    socket.bind(localAddress, config.getBacklog());
  }
  
  protected void doClose() throws Exception
  {
    socket.close();
  }
  
  protected int doReadMessages(List<Object> buf) throws Exception
  {
    if (socket.isClosed()) {
      return -1;
    }
    try
    {
      Socket s = socket.accept();
      try {
        buf.add(new OioSocketChannel(this, s));
        return 1;
      } catch (Throwable t) {
        logger.warn("Failed to create a new channel from an accepted socket.", t);
        try {
          s.close();
        } catch (Throwable t2) {
          logger.warn("Failed to close a socket.", t2);
        }
      }
      


      return 0;
    } catch (SocketTimeoutException localSocketTimeoutException) {}
  }
  
  protected void doWrite(ChannelOutboundBuffer in) throws Exception {
    throw new UnsupportedOperationException();
  }
  
  protected Object filterOutboundMessage(Object msg) throws Exception
  {
    throw new UnsupportedOperationException();
  }
  
  protected void doConnect(SocketAddress remoteAddress, SocketAddress localAddress)
    throws Exception
  {
    throw new UnsupportedOperationException();
  }
  
  protected SocketAddress remoteAddress0()
  {
    return null;
  }
  
  protected void doDisconnect() throws Exception
  {
    throw new UnsupportedOperationException();
  }
  
  @Deprecated
  protected void setReadPending(boolean readPending)
  {
    super.setReadPending(readPending);
  }
  
  final void clearReadPending0() {
    super.clearReadPending();
  }
}
