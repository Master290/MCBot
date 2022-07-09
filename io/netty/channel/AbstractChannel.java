package io.netty.channel;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.socket.ChannelOutputShutdownEvent;
import io.netty.channel.socket.ChannelOutputShutdownException;
import io.netty.util.DefaultAttributeMap;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetConnectedException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;


















public abstract class AbstractChannel
  extends DefaultAttributeMap
  implements Channel
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractChannel.class);
  
  private final Channel parent;
  private final ChannelId id;
  private final Channel.Unsafe unsafe;
  private final DefaultChannelPipeline pipeline;
  private final VoidChannelPromise unsafeVoidPromise = new VoidChannelPromise(this, false);
  private final CloseFuture closeFuture = new CloseFuture(this);
  
  private volatile SocketAddress localAddress;
  
  private volatile SocketAddress remoteAddress;
  
  private volatile EventLoop eventLoop;
  
  private volatile boolean registered;
  
  private boolean closeInitiated;
  
  private Throwable initialCloseCause;
  
  private boolean strValActive;
  
  private String strVal;
  
  protected AbstractChannel(Channel parent)
  {
    this.parent = parent;
    id = newId();
    unsafe = newUnsafe();
    pipeline = newChannelPipeline();
  }
  





  protected AbstractChannel(Channel parent, ChannelId id)
  {
    this.parent = parent;
    this.id = id;
    unsafe = newUnsafe();
    pipeline = newChannelPipeline();
  }
  
  protected final int maxMessagesPerWrite() {
    ChannelConfig config = config();
    if ((config instanceof DefaultChannelConfig)) {
      return ((DefaultChannelConfig)config).getMaxMessagesPerWrite();
    }
    Integer value = (Integer)config.getOption(ChannelOption.MAX_MESSAGES_PER_WRITE);
    if (value == null) {
      return Integer.MAX_VALUE;
    }
    return value.intValue();
  }
  
  public final ChannelId id()
  {
    return id;
  }
  



  protected ChannelId newId()
  {
    return DefaultChannelId.newInstance();
  }
  


  protected DefaultChannelPipeline newChannelPipeline()
  {
    return new DefaultChannelPipeline(this);
  }
  
  public boolean isWritable()
  {
    ChannelOutboundBuffer buf = unsafe.outboundBuffer();
    return (buf != null) && (buf.isWritable());
  }
  
  public long bytesBeforeUnwritable()
  {
    ChannelOutboundBuffer buf = unsafe.outboundBuffer();
    

    return buf != null ? buf.bytesBeforeUnwritable() : 0L;
  }
  
  public long bytesBeforeWritable()
  {
    ChannelOutboundBuffer buf = unsafe.outboundBuffer();
    

    return buf != null ? buf.bytesBeforeWritable() : Long.MAX_VALUE;
  }
  
  public Channel parent()
  {
    return parent;
  }
  
  public ChannelPipeline pipeline()
  {
    return pipeline;
  }
  
  public ByteBufAllocator alloc()
  {
    return config().getAllocator();
  }
  
  public EventLoop eventLoop()
  {
    EventLoop eventLoop = this.eventLoop;
    if (eventLoop == null) {
      throw new IllegalStateException("channel not registered to an event loop");
    }
    return eventLoop;
  }
  
  public SocketAddress localAddress()
  {
    SocketAddress localAddress = this.localAddress;
    if (localAddress == null) {
      try {
        this.localAddress = (localAddress = unsafe().localAddress());
      } catch (Error e) {
        throw e;
      }
      catch (Throwable t) {
        return null;
      }
    }
    return localAddress;
  }
  


  @Deprecated
  protected void invalidateLocalAddress()
  {
    localAddress = null;
  }
  
  public SocketAddress remoteAddress()
  {
    SocketAddress remoteAddress = this.remoteAddress;
    if (remoteAddress == null) {
      try {
        this.remoteAddress = (remoteAddress = unsafe().remoteAddress());
      } catch (Error e) {
        throw e;
      }
      catch (Throwable t) {
        return null;
      }
    }
    return remoteAddress;
  }
  


  @Deprecated
  protected void invalidateRemoteAddress()
  {
    remoteAddress = null;
  }
  
  public boolean isRegistered()
  {
    return registered;
  }
  
  public ChannelFuture bind(SocketAddress localAddress)
  {
    return pipeline.bind(localAddress);
  }
  
  public ChannelFuture connect(SocketAddress remoteAddress)
  {
    return pipeline.connect(remoteAddress);
  }
  
  public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress)
  {
    return pipeline.connect(remoteAddress, localAddress);
  }
  
  public ChannelFuture disconnect()
  {
    return pipeline.disconnect();
  }
  
  public ChannelFuture close()
  {
    return pipeline.close();
  }
  
  public ChannelFuture deregister()
  {
    return pipeline.deregister();
  }
  
  public Channel flush()
  {
    pipeline.flush();
    return this;
  }
  
  public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise)
  {
    return pipeline.bind(localAddress, promise);
  }
  
  public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise)
  {
    return pipeline.connect(remoteAddress, promise);
  }
  
  public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise)
  {
    return pipeline.connect(remoteAddress, localAddress, promise);
  }
  
  public ChannelFuture disconnect(ChannelPromise promise)
  {
    return pipeline.disconnect(promise);
  }
  
  public ChannelFuture close(ChannelPromise promise)
  {
    return pipeline.close(promise);
  }
  
  public ChannelFuture deregister(ChannelPromise promise)
  {
    return pipeline.deregister(promise);
  }
  
  public Channel read()
  {
    pipeline.read();
    return this;
  }
  
  public ChannelFuture write(Object msg)
  {
    return pipeline.write(msg);
  }
  
  public ChannelFuture write(Object msg, ChannelPromise promise)
  {
    return pipeline.write(msg, promise);
  }
  
  public ChannelFuture writeAndFlush(Object msg)
  {
    return pipeline.writeAndFlush(msg);
  }
  
  public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise)
  {
    return pipeline.writeAndFlush(msg, promise);
  }
  
  public ChannelPromise newPromise()
  {
    return pipeline.newPromise();
  }
  
  public ChannelProgressivePromise newProgressivePromise()
  {
    return pipeline.newProgressivePromise();
  }
  
  public ChannelFuture newSucceededFuture()
  {
    return pipeline.newSucceededFuture();
  }
  
  public ChannelFuture newFailedFuture(Throwable cause)
  {
    return pipeline.newFailedFuture(cause);
  }
  
  public ChannelFuture closeFuture()
  {
    return closeFuture;
  }
  
  public Channel.Unsafe unsafe()
  {
    return unsafe;
  }
  



  protected abstract AbstractUnsafe newUnsafe();
  



  public final int hashCode()
  {
    return id.hashCode();
  }
  




  public final boolean equals(Object o)
  {
    return this == o;
  }
  
  public final int compareTo(Channel o)
  {
    if (this == o) {
      return 0;
    }
    
    return id().compareTo(o.id());
  }
  






  public String toString()
  {
    boolean active = isActive();
    if ((strValActive == active) && (strVal != null)) {
      return strVal;
    }
    
    SocketAddress remoteAddr = remoteAddress();
    SocketAddress localAddr = localAddress();
    if (remoteAddr != null)
    {







      StringBuilder buf = new StringBuilder(96).append("[id: 0x").append(id.asShortText()).append(", L:").append(localAddr).append(active ? " - " : " ! ").append("R:").append(remoteAddr).append(']');
      strVal = buf.toString();
    } else if (localAddr != null)
    {




      StringBuilder buf = new StringBuilder(64).append("[id: 0x").append(id.asShortText()).append(", L:").append(localAddr).append(']');
      strVal = buf.toString();

    }
    else
    {
      StringBuilder buf = new StringBuilder(16).append("[id: 0x").append(id.asShortText()).append(']');
      strVal = buf.toString();
    }
    
    strValActive = active;
    return strVal;
  }
  


  public final ChannelPromise voidPromise() { return pipeline.voidPromise(); }
  
  protected abstract boolean isCompatible(EventLoop paramEventLoop);
  
  protected abstract SocketAddress localAddress0();
  
  protected abstract SocketAddress remoteAddress0();
  protected abstract class AbstractUnsafe implements Channel.Unsafe { protected AbstractUnsafe() {}
    private volatile ChannelOutboundBuffer outboundBuffer = new ChannelOutboundBuffer(AbstractChannel.this);
    private RecvByteBufAllocator.Handle recvHandle;
    private boolean inFlush0;
    
    private boolean neverRegistered = true;
    
    private void assertEventLoop() {
      assert ((!registered) || (eventLoop.inEventLoop()));
    }
    
    public RecvByteBufAllocator.Handle recvBufAllocHandle()
    {
      if (recvHandle == null) {
        recvHandle = config().getRecvByteBufAllocator().newHandle();
      }
      return recvHandle;
    }
    
    public final ChannelOutboundBuffer outboundBuffer()
    {
      return outboundBuffer;
    }
    
    public final SocketAddress localAddress()
    {
      return localAddress0();
    }
    
    public final SocketAddress remoteAddress()
    {
      return remoteAddress0();
    }
    
    public final void register(EventLoop eventLoop, final ChannelPromise promise)
    {
      ObjectUtil.checkNotNull(eventLoop, "eventLoop");
      if (isRegistered()) {
        promise.setFailure(new IllegalStateException("registered to an event loop already"));
        return;
      }
      if (!isCompatible(eventLoop)) {
        promise.setFailure(new IllegalStateException("incompatible event loop type: " + eventLoop
          .getClass().getName()));
        return;
      }
      
      AbstractChannel.this.eventLoop = eventLoop;
      
      if (eventLoop.inEventLoop()) {
        register0(promise);
      } else {
        try {
          eventLoop.execute(new Runnable()
          {
            public void run() {
              AbstractChannel.AbstractUnsafe.this.register0(promise);
            }
          });
        } catch (Throwable t) {
          AbstractChannel.logger.warn("Force-closing a channel whose registration task was not accepted by an event loop: {}", AbstractChannel.this, t);
          

          closeForcibly();
          closeFuture.setClosed();
          safeSetFailure(promise, t);
        }
      }
    }
    
    private void register0(ChannelPromise promise)
    {
      try
      {
        if ((!promise.setUncancellable()) || (!ensureOpen(promise))) {
          return;
        }
        boolean firstRegistration = neverRegistered;
        doRegister();
        neverRegistered = false;
        registered = true;
        


        pipeline.invokeHandlerAddedIfNeeded();
        
        safeSetSuccess(promise);
        pipeline.fireChannelRegistered();
        

        if (isActive()) {
          if (firstRegistration) {
            pipeline.fireChannelActive();
          } else if (config().isAutoRead())
          {



            beginRead();
          }
        }
      }
      catch (Throwable t) {
        closeForcibly();
        closeFuture.setClosed();
        safeSetFailure(promise, t);
      }
    }
    
    public final void bind(SocketAddress localAddress, ChannelPromise promise)
    {
      assertEventLoop();
      
      if ((!promise.setUncancellable()) || (!ensureOpen(promise))) {
        return;
      }
      

      if ((Boolean.TRUE.equals(config().getOption(ChannelOption.SO_BROADCAST))) && ((localAddress instanceof InetSocketAddress)))
      {
        if ((!((InetSocketAddress)localAddress).getAddress().isAnyLocalAddress()) && 
          (!PlatformDependent.isWindows()) && (!PlatformDependent.maybeSuperUser()))
        {

          AbstractChannel.logger.warn("A non-root user can't receive a broadcast packet if the socket is not bound to a wildcard address; binding to a non-wildcard address (" + localAddress + ") anyway as requested.");
        }
      }
      


      boolean wasActive = isActive();
      try {
        doBind(localAddress);
      } catch (Throwable t) {
        safeSetFailure(promise, t);
        closeIfClosed();
        return;
      }
      
      if ((!wasActive) && (isActive())) {
        invokeLater(new Runnable()
        {
          public void run() {
            pipeline.fireChannelActive();
          }
        });
      }
      
      safeSetSuccess(promise);
    }
    
    public final void disconnect(ChannelPromise promise)
    {
      assertEventLoop();
      
      if (!promise.setUncancellable()) {
        return;
      }
      
      boolean wasActive = isActive();
      try {
        doDisconnect();
        
        remoteAddress = null;
        localAddress = null;
      } catch (Throwable t) {
        safeSetFailure(promise, t);
        closeIfClosed();
        return;
      }
      
      if ((wasActive) && (!isActive())) {
        invokeLater(new Runnable()
        {
          public void run() {
            pipeline.fireChannelInactive();
          }
        });
      }
      
      safeSetSuccess(promise);
      closeIfClosed();
    }
    
    public void close(ChannelPromise promise)
    {
      assertEventLoop();
      

      ClosedChannelException closedChannelException = StacklessClosedChannelException.newInstance(AbstractChannel.class, "close(ChannelPromise)");
      close(promise, closedChannelException, closedChannelException, false);
    }
    




    public final void shutdownOutput(ChannelPromise promise)
    {
      assertEventLoop();
      shutdownOutput(promise, null);
    }
    




    private void shutdownOutput(final ChannelPromise promise, Throwable cause)
    {
      if (!promise.setUncancellable()) {
        return;
      }
      
      final ChannelOutboundBuffer outboundBuffer = this.outboundBuffer;
      if (outboundBuffer == null) {
        promise.setFailure(new ClosedChannelException());
        return;
      }
      this.outboundBuffer = null;
      
      final Throwable shutdownCause = cause == null ? new ChannelOutputShutdownException("Channel output shutdown") : new ChannelOutputShutdownException("Channel output shutdown", cause);
      

      Executor closeExecutor = prepareToClose();
      if (closeExecutor != null) {
        closeExecutor.execute(new Runnable()
        {
          public void run()
          {
            try {
              doShutdownOutput();
              promise.setSuccess();
            } catch (Throwable err) {
              promise.setFailure(err);
            }
            finally {
              eventLoop().execute(new Runnable()
              {
                public void run() {
                  AbstractChannel.AbstractUnsafe.this.closeOutboundBufferForShutdown(pipeline, val$outboundBuffer, val$shutdownCause);
                }
              });
            }
          }
        });
      } else {
        try
        {
          doShutdownOutput();
          promise.setSuccess();
        } catch (Throwable err) {
          promise.setFailure(err);
        } finally {
          closeOutboundBufferForShutdown(pipeline, outboundBuffer, shutdownCause);
        }
      }
    }
    
    private void closeOutboundBufferForShutdown(ChannelPipeline pipeline, ChannelOutboundBuffer buffer, Throwable cause)
    {
      buffer.failFlushed(cause, false);
      buffer.close(cause, true);
      pipeline.fireUserEventTriggered(ChannelOutputShutdownEvent.INSTANCE);
    }
    
    private void close(final ChannelPromise promise, final Throwable cause, final ClosedChannelException closeCause, final boolean notify)
    {
      if (!promise.setUncancellable()) {
        return;
      }
      
      if (closeInitiated) {
        if (closeFuture.isDone())
        {
          safeSetSuccess(promise);
        } else if (!(promise instanceof VoidChannelPromise))
        {
          closeFuture.addListener(new ChannelFutureListener()
          {
            public void operationComplete(ChannelFuture future) throws Exception {
              promise.setSuccess();
            }
          });
        }
        return;
      }
      
      closeInitiated = true;
      
      final boolean wasActive = isActive();
      final ChannelOutboundBuffer outboundBuffer = this.outboundBuffer;
      this.outboundBuffer = null;
      Executor closeExecutor = prepareToClose();
      if (closeExecutor != null) {
        closeExecutor.execute(new Runnable()
        {
          public void run()
          {
            try {
              AbstractChannel.AbstractUnsafe.this.doClose0(promise);
              

              AbstractChannel.AbstractUnsafe.this.invokeLater(new Runnable()
              {
                public void run() {
                  if (val$outboundBuffer != null)
                  {
                    val$outboundBuffer.failFlushed(val$cause, val$notify);
                    val$outboundBuffer.close(val$closeCause);
                  }
                  AbstractChannel.AbstractUnsafe.this.fireChannelInactiveAndDeregister(val$wasActive);
                }
              });
            }
            finally
            {
              AbstractChannel.AbstractUnsafe.this.invokeLater(new Runnable()
              {
                public void run() {
                  if (val$outboundBuffer != null)
                  {
                    val$outboundBuffer.failFlushed(val$cause, val$notify);
                    val$outboundBuffer.close(val$closeCause);
                  }
                  AbstractChannel.AbstractUnsafe.this.fireChannelInactiveAndDeregister(val$wasActive);
                }
              });
            }
          }
        });
      }
      else {
        try {
          doClose0(promise);
        } finally {
          if (outboundBuffer != null)
          {
            outboundBuffer.failFlushed(cause, notify);
            outboundBuffer.close(closeCause);
          }
        }
        if (inFlush0) {
          invokeLater(new Runnable()
          {
            public void run() {
              AbstractChannel.AbstractUnsafe.this.fireChannelInactiveAndDeregister(wasActive);
            }
          });
        } else {
          fireChannelInactiveAndDeregister(wasActive);
        }
      }
    }
    
    private void doClose0(ChannelPromise promise) {
      try {
        doClose();
        closeFuture.setClosed();
        safeSetSuccess(promise);
      } catch (Throwable t) {
        closeFuture.setClosed();
        safeSetFailure(promise, t);
      }
    }
    
    private void fireChannelInactiveAndDeregister(boolean wasActive) {
      deregister(voidPromise(), (wasActive) && (!isActive()));
    }
    
    public final void closeForcibly()
    {
      assertEventLoop();
      try
      {
        doClose();
      } catch (Exception e) {
        AbstractChannel.logger.warn("Failed to close a channel.", e);
      }
    }
    
    public final void deregister(ChannelPromise promise)
    {
      assertEventLoop();
      
      deregister(promise, false);
    }
    
    private void deregister(final ChannelPromise promise, final boolean fireChannelInactive) {
      if (!promise.setUncancellable()) {
        return;
      }
      
      if (!registered) {
        safeSetSuccess(promise);
        return;
      }
      









      invokeLater(new Runnable()
      {
        public void run() {
          try {
            doDeregister();
          } catch (Throwable t) {
            AbstractChannel.logger.warn("Unexpected exception occurred while deregistering a channel.", t);
          } finally {
            if (fireChannelInactive) {
              pipeline.fireChannelInactive();
            }
            



            if (registered) {
              registered = false;
              pipeline.fireChannelUnregistered();
            }
            safeSetSuccess(promise);
          }
        }
      });
    }
    
    public final void beginRead()
    {
      assertEventLoop();
      try
      {
        doBeginRead();
      } catch (Exception e) {
        invokeLater(new Runnable()
        {
          public void run() {
            pipeline.fireExceptionCaught(e);
          }
        });
        close(voidPromise());
      }
    }
    
    public final void write(Object msg, ChannelPromise promise)
    {
      assertEventLoop();
      
      ChannelOutboundBuffer outboundBuffer = this.outboundBuffer;
      if (outboundBuffer == null)
      {
        try {
          ReferenceCountUtil.release(msg);

        }
        finally
        {

          safeSetFailure(promise, 
            newClosedChannelException(initialCloseCause, "write(Object, ChannelPromise)"));
        }
        return;
      }
      
      try
      {
        msg = filterOutboundMessage(msg);
        int size = pipeline.estimatorHandle().size(msg);
        if (size < 0) {
          size = 0;
        }
      } catch (Throwable t) {
        try {
          ReferenceCountUtil.release(msg);
        } finally {
          safeSetFailure(promise, t);
        }
        return;
      }
      int size;
      outboundBuffer.addMessage(msg, size, promise);
    }
    
    public final void flush()
    {
      assertEventLoop();
      
      ChannelOutboundBuffer outboundBuffer = this.outboundBuffer;
      if (outboundBuffer == null) {
        return;
      }
      
      outboundBuffer.addFlush();
      flush0();
    }
    
    protected void flush0()
    {
      if (inFlush0)
      {
        return;
      }
      
      ChannelOutboundBuffer outboundBuffer = this.outboundBuffer;
      if ((outboundBuffer == null) || (outboundBuffer.isEmpty())) {
        return;
      }
      
      inFlush0 = true;
      

      if (!isActive()) {
        try
        {
          if (!outboundBuffer.isEmpty()) {
            if (isOpen()) {
              outboundBuffer.failFlushed(new NotYetConnectedException(), true);
            }
            else {
              outboundBuffer.failFlushed(newClosedChannelException(initialCloseCause, "flush0()"), false);
            }
          }
          
          inFlush0 = false; } finally { inFlush0 = false;
        }
      }
      
      try
      {
        doWrite(outboundBuffer);
      } catch (Throwable t) {
        handleWriteError(t);
      } finally {
        inFlush0 = false;
      }
    }
    
    protected final void handleWriteError(Throwable t) {
      if (((t instanceof IOException)) && (config().isAutoClose()))
      {







        initialCloseCause = t;
        close(voidPromise(), t, newClosedChannelException(t, "flush0()"), false);
      } else {
        try {
          shutdownOutput(voidPromise(), t);
        } catch (Throwable t2) {
          initialCloseCause = t;
          close(voidPromise(), t2, newClosedChannelException(t, "flush0()"), false);
        }
      }
    }
    
    private ClosedChannelException newClosedChannelException(Throwable cause, String method)
    {
      ClosedChannelException exception = StacklessClosedChannelException.newInstance(AbstractUnsafe.class, method);
      if (cause != null) {
        exception.initCause(cause);
      }
      return exception;
    }
    
    public final ChannelPromise voidPromise()
    {
      assertEventLoop();
      
      return unsafeVoidPromise;
    }
    
    protected final boolean ensureOpen(ChannelPromise promise) {
      if (isOpen()) {
        return true;
      }
      
      safeSetFailure(promise, newClosedChannelException(initialCloseCause, "ensureOpen(ChannelPromise)"));
      return false;
    }
    


    protected final void safeSetSuccess(ChannelPromise promise)
    {
      if ((!(promise instanceof VoidChannelPromise)) && (!promise.trySuccess())) {
        AbstractChannel.logger.warn("Failed to mark a promise as success because it is done already: {}", promise);
      }
    }
    


    protected final void safeSetFailure(ChannelPromise promise, Throwable cause)
    {
      if ((!(promise instanceof VoidChannelPromise)) && (!promise.tryFailure(cause))) {
        AbstractChannel.logger.warn("Failed to mark a promise as failure because it's done already: {}", promise, cause);
      }
    }
    
    protected final void closeIfClosed() {
      if (isOpen()) {
        return;
      }
      close(voidPromise());
    }
    









    private void invokeLater(Runnable task)
    {
      try
      {
        eventLoop().execute(task);
      } catch (RejectedExecutionException e) {
        AbstractChannel.logger.warn("Can't invoke task later as EventLoop rejected it", e);
      }
    }
    


    protected final Throwable annotateConnectException(Throwable cause, SocketAddress remoteAddress)
    {
      if ((cause instanceof ConnectException)) {
        return new AbstractChannel.AnnotatedConnectException((ConnectException)cause, remoteAddress);
      }
      if ((cause instanceof NoRouteToHostException)) {
        return new AbstractChannel.AnnotatedNoRouteToHostException((NoRouteToHostException)cause, remoteAddress);
      }
      if ((cause instanceof SocketException)) {
        return new AbstractChannel.AnnotatedSocketException((SocketException)cause, remoteAddress);
      }
      
      return cause;
    }
    





    protected Executor prepareToClose()
    {
      return null;
    }
  }
  






  protected void doRegister()
    throws Exception
  {}
  






  protected abstract void doBind(SocketAddress paramSocketAddress)
    throws Exception;
  






  protected abstract void doDisconnect()
    throws Exception;
  






  protected abstract void doClose()
    throws Exception;
  





  protected void doShutdownOutput()
    throws Exception
  {
    doClose();
  }
  



  protected void doDeregister()
    throws Exception
  {}
  



  protected abstract void doBeginRead()
    throws Exception;
  



  protected abstract void doWrite(ChannelOutboundBuffer paramChannelOutboundBuffer)
    throws Exception;
  


  protected Object filterOutboundMessage(Object msg)
    throws Exception
  {
    return msg;
  }
  
  protected void validateFileRegion(DefaultFileRegion region, long position) throws IOException {
    DefaultFileRegion.validate(region, position);
  }
  
  static final class CloseFuture extends DefaultChannelPromise
  {
    CloseFuture(AbstractChannel ch) {
      super();
    }
    
    public ChannelPromise setSuccess()
    {
      throw new IllegalStateException();
    }
    
    public ChannelPromise setFailure(Throwable cause)
    {
      throw new IllegalStateException();
    }
    
    public boolean trySuccess()
    {
      throw new IllegalStateException();
    }
    
    public boolean tryFailure(Throwable cause)
    {
      throw new IllegalStateException();
    }
    
    boolean setClosed() {
      return super.trySuccess();
    }
  }
  
  private static final class AnnotatedConnectException extends ConnectException
  {
    private static final long serialVersionUID = 3901958112696433556L;
    
    AnnotatedConnectException(ConnectException exception, SocketAddress remoteAddress) {
      super();
      initCause(exception);
    }
    

    public Throwable fillInStackTrace()
    {
      return this;
    }
  }
  
  private static final class AnnotatedNoRouteToHostException extends NoRouteToHostException
  {
    private static final long serialVersionUID = -6801433937592080623L;
    
    AnnotatedNoRouteToHostException(NoRouteToHostException exception, SocketAddress remoteAddress) {
      super();
      initCause(exception);
    }
    

    public Throwable fillInStackTrace()
    {
      return this;
    }
  }
  
  private static final class AnnotatedSocketException extends SocketException
  {
    private static final long serialVersionUID = 3896743275010454039L;
    
    AnnotatedSocketException(SocketException exception, SocketAddress remoteAddress) {
      super();
      initCause(exception);
    }
    

    public Throwable fillInStackTrace()
    {
      return this;
    }
  }
}
