package io.netty.channel.local;

import io.netty.channel.AbstractChannel;
import io.netty.channel.AbstractChannel.AbstractUnsafe;
import io.netty.channel.Channel;
import io.netty.channel.Channel.Unsafe;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.EventLoop;
import io.netty.channel.PreferHeapByteBufAllocator;
import io.netty.channel.RecvByteBufAllocator.Handle;
import io.netty.channel.SingleThreadEventLoop;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.SingleThreadEventExecutor;
import io.netty.util.internal.InternalThreadLocalMap;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.net.ConnectException;
import java.net.SocketAddress;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ConnectionPendingException;
import java.nio.channels.NotYetConnectedException;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
















public class LocalChannel
  extends AbstractChannel
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(LocalChannel.class);
  

  private static final AtomicReferenceFieldUpdater<LocalChannel, Future> FINISH_READ_FUTURE_UPDATER = AtomicReferenceFieldUpdater.newUpdater(LocalChannel.class, Future.class, "finishReadFuture");
  private static final ChannelMetadata METADATA = new ChannelMetadata(false);
  private static final int MAX_READER_STACK_DEPTH = 8;
  
  private static enum State { OPEN,  BOUND,  CONNECTED,  CLOSED;
    private State() {} }
  private final ChannelConfig config = new DefaultChannelConfig(this);
  
  final Queue<Object> inboundBuffer = PlatformDependent.newSpscQueue();
  private final Runnable readTask = new Runnable()
  {
    public void run()
    {
      if (!inboundBuffer.isEmpty()) {
        LocalChannel.this.readInbound();
      }
    }
  };
  
  private final Runnable shutdownHook = new Runnable()
  {
    public void run() {
      unsafe().close(unsafe().voidPromise());
    }
  };
  private volatile State state;
  private volatile LocalChannel peer;
  private volatile LocalAddress localAddress;
  private volatile LocalAddress remoteAddress;
  private volatile ChannelPromise connectPromise;
  private volatile boolean readInProgress;
  private volatile boolean writeInProgress;
  private volatile Future<?> finishReadFuture;
  
  public LocalChannel()
  {
    super(null);
    config().setAllocator(new PreferHeapByteBufAllocator(config.getAllocator()));
  }
  
  protected LocalChannel(LocalServerChannel parent, LocalChannel peer) {
    super(parent);
    config().setAllocator(new PreferHeapByteBufAllocator(config.getAllocator()));
    this.peer = peer;
    localAddress = parent.localAddress();
    remoteAddress = peer.localAddress();
  }
  
  public ChannelMetadata metadata()
  {
    return METADATA;
  }
  
  public ChannelConfig config()
  {
    return config;
  }
  
  public LocalServerChannel parent()
  {
    return (LocalServerChannel)super.parent();
  }
  
  public LocalAddress localAddress()
  {
    return (LocalAddress)super.localAddress();
  }
  
  public LocalAddress remoteAddress()
  {
    return (LocalAddress)super.remoteAddress();
  }
  
  public boolean isOpen()
  {
    return state != State.CLOSED;
  }
  
  public boolean isActive()
  {
    return state == State.CONNECTED;
  }
  
  protected AbstractChannel.AbstractUnsafe newUnsafe()
  {
    return new LocalUnsafe(null);
  }
  
  protected boolean isCompatible(EventLoop loop)
  {
    return loop instanceof SingleThreadEventLoop;
  }
  
  protected SocketAddress localAddress0()
  {
    return localAddress;
  }
  
  protected SocketAddress remoteAddress0()
  {
    return remoteAddress;
  }
  




  protected void doRegister()
    throws Exception
  {
    if ((this.peer != null) && (parent() != null))
    {

      final LocalChannel peer = this.peer;
      state = State.CONNECTED;
      
      remoteAddress = (parent() == null ? null : parent().localAddress());
      state = State.CONNECTED;
      




      peer.eventLoop().execute(new Runnable()
      {
        public void run() {
          ChannelPromise promise = peerconnectPromise;
          


          if ((promise != null) && (promise.trySuccess())) {
            peer.pipeline().fireChannelActive();
          }
        }
      });
    }
    ((SingleThreadEventExecutor)eventLoop()).addShutdownHook(shutdownHook);
  }
  
  protected void doBind(SocketAddress localAddress)
    throws Exception
  {
    this.localAddress = LocalChannelRegistry.register(this, this.localAddress, localAddress);
    
    state = State.BOUND;
  }
  
  protected void doDisconnect() throws Exception
  {
    doClose();
  }
  
  protected void doClose() throws Exception
  {
    final LocalChannel peer = this.peer;
    State oldState = state;
    try {
      if (oldState != State.CLOSED)
      {
        if (localAddress != null) {
          if (parent() == null) {
            LocalChannelRegistry.unregister(localAddress);
          }
          localAddress = null;
        }
        


        state = State.CLOSED;
        

        if ((writeInProgress) && (peer != null)) {
          finishPeerRead(peer);
        }
        
        ChannelPromise promise = connectPromise;
        if (promise != null)
        {
          promise.tryFailure(new ClosedChannelException());
          connectPromise = null;
        }
      }
      
      if (peer != null) {
        this.peer = null;
        


        EventLoop peerEventLoop = peer.eventLoop();
        final boolean peerIsActive = peer.isActive();
        try {
          peerEventLoop.execute(new Runnable()
          {
            public void run() {
              peer.tryClose(peerIsActive);
            }
          });
        } catch (Throwable cause) {
          logger.warn("Releasing Inbound Queues for channels {}-{} because exception occurred!", new Object[] { this, peer, cause });
          
          if (peerEventLoop.inEventLoop()) {
            peer.releaseInboundBuffers();
          }
          else
          {
            peer.close();
          }
          PlatformDependent.throwException(cause);
        }
      }
    }
    finally {
      if ((oldState != null) && (oldState != State.CLOSED))
      {



        releaseInboundBuffers();
      }
    }
  }
  
  private void tryClose(boolean isActive) {
    if (isActive) {
      unsafe().close(unsafe().voidPromise());
    } else {
      releaseInboundBuffers();
    }
  }
  
  protected void doDeregister()
    throws Exception
  {
    ((SingleThreadEventExecutor)eventLoop()).removeShutdownHook(shutdownHook);
  }
  
  private void readInbound() {
    RecvByteBufAllocator.Handle handle = unsafe().recvBufAllocHandle();
    handle.reset(config());
    ChannelPipeline pipeline = pipeline();
    do {
      Object received = inboundBuffer.poll();
      if (received == null) {
        break;
      }
      pipeline.fireChannelRead(received);
    } while (handle.continueReading());
    
    pipeline.fireChannelReadComplete();
  }
  
  protected void doBeginRead() throws Exception
  {
    if (readInProgress) {
      return;
    }
    
    Queue<Object> inboundBuffer = this.inboundBuffer;
    if (inboundBuffer.isEmpty()) {
      readInProgress = true;
      return;
    }
    
    InternalThreadLocalMap threadLocals = InternalThreadLocalMap.get();
    Integer stackDepth = Integer.valueOf(threadLocals.localChannelReaderStackDepth());
    if (stackDepth.intValue() < 8) {
      threadLocals.setLocalChannelReaderStackDepth(stackDepth.intValue() + 1);
      try {
        readInbound();
      } finally {
        threadLocals.setLocalChannelReaderStackDepth(stackDepth.intValue());
      }
    } else {
      try {
        eventLoop().execute(readTask);
      } catch (Throwable cause) {
        logger.warn("Closing Local channels {}-{} because exception occurred!", new Object[] { this, peer, cause });
        close();
        peer.close();
        PlatformDependent.throwException(cause);
      }
    }
  }
  
  protected void doWrite(ChannelOutboundBuffer in) throws Exception
  {
    switch (6.$SwitchMap$io$netty$channel$local$LocalChannel$State[state.ordinal()]) {
    case 1: 
    case 2: 
      throw new NotYetConnectedException();
    case 3: 
      throw new ClosedChannelException();
    }
    
    

    LocalChannel peer = this.peer;
    
    writeInProgress = true;
    try {
      ClosedChannelException exception = null;
      for (;;) {
        Object msg = in.current();
        if (msg == null) {
          break;
        }
        
        try
        {
          if (state == State.CONNECTED) {
            inboundBuffer.add(ReferenceCountUtil.retain(msg));
            in.remove();
          } else {
            if (exception == null) {
              exception = new ClosedChannelException();
            }
            in.remove(exception);
          }
        } catch (Throwable cause) {
          in.remove(cause);
        }
        
      }
      

    }
    finally
    {
      writeInProgress = false;
    }
    
    finishPeerRead(peer);
  }
  
  private void finishPeerRead(LocalChannel peer)
  {
    if ((peer.eventLoop() == eventLoop()) && (!writeInProgress)) {
      finishPeerRead0(peer);
    } else {
      runFinishPeerReadTask(peer);
    }
  }
  

  private void runFinishPeerReadTask(final LocalChannel peer)
  {
    Runnable finishPeerReadTask = new Runnable()
    {
      public void run() {
        LocalChannel.this.finishPeerRead0(peer);
      }
    };
    try {
      if (writeInProgress) {
        finishReadFuture = peer.eventLoop().submit(finishPeerReadTask);
      } else {
        peer.eventLoop().execute(finishPeerReadTask);
      }
    } catch (Throwable cause) {
      logger.warn("Closing Local channels {}-{} because exception occurred!", new Object[] { this, peer, cause });
      close();
      peer.close();
      PlatformDependent.throwException(cause);
    }
  }
  
  private void releaseInboundBuffers() {
    assert ((eventLoop() == null) || (eventLoop().inEventLoop()));
    readInProgress = false;
    Queue<Object> inboundBuffer = this.inboundBuffer;
    Object msg;
    while ((msg = inboundBuffer.poll()) != null) {
      ReferenceCountUtil.release(msg);
    }
  }
  
  private void finishPeerRead0(LocalChannel peer) {
    Future<?> peerFinishReadFuture = finishReadFuture;
    if (peerFinishReadFuture != null) {
      if (!peerFinishReadFuture.isDone()) {
        runFinishPeerReadTask(peer);
        return;
      }
      
      FINISH_READ_FUTURE_UPDATER.compareAndSet(peer, peerFinishReadFuture, null);
    }
    


    if ((readInProgress) && (!inboundBuffer.isEmpty())) {
      readInProgress = false;
      peer.readInbound();
    }
  }
  
  private class LocalUnsafe extends AbstractChannel.AbstractUnsafe { private LocalUnsafe() { super(); }
    

    public void connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise)
    {
      if ((!promise.setUncancellable()) || (!ensureOpen(promise))) {
        return;
      }
      
      if (state == LocalChannel.State.CONNECTED) {
        Exception cause = new AlreadyConnectedException();
        safeSetFailure(promise, cause);
        pipeline().fireExceptionCaught(cause);
        return;
      }
      
      if (connectPromise != null) {
        throw new ConnectionPendingException();
      }
      
      connectPromise = promise;
      
      if (state != LocalChannel.State.BOUND)
      {
        if (localAddress == null) {
          localAddress = new LocalAddress(LocalChannel.this);
        }
      }
      
      if (localAddress != null) {
        try {
          doBind(localAddress);
        } catch (Throwable t) {
          safeSetFailure(promise, t);
          close(voidPromise());
          return;
        }
      }
      
      Channel boundChannel = LocalChannelRegistry.get(remoteAddress);
      if (!(boundChannel instanceof LocalServerChannel)) {
        Exception cause = new ConnectException("connection refused: " + remoteAddress);
        safeSetFailure(promise, cause);
        close(voidPromise());
        return;
      }
      
      LocalServerChannel serverChannel = (LocalServerChannel)boundChannel;
      peer = serverChannel.serve(LocalChannel.this);
    }
  }
}
