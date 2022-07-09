package io.netty.channel.epoll;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.AbstractChannel;
import io.netty.channel.AbstractChannel.AbstractUnsafe;
import io.netty.channel.Channel;
import io.netty.channel.Channel.Unsafe;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ConnectTimeoutException;
import io.netty.channel.EventLoop;
import io.netty.channel.RecvByteBufAllocator.ExtendedHandle;
import io.netty.channel.RecvByteBufAllocator.Handle;
import io.netty.channel.socket.ChannelInputShutdownEvent;
import io.netty.channel.socket.ChannelInputShutdownReadComplete;
import io.netty.channel.socket.SocketChannelConfig;
import io.netty.channel.unix.FileDescriptor;
import io.netty.channel.unix.IovArray;
import io.netty.channel.unix.Socket;
import io.netty.channel.unix.UnixChannel;
import io.netty.channel.unix.UnixChannelUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.ObjectUtil;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ConnectionPendingException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.UnresolvedAddressException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;












abstract class AbstractEpollChannel
  extends AbstractChannel
  implements UnixChannel
{
  private static final ChannelMetadata METADATA = new ChannelMetadata(false);
  
  final LinuxSocket socket;
  
  private ChannelPromise connectPromise;
  
  private ScheduledFuture<?> connectTimeoutFuture;
  
  private SocketAddress requestedRemoteAddress;
  
  private volatile SocketAddress local;
  
  private volatile SocketAddress remote;
  protected int flags = Native.EPOLLET;
  boolean inputClosedSeenErrorOnRead;
  boolean epollInReadyRunnablePending;
  protected volatile boolean active;
  
  AbstractEpollChannel(LinuxSocket fd)
  {
    this(null, fd, false);
  }
  
  AbstractEpollChannel(Channel parent, LinuxSocket fd, boolean active) {
    super(parent);
    socket = ((LinuxSocket)ObjectUtil.checkNotNull(fd, "fd"));
    this.active = active;
    if (active)
    {

      local = fd.localAddress();
      remote = fd.remoteAddress();
    }
  }
  
  AbstractEpollChannel(Channel parent, LinuxSocket fd, SocketAddress remote) {
    super(parent);
    socket = ((LinuxSocket)ObjectUtil.checkNotNull(fd, "fd"));
    active = true;
    

    this.remote = remote;
    local = fd.localAddress();
  }
  
  static boolean isSoErrorZero(Socket fd) {
    try {
      return fd.getSoError() == 0;
    } catch (IOException e) {
      throw new ChannelException(e);
    }
  }
  
  void setFlag(int flag) throws IOException {
    if (!isFlagSet(flag)) {
      flags |= flag;
      modifyEvents();
    }
  }
  
  void clearFlag(int flag) throws IOException {
    if (isFlagSet(flag)) {
      flags &= (flag ^ 0xFFFFFFFF);
      modifyEvents();
    }
  }
  
  boolean isFlagSet(int flag) {
    return (flags & flag) != 0;
  }
  
  public final FileDescriptor fd()
  {
    return socket;
  }
  

  public abstract EpollChannelConfig config();
  
  public boolean isActive()
  {
    return active;
  }
  
  public ChannelMetadata metadata()
  {
    return METADATA;
  }
  
  protected void doClose() throws Exception
  {
    active = false;
    

    inputClosedSeenErrorOnRead = true;
    try {
      ChannelPromise promise = connectPromise;
      if (promise != null)
      {
        promise.tryFailure(new ClosedChannelException());
        connectPromise = null;
      }
      
      ScheduledFuture<?> future = connectTimeoutFuture;
      if (future != null) {
        future.cancel(false);
        connectTimeoutFuture = null;
      }
      
      if (isRegistered())
      {



        EventLoop loop = eventLoop();
        if (loop.inEventLoop()) {
          doDeregister();
        } else {
          loop.execute(new Runnable()
          {
            public void run() {
              try {
                doDeregister();
              } catch (Throwable cause) {
                pipeline().fireExceptionCaught(cause);
              }
            }
          });
        }
      }
    } finally {
      socket.close();
    }
  }
  
  void resetCachedAddresses() {
    local = socket.localAddress();
    remote = socket.remoteAddress();
  }
  
  protected void doDisconnect() throws Exception
  {
    doClose();
  }
  
  protected boolean isCompatible(EventLoop loop)
  {
    return loop instanceof EpollEventLoop;
  }
  
  public boolean isOpen()
  {
    return socket.isOpen();
  }
  
  protected void doDeregister() throws Exception
  {
    ((EpollEventLoop)eventLoop()).remove(this);
  }
  
  protected final void doBeginRead()
    throws Exception
  {
    AbstractEpollUnsafe unsafe = (AbstractEpollUnsafe)unsafe();
    readPending = true;
    



    setFlag(Native.EPOLLIN);
    


    if (maybeMoreDataToRead) {
      unsafe.executeEpollInReadyRunnable(config());
    }
  }
  
  final boolean shouldBreakEpollInReady(ChannelConfig config) {
    return (socket.isInputShutdown()) && ((inputClosedSeenErrorOnRead) || (!isAllowHalfClosure(config)));
  }
  
  private static boolean isAllowHalfClosure(ChannelConfig config) {
    if ((config instanceof EpollDomainSocketChannelConfig)) {
      return ((EpollDomainSocketChannelConfig)config).isAllowHalfClosure();
    }
    return ((config instanceof SocketChannelConfig)) && 
      (((SocketChannelConfig)config).isAllowHalfClosure());
  }
  
  final void clearEpollIn()
  {
    if (isRegistered()) {
      EventLoop loop = eventLoop();
      final AbstractEpollUnsafe unsafe = (AbstractEpollUnsafe)unsafe();
      if (loop.inEventLoop()) {
        unsafe.clearEpollIn0();
      }
      else {
        loop.execute(new Runnable()
        {
          public void run() {
            if ((!unsafereadPending) && (!config().isAutoRead()))
            {
              unsafe.clearEpollIn0();
            }
          }
        });
      }
    }
    else
    {
      flags &= (Native.EPOLLIN ^ 0xFFFFFFFF);
    }
  }
  
  private void modifyEvents() throws IOException {
    if ((isOpen()) && (isRegistered())) {
      ((EpollEventLoop)eventLoop()).modify(this);
    }
  }
  


  protected void doRegister()
    throws Exception
  {
    epollInReadyRunnablePending = false;
    ((EpollEventLoop)eventLoop()).add(this);
  }
  


  protected abstract AbstractEpollUnsafe newUnsafe();
  

  protected final ByteBuf newDirectBuffer(ByteBuf buf)
  {
    return newDirectBuffer(buf, buf);
  }
  




  protected final ByteBuf newDirectBuffer(Object holder, ByteBuf buf)
  {
    int readableBytes = buf.readableBytes();
    if (readableBytes == 0) {
      ReferenceCountUtil.release(holder);
      return Unpooled.EMPTY_BUFFER;
    }
    
    ByteBufAllocator alloc = alloc();
    if (alloc.isDirectBufferPooled()) {
      return newDirectBuffer0(holder, buf, alloc, readableBytes);
    }
    
    ByteBuf directBuf = ByteBufUtil.threadLocalDirectBuffer();
    if (directBuf == null) {
      return newDirectBuffer0(holder, buf, alloc, readableBytes);
    }
    
    directBuf.writeBytes(buf, buf.readerIndex(), readableBytes);
    ReferenceCountUtil.safeRelease(holder);
    return directBuf;
  }
  
  private static ByteBuf newDirectBuffer0(Object holder, ByteBuf buf, ByteBufAllocator alloc, int capacity) {
    ByteBuf directBuf = alloc.directBuffer(capacity);
    directBuf.writeBytes(buf, buf.readerIndex(), capacity);
    ReferenceCountUtil.safeRelease(holder);
    return directBuf;
  }
  
  protected static void checkResolvable(InetSocketAddress addr) {
    if (addr.isUnresolved()) {
      throw new UnresolvedAddressException();
    }
  }
  

  protected final int doReadBytes(ByteBuf byteBuf)
    throws Exception
  {
    int writerIndex = byteBuf.writerIndex();
    
    unsafe().recvBufAllocHandle().attemptedBytesRead(byteBuf.writableBytes());
    int localReadAmount; int localReadAmount; if (byteBuf.hasMemoryAddress()) {
      localReadAmount = socket.readAddress(byteBuf.memoryAddress(), writerIndex, byteBuf.capacity());
    } else {
      ByteBuffer buf = byteBuf.internalNioBuffer(writerIndex, byteBuf.writableBytes());
      localReadAmount = socket.read(buf, buf.position(), buf.limit());
    }
    if (localReadAmount > 0) {
      byteBuf.writerIndex(writerIndex + localReadAmount);
    }
    return localReadAmount;
  }
  
  protected final int doWriteBytes(ChannelOutboundBuffer in, ByteBuf buf) throws Exception {
    if (buf.hasMemoryAddress()) {
      int localFlushedAmount = socket.writeAddress(buf.memoryAddress(), buf.readerIndex(), buf.writerIndex());
      if (localFlushedAmount > 0) {
        in.removeBytes(localFlushedAmount);
        return 1;
      }
    }
    else {
      ByteBuffer nioBuf = buf.nioBufferCount() == 1 ? buf.internalNioBuffer(buf.readerIndex(), buf.readableBytes()) : buf.nioBuffer();
      int localFlushedAmount = socket.write(nioBuf, nioBuf.position(), nioBuf.limit());
      if (localFlushedAmount > 0) {
        nioBuf.position(nioBuf.position() + localFlushedAmount);
        in.removeBytes(localFlushedAmount);
        return 1;
      }
    }
    return Integer.MAX_VALUE;
  }
  



  final long doWriteOrSendBytes(ByteBuf data, InetSocketAddress remoteAddress, boolean fastOpen)
    throws IOException
  {
    assert ((!fastOpen) || (remoteAddress != null)) : "fastOpen requires a remote address";
    if (data.hasMemoryAddress()) {
      long memoryAddress = data.memoryAddress();
      if (remoteAddress == null) {
        return socket.writeAddress(memoryAddress, data.readerIndex(), data.writerIndex());
      }
      return socket.sendToAddress(memoryAddress, data.readerIndex(), data.writerIndex(), remoteAddress
        .getAddress(), remoteAddress.getPort(), fastOpen);
    }
    
    if (data.nioBufferCount() > 1) {
      IovArray array = ((EpollEventLoop)eventLoop()).cleanIovArray();
      array.add(data, data.readerIndex(), data.readableBytes());
      int cnt = array.count();
      assert (cnt != 0);
      
      if (remoteAddress == null) {
        return socket.writevAddresses(array.memoryAddress(0), cnt);
      }
      return socket.sendToAddresses(array.memoryAddress(0), cnt, remoteAddress
        .getAddress(), remoteAddress.getPort(), fastOpen);
    }
    
    ByteBuffer nioData = data.internalNioBuffer(data.readerIndex(), data.readableBytes());
    if (remoteAddress == null) {
      return socket.write(nioData, nioData.position(), nioData.limit());
    }
    return socket.sendTo(nioData, nioData.position(), nioData.limit(), remoteAddress
      .getAddress(), remoteAddress.getPort(), fastOpen); }
  
  protected abstract class AbstractEpollUnsafe extends AbstractChannel.AbstractUnsafe { boolean readPending; boolean maybeMoreDataToRead;
    protected AbstractEpollUnsafe() { super(); }
    

    private EpollRecvByteAllocatorHandle allocHandle;
    private final Runnable epollInReadyRunnable = new Runnable()
    {
      public void run() {
        epollInReadyRunnablePending = false;
        epollInReady();
      }
    };
    

    abstract void epollInReady();
    

    final void epollInBefore()
    {
      maybeMoreDataToRead = false;
    }
    
    final void epollInFinally(ChannelConfig config) {
      maybeMoreDataToRead = allocHandle.maybeMoreDataToRead();
      
      if ((allocHandle.isReceivedRdHup()) || ((readPending) && (maybeMoreDataToRead)))
      {






        executeEpollInReadyRunnable(config);
      } else if ((!readPending) && (!config.isAutoRead()))
      {





        clearEpollIn();
      }
    }
    
    final void executeEpollInReadyRunnable(ChannelConfig config) {
      if ((epollInReadyRunnablePending) || (!isActive()) || (shouldBreakEpollInReady(config))) {
        return;
      }
      epollInReadyRunnablePending = true;
      eventLoop().execute(epollInReadyRunnable);
    }
    



    final void epollRdHupReady()
    {
      recvBufAllocHandle().receivedRdHup();
      
      if (isActive())
      {


        epollInReady();
      }
      else {
        shutdownInput(true);
      }
      

      clearEpollRdHup();
    }
    

    private void clearEpollRdHup()
    {
      try
      {
        clearFlag(Native.EPOLLRDHUP);
      } catch (IOException e) {
        pipeline().fireExceptionCaught(e);
        close(voidPromise());
      }
    }
    


    void shutdownInput(boolean rdHup)
    {
      if (!socket.isInputShutdown()) {
        if (AbstractEpollChannel.isAllowHalfClosure(config())) {
          try {
            socket.shutdown(true, false);
          }
          catch (IOException ignored)
          {
            fireEventAndClose(ChannelInputShutdownEvent.INSTANCE);
            return;
          }
          catch (NotYetConnectedException localNotYetConnectedException) {}
          

          clearEpollIn();
          pipeline().fireUserEventTriggered(ChannelInputShutdownEvent.INSTANCE);
        } else {
          close(voidPromise());
        }
      } else if (!rdHup) {
        inputClosedSeenErrorOnRead = true;
        pipeline().fireUserEventTriggered(ChannelInputShutdownReadComplete.INSTANCE);
      }
    }
    
    private void fireEventAndClose(Object evt) {
      pipeline().fireUserEventTriggered(evt);
      close(voidPromise());
    }
    
    public EpollRecvByteAllocatorHandle recvBufAllocHandle()
    {
      if (allocHandle == null) {
        allocHandle = newEpollHandle((RecvByteBufAllocator.ExtendedHandle)super.recvBufAllocHandle());
      }
      return allocHandle;
    }
    



    EpollRecvByteAllocatorHandle newEpollHandle(RecvByteBufAllocator.ExtendedHandle handle)
    {
      return new EpollRecvByteAllocatorHandle(handle);
    }
    



    protected final void flush0()
    {
      if (!isFlagSet(Native.EPOLLOUT)) {
        super.flush0();
      }
    }
    


    final void epollOutReady()
    {
      if (connectPromise != null)
      {
        finishConnect();
      } else if (!socket.isOutputShutdown())
      {
        super.flush0();
      }
    }
    
    protected final void clearEpollIn0() {
      assert (eventLoop().inEventLoop());
      try {
        readPending = false;
        clearFlag(Native.EPOLLIN);
      }
      catch (IOException e)
      {
        pipeline().fireExceptionCaught(e);
        unsafe().close(unsafe().voidPromise());
      }
    }
    

    public void connect(final SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise)
    {
      if ((!promise.setUncancellable()) || (!ensureOpen(promise))) {
        return;
      }
      try
      {
        if (connectPromise != null) {
          throw new ConnectionPendingException();
        }
        
        boolean wasActive = isActive();
        if (doConnect(remoteAddress, localAddress)) {
          fulfillConnectPromise(promise, wasActive);
        } else {
          connectPromise = promise;
          requestedRemoteAddress = remoteAddress;
          

          int connectTimeoutMillis = config().getConnectTimeoutMillis();
          if (connectTimeoutMillis > 0) {
            connectTimeoutFuture = eventLoop().schedule(new Runnable()
            {
              public void run() {
                ChannelPromise connectPromise = AbstractEpollChannel.this.connectPromise;
                if ((connectPromise != null) && (!connectPromise.isDone()) && 
                  (connectPromise.tryFailure(new ConnectTimeoutException("connection timed out: " + remoteAddress))))
                {
                  close(voidPromise()); } } }, connectTimeoutMillis, TimeUnit.MILLISECONDS);
          }
          



          promise.addListener(new ChannelFutureListener()
          {
            public void operationComplete(ChannelFuture future) throws Exception {
              if (future.isCancelled()) {
                if (connectTimeoutFuture != null) {
                  connectTimeoutFuture.cancel(false);
                }
                connectPromise = null;
                close(voidPromise());
              }
            }
          });
        }
      } catch (Throwable t) {
        closeIfClosed();
        promise.tryFailure(annotateConnectException(t, remoteAddress));
      }
    }
    
    private void fulfillConnectPromise(ChannelPromise promise, boolean wasActive) {
      if (promise == null)
      {
        return;
      }
      AbstractEpollChannel.this.active = true;
      


      boolean active = isActive();
      

      boolean promiseSet = promise.trySuccess();
      


      if ((!wasActive) && (active)) {
        pipeline().fireChannelActive();
      }
      

      if (!promiseSet) {
        close(voidPromise());
      }
    }
    
    private void fulfillConnectPromise(ChannelPromise promise, Throwable cause) {
      if (promise == null)
      {
        return;
      }
      

      promise.tryFailure(cause);
      closeIfClosed();
    }
    


    private void finishConnect()
    {
      assert (eventLoop().inEventLoop());
      
      boolean connectStillInProgress = false;
      try {
        boolean wasActive = isActive();
        if (!doFinishConnect()) {
          connectStillInProgress = true;
          return;
        }
        fulfillConnectPromise(connectPromise, wasActive);
      } catch (Throwable t) {
        fulfillConnectPromise(connectPromise, annotateConnectException(t, requestedRemoteAddress));
      } finally {
        if (!connectStillInProgress)
        {

          if (connectTimeoutFuture != null) {
            connectTimeoutFuture.cancel(false);
          }
          connectPromise = null;
        }
      }
    }
    

    private boolean doFinishConnect()
      throws Exception
    {
      if (socket.finishConnect()) {
        clearFlag(Native.EPOLLOUT);
        if ((requestedRemoteAddress instanceof InetSocketAddress)) {
          remote = UnixChannelUtil.computeRemoteAddr((InetSocketAddress)requestedRemoteAddress, socket.remoteAddress());
        }
        requestedRemoteAddress = null;
        
        return true;
      }
      setFlag(Native.EPOLLOUT);
      return false;
    }
  }
  
  protected void doBind(SocketAddress local) throws Exception
  {
    if ((local instanceof InetSocketAddress)) {
      checkResolvable((InetSocketAddress)local);
    }
    socket.bind(local);
    this.local = socket.localAddress();
  }
  

  protected boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress)
    throws Exception
  {
    if ((localAddress instanceof InetSocketAddress)) {
      checkResolvable((InetSocketAddress)localAddress);
    }
    
    InetSocketAddress remoteSocketAddr = (remoteAddress instanceof InetSocketAddress) ? (InetSocketAddress)remoteAddress : null;
    
    if (remoteSocketAddr != null) {
      checkResolvable(remoteSocketAddr);
    }
    
    if (remote != null)
    {


      throw new AlreadyConnectedException();
    }
    
    if (localAddress != null) {
      socket.bind(localAddress);
    }
    
    boolean connected = doConnect0(remoteAddress);
    if (connected)
    {
      remote = (remoteSocketAddr == null ? remoteAddress : UnixChannelUtil.computeRemoteAddr(remoteSocketAddr, socket.remoteAddress()));
    }
    


    local = socket.localAddress();
    return connected;
  }
  
  boolean doConnect0(SocketAddress remote) throws Exception {
    boolean success = false;
    try {
      boolean connected = socket.connect(remote);
      if (!connected) {
        setFlag(Native.EPOLLOUT);
      }
      success = true;
      return connected;
    } finally {
      if (!success) {
        doClose();
      }
    }
  }
  
  protected SocketAddress localAddress0()
  {
    return local;
  }
  
  protected SocketAddress remoteAddress0()
  {
    return remote;
  }
}
