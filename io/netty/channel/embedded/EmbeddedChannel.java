package io.netty.channel.embedded;

import io.netty.channel.AbstractChannel;
import io.netty.channel.AbstractChannel.AbstractUnsafe;
import io.netty.channel.Channel;
import io.netty.channel.Channel.Unsafe;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.DefaultChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.channel.RecvByteBufAllocator.Handle;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.RecyclableArrayList;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayDeque;
import java.util.Queue;

















public class EmbeddedChannel
  extends AbstractChannel
{
  private static final SocketAddress LOCAL_ADDRESS = new EmbeddedSocketAddress();
  private static final SocketAddress REMOTE_ADDRESS = new EmbeddedSocketAddress();
  
  private static final ChannelHandler[] EMPTY_HANDLERS = new ChannelHandler[0];
  private static enum State { OPEN,  ACTIVE,  CLOSED;
    private State() {} }
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(EmbeddedChannel.class);
  
  private static final ChannelMetadata METADATA_NO_DISCONNECT = new ChannelMetadata(false);
  private static final ChannelMetadata METADATA_DISCONNECT = new ChannelMetadata(true);
  
  private final EmbeddedEventLoop loop = new EmbeddedEventLoop();
  private final ChannelFutureListener recordExceptionListener = new ChannelFutureListener()
  {
    public void operationComplete(ChannelFuture future) throws Exception {
      EmbeddedChannel.this.recordException(future);
    }
  };
  
  private final ChannelMetadata metadata;
  
  private final ChannelConfig config;
  
  private Queue<Object> inboundMessages;
  
  private Queue<Object> outboundMessages;
  private Throwable lastException;
  private State state;
  
  public EmbeddedChannel()
  {
    this(EMPTY_HANDLERS);
  }
  




  public EmbeddedChannel(ChannelId channelId)
  {
    this(channelId, EMPTY_HANDLERS);
  }
  




  public EmbeddedChannel(ChannelHandler... handlers)
  {
    this(EmbeddedChannelId.INSTANCE, handlers);
  }
  






  public EmbeddedChannel(boolean hasDisconnect, ChannelHandler... handlers)
  {
    this(EmbeddedChannelId.INSTANCE, hasDisconnect, handlers);
  }
  








  public EmbeddedChannel(boolean register, boolean hasDisconnect, ChannelHandler... handlers)
  {
    this(EmbeddedChannelId.INSTANCE, register, hasDisconnect, handlers);
  }
  






  public EmbeddedChannel(ChannelId channelId, ChannelHandler... handlers)
  {
    this(channelId, false, handlers);
  }
  








  public EmbeddedChannel(ChannelId channelId, boolean hasDisconnect, ChannelHandler... handlers)
  {
    this(channelId, true, hasDisconnect, handlers);
  }
  











  public EmbeddedChannel(ChannelId channelId, boolean register, boolean hasDisconnect, ChannelHandler... handlers)
  {
    this(null, channelId, register, hasDisconnect, handlers);
  }
  












  public EmbeddedChannel(Channel parent, ChannelId channelId, boolean register, boolean hasDisconnect, ChannelHandler... handlers)
  {
    super(parent, channelId);
    metadata = metadata(hasDisconnect);
    config = new DefaultChannelConfig(this);
    setup(register, handlers);
  }
  










  public EmbeddedChannel(ChannelId channelId, boolean hasDisconnect, ChannelConfig config, ChannelHandler... handlers)
  {
    super(null, channelId);
    metadata = metadata(hasDisconnect);
    this.config = ((ChannelConfig)ObjectUtil.checkNotNull(config, "config"));
    setup(true, handlers);
  }
  
  private static ChannelMetadata metadata(boolean hasDisconnect) {
    return hasDisconnect ? METADATA_DISCONNECT : METADATA_NO_DISCONNECT;
  }
  
  private void setup(boolean register, final ChannelHandler... handlers) {
    ObjectUtil.checkNotNull(handlers, "handlers");
    ChannelPipeline p = pipeline();
    p.addLast(new ChannelHandler[] { new ChannelInitializer()
    {
      protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        for (ChannelHandler h : handlers) {
          if (h == null) {
            break;
          }
          pipeline.addLast(new ChannelHandler[] { h });
        }
      }
    } });
    if (register) {
      ChannelFuture future = loop.register(this);
      assert (future.isDone());
    }
  }
  

  public void register()
    throws Exception
  {
    ChannelFuture future = loop.register(this);
    assert (future.isDone());
    Throwable cause = future.cause();
    if (cause != null) {
      PlatformDependent.throwException(cause);
    }
  }
  
  protected final DefaultChannelPipeline newChannelPipeline()
  {
    return new EmbeddedChannelPipeline(this);
  }
  
  public ChannelMetadata metadata()
  {
    return metadata;
  }
  
  public ChannelConfig config()
  {
    return config;
  }
  
  public boolean isOpen()
  {
    return state != State.CLOSED;
  }
  
  public boolean isActive()
  {
    return state == State.ACTIVE;
  }
  


  public Queue<Object> inboundMessages()
  {
    if (inboundMessages == null) {
      inboundMessages = new ArrayDeque();
    }
    return inboundMessages;
  }
  


  @Deprecated
  public Queue<Object> lastInboundBuffer()
  {
    return inboundMessages();
  }
  


  public Queue<Object> outboundMessages()
  {
    if (outboundMessages == null) {
      outboundMessages = new ArrayDeque();
    }
    return outboundMessages;
  }
  


  @Deprecated
  public Queue<Object> lastOutboundBuffer()
  {
    return outboundMessages();
  }
  



  public <T> T readInbound()
  {
    T message = poll(inboundMessages);
    if (message != null) {
      ReferenceCountUtil.touch(message, "Caller of readInbound() will handle the message from this point");
    }
    return message;
  }
  



  public <T> T readOutbound()
  {
    T message = poll(outboundMessages);
    if (message != null) {
      ReferenceCountUtil.touch(message, "Caller of readOutbound() will handle the message from this point.");
    }
    return message;
  }
  






  public boolean writeInbound(Object... msgs)
  {
    ensureOpen();
    if (msgs.length == 0) {
      return isNotEmpty(inboundMessages);
    }
    
    ChannelPipeline p = pipeline();
    for (Object m : msgs) {
      p.fireChannelRead(m);
    }
    
    flushInbound(false, voidPromise());
    return isNotEmpty(inboundMessages);
  }
  





  public ChannelFuture writeOneInbound(Object msg)
  {
    return writeOneInbound(msg, newPromise());
  }
  





  public ChannelFuture writeOneInbound(Object msg, ChannelPromise promise)
  {
    if (checkOpen(true)) {
      pipeline().fireChannelRead(msg);
    }
    return checkException(promise);
  }
  




  public EmbeddedChannel flushInbound()
  {
    flushInbound(true, voidPromise());
    return this;
  }
  
  private ChannelFuture flushInbound(boolean recordException, ChannelPromise promise) {
    if (checkOpen(recordException)) {
      pipeline().fireChannelReadComplete();
      runPendingTasks();
    }
    
    return checkException(promise);
  }
  





  public boolean writeOutbound(Object... msgs)
  {
    ensureOpen();
    if (msgs.length == 0) {
      return isNotEmpty(outboundMessages);
    }
    
    RecyclableArrayList futures = RecyclableArrayList.newInstance(msgs.length);
    try {
      for (Object m : msgs) {
        if (m == null) {
          break;
        }
        futures.add(write(m));
      }
      
      flushOutbound0();
      
      int size = futures.size();
      for (int i = 0; i < size; i++) {
        ChannelFuture future = (ChannelFuture)futures.get(i);
        if (future.isDone()) {
          recordException(future);
        }
        else {
          future.addListener(recordExceptionListener);
        }
      }
      
      checkException();
      return isNotEmpty(outboundMessages);
    } finally {
      futures.recycle();
    }
  }
  





  public ChannelFuture writeOneOutbound(Object msg)
  {
    return writeOneOutbound(msg, newPromise());
  }
  





  public ChannelFuture writeOneOutbound(Object msg, ChannelPromise promise)
  {
    if (checkOpen(true)) {
      return write(msg, promise);
    }
    return checkException(promise);
  }
  




  public EmbeddedChannel flushOutbound()
  {
    if (checkOpen(true)) {
      flushOutbound0();
    }
    checkException(voidPromise());
    return this;
  }
  

  private void flushOutbound0()
  {
    runPendingTasks();
    
    flush();
  }
  




  public boolean finish()
  {
    return finish(false);
  }
  





  public boolean finishAndReleaseAll()
  {
    return finish(true);
  }
  





  private boolean finish(boolean releaseAll)
  {
    close();
    try {
      checkException();
      return (isNotEmpty(inboundMessages)) || (isNotEmpty(outboundMessages));
    } finally {
      if (releaseAll) {
        releaseAll(inboundMessages);
        releaseAll(outboundMessages);
      }
    }
  }
  



  public boolean releaseInbound()
  {
    return releaseAll(inboundMessages);
  }
  



  public boolean releaseOutbound()
  {
    return releaseAll(outboundMessages);
  }
  
  private static boolean releaseAll(Queue<Object> queue) {
    if (isNotEmpty(queue)) {
      for (;;) {
        Object msg = queue.poll();
        if (msg == null) {
          break;
        }
        ReferenceCountUtil.release(msg);
      }
      return true;
    }
    return false;
  }
  
  private void finishPendingTasks(boolean cancel) {
    runPendingTasks();
    if (cancel)
    {
      loop.cancelScheduledTasks();
    }
  }
  
  public final ChannelFuture close()
  {
    return close(newPromise());
  }
  
  public final ChannelFuture disconnect()
  {
    return disconnect(newPromise());
  }
  


  public final ChannelFuture close(ChannelPromise promise)
  {
    runPendingTasks();
    ChannelFuture future = super.close(promise);
    

    finishPendingTasks(true);
    return future;
  }
  
  public final ChannelFuture disconnect(ChannelPromise promise)
  {
    ChannelFuture future = super.disconnect(promise);
    finishPendingTasks(!metadata.hasDisconnect());
    return future;
  }
  
  private static boolean isNotEmpty(Queue<Object> queue) {
    return (queue != null) && (!queue.isEmpty());
  }
  
  private static Object poll(Queue<Object> queue) {
    return queue != null ? queue.poll() : null;
  }
  


  public void runPendingTasks()
  {
    try
    {
      loop.runTasks();
    } catch (Exception e) {
      recordException(e);
    }
    try
    {
      loop.runScheduledTasks();
    } catch (Exception e) {
      recordException(e);
    }
  }
  



  public long runScheduledPendingTasks()
  {
    try
    {
      return loop.runScheduledTasks();
    } catch (Exception e) {
      recordException(e); }
    return loop.nextScheduledTask();
  }
  
  private void recordException(ChannelFuture future)
  {
    if (!future.isSuccess()) {
      recordException(future.cause());
    }
  }
  
  private void recordException(Throwable cause) {
    if (lastException == null) {
      lastException = cause;
    } else {
      logger.warn("More than one exception was raised. Will report only the first one and log others.", cause);
    }
  }
  




  private ChannelFuture checkException(ChannelPromise promise)
  {
    Throwable t = lastException;
    if (t != null) {
      lastException = null;
      
      if (promise.isVoid()) {
        PlatformDependent.throwException(t);
      }
      
      return promise.setFailure(t);
    }
    
    return promise.setSuccess();
  }
  


  public void checkException()
  {
    checkException(voidPromise());
  }
  



  private boolean checkOpen(boolean recordException)
  {
    if (!isOpen()) {
      if (recordException) {
        recordException(new ClosedChannelException());
      }
      return false;
    }
    
    return true;
  }
  


  protected final void ensureOpen()
  {
    if (!checkOpen(true)) {
      checkException();
    }
  }
  
  protected boolean isCompatible(EventLoop loop)
  {
    return loop instanceof EmbeddedEventLoop;
  }
  
  protected SocketAddress localAddress0()
  {
    return isActive() ? LOCAL_ADDRESS : null;
  }
  
  protected SocketAddress remoteAddress0()
  {
    return isActive() ? REMOTE_ADDRESS : null;
  }
  
  protected void doRegister() throws Exception
  {
    state = State.ACTIVE;
  }
  
  protected void doBind(SocketAddress localAddress)
    throws Exception
  {}
  
  protected void doDisconnect()
    throws Exception
  {
    if (!metadata.hasDisconnect()) {
      doClose();
    }
  }
  
  protected void doClose() throws Exception
  {
    state = State.CLOSED;
  }
  

  protected void doBeginRead()
    throws Exception
  {}
  
  protected AbstractChannel.AbstractUnsafe newUnsafe()
  {
    return new EmbeddedUnsafe(null);
  }
  
  public Channel.Unsafe unsafe()
  {
    return unsafewrapped;
  }
  
  protected void doWrite(ChannelOutboundBuffer in) throws Exception
  {
    for (;;) {
      Object msg = in.current();
      if (msg == null) {
        break;
      }
      
      ReferenceCountUtil.retain(msg);
      handleOutboundMessage(msg);
      in.remove();
    }
  }
  




  protected void handleOutboundMessage(Object msg)
  {
    outboundMessages().add(msg);
  }
  




  protected void handleInboundMessage(Object msg) { inboundMessages().add(msg); }
  
  private final class EmbeddedUnsafe extends AbstractChannel.AbstractUnsafe {
    private EmbeddedUnsafe() { super(); }
    


    final Channel.Unsafe wrapped = new Channel.Unsafe()
    {
      public RecvByteBufAllocator.Handle recvBufAllocHandle() {
        return EmbeddedChannel.EmbeddedUnsafe.this.recvBufAllocHandle();
      }
      
      public SocketAddress localAddress()
      {
        return EmbeddedChannel.EmbeddedUnsafe.this.localAddress();
      }
      
      public SocketAddress remoteAddress()
      {
        return EmbeddedChannel.EmbeddedUnsafe.this.remoteAddress();
      }
      
      public void register(EventLoop eventLoop, ChannelPromise promise)
      {
        EmbeddedChannel.EmbeddedUnsafe.this.register(eventLoop, promise);
        runPendingTasks();
      }
      
      public void bind(SocketAddress localAddress, ChannelPromise promise)
      {
        EmbeddedChannel.EmbeddedUnsafe.this.bind(localAddress, promise);
        runPendingTasks();
      }
      
      public void connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise)
      {
        EmbeddedChannel.EmbeddedUnsafe.this.connect(remoteAddress, localAddress, promise);
        runPendingTasks();
      }
      
      public void disconnect(ChannelPromise promise)
      {
        EmbeddedChannel.EmbeddedUnsafe.this.disconnect(promise);
        runPendingTasks();
      }
      
      public void close(ChannelPromise promise)
      {
        EmbeddedChannel.EmbeddedUnsafe.this.close(promise);
        runPendingTasks();
      }
      
      public void closeForcibly()
      {
        EmbeddedChannel.EmbeddedUnsafe.this.closeForcibly();
        runPendingTasks();
      }
      
      public void deregister(ChannelPromise promise)
      {
        EmbeddedChannel.EmbeddedUnsafe.this.deregister(promise);
        runPendingTasks();
      }
      
      public void beginRead()
      {
        EmbeddedChannel.EmbeddedUnsafe.this.beginRead();
        runPendingTasks();
      }
      
      public void write(Object msg, ChannelPromise promise)
      {
        EmbeddedChannel.EmbeddedUnsafe.this.write(msg, promise);
        runPendingTasks();
      }
      
      public void flush()
      {
        EmbeddedChannel.EmbeddedUnsafe.this.flush();
        runPendingTasks();
      }
      
      public ChannelPromise voidPromise()
      {
        return EmbeddedChannel.EmbeddedUnsafe.this.voidPromise();
      }
      
      public ChannelOutboundBuffer outboundBuffer()
      {
        return EmbeddedChannel.EmbeddedUnsafe.this.outboundBuffer();
      }
    };
    
    public void connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise)
    {
      safeSetSuccess(promise);
    }
  }
  
  private final class EmbeddedChannelPipeline extends DefaultChannelPipeline {
    EmbeddedChannelPipeline(EmbeddedChannel channel) {
      super();
    }
    
    protected void onUnhandledInboundException(Throwable cause)
    {
      EmbeddedChannel.this.recordException(cause);
    }
    
    protected void onUnhandledInboundMessage(ChannelHandlerContext ctx, Object msg)
    {
      handleInboundMessage(msg);
    }
  }
}
