package io.netty.channel.kqueue;

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
import io.netty.channel.unix.UnixChannel;
import io.netty.channel.unix.UnixChannelUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.ObjectUtil;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.ConnectionPendingException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.UnresolvedAddressException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;












abstract class AbstractKQueueChannel
  extends AbstractChannel
  implements UnixChannel
{
  private static final ChannelMetadata METADATA = new ChannelMetadata(false);
  
  private ChannelPromise connectPromise;
  
  private ScheduledFuture<?> connectTimeoutFuture;
  
  private SocketAddress requestedRemoteAddress;
  
  final BsdSocket socket;
  private boolean readFilterEnabled;
  private boolean writeFilterEnabled;
  boolean readReadyRunnablePending;
  boolean inputClosedSeenErrorOnRead;
  protected volatile boolean active;
  private volatile SocketAddress local;
  private volatile SocketAddress remote;
  
  AbstractKQueueChannel(Channel parent, BsdSocket fd, boolean active)
  {
    super(parent);
    socket = ((BsdSocket)ObjectUtil.checkNotNull(fd, "fd"));
    this.active = active;
    if (active)
    {

      local = fd.localAddress();
      remote = fd.remoteAddress();
    }
  }
  
  AbstractKQueueChannel(Channel parent, BsdSocket fd, SocketAddress remote) {
    super(parent);
    socket = ((BsdSocket)ObjectUtil.checkNotNull(fd, "fd"));
    active = true;
    

    this.remote = remote;
    local = fd.localAddress();
  }
  
  static boolean isSoErrorZero(BsdSocket fd) {
    try {
      return fd.getSoError() == 0;
    } catch (IOException e) {
      throw new ChannelException(e);
    }
  }
  
  public final FileDescriptor fd()
  {
    return socket;
  }
  
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
    socket.close();
  }
  
  protected void doDisconnect() throws Exception
  {
    doClose();
  }
  
  void resetCachedAddresses() {
    local = socket.localAddress();
    remote = socket.remoteAddress();
  }
  
  protected boolean isCompatible(EventLoop loop)
  {
    return loop instanceof KQueueEventLoop;
  }
  
  public boolean isOpen()
  {
    return socket.isOpen();
  }
  
  protected void doDeregister() throws Exception
  {
    ((KQueueEventLoop)eventLoop()).remove(this);
    


    readFilterEnabled = false;
    writeFilterEnabled = false;
  }
  
  void unregisterFilters() throws Exception
  {
    readFilter(false);
    writeFilter(false);
    evSet0(Native.EVFILT_SOCK, Native.EV_DELETE, 0);
  }
  
  protected final void doBeginRead()
    throws Exception
  {
    AbstractKQueueUnsafe unsafe = (AbstractKQueueUnsafe)unsafe();
    readPending = true;
    



    readFilter(true);
    


    if (maybeMoreDataToRead) {
      unsafe.executeReadReadyRunnable(config());
    }
  }
  


  protected void doRegister()
    throws Exception
  {
    readReadyRunnablePending = false;
    
    ((KQueueEventLoop)eventLoop()).add(this);
    

    if (writeFilterEnabled) {
      evSet0(Native.EVFILT_WRITE, Native.EV_ADD_CLEAR_ENABLE);
    }
    if (readFilterEnabled) {
      evSet0(Native.EVFILT_READ, Native.EV_ADD_CLEAR_ENABLE);
    }
    evSet0(Native.EVFILT_SOCK, Native.EV_ADD, Native.NOTE_RDHUP);
  }
  


  protected abstract AbstractKQueueUnsafe newUnsafe();
  

  public abstract KQueueChannelConfig config();
  

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
  
  final boolean shouldBreakReadReady(ChannelConfig config) {
    return (socket.isInputShutdown()) && ((inputClosedSeenErrorOnRead) || (!isAllowHalfClosure(config)));
  }
  
  private static boolean isAllowHalfClosure(ChannelConfig config) {
    if ((config instanceof KQueueDomainSocketChannelConfig)) {
      return ((KQueueDomainSocketChannelConfig)config).isAllowHalfClosure();
    }
    
    return ((config instanceof SocketChannelConfig)) && 
      (((SocketChannelConfig)config).isAllowHalfClosure());
  }
  
  final void clearReadFilter()
  {
    if (isRegistered()) {
      EventLoop loop = eventLoop();
      final AbstractKQueueUnsafe unsafe = (AbstractKQueueUnsafe)unsafe();
      if (loop.inEventLoop()) {
        unsafe.clearReadFilter0();
      }
      else {
        loop.execute(new Runnable()
        {
          public void run() {
            if ((!unsafereadPending) && (!config().isAutoRead()))
            {
              unsafe.clearReadFilter0();
            }
          }
        });
      }
    }
    else
    {
      readFilterEnabled = false;
    }
  }
  
  void readFilter(boolean readFilterEnabled) throws IOException {
    if (this.readFilterEnabled != readFilterEnabled) {
      this.readFilterEnabled = readFilterEnabled;
      evSet(Native.EVFILT_READ, readFilterEnabled ? Native.EV_ADD_CLEAR_ENABLE : Native.EV_DELETE_DISABLE);
    }
  }
  
  void writeFilter(boolean writeFilterEnabled) throws IOException {
    if (this.writeFilterEnabled != writeFilterEnabled) {
      this.writeFilterEnabled = writeFilterEnabled;
      evSet(Native.EVFILT_WRITE, writeFilterEnabled ? Native.EV_ADD_CLEAR_ENABLE : Native.EV_DELETE_DISABLE);
    }
  }
  
  private void evSet(short filter, short flags) {
    if (isRegistered()) {
      evSet0(filter, flags);
    }
  }
  
  private void evSet0(short filter, short flags) {
    evSet0(filter, flags, 0);
  }
  
  private void evSet0(short filter, short flags, int fflags)
  {
    if (isOpen())
      ((KQueueEventLoop)eventLoop()).evSet(this, filter, flags, fflags); }
  
  abstract class AbstractKQueueUnsafe extends AbstractChannel.AbstractUnsafe { boolean readPending;
    
    AbstractKQueueUnsafe() { super(); }
    
    boolean maybeMoreDataToRead;
    private KQueueRecvByteAllocatorHandle allocHandle;
    private final Runnable readReadyRunnable = new Runnable()
    {
      public void run() {
        readReadyRunnablePending = false;
        readReady(recvBufAllocHandle());
      }
    };
    
    final void readReady(long numberBytesPending) {
      KQueueRecvByteAllocatorHandle allocHandle = recvBufAllocHandle();
      allocHandle.numberBytesPending(numberBytesPending);
      readReady(allocHandle);
    }
    
    abstract void readReady(KQueueRecvByteAllocatorHandle paramKQueueRecvByteAllocatorHandle);
    
    final void readReadyBefore() {
      maybeMoreDataToRead = false;
    }
    
    final void readReadyFinally(ChannelConfig config) {
      maybeMoreDataToRead = allocHandle.maybeMoreDataToRead();
      
      if ((allocHandle.isReadEOF()) || ((readPending) && (maybeMoreDataToRead)))
      {






        executeReadReadyRunnable(config);
      } else if ((!readPending) && (!config.isAutoRead()))
      {





        clearReadFilter0();
      }
    }
    
    final boolean failConnectPromise(Throwable cause) {
      if (AbstractKQueueChannel.this.connectPromise != null)
      {


        ChannelPromise connectPromise = AbstractKQueueChannel.this.connectPromise;
        AbstractKQueueChannel.this.connectPromise = null;
        if (connectPromise.tryFailure((cause instanceof ConnectException) ? cause : new ConnectException("failed to connect")
          .initCause(cause))) {
          closeIfClosed();
          return true;
        }
      }
      return false;
    }
    
    final void writeReady() {
      if (connectPromise != null)
      {
        finishConnect();
      } else if (!socket.isOutputShutdown())
      {
        super.flush0();
      }
    }
    







    void shutdownInput(boolean readEOF)
    {
      if ((readEOF) && (connectPromise != null)) {
        finishConnect();
      }
      if (!socket.isInputShutdown()) {
        if (AbstractKQueueChannel.isAllowHalfClosure(config())) {
          try {
            socket.shutdown(true, false);
          }
          catch (IOException ignored)
          {
            fireEventAndClose(ChannelInputShutdownEvent.INSTANCE);
            return;
          }
          catch (NotYetConnectedException localNotYetConnectedException) {}
          

          pipeline().fireUserEventTriggered(ChannelInputShutdownEvent.INSTANCE);
        } else {
          close(voidPromise());
        }
      } else if (!readEOF) {
        inputClosedSeenErrorOnRead = true;
        pipeline().fireUserEventTriggered(ChannelInputShutdownReadComplete.INSTANCE);
      }
    }
    
    final void readEOF()
    {
      KQueueRecvByteAllocatorHandle allocHandle = recvBufAllocHandle();
      allocHandle.readEOF();
      
      if (isActive())
      {


        readReady(allocHandle);
      }
      else {
        shutdownInput(true);
      }
    }
    
    public KQueueRecvByteAllocatorHandle recvBufAllocHandle()
    {
      if (allocHandle == null)
      {
        allocHandle = new KQueueRecvByteAllocatorHandle((RecvByteBufAllocator.ExtendedHandle)super.recvBufAllocHandle());
      }
      return allocHandle;
    }
    



    protected final void flush0()
    {
      if (!writeFilterEnabled) {
        super.flush0();
      }
    }
    
    final void executeReadReadyRunnable(ChannelConfig config) {
      if ((readReadyRunnablePending) || (!isActive()) || (shouldBreakReadReady(config))) {
        return;
      }
      readReadyRunnablePending = true;
      eventLoop().execute(readReadyRunnable);
    }
    
    protected final void clearReadFilter0() {
      assert (eventLoop().inEventLoop());
      try {
        readPending = false;
        readFilter(false);
      }
      catch (IOException e)
      {
        pipeline().fireExceptionCaught(e);
        unsafe().close(unsafe().voidPromise());
      }
    }
    
    private void fireEventAndClose(Object evt) {
      pipeline().fireUserEventTriggered(evt);
      close(voidPromise());
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
                ChannelPromise connectPromise = AbstractKQueueChannel.this.connectPromise;
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
      AbstractKQueueChannel.this.active = true;
      


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
    
    private boolean doFinishConnect() throws Exception {
      if (socket.finishConnect()) {
        writeFilter(false);
        if ((requestedRemoteAddress instanceof InetSocketAddress)) {
          remote = UnixChannelUtil.computeRemoteAddr((InetSocketAddress)requestedRemoteAddress, socket.remoteAddress());
        }
        requestedRemoteAddress = null;
        return true;
      }
      writeFilter(true);
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
  
  private boolean doConnect0(SocketAddress remote) throws Exception {
    boolean success = false;
    try {
      boolean connected = socket.connect(remote);
      if (!connected) {
        writeFilter(true);
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
