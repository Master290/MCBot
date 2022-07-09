package io.netty.channel.socket.oio;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ConnectTimeoutException;
import io.netty.channel.EventLoop;
import io.netty.channel.oio.OioByteStreamChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.internal.SocketUtils;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;





















@Deprecated
public class OioSocketChannel
  extends OioByteStreamChannel
  implements SocketChannel
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(OioSocketChannel.class);
  
  private final Socket socket;
  
  private final OioSocketChannelConfig config;
  

  public OioSocketChannel()
  {
    this(new Socket());
  }
  




  public OioSocketChannel(Socket socket)
  {
    this(null, socket);
  }
  






  public OioSocketChannel(Channel parent, Socket socket)
  {
    super(parent);
    this.socket = socket;
    config = new DefaultOioSocketChannelConfig(this, socket);
    
    boolean success = false;
    try {
      if (socket.isConnected()) {
        activate(socket.getInputStream(), socket.getOutputStream());
      }
      socket.setSoTimeout(1000);
      success = true; return;
    } catch (Exception e) {
      throw new ChannelException("failed to initialize a socket", e);
    } finally {
      if (!success) {
        try {
          socket.close();
        } catch (IOException e) {
          logger.warn("Failed to close a socket.", e);
        }
      }
    }
  }
  
  public ServerSocketChannel parent()
  {
    return (ServerSocketChannel)super.parent();
  }
  
  public OioSocketChannelConfig config()
  {
    return config;
  }
  
  public boolean isOpen()
  {
    return !socket.isClosed();
  }
  
  public boolean isActive()
  {
    return (!socket.isClosed()) && (socket.isConnected());
  }
  
  public boolean isOutputShutdown()
  {
    return (socket.isOutputShutdown()) || (!isActive());
  }
  
  public boolean isInputShutdown()
  {
    return (socket.isInputShutdown()) || (!isActive());
  }
  
  public boolean isShutdown()
  {
    return ((socket.isInputShutdown()) && (socket.isOutputShutdown())) || (!isActive());
  }
  
  protected final void doShutdownOutput()
    throws Exception
  {
    shutdownOutput0();
  }
  
  public ChannelFuture shutdownOutput()
  {
    return shutdownOutput(newPromise());
  }
  
  public ChannelFuture shutdownInput()
  {
    return shutdownInput(newPromise());
  }
  
  public ChannelFuture shutdown()
  {
    return shutdown(newPromise());
  }
  
  protected int doReadBytes(ByteBuf buf) throws Exception
  {
    if (socket.isClosed()) {
      return -1;
    }
    try {
      return super.doReadBytes(buf);
    } catch (SocketTimeoutException ignored) {}
    return 0;
  }
  

  public ChannelFuture shutdownOutput(final ChannelPromise promise)
  {
    EventLoop loop = eventLoop();
    if (loop.inEventLoop()) {
      shutdownOutput0(promise);
    } else {
      loop.execute(new Runnable()
      {
        public void run() {
          OioSocketChannel.this.shutdownOutput0(promise);
        }
      });
    }
    return promise;
  }
  
  private void shutdownOutput0(ChannelPromise promise) {
    try {
      shutdownOutput0();
      promise.setSuccess();
    } catch (Throwable t) {
      promise.setFailure(t);
    }
  }
  
  private void shutdownOutput0() throws IOException {
    socket.shutdownOutput();
  }
  
  public ChannelFuture shutdownInput(final ChannelPromise promise)
  {
    EventLoop loop = eventLoop();
    if (loop.inEventLoop()) {
      shutdownInput0(promise);
    } else {
      loop.execute(new Runnable()
      {
        public void run() {
          OioSocketChannel.this.shutdownInput0(promise);
        }
      });
    }
    return promise;
  }
  
  private void shutdownInput0(ChannelPromise promise) {
    try {
      socket.shutdownInput();
      promise.setSuccess();
    } catch (Throwable t) {
      promise.setFailure(t);
    }
  }
  
  public ChannelFuture shutdown(final ChannelPromise promise)
  {
    ChannelFuture shutdownOutputFuture = shutdownOutput();
    if (shutdownOutputFuture.isDone()) {
      shutdownOutputDone(shutdownOutputFuture, promise);
    } else {
      shutdownOutputFuture.addListener(new ChannelFutureListener()
      {
        public void operationComplete(ChannelFuture shutdownOutputFuture) throws Exception {
          OioSocketChannel.this.shutdownOutputDone(shutdownOutputFuture, promise);
        }
      });
    }
    return promise;
  }
  
  private void shutdownOutputDone(final ChannelFuture shutdownOutputFuture, final ChannelPromise promise) {
    ChannelFuture shutdownInputFuture = shutdownInput();
    if (shutdownInputFuture.isDone()) {
      shutdownDone(shutdownOutputFuture, shutdownInputFuture, promise);
    } else {
      shutdownInputFuture.addListener(new ChannelFutureListener()
      {
        public void operationComplete(ChannelFuture shutdownInputFuture) throws Exception {
          OioSocketChannel.shutdownDone(shutdownOutputFuture, shutdownInputFuture, promise);
        }
      });
    }
  }
  

  private static void shutdownDone(ChannelFuture shutdownOutputFuture, ChannelFuture shutdownInputFuture, ChannelPromise promise)
  {
    Throwable shutdownOutputCause = shutdownOutputFuture.cause();
    Throwable shutdownInputCause = shutdownInputFuture.cause();
    if (shutdownOutputCause != null) {
      if (shutdownInputCause != null) {
        logger.debug("Exception suppressed because a previous exception occurred.", shutdownInputCause);
      }
      
      promise.setFailure(shutdownOutputCause);
    } else if (shutdownInputCause != null) {
      promise.setFailure(shutdownInputCause);
    } else {
      promise.setSuccess();
    }
  }
  
  public InetSocketAddress localAddress()
  {
    return (InetSocketAddress)super.localAddress();
  }
  
  public InetSocketAddress remoteAddress()
  {
    return (InetSocketAddress)super.remoteAddress();
  }
  
  protected SocketAddress localAddress0()
  {
    return socket.getLocalSocketAddress();
  }
  
  protected SocketAddress remoteAddress0()
  {
    return socket.getRemoteSocketAddress();
  }
  
  protected void doBind(SocketAddress localAddress) throws Exception
  {
    SocketUtils.bind(socket, localAddress);
  }
  
  protected void doConnect(SocketAddress remoteAddress, SocketAddress localAddress)
    throws Exception
  {
    if (localAddress != null) {
      SocketUtils.bind(socket, localAddress);
    }
    
    boolean success = false;
    try {
      SocketUtils.connect(socket, remoteAddress, config().getConnectTimeoutMillis());
      activate(socket.getInputStream(), socket.getOutputStream());
      success = true;
    } catch (SocketTimeoutException e) {
      ConnectTimeoutException cause = new ConnectTimeoutException("connection timed out: " + remoteAddress);
      cause.setStackTrace(e.getStackTrace());
      throw cause;
    } finally {
      if (!success) {
        doClose();
      }
    }
  }
  
  protected void doDisconnect() throws Exception
  {
    doClose();
  }
  
  protected void doClose() throws Exception
  {
    socket.close();
  }
  
  protected boolean checkInputShutdown() {
    if (isInputShutdown()) {
      try {
        Thread.sleep(config().getSoTimeout());
      }
      catch (Throwable localThrowable) {}
      
      return true;
    }
    return false;
  }
  
  @Deprecated
  protected void setReadPending(boolean readPending)
  {
    super.setReadPending(readPending);
  }
  
  final void clearReadPending0() {
    clearReadPending();
  }
}
