package io.netty.handler.ssl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.AbstractCoalescingBufferQueue;
import io.netty.channel.Channel;
import io.netty.channel.Channel.Unsafe;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.ByteToMessageDecoder.Cumulator;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.UnsupportedMessageTypeException;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.ImmediateExecutor;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.PromiseNotifier;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;















































































































public class SslHandler
  extends ByteToMessageDecoder
  implements ChannelOutboundHandler
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(SslHandler.class);
  private static final Pattern IGNORABLE_CLASS_IN_STACK = Pattern.compile("^.*(?:Socket|Datagram|Sctp|Udt)Channel.*$");
  
  private static final Pattern IGNORABLE_ERROR_MESSAGE = Pattern.compile("^.*(?:connection.*(?:reset|closed|abort|broken)|broken.*pipe).*$", 2);
  
  private static final int STATE_SENT_FIRST_MESSAGE = 1;
  
  private static final int STATE_FLUSHED_BEFORE_HANDSHAKE = 2;
  
  private static final int STATE_READ_DURING_HANDSHAKE = 4;
  
  private static final int STATE_HANDSHAKE_STARTED = 8;
  
  private static final int STATE_NEEDS_FLUSH = 16;
  
  private static final int STATE_OUTBOUND_CLOSED = 32;
  
  private static final int STATE_CLOSE_NOTIFY = 64;
  
  private static final int STATE_PROCESS_TASK = 128;
  private static final int STATE_FIRE_CHANNEL_READ = 256;
  private static final int STATE_UNWRAP_REENTRY = 512;
  private static final int MAX_PLAINTEXT_LENGTH = 16384;
  private volatile ChannelHandlerContext ctx;
  private final SSLEngine engine;
  private final SslEngineType engineType;
  private final Executor delegatedTaskExecutor;
  private final boolean jdkCompatibilityMode;
  
  private static abstract enum SslEngineType
  {
    TCNATIVE(true, ByteToMessageDecoder.COMPOSITE_CUMULATOR), 
    











































    CONSCRYPT(true, ByteToMessageDecoder.COMPOSITE_CUMULATOR), 
    









































    JDK(false, ByteToMessageDecoder.MERGE_CUMULATOR);
    















    final boolean wantsDirectBuffer;
    














    final ByteToMessageDecoder.Cumulator cumulator;
    














    static SslEngineType forEngine(SSLEngine engine)
    {
      return (engine instanceof ConscryptAlpnSslEngine) ? CONSCRYPT : (engine instanceof ReferenceCountedOpenSslEngine) ? TCNATIVE : JDK;
    }
    
    private SslEngineType(boolean wantsDirectBuffer, ByteToMessageDecoder.Cumulator cumulator)
    {
      this.wantsDirectBuffer = wantsDirectBuffer;
      this.cumulator = cumulator;
    }
    







    abstract SSLEngineResult unwrap(SslHandler paramSslHandler, ByteBuf paramByteBuf1, int paramInt, ByteBuf paramByteBuf2)
      throws SSLException;
    






    abstract int calculatePendingData(SslHandler paramSslHandler, int paramInt);
    






    abstract boolean jdkCompatibilityMode(SSLEngine paramSSLEngine);
    






    abstract ByteBuf allocateWrapBuffer(SslHandler paramSslHandler, ByteBufAllocator paramByteBufAllocator, int paramInt1, int paramInt2);
  }
  






  private final ByteBuffer[] singleBuffer = new ByteBuffer[1];
  
  private final boolean startTls;
  
  private final SslTasksRunner sslTaskRunnerForUnwrap = new SslTasksRunner(true);
  private final SslTasksRunner sslTaskRunner = new SslTasksRunner(false);
  
  private SslHandlerCoalescingBufferQueue pendingUnencryptedWrites;
  private Promise<Channel> handshakePromise = new LazyChannelPromise(null);
  private final LazyChannelPromise sslClosePromise = new LazyChannelPromise(null);
  
  private int packetLength;
  
  private short state;
  private volatile long handshakeTimeoutMillis = 10000L;
  private volatile long closeNotifyFlushTimeoutMillis = 3000L;
  private volatile long closeNotifyReadTimeoutMillis;
  volatile int wrapDataSize = 16384;
  




  public SslHandler(SSLEngine engine)
  {
    this(engine, false);
  }
  






  public SslHandler(SSLEngine engine, boolean startTls)
  {
    this(engine, startTls, ImmediateExecutor.INSTANCE);
  }
  






  public SslHandler(SSLEngine engine, Executor delegatedTaskExecutor)
  {
    this(engine, false, delegatedTaskExecutor);
  }
  








  public SslHandler(SSLEngine engine, boolean startTls, Executor delegatedTaskExecutor)
  {
    this.engine = ((SSLEngine)ObjectUtil.checkNotNull(engine, "engine"));
    this.delegatedTaskExecutor = ((Executor)ObjectUtil.checkNotNull(delegatedTaskExecutor, "delegatedTaskExecutor"));
    engineType = SslEngineType.forEngine(engine);
    this.startTls = startTls;
    jdkCompatibilityMode = engineType.jdkCompatibilityMode(engine);
    setCumulator(engineType.cumulator);
  }
  
  public long getHandshakeTimeoutMillis() {
    return handshakeTimeoutMillis;
  }
  
  public void setHandshakeTimeout(long handshakeTimeout, TimeUnit unit) {
    ObjectUtil.checkNotNull(unit, "unit");
    setHandshakeTimeoutMillis(unit.toMillis(handshakeTimeout));
  }
  
  public void setHandshakeTimeoutMillis(long handshakeTimeoutMillis) {
    this.handshakeTimeoutMillis = ObjectUtil.checkPositiveOrZero(handshakeTimeoutMillis, "handshakeTimeoutMillis");
  }
  




















  public final void setWrapDataSize(int wrapDataSize)
  {
    this.wrapDataSize = wrapDataSize;
  }
  


  @Deprecated
  public long getCloseNotifyTimeoutMillis()
  {
    return getCloseNotifyFlushTimeoutMillis();
  }
  


  @Deprecated
  public void setCloseNotifyTimeout(long closeNotifyTimeout, TimeUnit unit)
  {
    setCloseNotifyFlushTimeout(closeNotifyTimeout, unit);
  }
  


  @Deprecated
  public void setCloseNotifyTimeoutMillis(long closeNotifyFlushTimeoutMillis)
  {
    setCloseNotifyFlushTimeoutMillis(closeNotifyFlushTimeoutMillis);
  }
  




  public final long getCloseNotifyFlushTimeoutMillis()
  {
    return closeNotifyFlushTimeoutMillis;
  }
  




  public final void setCloseNotifyFlushTimeout(long closeNotifyFlushTimeout, TimeUnit unit)
  {
    setCloseNotifyFlushTimeoutMillis(unit.toMillis(closeNotifyFlushTimeout));
  }
  


  public final void setCloseNotifyFlushTimeoutMillis(long closeNotifyFlushTimeoutMillis)
  {
    this.closeNotifyFlushTimeoutMillis = ObjectUtil.checkPositiveOrZero(closeNotifyFlushTimeoutMillis, "closeNotifyFlushTimeoutMillis");
  }
  





  public final long getCloseNotifyReadTimeoutMillis()
  {
    return closeNotifyReadTimeoutMillis;
  }
  




  public final void setCloseNotifyReadTimeout(long closeNotifyReadTimeout, TimeUnit unit)
  {
    setCloseNotifyReadTimeoutMillis(unit.toMillis(closeNotifyReadTimeout));
  }
  


  public final void setCloseNotifyReadTimeoutMillis(long closeNotifyReadTimeoutMillis)
  {
    this.closeNotifyReadTimeoutMillis = ObjectUtil.checkPositiveOrZero(closeNotifyReadTimeoutMillis, "closeNotifyReadTimeoutMillis");
  }
  



  public SSLEngine engine()
  {
    return engine;
  }
  




  public String applicationProtocol()
  {
    SSLEngine engine = engine();
    if (!(engine instanceof ApplicationProtocolAccessor)) {
      return null;
    }
    
    return ((ApplicationProtocolAccessor)engine).getNegotiatedApplicationProtocol();
  }
  





  public Future<Channel> handshakeFuture()
  {
    return handshakePromise;
  }
  


  @Deprecated
  public ChannelFuture close()
  {
    return closeOutbound();
  }
  


  @Deprecated
  public ChannelFuture close(ChannelPromise promise)
  {
    return closeOutbound(promise);
  }
  





  public ChannelFuture closeOutbound()
  {
    return closeOutbound(ctx.newPromise());
  }
  





  public ChannelFuture closeOutbound(final ChannelPromise promise)
  {
    ChannelHandlerContext ctx = this.ctx;
    if (ctx.executor().inEventLoop()) {
      closeOutbound0(promise);
    } else {
      ctx.executor().execute(new Runnable()
      {
        public void run() {
          SslHandler.this.closeOutbound0(promise);
        }
      });
    }
    return promise;
  }
  
  private void closeOutbound0(ChannelPromise promise) {
    setState(32);
    engine.closeOutbound();
    try {
      flush(ctx, promise);
    } catch (Exception e) {
      if (!promise.tryFailure(e)) {
        logger.warn("{} flush() raised a masked exception.", ctx.channel(), e);
      }
    }
  }
  






  public Future<Channel> sslCloseFuture()
  {
    return sslClosePromise;
  }
  
  public void handlerRemoved0(ChannelHandlerContext ctx) throws Exception
  {
    if (!pendingUnencryptedWrites.isEmpty())
    {
      pendingUnencryptedWrites.releaseAndFailAll(ctx, new ChannelException("Pending write on removal of SslHandler"));
    }
    
    pendingUnencryptedWrites = null;
    
    SSLHandshakeException cause = null;
    


    if (!handshakePromise.isDone()) {
      cause = new SSLHandshakeException("SslHandler removed before handshake completed");
      if (handshakePromise.tryFailure(cause)) {
        ctx.fireUserEventTriggered(new SslHandshakeCompletionEvent(cause));
      }
    }
    if (!sslClosePromise.isDone()) {
      if (cause == null) {
        cause = new SSLHandshakeException("SslHandler removed before handshake completed");
      }
      notifyClosePromise(cause);
    }
    
    if ((engine instanceof ReferenceCounted)) {
      ((ReferenceCounted)engine).release();
    }
  }
  
  public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception
  {
    ctx.bind(localAddress, promise);
  }
  
  public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise)
    throws Exception
  {
    ctx.connect(remoteAddress, localAddress, promise);
  }
  
  public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception
  {
    ctx.deregister(promise);
  }
  
  public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise)
    throws Exception
  {
    closeOutboundAndChannel(ctx, promise, true);
  }
  
  public void close(ChannelHandlerContext ctx, ChannelPromise promise)
    throws Exception
  {
    closeOutboundAndChannel(ctx, promise, false);
  }
  
  public void read(ChannelHandlerContext ctx) throws Exception
  {
    if (!handshakePromise.isDone()) {
      setState(4);
    }
    
    ctx.read();
  }
  
  private static IllegalStateException newPendingWritesNullException() {
    return new IllegalStateException("pendingUnencryptedWrites is null, handlerRemoved0 called?");
  }
  
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception
  {
    if (!(msg instanceof ByteBuf)) {
      UnsupportedMessageTypeException exception = new UnsupportedMessageTypeException(msg, new Class[] { ByteBuf.class });
      ReferenceCountUtil.safeRelease(msg);
      promise.setFailure(exception);
    } else if (pendingUnencryptedWrites == null) {
      ReferenceCountUtil.safeRelease(msg);
      promise.setFailure(newPendingWritesNullException());
    } else {
      pendingUnencryptedWrites.add((ByteBuf)msg, promise);
    }
  }
  

  public void flush(ChannelHandlerContext ctx)
    throws Exception
  {
    if ((startTls) && (!isStateSet(1))) {
      setState(1);
      pendingUnencryptedWrites.writeAndRemoveAll(ctx);
      forceFlush(ctx);
      

      startHandshakeProcessing(true);
      return;
    }
    
    if (isStateSet(128)) {
      return;
    }
    try
    {
      wrapAndFlush(ctx);
    } catch (Throwable cause) {
      setHandshakeFailure(ctx, cause);
      PlatformDependent.throwException(cause);
    }
  }
  
  private void wrapAndFlush(ChannelHandlerContext ctx) throws SSLException {
    if (pendingUnencryptedWrites.isEmpty())
    {



      pendingUnencryptedWrites.add(Unpooled.EMPTY_BUFFER, ctx.newPromise());
    }
    if (!handshakePromise.isDone()) {
      setState(2);
    }
    try {
      wrap(ctx, false);
      


      forceFlush(ctx); } finally { forceFlush(ctx);
    }
  }
  
  private void wrap(ChannelHandlerContext ctx, boolean inUnwrap) throws SSLException
  {
    ByteBuf out = null;
    ByteBufAllocator alloc = ctx.alloc();
    try {
      int wrapDataSize = this.wrapDataSize;
      

      while (!ctx.isRemoved()) {
        ChannelPromise promise = ctx.newPromise();
        

        ByteBuf buf = wrapDataSize > 0 ? pendingUnencryptedWrites.remove(alloc, wrapDataSize, promise) : pendingUnencryptedWrites.removeFirst(promise);
        if (buf == null) {
          break;
        }
        
        if (out == null) {
          out = allocateOutNetBuf(ctx, buf.readableBytes(), buf.nioBufferCount());
        }
        
        SSLEngineResult result = wrap(alloc, engine, buf, out);
        if (buf.isReadable()) {
          pendingUnencryptedWrites.addFirst(buf, promise);
          

          promise = null;
        } else {
          buf.release();
        }
        


        if (out.isReadable()) {
          ByteBuf b = out;
          out = null;
          if (promise != null) {
            ctx.write(b, promise);
          } else {
            ctx.write(b);
          }
        } else if (promise != null) {
          ctx.write(Unpooled.EMPTY_BUFFER, promise);
        }
        

        if (result.getStatus() == SSLEngineResult.Status.CLOSED)
        {

          Throwable exception = handshakePromise.cause();
          if (exception == null) {
            exception = sslClosePromise.cause();
            if (exception == null) {
              exception = new SslClosedEngineException("SSLEngine closed already");
            }
          }
          pendingUnencryptedWrites.releaseAndFailAll(ctx, exception);
          return;
        }
        switch (11.$SwitchMap$javax$net$ssl$SSLEngineResult$HandshakeStatus[result.getHandshakeStatus().ordinal()]) {
        case 1: 
          if (runDelegatedTasks(inUnwrap)) {
            break;
          }
          break;
        

        case 2: 
        case 3: 
          setHandshakeSuccess();
          break;
        



        case 4: 
          if ((result.bytesProduced() > 0) && (pendingUnencryptedWrites.isEmpty())) {
            pendingUnencryptedWrites.add(Unpooled.EMPTY_BUFFER);
          }
          

          break;
        case 5: 
          readIfNeeded(ctx);
          return;
        
        default: 
          throw new IllegalStateException("Unknown handshake status: " + result.getHandshakeStatus());
        }
      }
    }
    finally {
      if (out != null) {
        out.release();
      }
      if (inUnwrap) {
        setState(16);
      }
    }
  }
  




  private boolean wrapNonAppData(final ChannelHandlerContext ctx, boolean inUnwrap)
    throws SSLException
  {
    ByteBuf out = null;
    ByteBufAllocator alloc = ctx.alloc();
    
    try
    {
      while (!ctx.isRemoved()) {
        if (out == null)
        {


          out = allocateOutNetBuf(ctx, 2048, 1);
        }
        SSLEngineResult result = wrap(alloc, engine, Unpooled.EMPTY_BUFFER, out);
        if (result.bytesProduced() > 0) {
          ctx.write(out).addListener(new ChannelFutureListener()
          {
            public void operationComplete(ChannelFuture future) {
              Throwable cause = future.cause();
              if (cause != null) {
                SslHandler.this.setHandshakeFailureTransportFailure(ctx, cause);
              }
            }
          });
          if (inUnwrap) {
            setState(16);
          }
          out = null;
        }
        
        SSLEngineResult.HandshakeStatus status = result.getHandshakeStatus();
        boolean bool; switch (11.$SwitchMap$javax$net$ssl$SSLEngineResult$HandshakeStatus[status.ordinal()])
        {



        case 2: 
          if ((setHandshakeSuccess()) && (inUnwrap) && (!pendingUnencryptedWrites.isEmpty())) {
            wrap(ctx, true);
          }
          return false;
        case 1: 
          if (runDelegatedTasks(inUnwrap)) {
            break;
          }
          break;
        

        case 5: 
          if ((inUnwrap) || (unwrapNonAppData(ctx) <= 0))
          {


            return false;
          }
          break;
        case 4: 
          break;
        case 3: 
          if ((setHandshakeSuccess()) && (inUnwrap) && (!pendingUnencryptedWrites.isEmpty())) {
            wrap(ctx, true);
          }
          

          if (!inUnwrap) {
            unwrapNonAppData(ctx);
          }
          return true;
        default: 
          throw new IllegalStateException("Unknown handshake status: " + result.getHandshakeStatus());
        }
        
        

        if ((result.bytesProduced() == 0) && (status != SSLEngineResult.HandshakeStatus.NEED_TASK)) {
          break;
        }
        


        if ((result.bytesConsumed() == 0) && (result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING)) {
          break;
        }
      }
    } finally {
      if (out != null) {
        out.release();
      }
    }
    return false;
  }
  
  /* Error */
  private SSLEngineResult wrap(ByteBufAllocator alloc, SSLEngine engine, ByteBuf in, ByteBuf out)
    throws SSLException
  {
    // Byte code:
    //   0: aconst_null
    //   1: astore 5
    //   3: aload_3
    //   4: invokevirtual 168	io/netty/buffer/ByteBuf:readerIndex	()I
    //   7: istore 6
    //   9: aload_3
    //   10: invokevirtual 133	io/netty/buffer/ByteBuf:readableBytes	()I
    //   13: istore 7
    //   15: aload_3
    //   16: invokevirtual 169	io/netty/buffer/ByteBuf:isDirect	()Z
    //   19: ifne +13 -> 32
    //   22: aload_0
    //   23: getfield 1	io/netty/handler/ssl/SslHandler:engineType	Lio/netty/handler/ssl/SslHandler$SslEngineType;
    //   26: getfield 170	io/netty/handler/ssl/SslHandler$SslEngineType:wantsDirectBuffer	Z
    //   29: ifne +48 -> 77
    //   32: aload_3
    //   33: instanceof 171
    //   36: ifne +32 -> 68
    //   39: aload_3
    //   40: invokevirtual 134	io/netty/buffer/ByteBuf:nioBufferCount	()I
    //   43: iconst_1
    //   44: if_icmpne +24 -> 68
    //   47: aload_0
    //   48: getfield 25	io/netty/handler/ssl/SslHandler:singleBuffer	[Ljava/nio/ByteBuffer;
    //   51: astore 8
    //   53: aload 8
    //   55: iconst_0
    //   56: aload_3
    //   57: iload 6
    //   59: iload 7
    //   61: invokevirtual 172	io/netty/buffer/ByteBuf:internalNioBuffer	(II)Ljava/nio/ByteBuffer;
    //   64: aastore
    //   65: goto +55 -> 120
    //   68: aload_3
    //   69: invokevirtual 173	io/netty/buffer/ByteBuf:nioBuffers	()[Ljava/nio/ByteBuffer;
    //   72: astore 8
    //   74: goto +46 -> 120
    //   77: aload_1
    //   78: iload 7
    //   80: invokeinterface 174 2 0
    //   85: astore 5
    //   87: aload 5
    //   89: aload_3
    //   90: iload 6
    //   92: iload 7
    //   94: invokevirtual 175	io/netty/buffer/ByteBuf:writeBytes	(Lio/netty/buffer/ByteBuf;II)Lio/netty/buffer/ByteBuf;
    //   97: pop
    //   98: aload_0
    //   99: getfield 25	io/netty/handler/ssl/SslHandler:singleBuffer	[Ljava/nio/ByteBuffer;
    //   102: astore 8
    //   104: aload 8
    //   106: iconst_0
    //   107: aload 5
    //   109: aload 5
    //   111: invokevirtual 168	io/netty/buffer/ByteBuf:readerIndex	()I
    //   114: iload 7
    //   116: invokevirtual 172	io/netty/buffer/ByteBuf:internalNioBuffer	(II)Ljava/nio/ByteBuffer;
    //   119: aastore
    //   120: aload 4
    //   122: aload 4
    //   124: invokevirtual 176	io/netty/buffer/ByteBuf:writerIndex	()I
    //   127: aload 4
    //   129: invokevirtual 177	io/netty/buffer/ByteBuf:writableBytes	()I
    //   132: invokevirtual 178	io/netty/buffer/ByteBuf:nioBuffer	(II)Ljava/nio/ByteBuffer;
    //   135: astore 9
    //   137: aload_2
    //   138: aload 8
    //   140: aload 9
    //   142: invokevirtual 179	javax/net/ssl/SSLEngine:wrap	([Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;)Ljavax/net/ssl/SSLEngineResult;
    //   145: astore 10
    //   147: aload_3
    //   148: aload 10
    //   150: invokevirtual 166	javax/net/ssl/SSLEngineResult:bytesConsumed	()I
    //   153: invokevirtual 180	io/netty/buffer/ByteBuf:skipBytes	(I)Lio/netty/buffer/ByteBuf;
    //   156: pop
    //   157: aload 4
    //   159: aload 4
    //   161: invokevirtual 176	io/netty/buffer/ByteBuf:writerIndex	()I
    //   164: aload 10
    //   166: invokevirtual 153	javax/net/ssl/SSLEngineResult:bytesProduced	()I
    //   169: iadd
    //   170: invokevirtual 181	io/netty/buffer/ByteBuf:writerIndex	(I)Lio/netty/buffer/ByteBuf;
    //   173: pop
    //   174: aload 10
    //   176: invokevirtual 142	javax/net/ssl/SSLEngineResult:getStatus	()Ljavax/net/ssl/SSLEngineResult$Status;
    //   179: getstatic 182	javax/net/ssl/SSLEngineResult$Status:BUFFER_OVERFLOW	Ljavax/net/ssl/SSLEngineResult$Status;
    //   182: if_acmpne +21 -> 203
    //   185: aload 4
    //   187: aload_2
    //   188: invokevirtual 183	javax/net/ssl/SSLEngine:getSession	()Ljavax/net/ssl/SSLSession;
    //   191: invokeinterface 184 1 0
    //   196: invokevirtual 185	io/netty/buffer/ByteBuf:ensureWritable	(I)Lio/netty/buffer/ByteBuf;
    //   199: pop
    //   200: goto +28 -> 228
    //   203: aload 10
    //   205: astore 11
    //   207: aload_0
    //   208: getfield 25	io/netty/handler/ssl/SslHandler:singleBuffer	[Ljava/nio/ByteBuffer;
    //   211: iconst_0
    //   212: aconst_null
    //   213: aastore
    //   214: aload 5
    //   216: ifnull +9 -> 225
    //   219: aload 5
    //   221: invokevirtual 139	io/netty/buffer/ByteBuf:release	()Z
    //   224: pop
    //   225: aload 11
    //   227: areturn
    //   228: goto -108 -> 120
    //   231: astore 12
    //   233: aload_0
    //   234: getfield 25	io/netty/handler/ssl/SslHandler:singleBuffer	[Ljava/nio/ByteBuffer;
    //   237: iconst_0
    //   238: aconst_null
    //   239: aastore
    //   240: aload 5
    //   242: ifnull +9 -> 251
    //   245: aload 5
    //   247: invokevirtual 139	io/netty/buffer/ByteBuf:release	()Z
    //   250: pop
    //   251: aload 12
    //   253: athrow
    // Line number table:
    //   Java source line #1008	-> byte code offset #0
    //   Java source line #1010	-> byte code offset #3
    //   Java source line #1011	-> byte code offset #9
    //   Java source line #1016	-> byte code offset #15
    //   Java source line #1021	-> byte code offset #32
    //   Java source line #1022	-> byte code offset #47
    //   Java source line #1025	-> byte code offset #53
    //   Java source line #1027	-> byte code offset #68
    //   Java source line #1033	-> byte code offset #77
    //   Java source line #1034	-> byte code offset #87
    //   Java source line #1035	-> byte code offset #98
    //   Java source line #1036	-> byte code offset #104
    //   Java source line #1040	-> byte code offset #120
    //   Java source line #1041	-> byte code offset #137
    //   Java source line #1042	-> byte code offset #147
    //   Java source line #1043	-> byte code offset #157
    //   Java source line #1045	-> byte code offset #174
    //   Java source line #1046	-> byte code offset #185
    //   Java source line #1048	-> byte code offset #203
    //   Java source line #1053	-> byte code offset #207
    //   Java source line #1055	-> byte code offset #214
    //   Java source line #1056	-> byte code offset #219
    //   Java source line #1048	-> byte code offset #225
    //   Java source line #1050	-> byte code offset #228
    //   Java source line #1053	-> byte code offset #231
    //   Java source line #1055	-> byte code offset #240
    //   Java source line #1056	-> byte code offset #245
    //   Java source line #1058	-> byte code offset #251
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	254	0	this	SslHandler
    //   0	254	1	alloc	ByteBufAllocator
    //   0	254	2	engine	SSLEngine
    //   0	254	3	in	ByteBuf
    //   0	254	4	out	ByteBuf
    //   1	245	5	newDirectIn	ByteBuf
    //   7	84	6	readerIndex	int
    //   13	102	7	readableBytes	int
    //   51	3	8	in0	ByteBuffer[]
    //   72	3	8	in0	ByteBuffer[]
    //   102	37	8	in0	ByteBuffer[]
    //   135	6	9	out0	ByteBuffer
    //   145	59	10	result	SSLEngineResult
    //   205	21	11	localSSLEngineResult1	SSLEngineResult
    //   231	21	12	localObject	Object
    // Exception table:
    //   from	to	target	type
    //   3	207	231	finally
    //   228	233	231	finally
  }
  
  public void channelInactive(ChannelHandlerContext ctx)
    throws Exception
  {
    boolean handshakeFailed = handshakePromise.cause() != null;
    
    ClosedChannelException exception = new ClosedChannelException();
    

    setHandshakeFailure(ctx, exception, !isStateSet(32), isStateSet(8), false);
    


    notifyClosePromise(exception);
    try
    {
      super.channelInactive(ctx);
    } catch (DecoderException e) {
      if ((!handshakeFailed) || (!(e.getCause() instanceof SSLException)))
      {




        throw e;
      }
    }
  }
  
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
  {
    if (ignoreException(cause))
    {

      if (logger.isDebugEnabled()) {
        logger.debug("{} Swallowing a harmless 'connection reset by peer / broken pipe' error that occurred while writing close_notify in response to the peer's close_notify", ctx
        
          .channel(), cause);
      }
      


      if (ctx.channel().isActive()) {
        ctx.close();
      }
    } else {
      ctx.fireExceptionCaught(cause);
    }
  }
  








  private boolean ignoreException(Throwable t)
  {
    if ((!(t instanceof SSLException)) && ((t instanceof IOException)) && (sslClosePromise.isDone())) {
      String message = t.getMessage();
      


      if ((message != null) && (IGNORABLE_ERROR_MESSAGE.matcher(message).matches())) {
        return true;
      }
      

      StackTraceElement[] elements = t.getStackTrace();
      for (StackTraceElement element : elements) {
        String classname = element.getClassName();
        String methodname = element.getMethodName();
        

        if (!classname.startsWith("io.netty."))
        {



          if ("read".equals(methodname))
          {




            if (IGNORABLE_CLASS_IN_STACK.matcher(classname).matches()) {
              return true;
            }
            


            try
            {
              Class<?> clazz = PlatformDependent.getClassLoader(getClass()).loadClass(classname);
              
              if ((SocketChannel.class.isAssignableFrom(clazz)) || 
                (DatagramChannel.class.isAssignableFrom(clazz))) {
                return true;
              }
              

              if ((PlatformDependent.javaVersion() >= 7) && 
                ("com.sun.nio.sctp.SctpChannel".equals(clazz.getSuperclass().getName()))) {
                return true;
              }
            } catch (Throwable cause) {
              if (logger.isDebugEnabled())
                logger.debug("Unexpected exception while loading class {} classname {}", new Object[] {
                  getClass(), classname, cause });
            }
          }
        }
      }
    }
    return false;
  }
  











  public static boolean isEncrypted(ByteBuf buffer)
  {
    if (buffer.readableBytes() < 5) {
      throw new IllegalArgumentException("buffer must have at least 5 readable bytes");
    }
    
    return SslUtils.getEncryptedPacketLength(buffer, buffer.readerIndex()) != -2;
  }
  
  private void decodeJdkCompatible(ChannelHandlerContext ctx, ByteBuf in) throws NotSslRecordException {
    int packetLength = this.packetLength;
    
    if (packetLength > 0) {
      if (in.readableBytes() >= packetLength) {}

    }
    else
    {
      int readableBytes = in.readableBytes();
      if (readableBytes < 5) {
        return;
      }
      packetLength = SslUtils.getEncryptedPacketLength(in, in.readerIndex());
      if (packetLength == -2)
      {

        NotSslRecordException e = new NotSslRecordException("not an SSL/TLS record: " + ByteBufUtil.hexDump(in));
        in.skipBytes(in.readableBytes());
        


        setHandshakeFailure(ctx, e);
        
        throw e;
      }
      assert (packetLength > 0);
      if (packetLength > readableBytes)
      {
        this.packetLength = packetLength;
        return;
      }
    }
    


    this.packetLength = 0;
    try {
      int bytesConsumed = unwrap(ctx, in, packetLength);
      if ((!$assertionsDisabled) && (bytesConsumed != packetLength) && (!engine.isInboundDone())) throw new AssertionError("we feed the SSLEngine a packets worth of data: " + packetLength + " but it only consumed: " + bytesConsumed);
    }
    catch (Throwable cause)
    {
      handleUnwrapThrowable(ctx, cause);
    }
  }
  
  private void decodeNonJdkCompatible(ChannelHandlerContext ctx, ByteBuf in) {
    try {
      unwrap(ctx, in, in.readableBytes());
    } catch (Throwable cause) {
      handleUnwrapThrowable(ctx, cause);
    }
  }
  


  private void handleUnwrapThrowable(ChannelHandlerContext ctx, Throwable cause)
  {
    try
    {
      if (handshakePromise.tryFailure(cause)) {
        ctx.fireUserEventTriggered(new SslHandshakeCompletionEvent(cause));
      }
      

      if (pendingUnencryptedWrites != null)
      {

        wrapAndFlush(ctx);
      }
    } catch (SSLException ex) {
      logger.debug("SSLException during trying to call SSLEngine.wrap(...) because of an previous SSLException, ignoring...", ex);
    }
    finally
    {
      setHandshakeFailure(ctx, cause, true, false, true);
    }
    PlatformDependent.throwException(cause);
  }
  
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws SSLException
  {
    if (isStateSet(128)) {
      return;
    }
    if (jdkCompatibilityMode) {
      decodeJdkCompatible(ctx, in);
    } else {
      decodeNonJdkCompatible(ctx, in);
    }
  }
  
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception
  {
    channelReadComplete0(ctx);
  }
  
  private void channelReadComplete0(ChannelHandlerContext ctx)
  {
    discardSomeReadBytes();
    
    flushIfNeeded(ctx);
    readIfNeeded(ctx);
    
    clearState(256);
    ctx.fireChannelReadComplete();
  }
  
  private void readIfNeeded(ChannelHandlerContext ctx)
  {
    if ((!ctx.channel().config().isAutoRead()) && (
      (!isStateSet(256)) || (!handshakePromise.isDone())))
    {

      ctx.read();
    }
  }
  
  private void flushIfNeeded(ChannelHandlerContext ctx) {
    if (isStateSet(16)) {
      forceFlush(ctx);
    }
  }
  

  private int unwrapNonAppData(ChannelHandlerContext ctx)
    throws SSLException
  {
    return unwrap(ctx, Unpooled.EMPTY_BUFFER, 0);
  }
  
  /* Error */
  private int unwrap(ChannelHandlerContext ctx, ByteBuf packet, int length)
    throws SSLException
  {
    // Byte code:
    //   0: iload_3
    //   1: istore 4
    //   3: iconst_0
    //   4: istore 5
    //   6: iconst_0
    //   7: istore 6
    //   9: iconst_0
    //   10: istore 7
    //   12: aload_0
    //   13: aload_1
    //   14: iload_3
    //   15: invokespecial 254	io/netty/handler/ssl/SslHandler:allocate	(Lio/netty/channel/ChannelHandlerContext;I)Lio/netty/buffer/ByteBuf;
    //   18: astore 8
    //   20: aload_0
    //   21: getfield 1	io/netty/handler/ssl/SslHandler:engineType	Lio/netty/handler/ssl/SslHandler$SslEngineType;
    //   24: aload_0
    //   25: aload_2
    //   26: iload_3
    //   27: aload 8
    //   29: invokevirtual 255	io/netty/handler/ssl/SslHandler$SslEngineType:unwrap	(Lio/netty/handler/ssl/SslHandler;Lio/netty/buffer/ByteBuf;ILio/netty/buffer/ByteBuf;)Ljavax/net/ssl/SSLEngineResult;
    //   32: astore 9
    //   34: aload 9
    //   36: invokevirtual 142	javax/net/ssl/SSLEngineResult:getStatus	()Ljavax/net/ssl/SSLEngineResult$Status;
    //   39: astore 10
    //   41: aload 9
    //   43: invokevirtual 150	javax/net/ssl/SSLEngineResult:getHandshakeStatus	()Ljavax/net/ssl/SSLEngineResult$HandshakeStatus;
    //   46: astore 11
    //   48: aload 9
    //   50: invokevirtual 153	javax/net/ssl/SSLEngineResult:bytesProduced	()I
    //   53: istore 12
    //   55: aload 9
    //   57: invokevirtual 166	javax/net/ssl/SSLEngineResult:bytesConsumed	()I
    //   60: istore 13
    //   62: aload_2
    //   63: iload 13
    //   65: invokevirtual 180	io/netty/buffer/ByteBuf:skipBytes	(I)Lio/netty/buffer/ByteBuf;
    //   68: pop
    //   69: iload_3
    //   70: iload 13
    //   72: isub
    //   73: istore_3
    //   74: aload 11
    //   76: getstatic 256	javax/net/ssl/SSLEngineResult$HandshakeStatus:FINISHED	Ljavax/net/ssl/SSLEngineResult$HandshakeStatus;
    //   79: if_acmpeq +11 -> 90
    //   82: aload 11
    //   84: getstatic 167	javax/net/ssl/SSLEngineResult$HandshakeStatus:NOT_HANDSHAKING	Ljavax/net/ssl/SSLEngineResult$HandshakeStatus;
    //   87: if_acmpne +46 -> 133
    //   90: iload 5
    //   92: aload 8
    //   94: invokevirtual 137	io/netty/buffer/ByteBuf:isReadable	()Z
    //   97: ifeq +13 -> 110
    //   100: aload_0
    //   101: invokespecial 257	io/netty/handler/ssl/SslHandler:setHandshakeSuccessUnwrapMarkReentry	()Z
    //   104: ifeq +13 -> 117
    //   107: goto +18 -> 125
    //   110: aload_0
    //   111: invokespecial 12	io/netty/handler/ssl/SslHandler:setHandshakeSuccess	()Z
    //   114: ifne +11 -> 125
    //   117: aload 11
    //   119: getstatic 256	javax/net/ssl/SSLEngineResult$HandshakeStatus:FINISHED	Ljavax/net/ssl/SSLEngineResult$HandshakeStatus;
    //   122: if_acmpne +7 -> 129
    //   125: iconst_1
    //   126: goto +4 -> 130
    //   129: iconst_0
    //   130: ior
    //   131: istore 5
    //   133: aload 8
    //   135: invokevirtual 137	io/netty/buffer/ByteBuf:isReadable	()Z
    //   138: ifeq +45 -> 183
    //   141: aload_0
    //   142: sipush 256
    //   145: invokespecial 81	io/netty/handler/ssl/SslHandler:setState	(I)V
    //   148: aload_0
    //   149: sipush 512
    //   152: invokespecial 122	io/netty/handler/ssl/SslHandler:isStateSet	(I)Z
    //   155: ifeq +16 -> 171
    //   158: iconst_1
    //   159: istore 7
    //   161: aload_0
    //   162: aload_1
    //   163: aload 8
    //   165: invokespecial 258	io/netty/handler/ssl/SslHandler:executeChannelRead	(Lio/netty/channel/ChannelHandlerContext;Lio/netty/buffer/ByteBuf;)V
    //   168: goto +12 -> 180
    //   171: aload_1
    //   172: aload 8
    //   174: invokeinterface 259 2 0
    //   179: pop
    //   180: aconst_null
    //   181: astore 8
    //   183: aload 10
    //   185: getstatic 143	javax/net/ssl/SSLEngineResult$Status:CLOSED	Ljavax/net/ssl/SSLEngineResult$Status;
    //   188: if_acmpne +9 -> 197
    //   191: iconst_1
    //   192: istore 6
    //   194: goto +71 -> 265
    //   197: aload 10
    //   199: getstatic 182	javax/net/ssl/SSLEngineResult$Status:BUFFER_OVERFLOW	Ljavax/net/ssl/SSLEngineResult$Status;
    //   202: if_acmpne +63 -> 265
    //   205: aload 8
    //   207: ifnull +9 -> 216
    //   210: aload 8
    //   212: invokevirtual 139	io/netty/buffer/ByteBuf:release	()Z
    //   215: pop
    //   216: aload_0
    //   217: getfield 26	io/netty/handler/ssl/SslHandler:engine	Ljavax/net/ssl/SSLEngine;
    //   220: invokevirtual 183	javax/net/ssl/SSLEngine:getSession	()Ljavax/net/ssl/SSLSession;
    //   223: invokeinterface 260 1 0
    //   228: istore 14
    //   230: aload_0
    //   231: aload_1
    //   232: aload_0
    //   233: getfield 1	io/netty/handler/ssl/SslHandler:engineType	Lio/netty/handler/ssl/SslHandler$SslEngineType;
    //   236: aload_0
    //   237: iload 14
    //   239: iload 12
    //   241: if_icmpge +8 -> 249
    //   244: iload 14
    //   246: goto +8 -> 254
    //   249: iload 14
    //   251: iload 12
    //   253: isub
    //   254: invokevirtual 261	io/netty/handler/ssl/SslHandler$SslEngineType:calculatePendingData	(Lio/netty/handler/ssl/SslHandler;I)I
    //   257: invokespecial 254	io/netty/handler/ssl/SslHandler:allocate	(Lio/netty/channel/ChannelHandlerContext;I)Lio/netty/buffer/ByteBuf;
    //   260: astore 8
    //   262: goto +123 -> 385
    //   265: aload 11
    //   267: getstatic 165	javax/net/ssl/SSLEngineResult$HandshakeStatus:NEED_TASK	Ljavax/net/ssl/SSLEngineResult$HandshakeStatus;
    //   270: if_acmpne +24 -> 294
    //   273: aload_0
    //   274: iconst_1
    //   275: invokespecial 152	io/netty/handler/ssl/SslHandler:runDelegatedTasks	(Z)Z
    //   278: istore 14
    //   280: iload 14
    //   282: ifne +9 -> 291
    //   285: iconst_0
    //   286: istore 5
    //   288: goto +106 -> 394
    //   291: goto +27 -> 318
    //   294: aload 11
    //   296: getstatic 262	javax/net/ssl/SSLEngineResult$HandshakeStatus:NEED_WRAP	Ljavax/net/ssl/SSLEngineResult$HandshakeStatus;
    //   299: if_acmpne +19 -> 318
    //   302: aload_0
    //   303: aload_1
    //   304: iconst_1
    //   305: invokespecial 9	io/netty/handler/ssl/SslHandler:wrapNonAppData	(Lio/netty/channel/ChannelHandlerContext;Z)Z
    //   308: ifeq +10 -> 318
    //   311: iload_3
    //   312: ifne +6 -> 318
    //   315: goto +79 -> 394
    //   318: aload 10
    //   320: getstatic 263	javax/net/ssl/SSLEngineResult$Status:BUFFER_UNDERFLOW	Ljavax/net/ssl/SSLEngineResult$Status;
    //   323: if_acmpeq +33 -> 356
    //   326: aload 11
    //   328: getstatic 165	javax/net/ssl/SSLEngineResult$HandshakeStatus:NEED_TASK	Ljavax/net/ssl/SSLEngineResult$HandshakeStatus;
    //   331: if_acmpeq +41 -> 372
    //   334: iload 13
    //   336: ifne +8 -> 344
    //   339: iload 12
    //   341: ifeq +15 -> 356
    //   344: iload_3
    //   345: ifne +27 -> 372
    //   348: aload 11
    //   350: getstatic 167	javax/net/ssl/SSLEngineResult$HandshakeStatus:NOT_HANDSHAKING	Ljavax/net/ssl/SSLEngineResult$HandshakeStatus;
    //   353: if_acmpne +19 -> 372
    //   356: aload 11
    //   358: getstatic 264	javax/net/ssl/SSLEngineResult$HandshakeStatus:NEED_UNWRAP	Ljavax/net/ssl/SSLEngineResult$HandshakeStatus;
    //   361: if_acmpne +33 -> 394
    //   364: aload_0
    //   365: aload_1
    //   366: invokespecial 155	io/netty/handler/ssl/SslHandler:readIfNeeded	(Lio/netty/channel/ChannelHandlerContext;)V
    //   369: goto +25 -> 394
    //   372: aload 8
    //   374: ifnonnull +11 -> 385
    //   377: aload_0
    //   378: aload_1
    //   379: iload_3
    //   380: invokespecial 254	io/netty/handler/ssl/SslHandler:allocate	(Lio/netty/channel/ChannelHandlerContext;I)Lio/netty/buffer/ByteBuf;
    //   383: astore 8
    //   385: aload_1
    //   386: invokeinterface 130 1 0
    //   391: ifeq -371 -> 20
    //   394: aload_0
    //   395: iconst_2
    //   396: invokespecial 122	io/netty/handler/ssl/SslHandler:isStateSet	(I)Z
    //   399: ifeq +23 -> 422
    //   402: aload_0
    //   403: getfield 38	io/netty/handler/ssl/SslHandler:handshakePromise	Lio/netty/util/concurrent/Promise;
    //   406: invokeinterface 95 1 0
    //   411: ifeq +11 -> 422
    //   414: aload_0
    //   415: iconst_2
    //   416: invokespecial 14	io/netty/handler/ssl/SslHandler:clearState	(I)V
    //   419: iconst_1
    //   420: istore 5
    //   422: iload 5
    //   424: ifeq +9 -> 433
    //   427: aload_0
    //   428: aload_1
    //   429: iconst_1
    //   430: invokespecial 11	io/netty/handler/ssl/SslHandler:wrap	(Lio/netty/channel/ChannelHandlerContext;Z)V
    //   433: aload 8
    //   435: ifnull +9 -> 444
    //   438: aload 8
    //   440: invokevirtual 139	io/netty/buffer/ByteBuf:release	()Z
    //   443: pop
    //   444: iload 6
    //   446: ifeq +63 -> 509
    //   449: iload 7
    //   451: ifeq +11 -> 462
    //   454: aload_0
    //   455: aload_1
    //   456: invokespecial 265	io/netty/handler/ssl/SslHandler:executeNotifyClosePromise	(Lio/netty/channel/ChannelHandlerContext;)V
    //   459: goto +50 -> 509
    //   462: aload_0
    //   463: aconst_null
    //   464: invokespecial 21	io/netty/handler/ssl/SslHandler:notifyClosePromise	(Ljava/lang/Throwable;)V
    //   467: goto +42 -> 509
    //   470: astore 15
    //   472: aload 8
    //   474: ifnull +9 -> 483
    //   477: aload 8
    //   479: invokevirtual 139	io/netty/buffer/ByteBuf:release	()Z
    //   482: pop
    //   483: iload 6
    //   485: ifeq +21 -> 506
    //   488: iload 7
    //   490: ifeq +11 -> 501
    //   493: aload_0
    //   494: aload_1
    //   495: invokespecial 265	io/netty/handler/ssl/SslHandler:executeNotifyClosePromise	(Lio/netty/channel/ChannelHandlerContext;)V
    //   498: goto +8 -> 506
    //   501: aload_0
    //   502: aconst_null
    //   503: invokespecial 21	io/netty/handler/ssl/SslHandler:notifyClosePromise	(Ljava/lang/Throwable;)V
    //   506: aload 15
    //   508: athrow
    //   509: iload 4
    //   511: iload_3
    //   512: isub
    //   513: ireturn
    // Line number table:
    //   Java source line #1334	-> byte code offset #0
    //   Java source line #1335	-> byte code offset #3
    //   Java source line #1336	-> byte code offset #6
    //   Java source line #1337	-> byte code offset #9
    //   Java source line #1338	-> byte code offset #12
    //   Java source line #1343	-> byte code offset #20
    //   Java source line #1344	-> byte code offset #34
    //   Java source line #1345	-> byte code offset #41
    //   Java source line #1346	-> byte code offset #48
    //   Java source line #1347	-> byte code offset #55
    //   Java source line #1352	-> byte code offset #62
    //   Java source line #1353	-> byte code offset #69
    //   Java source line #1358	-> byte code offset #74
    //   Java source line #1359	-> byte code offset #90
    //   Java source line #1360	-> byte code offset #101
    //   Java source line #1367	-> byte code offset #133
    //   Java source line #1368	-> byte code offset #141
    //   Java source line #1369	-> byte code offset #148
    //   Java source line #1370	-> byte code offset #158
    //   Java source line #1371	-> byte code offset #161
    //   Java source line #1373	-> byte code offset #171
    //   Java source line #1375	-> byte code offset #180
    //   Java source line #1378	-> byte code offset #183
    //   Java source line #1379	-> byte code offset #191
    //   Java source line #1380	-> byte code offset #197
    //   Java source line #1381	-> byte code offset #205
    //   Java source line #1382	-> byte code offset #210
    //   Java source line #1384	-> byte code offset #216
    //   Java source line #1389	-> byte code offset #230
    //   Java source line #1391	-> byte code offset #262
    //   Java source line #1394	-> byte code offset #265
    //   Java source line #1395	-> byte code offset #273
    //   Java source line #1396	-> byte code offset #280
    //   Java source line #1402	-> byte code offset #285
    //   Java source line #1403	-> byte code offset #288
    //   Java source line #1405	-> byte code offset #291
    //   Java source line #1409	-> byte code offset #302
    //   Java source line #1410	-> byte code offset #315
    //   Java source line #1414	-> byte code offset #318
    //   Java source line #1418	-> byte code offset #356
    //   Java source line #1421	-> byte code offset #364
    //   Java source line #1425	-> byte code offset #372
    //   Java source line #1426	-> byte code offset #377
    //   Java source line #1428	-> byte code offset #385
    //   Java source line #1430	-> byte code offset #394
    //   Java source line #1435	-> byte code offset #414
    //   Java source line #1436	-> byte code offset #419
    //   Java source line #1439	-> byte code offset #422
    //   Java source line #1440	-> byte code offset #427
    //   Java source line #1443	-> byte code offset #433
    //   Java source line #1444	-> byte code offset #438
    //   Java source line #1447	-> byte code offset #444
    //   Java source line #1448	-> byte code offset #449
    //   Java source line #1449	-> byte code offset #454
    //   Java source line #1451	-> byte code offset #462
    //   Java source line #1443	-> byte code offset #470
    //   Java source line #1444	-> byte code offset #477
    //   Java source line #1447	-> byte code offset #483
    //   Java source line #1448	-> byte code offset #488
    //   Java source line #1449	-> byte code offset #493
    //   Java source line #1451	-> byte code offset #501
    //   Java source line #1454	-> byte code offset #506
    //   Java source line #1455	-> byte code offset #509
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	514	0	this	SslHandler
    //   0	514	1	ctx	ChannelHandlerContext
    //   0	514	2	packet	ByteBuf
    //   0	514	3	length	int
    //   3	511	4	originalLength	int
    //   6	508	5	wrapLater	boolean
    //   9	505	6	notifyClosure	boolean
    //   12	502	7	executedRead	boolean
    //   20	494	8	decodeOut	ByteBuf
    //   34	351	9	result	SSLEngineResult
    //   41	344	10	status	SSLEngineResult.Status
    //   48	337	11	handshakeStatus	SSLEngineResult.HandshakeStatus
    //   55	330	12	produced	int
    //   62	323	13	consumed	int
    //   230	35	14	applicationBufferSize	int
    //   280	11	14	pending	boolean
    // Exception table:
    //   from	to	target	type
    //   20	433	470	finally
    //   470	472	470	finally
  }
  
  private boolean setHandshakeSuccessUnwrapMarkReentry()
  {
    boolean setReentryState = !isStateSet(512);
    if (setReentryState) {
      setState(512);
    }
    try {
      return setHandshakeSuccess();
    }
    finally
    {
      if (setReentryState) {
        clearState(512);
      }
    }
  }
  
  private void executeNotifyClosePromise(ChannelHandlerContext ctx) {
    try {
      ctx.executor().execute(new Runnable()
      {
        public void run() {
          SslHandler.this.notifyClosePromise(null);
        }
      });
    } catch (RejectedExecutionException e) {
      notifyClosePromise(e);
    }
  }
  
  private void executeChannelRead(final ChannelHandlerContext ctx, final ByteBuf decodedOut) {
    try {
      ctx.executor().execute(new Runnable()
      {
        public void run() {
          ctx.fireChannelRead(decodedOut);
        }
      });
    } catch (RejectedExecutionException e) {
      decodedOut.release();
      throw e;
    }
  }
  
  private static ByteBuffer toByteBuffer(ByteBuf out, int index, int len) {
    return out.nioBufferCount() == 1 ? out.internalNioBuffer(index, len) : out
      .nioBuffer(index, len);
  }
  
  private static boolean inEventLoop(Executor executor) {
    return ((executor instanceof EventExecutor)) && (((EventExecutor)executor).inEventLoop());
  }
  






  private boolean runDelegatedTasks(boolean inUnwrap)
  {
    if ((delegatedTaskExecutor == ImmediateExecutor.INSTANCE) || (inEventLoop(delegatedTaskExecutor)))
    {
      for (;;)
      {
        Runnable task = engine.getDelegatedTask();
        if (task == null) {
          return true;
        }
        setState(128);
        if ((task instanceof AsyncRunnable))
        {
          boolean pending = false;
          try {
            AsyncRunnable asyncTask = (AsyncRunnable)task;
            AsyncTaskCompletionHandler completionHandler = new AsyncTaskCompletionHandler(inUnwrap);
            asyncTask.run(completionHandler);
            pending = completionHandler.resumeLater();
            if (pending) {
              return false;
            }
          } finally {
            if (!pending)
            {

              clearState(128);
            }
          }
        } else {
          try {
            task.run();
          } finally {
            clearState(128);
          }
        }
      }
    }
    executeDelegatedTask(inUnwrap);
    return false;
  }
  
  private SslTasksRunner getTaskRunner(boolean inUnwrap)
  {
    return inUnwrap ? sslTaskRunnerForUnwrap : sslTaskRunner;
  }
  
  private void executeDelegatedTask(boolean inUnwrap) {
    executeDelegatedTask(getTaskRunner(inUnwrap));
  }
  
  private void executeDelegatedTask(SslTasksRunner task) {
    setState(128);
    try {
      delegatedTaskExecutor.execute(task);
    } catch (RejectedExecutionException e) {
      clearState(128);
      throw e;
    }
  }
  
  private final class AsyncTaskCompletionHandler implements Runnable {
    private final boolean inUnwrap;
    boolean didRun;
    boolean resumeLater;
    
    AsyncTaskCompletionHandler(boolean inUnwrap) {
      this.inUnwrap = inUnwrap;
    }
    
    public void run()
    {
      didRun = true;
      if (resumeLater) {
        SslHandler.this.getTaskRunner(inUnwrap).runComplete();
      }
    }
    
    boolean resumeLater() {
      if (!didRun) {
        resumeLater = true;
        return true;
      }
      return false;
    }
  }
  

  private final class SslTasksRunner
    implements Runnable
  {
    private final boolean inUnwrap;
    
    private final Runnable runCompleteTask = new Runnable()
    {
      public void run() {
        runComplete();
      }
    };
    
    SslTasksRunner(boolean inUnwrap) {
      this.inUnwrap = inUnwrap;
    }
    
    private void taskError(Throwable e)
    {
      if (inUnwrap)
      {

        try
        {

          SslHandler.this.handleUnwrapThrowable(ctx, e);
        } catch (Throwable cause) {
          safeExceptionCaught(cause);
        }
      } else {
        SslHandler.this.setHandshakeFailure(ctx, e);
        SslHandler.this.forceFlush(ctx);
      }
    }
    
    private void safeExceptionCaught(Throwable cause)
    {
      try {
        exceptionCaught(ctx, wrapIfNeeded(cause));
      } catch (Throwable error) {
        ctx.fireExceptionCaught(error);
      }
    }
    
    private Throwable wrapIfNeeded(Throwable cause) {
      if (!inUnwrap)
      {
        return cause;
      }
      

      return (cause instanceof DecoderException) ? cause : new DecoderException(cause);
    }
    
    private void tryDecodeAgain() {
      try {
        channelRead(ctx, Unpooled.EMPTY_BUFFER);
      } catch (Throwable cause) {
        safeExceptionCaught(cause);

      }
      finally
      {
        SslHandler.this.channelReadComplete0(ctx);
      }
    }
    



    private void resumeOnEventExecutor()
    {
      assert (ctx.executor().inEventLoop());
      SslHandler.this.clearState(128);
      try {
        SSLEngineResult.HandshakeStatus status = engine.getHandshakeStatus();
        switch (SslHandler.11.$SwitchMap$javax$net$ssl$SSLEngineResult$HandshakeStatus[status.ordinal()])
        {

        case 1: 
          SslHandler.this.executeDelegatedTask(this);
          
          break;
        


        case 2: 
        case 3: 
          SslHandler.this.setHandshakeSuccess();
          
          try
          {
            SslHandler.this.wrap(ctx, inUnwrap);
          } catch (Throwable e) {
            taskError(e);
            return;
          }
          if (inUnwrap)
          {

            SslHandler.this.unwrapNonAppData(ctx);
          }
          

          SslHandler.this.forceFlush(ctx);
          
          tryDecodeAgain();
          break;
        

        case 5: 
          try
          {
            SslHandler.this.unwrapNonAppData(ctx);
          } catch (SSLException e) {
            SslHandler.this.handleUnwrapThrowable(ctx, e);
            return;
          }
          tryDecodeAgain();
          break;
        

        case 4: 
          try
          {
            if ((!SslHandler.this.wrapNonAppData(ctx, false)) && (inUnwrap))
            {



              SslHandler.this.unwrapNonAppData(ctx);
            }
            

            SslHandler.this.forceFlush(ctx);
          } catch (Throwable e) {
            taskError(e);
            return;
          }
          

          tryDecodeAgain();
          break;
        

        default: 
          throw new AssertionError(); }
        
      } catch (Throwable cause) {
        safeExceptionCaught(cause);
      }
    }
    
    void runComplete() {
      EventExecutor executor = ctx.executor();
      if (executor.inEventLoop()) {
        resumeOnEventExecutor();
      }
      else {
        executor.execute(new Runnable()
        {
          public void run() {
            SslHandler.SslTasksRunner.this.resumeOnEventExecutor();
          }
        });
      }
    }
    
    public void run()
    {
      try {
        Runnable task = engine.getDelegatedTask();
        if (task == null)
        {
          return;
        }
        if ((task instanceof AsyncRunnable)) {
          AsyncRunnable asyncTask = (AsyncRunnable)task;
          asyncTask.run(runCompleteTask);
        } else {
          task.run();
          runComplete();
        }
      } catch (Throwable cause) {
        handleException(cause);
      }
    }
    
    private void handleException(final Throwable cause) {
      EventExecutor executor = ctx.executor();
      if (executor.inEventLoop()) {
        SslHandler.this.clearState(128);
        safeExceptionCaught(cause);
      } else {
        try {
          executor.execute(new Runnable()
          {
            public void run() {
              SslHandler.this.clearState(128);
              SslHandler.SslTasksRunner.this.safeExceptionCaught(cause);
            }
          });
        } catch (RejectedExecutionException ignore) {
          SslHandler.this.clearState(128);
          

          ctx.fireExceptionCaught(cause);
        }
      }
    }
  }
  




  private boolean setHandshakeSuccess()
  {
    boolean notified;
    


    if ((notified = (!handshakePromise.isDone()) && (handshakePromise.trySuccess(ctx.channel())) ? 1 : 0) != 0) {
      if (logger.isDebugEnabled()) {
        SSLSession session = engine.getSession();
        logger.debug("{} HANDSHAKEN: protocol:{} cipher suite:{}", new Object[] {ctx
        
          .channel(), session
          .getProtocol(), session
          .getCipherSuite() });
      }
      ctx.fireUserEventTriggered(SslHandshakeCompletionEvent.SUCCESS);
    }
    if (isStateSet(4)) {
      clearState(4);
      if (!ctx.channel().config().isAutoRead()) {
        ctx.read();
      }
    }
    return notified;
  }
  


  private void setHandshakeFailure(ChannelHandlerContext ctx, Throwable cause)
  {
    setHandshakeFailure(ctx, cause, true, true, false);
  }
  



  private void setHandshakeFailure(ChannelHandlerContext ctx, Throwable cause, boolean closeInbound, boolean notify, boolean alwaysFlushAndClose)
  {
    try
    {
      setState(32);
      engine.closeOutbound();
      
      if (closeInbound) {
        try {
          engine.closeInbound();
        } catch (SSLException e) {
          if (logger.isDebugEnabled())
          {



            String msg = e.getMessage();
            if ((msg == null) || ((!msg.contains("possible truncation attack")) && 
              (!msg.contains("closing inbound before receiving peer's close_notify")))) {
              logger.debug("{} SSLEngine.closeInbound() raised an exception.", ctx.channel(), e);
            }
          }
        }
      }
      if ((handshakePromise.tryFailure(cause)) || (alwaysFlushAndClose)) {
        SslUtils.handleHandshakeFailure(ctx, cause, notify);
      }
    }
    finally {
      releaseAndFailAll(ctx, cause);
    }
  }
  

  private void setHandshakeFailureTransportFailure(ChannelHandlerContext ctx, Throwable cause)
  {
    try
    {
      SSLException transportFailure = new SSLException("failure when writing TLS control frames", cause);
      releaseAndFailAll(ctx, transportFailure);
      if (handshakePromise.tryFailure(transportFailure)) {
        ctx.fireUserEventTriggered(new SslHandshakeCompletionEvent(transportFailure));
      }
    } finally {
      ctx.close();
    }
  }
  
  private void releaseAndFailAll(ChannelHandlerContext ctx, Throwable cause) {
    if (pendingUnencryptedWrites != null) {
      pendingUnencryptedWrites.releaseAndFailAll(ctx, cause);
    }
  }
  
  private void notifyClosePromise(Throwable cause) {
    if (cause == null) {
      if (sslClosePromise.trySuccess(ctx.channel())) {
        ctx.fireUserEventTriggered(SslCloseCompletionEvent.SUCCESS);
      }
    }
    else if (sslClosePromise.tryFailure(cause)) {
      ctx.fireUserEventTriggered(new SslCloseCompletionEvent(cause));
    }
  }
  
  private void closeOutboundAndChannel(ChannelHandlerContext ctx, final ChannelPromise promise, boolean disconnect)
    throws Exception
  {
    setState(32);
    engine.closeOutbound();
    
    if (!ctx.channel().isActive()) {
      if (disconnect) {
        ctx.disconnect(promise);
      } else {
        ctx.close(promise);
      }
      return;
    }
    
    ChannelPromise closeNotifyPromise = ctx.newPromise();
    try {
      flush(ctx, closeNotifyPromise);
    } finally {
      if (!isStateSet(64)) {
        setState(64);
        







        safeClose(ctx, closeNotifyPromise, (ChannelPromise)PromiseNotifier.cascade(false, ctx.newPromise(), promise));
      }
      else {
        sslClosePromise.addListener(new FutureListener()
        {
          public void operationComplete(Future<Channel> future) {
            promise.setSuccess();
          }
        });
      }
    }
  }
  
  private void flush(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
    if (pendingUnencryptedWrites != null) {
      pendingUnencryptedWrites.add(Unpooled.EMPTY_BUFFER, promise);
    } else {
      promise.setFailure(newPendingWritesNullException());
    }
    flush(ctx);
  }
  
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception
  {
    this.ctx = ctx;
    
    Channel channel = ctx.channel();
    pendingUnencryptedWrites = new SslHandlerCoalescingBufferQueue(channel, 16);
    boolean fastOpen = Boolean.TRUE.equals(channel.config().getOption(ChannelOption.TCP_FASTOPEN_CONNECT));
    boolean active = channel.isActive();
    if ((active) || (fastOpen))
    {


      startHandshakeProcessing(active);
      
      ChannelOutboundBuffer outboundBuffer;
      
      if ((fastOpen) && (((outboundBuffer = channel.unsafe().outboundBuffer()) == null) || 
        (outboundBuffer.totalPendingWriteBytes() > 0L))) {
        setState(16);
      }
    }
  }
  
  private void startHandshakeProcessing(boolean flushAtEnd) {
    if (!isStateSet(8)) {
      setState(8);
      if (engine.getUseClientMode())
      {


        handshake(flushAtEnd);
      }
      applyHandshakeTimeout();
    } else if (isStateSet(16)) {
      forceFlush(ctx);
    }
  }
  


  public Future<Channel> renegotiate()
  {
    ChannelHandlerContext ctx = this.ctx;
    if (ctx == null) {
      throw new IllegalStateException();
    }
    
    return renegotiate(ctx.executor().newPromise());
  }
  


  public Future<Channel> renegotiate(final Promise<Channel> promise)
  {
    ObjectUtil.checkNotNull(promise, "promise");
    
    ChannelHandlerContext ctx = this.ctx;
    if (ctx == null) {
      throw new IllegalStateException();
    }
    
    EventExecutor executor = ctx.executor();
    if (!executor.inEventLoop()) {
      executor.execute(new Runnable()
      {
        public void run() {
          SslHandler.this.renegotiateOnEventLoop(promise);
        }
      });
      return promise;
    }
    
    renegotiateOnEventLoop(promise);
    return promise;
  }
  
  private void renegotiateOnEventLoop(Promise<Channel> newHandshakePromise) {
    Promise<Channel> oldHandshakePromise = handshakePromise;
    if (!oldHandshakePromise.isDone())
    {

      PromiseNotifier.cascade(oldHandshakePromise, newHandshakePromise);
    } else {
      handshakePromise = newHandshakePromise;
      handshake(true);
      applyHandshakeTimeout();
    }
  }
  





  private void handshake(boolean flushAtEnd)
  {
    if (engine.getHandshakeStatus() != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING)
    {

      return;
    }
    if (handshakePromise.isDone())
    {




      return;
    }
    

    ChannelHandlerContext ctx = this.ctx;
    try {
      engine.beginHandshake();
      wrapNonAppData(ctx, false);
    } catch (Throwable e) {
      setHandshakeFailure(ctx, e);
    } finally {
      if (flushAtEnd) {
        forceFlush(ctx);
      }
    }
  }
  
  private void applyHandshakeTimeout() {
    final Promise<Channel> localHandshakePromise = handshakePromise;
    

    final long handshakeTimeoutMillis = this.handshakeTimeoutMillis;
    if ((handshakeTimeoutMillis <= 0L) || (localHandshakePromise.isDone())) {
      return;
    }
    
    final ScheduledFuture<?> timeoutFuture = ctx.executor().schedule(new Runnable()
    {
      public void run() {
        if (localHandshakePromise.isDone()) {
          return;
        }
        SSLException exception = new SslHandshakeTimeoutException("handshake timed out after " + handshakeTimeoutMillis + "ms");
        try
        {
          if (localHandshakePromise.tryFailure(exception)) {
            SslUtils.handleHandshakeFailure(ctx, exception, true);
          }
          
          SslHandler.this.releaseAndFailAll(ctx, exception); } finally { SslHandler.this.releaseAndFailAll(ctx, exception); } } }, handshakeTimeoutMillis, TimeUnit.MILLISECONDS);
    




    localHandshakePromise.addListener(new FutureListener()
    {
      public void operationComplete(Future<Channel> f) throws Exception {
        timeoutFuture.cancel(false);
      }
    });
  }
  
  private void forceFlush(ChannelHandlerContext ctx) {
    clearState(16);
    ctx.flush();
  }
  


  public void channelActive(ChannelHandlerContext ctx)
    throws Exception
  {
    if (!startTls) {
      startHandshakeProcessing(true);
    }
    ctx.fireChannelActive();
  }
  

  private void safeClose(final ChannelHandlerContext ctx, final ChannelFuture flushFuture, final ChannelPromise promise)
  {
    if (!ctx.channel().isActive()) {
      ctx.close(promise); return;
    }
    
    ScheduledFuture<?> timeoutFuture;
    final ScheduledFuture<?> timeoutFuture;
    if (!flushFuture.isDone()) {
      long closeNotifyTimeout = closeNotifyFlushTimeoutMillis;
      ScheduledFuture<?> timeoutFuture; if (closeNotifyTimeout > 0L)
      {
        timeoutFuture = ctx.executor().schedule(new Runnable()
        {
          public void run()
          {
            if (!flushFuture.isDone()) {
              SslHandler.logger.warn("{} Last write attempt timed out; force-closing the connection.", ctx
                .channel());
              SslHandler.addCloseListener(ctx.close(ctx.newPromise()), promise); } } }, closeNotifyTimeout, TimeUnit.MILLISECONDS);

      }
      else
      {
        timeoutFuture = null;
      }
    } else {
      timeoutFuture = null;
    }
    

    flushFuture.addListener(new ChannelFutureListener()
    {
      public void operationComplete(ChannelFuture f) {
        if (timeoutFuture != null) {
          timeoutFuture.cancel(false);
        }
        final long closeNotifyReadTimeout = closeNotifyReadTimeoutMillis;
        if (closeNotifyReadTimeout <= 0L)
        {

          SslHandler.addCloseListener(ctx.close(ctx.newPromise()), promise);
        } else {
          ScheduledFuture<?> closeNotifyReadTimeoutFuture;
          final ScheduledFuture<?> closeNotifyReadTimeoutFuture;
          if (!sslClosePromise.isDone()) {
            closeNotifyReadTimeoutFuture = ctx.executor().schedule(new Runnable()
            {
              public void run() {
                if (!sslClosePromise.isDone()) {
                  SslHandler.logger.debug("{} did not receive close_notify in {}ms; force-closing the connection.", val$ctx
                  
                    .channel(), Long.valueOf(closeNotifyReadTimeout));
                  

                  SslHandler.addCloseListener(val$ctx.close(val$ctx.newPromise()), val$promise); } } }, closeNotifyReadTimeout, TimeUnit.MILLISECONDS);

          }
          else
          {
            closeNotifyReadTimeoutFuture = null;
          }
          

          sslClosePromise.addListener(new FutureListener()
          {
            public void operationComplete(Future<Channel> future) throws Exception {
              if (closeNotifyReadTimeoutFuture != null) {
                closeNotifyReadTimeoutFuture.cancel(false);
              }
              SslHandler.addCloseListener(val$ctx.close(val$ctx.newPromise()), val$promise);
            }
          });
        }
      }
    });
  }
  





  private static void addCloseListener(ChannelFuture future, ChannelPromise promise)
  {
    PromiseNotifier.cascade(false, future, promise);
  }
  



  private ByteBuf allocate(ChannelHandlerContext ctx, int capacity)
  {
    ByteBufAllocator alloc = ctx.alloc();
    if (engineType.wantsDirectBuffer) {
      return alloc.directBuffer(capacity);
    }
    return alloc.buffer(capacity);
  }
  




  private ByteBuf allocateOutNetBuf(ChannelHandlerContext ctx, int pendingBytes, int numComponents)
  {
    return engineType.allocateWrapBuffer(this, ctx.alloc(), pendingBytes, numComponents);
  }
  
  private boolean isStateSet(int bit) {
    return (state & bit) == bit;
  }
  
  private void setState(int bit) {
    state = ((short)(state | bit));
  }
  
  private void clearState(int bit) {
    state = ((short)(state & (bit ^ 0xFFFFFFFF)));
  }
  



  private final class SslHandlerCoalescingBufferQueue
    extends AbstractCoalescingBufferQueue
  {
    SslHandlerCoalescingBufferQueue(Channel channel, int initSize)
    {
      super(initSize);
    }
    
    protected ByteBuf compose(ByteBufAllocator alloc, ByteBuf cumulation, ByteBuf next)
    {
      int wrapDataSize = SslHandler.this.wrapDataSize;
      if ((cumulation instanceof CompositeByteBuf)) {
        CompositeByteBuf composite = (CompositeByteBuf)cumulation;
        int numComponents = composite.numComponents();
        if ((numComponents == 0) || 
          (!SslHandler.attemptCopyToCumulation(composite.internalComponent(numComponents - 1), next, wrapDataSize))) {
          composite.addComponent(true, next);
        }
        return composite;
      }
      return SslHandler.attemptCopyToCumulation(cumulation, next, wrapDataSize) ? cumulation : 
        copyAndCompose(alloc, cumulation, next);
    }
    
    protected ByteBuf composeFirst(ByteBufAllocator allocator, ByteBuf first)
    {
      if ((first instanceof CompositeByteBuf)) {
        CompositeByteBuf composite = (CompositeByteBuf)first;
        if (engineType.wantsDirectBuffer) {
          first = allocator.directBuffer(composite.readableBytes());
        } else {
          first = allocator.heapBuffer(composite.readableBytes());
        }
        try {
          first.writeBytes(composite);
        } catch (Throwable cause) {
          first.release();
          PlatformDependent.throwException(cause);
        }
        composite.release();
      }
      return first;
    }
    
    protected ByteBuf removeEmptyValue()
    {
      return null;
    }
  }
  
  private static boolean attemptCopyToCumulation(ByteBuf cumulation, ByteBuf next, int wrapDataSize) {
    int inReadableBytes = next.readableBytes();
    int cumulationCapacity = cumulation.capacity();
    if (wrapDataSize - cumulation.readableBytes() >= inReadableBytes)
    {


      if ((!cumulation.isWritable(inReadableBytes)) || (cumulationCapacity < wrapDataSize)) { if (cumulationCapacity < wrapDataSize)
        {
          if (!ByteBufUtil.ensureWritableSuccess(cumulation.ensureWritable(inReadableBytes, false))) {} }
      } else { cumulation.writeBytes(next);
        next.release();
        return true;
      } }
    return false;
  }
  
  private final class LazyChannelPromise extends DefaultPromise<Channel> {
    private LazyChannelPromise() {}
    
    protected EventExecutor executor() {
      if (ctx == null) {
        throw new IllegalStateException();
      }
      return ctx.executor();
    }
    
    protected void checkDeadLock()
    {
      if (ctx == null)
      {





        return;
      }
      super.checkDeadLock();
    }
  }
}
