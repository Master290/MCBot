package io.netty.channel.socket.nio;

import io.netty.buffer.ByteBuf;
import io.netty.channel.AbstractChannel.AbstractUnsafe;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.FileRegion;
import io.netty.channel.RecvByteBufAllocator.Handle;
import io.netty.channel.nio.AbstractNioByteChannel;
import io.netty.channel.nio.AbstractNioByteChannel.NioByteUnsafe;
import io.netty.channel.nio.AbstractNioChannel.AbstractNioUnsafe;
import io.netty.channel.nio.AbstractNioChannel.NioUnsafe;
import io.netty.channel.socket.DefaultSocketChannelConfig;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannelConfig;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.SocketUtils;
import io.netty.util.internal.SuppressJava6Requirement;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.SelectorProvider;
import java.util.Map;
import java.util.concurrent.Executor;

















public class NioSocketChannel
  extends AbstractNioByteChannel
  implements io.netty.channel.socket.SocketChannel
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(NioSocketChannel.class);
  private static final SelectorProvider DEFAULT_SELECTOR_PROVIDER = SelectorProvider.provider();
  

  private final SocketChannelConfig config;
  

  private static java.nio.channels.SocketChannel newSocket(SelectorProvider provider)
  {
    try
    {
      return provider.openSocketChannel();
    } catch (IOException e) {
      throw new ChannelException("Failed to open a socket.", e);
    }
  }
  




  public NioSocketChannel()
  {
    this(DEFAULT_SELECTOR_PROVIDER);
  }
  


  public NioSocketChannel(SelectorProvider provider)
  {
    this(newSocket(provider));
  }
  


  public NioSocketChannel(java.nio.channels.SocketChannel socket)
  {
    this(null, socket);
  }
  





  public NioSocketChannel(Channel parent, java.nio.channels.SocketChannel socket)
  {
    super(parent, socket);
    config = new NioSocketChannelConfig(this, socket.socket(), null);
  }
  
  public ServerSocketChannel parent()
  {
    return (ServerSocketChannel)super.parent();
  }
  
  public SocketChannelConfig config()
  {
    return config;
  }
  
  protected java.nio.channels.SocketChannel javaChannel()
  {
    return (java.nio.channels.SocketChannel)super.javaChannel();
  }
  
  public boolean isActive()
  {
    java.nio.channels.SocketChannel ch = javaChannel();
    return (ch.isOpen()) && (ch.isConnected());
  }
  
  public boolean isOutputShutdown()
  {
    return (javaChannel().socket().isOutputShutdown()) || (!isActive());
  }
  
  public boolean isInputShutdown()
  {
    return (javaChannel().socket().isInputShutdown()) || (!isActive());
  }
  
  public boolean isShutdown()
  {
    Socket socket = javaChannel().socket();
    return ((socket.isInputShutdown()) && (socket.isOutputShutdown())) || (!isActive());
  }
  
  public InetSocketAddress localAddress()
  {
    return (InetSocketAddress)super.localAddress();
  }
  
  public InetSocketAddress remoteAddress()
  {
    return (InetSocketAddress)super.remoteAddress();
  }
  
  @SuppressJava6Requirement(reason="Usage guarded by java version check")
  protected final void doShutdownOutput()
    throws Exception
  {
    if (PlatformDependent.javaVersion() >= 7) {
      javaChannel().shutdownOutput();
    } else {
      javaChannel().socket().shutdownOutput();
    }
  }
  
  public ChannelFuture shutdownOutput()
  {
    return shutdownOutput(newPromise());
  }
  
  public ChannelFuture shutdownOutput(final ChannelPromise promise)
  {
    EventLoop loop = eventLoop();
    if (loop.inEventLoop()) {
      ((AbstractChannel.AbstractUnsafe)unsafe()).shutdownOutput(promise);
    } else {
      loop.execute(new Runnable()
      {
        public void run() {
          ((AbstractChannel.AbstractUnsafe)unsafe()).shutdownOutput(promise);
        }
      });
    }
    return promise;
  }
  
  public ChannelFuture shutdownInput()
  {
    return shutdownInput(newPromise());
  }
  
  protected boolean isInputShutdown0()
  {
    return isInputShutdown();
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
          NioSocketChannel.this.shutdownInput0(promise);
        }
      });
    }
    return promise;
  }
  
  public ChannelFuture shutdown()
  {
    return shutdown(newPromise());
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
          NioSocketChannel.this.shutdownOutputDone(shutdownOutputFuture, promise);
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
          NioSocketChannel.shutdownDone(shutdownOutputFuture, shutdownInputFuture, promise);
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
  
  private void shutdownInput0(ChannelPromise promise) {
    try { shutdownInput0();
      promise.setSuccess();
    } catch (Throwable t) {
      promise.setFailure(t);
    }
  }
  
  @SuppressJava6Requirement(reason="Usage guarded by java version check")
  private void shutdownInput0() throws Exception {
    if (PlatformDependent.javaVersion() >= 7) {
      javaChannel().shutdownInput();
    } else {
      javaChannel().socket().shutdownInput();
    }
  }
  
  protected SocketAddress localAddress0()
  {
    return javaChannel().socket().getLocalSocketAddress();
  }
  
  protected SocketAddress remoteAddress0()
  {
    return javaChannel().socket().getRemoteSocketAddress();
  }
  
  protected void doBind(SocketAddress localAddress) throws Exception
  {
    doBind0(localAddress);
  }
  
  private void doBind0(SocketAddress localAddress) throws Exception {
    if (PlatformDependent.javaVersion() >= 7) {
      SocketUtils.bind(javaChannel(), localAddress);
    } else {
      SocketUtils.bind(javaChannel().socket(), localAddress);
    }
  }
  
  protected boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception
  {
    if (localAddress != null) {
      doBind0(localAddress);
    }
    
    boolean success = false;
    try {
      boolean connected = SocketUtils.connect(javaChannel(), remoteAddress);
      if (!connected) {
        selectionKey().interestOps(8);
      }
      success = true;
      return connected;
    } finally {
      if (!success) {
        doClose();
      }
    }
  }
  
  protected void doFinishConnect() throws Exception
  {
    if (!javaChannel().finishConnect()) {
      throw new Error();
    }
  }
  
  protected void doDisconnect() throws Exception
  {
    doClose();
  }
  
  protected void doClose() throws Exception
  {
    super.doClose();
    javaChannel().close();
  }
  
  protected int doReadBytes(ByteBuf byteBuf) throws Exception
  {
    RecvByteBufAllocator.Handle allocHandle = unsafe().recvBufAllocHandle();
    allocHandle.attemptedBytesRead(byteBuf.writableBytes());
    return byteBuf.writeBytes(javaChannel(), allocHandle.attemptedBytesRead());
  }
  
  protected int doWriteBytes(ByteBuf buf) throws Exception
  {
    int expectedWrittenBytes = buf.readableBytes();
    return buf.readBytes(javaChannel(), expectedWrittenBytes);
  }
  
  protected long doWriteFileRegion(FileRegion region) throws Exception
  {
    long position = region.transferred();
    return region.transferTo(javaChannel(), position);
  }
  


  private void adjustMaxBytesPerGatheringWrite(int attempted, int written, int oldMaxBytesPerGatheringWrite)
  {
    if (attempted == written) {
      if (attempted << 1 > oldMaxBytesPerGatheringWrite) {
        ((NioSocketChannelConfig)config).setMaxBytesPerGatheringWrite(attempted << 1);
      }
    } else if ((attempted > 4096) && (written < attempted >>> 1)) {
      ((NioSocketChannelConfig)config).setMaxBytesPerGatheringWrite(attempted >>> 1);
    }
  }
  
  protected void doWrite(ChannelOutboundBuffer in) throws Exception
  {
    java.nio.channels.SocketChannel ch = javaChannel();
    int writeSpinCount = config().getWriteSpinCount();
    do {
      if (in.isEmpty())
      {
        clearOpWrite();
        
        return;
      }
      

      int maxBytesPerGatheringWrite = ((NioSocketChannelConfig)config).getMaxBytesPerGatheringWrite();
      ByteBuffer[] nioBuffers = in.nioBuffers(1024, maxBytesPerGatheringWrite);
      int nioBufferCnt = in.nioBufferCount();
      


      switch (nioBufferCnt)
      {
      case 0: 
        writeSpinCount -= doWrite0(in);
        break;
      


      case 1: 
        ByteBuffer buffer = nioBuffers[0];
        int attemptedBytes = buffer.remaining();
        int localWrittenBytes = ch.write(buffer);
        if (localWrittenBytes <= 0) {
          incompleteWrite(true);
          return;
        }
        adjustMaxBytesPerGatheringWrite(attemptedBytes, localWrittenBytes, maxBytesPerGatheringWrite);
        in.removeBytes(localWrittenBytes);
        writeSpinCount--;
        break;
      



      default: 
        long attemptedBytes = in.nioBufferSize();
        long localWrittenBytes = ch.write(nioBuffers, 0, nioBufferCnt);
        if (localWrittenBytes <= 0L) {
          incompleteWrite(true);
          return;
        }
        
        adjustMaxBytesPerGatheringWrite((int)attemptedBytes, (int)localWrittenBytes, maxBytesPerGatheringWrite);
        
        in.removeBytes(localWrittenBytes);
        writeSpinCount--;
      
      }
      
    } while (writeSpinCount > 0);
    
    incompleteWrite(writeSpinCount < 0);
  }
  


  protected AbstractNioChannel.AbstractNioUnsafe newUnsafe() { return new NioSocketChannelUnsafe(null); }
  
  private final class NioSocketChannelUnsafe extends AbstractNioByteChannel.NioByteUnsafe {
    private NioSocketChannelUnsafe() { super(); }
    
    protected Executor prepareToClose() {
      try {
        if ((javaChannel().isOpen()) && (config().getSoLinger() > 0))
        {



          doDeregister();
          return GlobalEventExecutor.INSTANCE;
        }
      }
      catch (Throwable localThrowable) {}
      


      return null;
    }
  }
  
  private final class NioSocketChannelConfig extends DefaultSocketChannelConfig {
    private volatile int maxBytesPerGatheringWrite = Integer.MAX_VALUE;
    
    private NioSocketChannelConfig(NioSocketChannel channel, Socket javaSocket) { super(javaSocket);
      calculateMaxBytesPerGatheringWrite();
    }
    
    protected void autoReadCleared()
    {
      clearReadPending();
    }
    
    public NioSocketChannelConfig setSendBufferSize(int sendBufferSize)
    {
      super.setSendBufferSize(sendBufferSize);
      calculateMaxBytesPerGatheringWrite();
      return this;
    }
    
    public <T> boolean setOption(ChannelOption<T> option, T value)
    {
      if ((PlatformDependent.javaVersion() >= 7) && ((option instanceof NioChannelOption))) {
        return NioChannelOption.setOption(jdkChannel(), (NioChannelOption)option, value);
      }
      return super.setOption(option, value);
    }
    
    public <T> T getOption(ChannelOption<T> option)
    {
      if ((PlatformDependent.javaVersion() >= 7) && ((option instanceof NioChannelOption))) {
        return NioChannelOption.getOption(jdkChannel(), (NioChannelOption)option);
      }
      return super.getOption(option);
    }
    
    public Map<ChannelOption<?>, Object> getOptions()
    {
      if (PlatformDependent.javaVersion() >= 7) {
        return getOptions(super.getOptions(), NioChannelOption.getOptions(jdkChannel()));
      }
      return super.getOptions();
    }
    
    void setMaxBytesPerGatheringWrite(int maxBytesPerGatheringWrite) {
      this.maxBytesPerGatheringWrite = maxBytesPerGatheringWrite;
    }
    
    int getMaxBytesPerGatheringWrite() {
      return maxBytesPerGatheringWrite;
    }
    
    private void calculateMaxBytesPerGatheringWrite()
    {
      int newSendBufferSize = getSendBufferSize() << 1;
      if (newSendBufferSize > 0) {
        setMaxBytesPerGatheringWrite(newSendBufferSize);
      }
    }
    
    private java.nio.channels.SocketChannel jdkChannel() {
      return ((NioSocketChannel)channel).javaChannel();
    }
  }
}
