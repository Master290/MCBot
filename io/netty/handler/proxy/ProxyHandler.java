package io.netty.handler.proxy;

import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.PendingWriteQueue;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ScheduledFuture;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.net.SocketAddress;
import java.nio.channels.ConnectionPendingException;
import java.util.concurrent.TimeUnit;
















public abstract class ProxyHandler
  extends ChannelDuplexHandler
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(ProxyHandler.class);
  

  private static final long DEFAULT_CONNECT_TIMEOUT_MILLIS = 10000L;
  

  static final String AUTH_NONE = "none";
  

  private final SocketAddress proxyAddress;
  

  private volatile SocketAddress destinationAddress;
  
  private volatile long connectTimeoutMillis = 10000L;
  
  private volatile ChannelHandlerContext ctx;
  private PendingWriteQueue pendingWrites;
  private boolean finished;
  private boolean suppressChannelReadComplete;
  private boolean flushedPrematurely;
  private final LazyChannelPromise connectPromise = new LazyChannelPromise(null);
  private ScheduledFuture<?> connectTimeoutFuture;
  private final ChannelFutureListener writeListener = new ChannelFutureListener()
  {
    public void operationComplete(ChannelFuture future) throws Exception {
      if (!future.isSuccess()) {
        ProxyHandler.this.setConnectFailure(future.cause());
      }
    }
  };
  
  protected ProxyHandler(SocketAddress proxyAddress) {
    this.proxyAddress = ((SocketAddress)ObjectUtil.checkNotNull(proxyAddress, "proxyAddress"));
  }
  



  public abstract String protocol();
  



  public abstract String authScheme();
  



  public final <T extends SocketAddress> T proxyAddress()
  {
    return proxyAddress;
  }
  



  public final <T extends SocketAddress> T destinationAddress()
  {
    return destinationAddress;
  }
  


  public final boolean isConnected()
  {
    return connectPromise.isSuccess();
  }
  



  public final Future<Channel> connectFuture()
  {
    return connectPromise;
  }
  



  public final long connectTimeoutMillis()
  {
    return connectTimeoutMillis;
  }
  



  public final void setConnectTimeoutMillis(long connectTimeoutMillis)
  {
    if (connectTimeoutMillis <= 0L) {
      connectTimeoutMillis = 0L;
    }
    
    this.connectTimeoutMillis = connectTimeoutMillis;
  }
  
  public final void handlerAdded(ChannelHandlerContext ctx) throws Exception
  {
    this.ctx = ctx;
    addCodec(ctx);
    
    if (ctx.channel().isActive())
    {

      sendInitialMessage(ctx);
    }
  }
  



  protected abstract void addCodec(ChannelHandlerContext paramChannelHandlerContext)
    throws Exception;
  



  protected abstract void removeEncoder(ChannelHandlerContext paramChannelHandlerContext)
    throws Exception;
  



  protected abstract void removeDecoder(ChannelHandlerContext paramChannelHandlerContext)
    throws Exception;
  


  public final void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise)
    throws Exception
  {
    if (destinationAddress != null) {
      promise.setFailure(new ConnectionPendingException());
      return;
    }
    
    destinationAddress = remoteAddress;
    ctx.connect(proxyAddress, localAddress, promise);
  }
  
  public final void channelActive(ChannelHandlerContext ctx) throws Exception
  {
    sendInitialMessage(ctx);
    ctx.fireChannelActive();
  }
  


  private void sendInitialMessage(ChannelHandlerContext ctx)
    throws Exception
  {
    long connectTimeoutMillis = this.connectTimeoutMillis;
    if (connectTimeoutMillis > 0L) {
      connectTimeoutFuture = ctx.executor().schedule(new Runnable()
      {
        public void run() {
          if (!connectPromise.isDone())
            ProxyHandler.this.setConnectFailure(new ProxyConnectException(exceptionMessage("timeout"))); } }, connectTimeoutMillis, TimeUnit.MILLISECONDS);
    }
    



    Object initialMessage = newInitialMessage(ctx);
    if (initialMessage != null) {
      sendToProxyServer(initialMessage);
    }
    
    readIfNeeded(ctx);
  }
  




  protected abstract Object newInitialMessage(ChannelHandlerContext paramChannelHandlerContext)
    throws Exception;
  



  protected final void sendToProxyServer(Object msg)
  {
    ctx.writeAndFlush(msg).addListener(writeListener);
  }
  
  public final void channelInactive(ChannelHandlerContext ctx) throws Exception
  {
    if (finished) {
      ctx.fireChannelInactive();
    }
    else {
      setConnectFailure(new ProxyConnectException(exceptionMessage("disconnected")));
    }
  }
  
  public final void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
  {
    if (finished) {
      ctx.fireExceptionCaught(cause);
    }
    else {
      setConnectFailure(cause);
    }
  }
  
  public final void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
  {
    if (finished)
    {
      suppressChannelReadComplete = false;
      ctx.fireChannelRead(msg);
    } else {
      suppressChannelReadComplete = true;
      Throwable cause = null;
      try {
        boolean done = handleResponse(ctx, msg);
        if (done) {
          setConnectSuccess();
        }
      } catch (Throwable t) {
        cause = t;
      } finally {
        ReferenceCountUtil.release(msg);
        if (cause != null) {
          setConnectFailure(cause);
        }
      }
    }
  }
  



  protected abstract boolean handleResponse(ChannelHandlerContext paramChannelHandlerContext, Object paramObject)
    throws Exception;
  


  private void setConnectSuccess()
  {
    finished = true;
    cancelConnectTimeoutFuture();
    
    if (!connectPromise.isDone()) {
      boolean removedCodec = true;
      
      removedCodec &= safeRemoveEncoder();
      
      ctx.fireUserEventTriggered(new ProxyConnectionEvent(
        protocol(), authScheme(), proxyAddress, destinationAddress));
      
      removedCodec &= safeRemoveDecoder();
      
      if (removedCodec) {
        writePendingWrites();
        
        if (flushedPrematurely) {
          ctx.flush();
        }
        connectPromise.trySuccess(ctx.channel());
      }
      else {
        Exception cause = new ProxyConnectException("failed to remove all codec handlers added by the proxy handler; bug?");
        
        failPendingWritesAndClose(cause);
      }
    }
  }
  
  private boolean safeRemoveDecoder() {
    try {
      removeDecoder(ctx);
      return true;
    } catch (Exception e) {
      logger.warn("Failed to remove proxy decoders:", e);
    }
    
    return false;
  }
  
  private boolean safeRemoveEncoder() {
    try {
      removeEncoder(ctx);
      return true;
    } catch (Exception e) {
      logger.warn("Failed to remove proxy encoders:", e);
    }
    
    return false;
  }
  
  private void setConnectFailure(Throwable cause) {
    finished = true;
    cancelConnectTimeoutFuture();
    
    if (!connectPromise.isDone())
    {
      if (!(cause instanceof ProxyConnectException))
      {
        cause = new ProxyConnectException(exceptionMessage(cause.toString()), cause);
      }
      
      safeRemoveDecoder();
      safeRemoveEncoder();
      failPendingWritesAndClose(cause);
    }
  }
  
  private void failPendingWritesAndClose(Throwable cause) {
    failPendingWrites(cause);
    connectPromise.tryFailure(cause);
    ctx.fireExceptionCaught(cause);
    ctx.close();
  }
  
  private void cancelConnectTimeoutFuture() {
    if (connectTimeoutFuture != null) {
      connectTimeoutFuture.cancel(false);
      connectTimeoutFuture = null;
    }
  }
  



  protected final String exceptionMessage(String msg)
  {
    if (msg == null) {
      msg = "";
    }
    







    StringBuilder buf = new StringBuilder(128 + msg.length()).append(protocol()).append(", ").append(authScheme()).append(", ").append(proxyAddress).append(" => ").append(destinationAddress);
    if (!msg.isEmpty()) {
      buf.append(", ").append(msg);
    }
    
    return buf.toString();
  }
  
  public final void channelReadComplete(ChannelHandlerContext ctx) throws Exception
  {
    if (suppressChannelReadComplete) {
      suppressChannelReadComplete = false;
      
      readIfNeeded(ctx);
    } else {
      ctx.fireChannelReadComplete();
    }
  }
  
  public final void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception
  {
    if (finished) {
      writePendingWrites();
      ctx.write(msg, promise);
    } else {
      addPendingWrite(ctx, msg, promise);
    }
  }
  
  public final void flush(ChannelHandlerContext ctx) throws Exception
  {
    if (finished) {
      writePendingWrites();
      ctx.flush();
    } else {
      flushedPrematurely = true;
    }
  }
  
  private static void readIfNeeded(ChannelHandlerContext ctx) {
    if (!ctx.channel().config().isAutoRead()) {
      ctx.read();
    }
  }
  
  private void writePendingWrites() {
    if (pendingWrites != null) {
      pendingWrites.removeAndWriteAll();
      pendingWrites = null;
    }
  }
  
  private void failPendingWrites(Throwable cause) {
    if (pendingWrites != null) {
      pendingWrites.removeAndFailAll(cause);
      pendingWrites = null;
    }
  }
  
  private void addPendingWrite(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
    PendingWriteQueue pendingWrites = this.pendingWrites;
    if (pendingWrites == null) {
      this.pendingWrites = (pendingWrites = new PendingWriteQueue(ctx));
    }
    pendingWrites.add(msg, promise);
  }
  
  private final class LazyChannelPromise extends DefaultPromise<Channel> {
    private LazyChannelPromise() {}
    
    protected EventExecutor executor() { if (ctx == null) {
        throw new IllegalStateException();
      }
      return ctx.executor();
    }
  }
}
