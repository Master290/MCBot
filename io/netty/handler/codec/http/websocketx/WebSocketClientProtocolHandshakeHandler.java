package io.netty.handler.codec.http.websocketx;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.internal.ObjectUtil;
import java.util.concurrent.TimeUnit;















class WebSocketClientProtocolHandshakeHandler
  extends ChannelInboundHandlerAdapter
{
  private static final long DEFAULT_HANDSHAKE_TIMEOUT_MS = 10000L;
  private final WebSocketClientHandshaker handshaker;
  private final long handshakeTimeoutMillis;
  private ChannelHandlerContext ctx;
  private ChannelPromise handshakePromise;
  
  WebSocketClientProtocolHandshakeHandler(WebSocketClientHandshaker handshaker)
  {
    this(handshaker, 10000L);
  }
  
  WebSocketClientProtocolHandshakeHandler(WebSocketClientHandshaker handshaker, long handshakeTimeoutMillis) {
    this.handshaker = handshaker;
    this.handshakeTimeoutMillis = ObjectUtil.checkPositive(handshakeTimeoutMillis, "handshakeTimeoutMillis");
  }
  
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception
  {
    this.ctx = ctx;
    handshakePromise = ctx.newPromise();
  }
  
  public void channelActive(final ChannelHandlerContext ctx) throws Exception
  {
    super.channelActive(ctx);
    handshaker.handshake(ctx.channel()).addListener(new ChannelFutureListener()
    {
      public void operationComplete(ChannelFuture future) throws Exception {
        if (!future.isSuccess()) {
          handshakePromise.tryFailure(future.cause());
          ctx.fireExceptionCaught(future.cause());
        } else {
          ctx.fireUserEventTriggered(WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_ISSUED);
        }
        
      }
    });
    applyHandshakeTimeout();
  }
  
  public void channelInactive(ChannelHandlerContext ctx) throws Exception
  {
    if (!handshakePromise.isDone()) {
      handshakePromise.tryFailure(new WebSocketClientHandshakeException("channel closed with handshake in progress"));
    }
    

    super.channelInactive(ctx);
  }
  
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
  {
    if (!(msg instanceof FullHttpResponse)) {
      ctx.fireChannelRead(msg);
      return;
    }
    
    FullHttpResponse response = (FullHttpResponse)msg;
    try {
      if (!handshaker.isHandshakeComplete()) {
        handshaker.finishHandshake(ctx.channel(), response);
        handshakePromise.trySuccess();
        ctx.fireUserEventTriggered(WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_COMPLETE);
        
        ctx.pipeline().remove(this);
        return;
      }
      throw new IllegalStateException("WebSocketClientHandshaker should have been non finished yet");
    } finally {
      response.release();
    }
  }
  
  private void applyHandshakeTimeout() {
    final ChannelPromise localHandshakePromise = handshakePromise;
    if ((handshakeTimeoutMillis <= 0L) || (localHandshakePromise.isDone())) {
      return;
    }
    
    final Future<?> timeoutFuture = ctx.executor().schedule(new Runnable()
    {
      public void run() {
        if (localHandshakePromise.isDone()) {
          return;
        }
        
        if (localHandshakePromise.tryFailure(new WebSocketClientHandshakeException("handshake timed out")))
        {

          ctx.flush().fireUserEventTriggered(WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_TIMEOUT).close(); } } }, handshakeTimeoutMillis, TimeUnit.MILLISECONDS);
    




    localHandshakePromise.addListener(new FutureListener()
    {
      public void operationComplete(Future<Void> f) throws Exception {
        timeoutFuture.cancel(false);
      }
    });
  }
  




  ChannelFuture getHandshakeFuture()
  {
    return handshakePromise;
  }
}
