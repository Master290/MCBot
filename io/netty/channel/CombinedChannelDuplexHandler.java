package io.netty.channel;

import io.netty.buffer.ByteBufAllocator;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.ThrowableUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.net.SocketAddress;




















public class CombinedChannelDuplexHandler<I extends ChannelInboundHandler, O extends ChannelOutboundHandler>
  extends ChannelDuplexHandler
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(CombinedChannelDuplexHandler.class);
  
  private DelegatingChannelHandlerContext inboundCtx;
  
  private DelegatingChannelHandlerContext outboundCtx;
  
  private volatile boolean handlerAdded;
  
  private I inboundHandler;
  
  private O outboundHandler;
  

  protected CombinedChannelDuplexHandler()
  {
    ensureNotSharable();
  }
  


  public CombinedChannelDuplexHandler(I inboundHandler, O outboundHandler)
  {
    ensureNotSharable();
    init(inboundHandler, outboundHandler);
  }
  







  protected final void init(I inboundHandler, O outboundHandler)
  {
    validate(inboundHandler, outboundHandler);
    this.inboundHandler = inboundHandler;
    this.outboundHandler = outboundHandler;
  }
  
  private void validate(I inboundHandler, O outboundHandler) {
    if (this.inboundHandler != null)
    {
      throw new IllegalStateException("init() can not be invoked if " + CombinedChannelDuplexHandler.class.getSimpleName() + " was constructed with non-default constructor.");
    }
    

    ObjectUtil.checkNotNull(inboundHandler, "inboundHandler");
    ObjectUtil.checkNotNull(outboundHandler, "outboundHandler");
    
    if ((inboundHandler instanceof ChannelOutboundHandler))
    {

      throw new IllegalArgumentException("inboundHandler must not implement " + ChannelOutboundHandler.class.getSimpleName() + " to get combined.");
    }
    if ((outboundHandler instanceof ChannelInboundHandler))
    {

      throw new IllegalArgumentException("outboundHandler must not implement " + ChannelInboundHandler.class.getSimpleName() + " to get combined.");
    }
  }
  
  protected final I inboundHandler() {
    return inboundHandler;
  }
  
  protected final O outboundHandler() {
    return outboundHandler;
  }
  
  private void checkAdded() {
    if (!handlerAdded) {
      throw new IllegalStateException("handler not added to pipeline yet");
    }
  }
  


  public final void removeInboundHandler()
  {
    checkAdded();
    inboundCtx.remove();
  }
  


  public final void removeOutboundHandler()
  {
    checkAdded();
    outboundCtx.remove();
  }
  
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception
  {
    if (inboundHandler == null)
    {

      throw new IllegalStateException("init() must be invoked before being added to a " + ChannelPipeline.class.getSimpleName() + " if " + CombinedChannelDuplexHandler.class.getSimpleName() + " was constructed with the default constructor.");
    }
    

    outboundCtx = new DelegatingChannelHandlerContext(ctx, outboundHandler);
    inboundCtx = new DelegatingChannelHandlerContext(ctx, inboundHandler)
    {
      public ChannelHandlerContext fireExceptionCaught(Throwable cause)
      {
        if (!outboundCtx.removed)
        {
          try
          {
            outboundHandler.exceptionCaught(outboundCtx, cause);
          } catch (Throwable error) {
            if (CombinedChannelDuplexHandler.logger.isDebugEnabled()) {
              CombinedChannelDuplexHandler.logger.debug("An exception {}was thrown by a user handler's exceptionCaught() method while handling the following exception:", 
              


                ThrowableUtil.stackTraceToString(error), cause);
            } else if (CombinedChannelDuplexHandler.logger.isWarnEnabled()) {
              CombinedChannelDuplexHandler.logger.warn("An exception '{}' [enable DEBUG level for full stacktrace] was thrown by a user handler's exceptionCaught() method while handling the following exception:", error, cause);
            }
            
          }
          
        }
        else {
          super.fireExceptionCaught(cause);
        }
        return this;

      }
      

    };
    handlerAdded = true;
    try
    {
      inboundHandler.handlerAdded(inboundCtx);
      
      outboundHandler.handlerAdded(outboundCtx); } finally { outboundHandler.handlerAdded(outboundCtx);
    }
  }
  
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception
  {
    try {
      inboundCtx.remove();
      
      outboundCtx.remove(); } finally { outboundCtx.remove();
    }
  }
  
  public void channelRegistered(ChannelHandlerContext ctx) throws Exception
  {
    assert (ctx == inboundCtx.ctx);
    if (!inboundCtx.removed) {
      inboundHandler.channelRegistered(inboundCtx);
    } else {
      inboundCtx.fireChannelRegistered();
    }
  }
  
  public void channelUnregistered(ChannelHandlerContext ctx) throws Exception
  {
    assert (ctx == inboundCtx.ctx);
    if (!inboundCtx.removed) {
      inboundHandler.channelUnregistered(inboundCtx);
    } else {
      inboundCtx.fireChannelUnregistered();
    }
  }
  
  public void channelActive(ChannelHandlerContext ctx) throws Exception
  {
    assert (ctx == inboundCtx.ctx);
    if (!inboundCtx.removed) {
      inboundHandler.channelActive(inboundCtx);
    } else {
      inboundCtx.fireChannelActive();
    }
  }
  
  public void channelInactive(ChannelHandlerContext ctx) throws Exception
  {
    assert (ctx == inboundCtx.ctx);
    if (!inboundCtx.removed) {
      inboundHandler.channelInactive(inboundCtx);
    } else {
      inboundCtx.fireChannelInactive();
    }
  }
  
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
  {
    assert (ctx == inboundCtx.ctx);
    if (!inboundCtx.removed) {
      inboundHandler.exceptionCaught(inboundCtx, cause);
    } else {
      inboundCtx.fireExceptionCaught(cause);
    }
  }
  
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception
  {
    assert (ctx == inboundCtx.ctx);
    if (!inboundCtx.removed) {
      inboundHandler.userEventTriggered(inboundCtx, evt);
    } else {
      inboundCtx.fireUserEventTriggered(evt);
    }
  }
  
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
  {
    assert (ctx == inboundCtx.ctx);
    if (!inboundCtx.removed) {
      inboundHandler.channelRead(inboundCtx, msg);
    } else {
      inboundCtx.fireChannelRead(msg);
    }
  }
  
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception
  {
    assert (ctx == inboundCtx.ctx);
    if (!inboundCtx.removed) {
      inboundHandler.channelReadComplete(inboundCtx);
    } else {
      inboundCtx.fireChannelReadComplete();
    }
  }
  
  public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception
  {
    assert (ctx == inboundCtx.ctx);
    if (!inboundCtx.removed) {
      inboundHandler.channelWritabilityChanged(inboundCtx);
    } else {
      inboundCtx.fireChannelWritabilityChanged();
    }
  }
  

  public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise)
    throws Exception
  {
    assert (ctx == outboundCtx.ctx);
    if (!outboundCtx.removed) {
      outboundHandler.bind(outboundCtx, localAddress, promise);
    } else {
      outboundCtx.bind(localAddress, promise);
    }
  }
  


  public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise)
    throws Exception
  {
    assert (ctx == outboundCtx.ctx);
    if (!outboundCtx.removed) {
      outboundHandler.connect(outboundCtx, remoteAddress, localAddress, promise);
    } else {
      outboundCtx.connect(remoteAddress, localAddress, promise);
    }
  }
  
  public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception
  {
    assert (ctx == outboundCtx.ctx);
    if (!outboundCtx.removed) {
      outboundHandler.disconnect(outboundCtx, promise);
    } else {
      outboundCtx.disconnect(promise);
    }
  }
  
  public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception
  {
    assert (ctx == outboundCtx.ctx);
    if (!outboundCtx.removed) {
      outboundHandler.close(outboundCtx, promise);
    } else {
      outboundCtx.close(promise);
    }
  }
  
  public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception
  {
    assert (ctx == outboundCtx.ctx);
    if (!outboundCtx.removed) {
      outboundHandler.deregister(outboundCtx, promise);
    } else {
      outboundCtx.deregister(promise);
    }
  }
  
  public void read(ChannelHandlerContext ctx) throws Exception
  {
    assert (ctx == outboundCtx.ctx);
    if (!outboundCtx.removed) {
      outboundHandler.read(outboundCtx);
    } else {
      outboundCtx.read();
    }
  }
  
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception
  {
    assert (ctx == outboundCtx.ctx);
    if (!outboundCtx.removed) {
      outboundHandler.write(outboundCtx, msg, promise);
    } else {
      outboundCtx.write(msg, promise);
    }
  }
  
  public void flush(ChannelHandlerContext ctx) throws Exception
  {
    assert (ctx == outboundCtx.ctx);
    if (!outboundCtx.removed) {
      outboundHandler.flush(outboundCtx);
    } else {
      outboundCtx.flush();
    }
  }
  
  private static class DelegatingChannelHandlerContext implements ChannelHandlerContext
  {
    private final ChannelHandlerContext ctx;
    private final ChannelHandler handler;
    boolean removed;
    
    DelegatingChannelHandlerContext(ChannelHandlerContext ctx, ChannelHandler handler) {
      this.ctx = ctx;
      this.handler = handler;
    }
    
    public Channel channel()
    {
      return ctx.channel();
    }
    
    public EventExecutor executor()
    {
      return ctx.executor();
    }
    
    public String name()
    {
      return ctx.name();
    }
    
    public ChannelHandler handler()
    {
      return ctx.handler();
    }
    
    public boolean isRemoved()
    {
      return (removed) || (ctx.isRemoved());
    }
    
    public ChannelHandlerContext fireChannelRegistered()
    {
      ctx.fireChannelRegistered();
      return this;
    }
    
    public ChannelHandlerContext fireChannelUnregistered()
    {
      ctx.fireChannelUnregistered();
      return this;
    }
    
    public ChannelHandlerContext fireChannelActive()
    {
      ctx.fireChannelActive();
      return this;
    }
    
    public ChannelHandlerContext fireChannelInactive()
    {
      ctx.fireChannelInactive();
      return this;
    }
    
    public ChannelHandlerContext fireExceptionCaught(Throwable cause)
    {
      ctx.fireExceptionCaught(cause);
      return this;
    }
    
    public ChannelHandlerContext fireUserEventTriggered(Object event)
    {
      ctx.fireUserEventTriggered(event);
      return this;
    }
    
    public ChannelHandlerContext fireChannelRead(Object msg)
    {
      ctx.fireChannelRead(msg);
      return this;
    }
    
    public ChannelHandlerContext fireChannelReadComplete()
    {
      ctx.fireChannelReadComplete();
      return this;
    }
    
    public ChannelHandlerContext fireChannelWritabilityChanged()
    {
      ctx.fireChannelWritabilityChanged();
      return this;
    }
    
    public ChannelFuture bind(SocketAddress localAddress)
    {
      return ctx.bind(localAddress);
    }
    
    public ChannelFuture connect(SocketAddress remoteAddress)
    {
      return ctx.connect(remoteAddress);
    }
    
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress)
    {
      return ctx.connect(remoteAddress, localAddress);
    }
    
    public ChannelFuture disconnect()
    {
      return ctx.disconnect();
    }
    
    public ChannelFuture close()
    {
      return ctx.close();
    }
    
    public ChannelFuture deregister()
    {
      return ctx.deregister();
    }
    
    public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise)
    {
      return ctx.bind(localAddress, promise);
    }
    
    public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise)
    {
      return ctx.connect(remoteAddress, promise);
    }
    

    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise)
    {
      return ctx.connect(remoteAddress, localAddress, promise);
    }
    
    public ChannelFuture disconnect(ChannelPromise promise)
    {
      return ctx.disconnect(promise);
    }
    
    public ChannelFuture close(ChannelPromise promise)
    {
      return ctx.close(promise);
    }
    
    public ChannelFuture deregister(ChannelPromise promise)
    {
      return ctx.deregister(promise);
    }
    
    public ChannelHandlerContext read()
    {
      ctx.read();
      return this;
    }
    
    public ChannelFuture write(Object msg)
    {
      return ctx.write(msg);
    }
    
    public ChannelFuture write(Object msg, ChannelPromise promise)
    {
      return ctx.write(msg, promise);
    }
    
    public ChannelHandlerContext flush()
    {
      ctx.flush();
      return this;
    }
    
    public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise)
    {
      return ctx.writeAndFlush(msg, promise);
    }
    
    public ChannelFuture writeAndFlush(Object msg)
    {
      return ctx.writeAndFlush(msg);
    }
    
    public ChannelPipeline pipeline()
    {
      return ctx.pipeline();
    }
    
    public ByteBufAllocator alloc()
    {
      return ctx.alloc();
    }
    
    public ChannelPromise newPromise()
    {
      return ctx.newPromise();
    }
    
    public ChannelProgressivePromise newProgressivePromise()
    {
      return ctx.newProgressivePromise();
    }
    
    public ChannelFuture newSucceededFuture()
    {
      return ctx.newSucceededFuture();
    }
    
    public ChannelFuture newFailedFuture(Throwable cause)
    {
      return ctx.newFailedFuture(cause);
    }
    
    public ChannelPromise voidPromise()
    {
      return ctx.voidPromise();
    }
    
    public <T> Attribute<T> attr(AttributeKey<T> key)
    {
      return ctx.channel().attr(key);
    }
    
    public <T> boolean hasAttr(AttributeKey<T> key)
    {
      return ctx.channel().hasAttr(key);
    }
    
    final void remove() {
      EventExecutor executor = executor();
      if (executor.inEventLoop()) {
        remove0();
      } else {
        executor.execute(new Runnable()
        {
          public void run() {
            CombinedChannelDuplexHandler.DelegatingChannelHandlerContext.this.remove0();
          }
        });
      }
    }
    
    private void remove0() {
      if (!removed) {
        removed = true;
        try {
          handler.handlerRemoved(this);
        } catch (Throwable cause) {
          fireExceptionCaught(new ChannelPipelineException(handler
            .getClass().getName() + ".handlerRemoved() has thrown an exception.", cause));
        }
      }
    }
  }
}
