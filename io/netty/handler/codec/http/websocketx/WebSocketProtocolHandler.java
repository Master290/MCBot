package io.netty.handler.codec.http.websocketx;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.PromiseNotifier;
import io.netty.util.concurrent.ScheduledFuture;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.List;
import java.util.concurrent.TimeUnit;














abstract class WebSocketProtocolHandler
  extends MessageToMessageDecoder<WebSocketFrame>
  implements ChannelOutboundHandler
{
  private final boolean dropPongFrames;
  private final WebSocketCloseStatus closeStatus;
  private final long forceCloseTimeoutMillis;
  private ChannelPromise closeSent;
  
  WebSocketProtocolHandler()
  {
    this(true);
  }
  






  WebSocketProtocolHandler(boolean dropPongFrames)
  {
    this(dropPongFrames, null, 0L);
  }
  

  WebSocketProtocolHandler(boolean dropPongFrames, WebSocketCloseStatus closeStatus, long forceCloseTimeoutMillis)
  {
    this.dropPongFrames = dropPongFrames;
    this.closeStatus = closeStatus;
    this.forceCloseTimeoutMillis = forceCloseTimeoutMillis;
  }
  
  protected void decode(ChannelHandlerContext ctx, WebSocketFrame frame, List<Object> out) throws Exception
  {
    if ((frame instanceof PingWebSocketFrame)) {
      frame.content().retain();
      ctx.channel().writeAndFlush(new PongWebSocketFrame(frame.content()));
      readIfNeeded(ctx);
      return;
    }
    if (((frame instanceof PongWebSocketFrame)) && (dropPongFrames)) {
      readIfNeeded(ctx);
      return;
    }
    
    out.add(frame.retain());
  }
  
  private static void readIfNeeded(ChannelHandlerContext ctx) {
    if (!ctx.channel().config().isAutoRead()) {
      ctx.read();
    }
  }
  
  public void close(final ChannelHandlerContext ctx, final ChannelPromise promise) throws Exception
  {
    if ((closeStatus == null) || (!ctx.channel().isActive())) {
      ctx.close(promise);
    } else {
      if (closeSent == null) {
        write(ctx, new CloseWebSocketFrame(closeStatus), ctx.newPromise());
      }
      flush(ctx);
      applyCloseSentTimeout(ctx);
      closeSent.addListener(new ChannelFutureListener()
      {
        public void operationComplete(ChannelFuture future) {
          ctx.close(promise);
        }
      });
    }
  }
  
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception
  {
    if (closeSent != null) {
      ReferenceCountUtil.release(msg);
      promise.setFailure(new ClosedChannelException());
    } else if ((msg instanceof CloseWebSocketFrame)) {
      closeSent(promise.unvoid());
      ctx.write(msg).addListener(new PromiseNotifier(false, new Promise[] { closeSent }));
    } else {
      ctx.write(msg, promise);
    }
  }
  
  void closeSent(ChannelPromise promise) {
    closeSent = promise;
  }
  
  private void applyCloseSentTimeout(ChannelHandlerContext ctx) {
    if ((closeSent.isDone()) || (forceCloseTimeoutMillis < 0L)) {
      return;
    }
    
    final ScheduledFuture<?> timeoutTask = ctx.executor().schedule(new Runnable()
    {
      public void run() {
        if (!closeSent.isDone())
          closeSent.tryFailure(buildHandshakeException("send close frame timed out")); } }, forceCloseTimeoutMillis, TimeUnit.MILLISECONDS);
    



    closeSent.addListener(new ChannelFutureListener()
    {
      public void operationComplete(ChannelFuture future) {
        timeoutTask.cancel(false);
      }
    });
  }
  



  protected WebSocketHandshakeException buildHandshakeException(String message)
  {
    return new WebSocketHandshakeException(message);
  }
  
  public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise)
    throws Exception
  {
    ctx.bind(localAddress, promise);
  }
  
  public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise)
    throws Exception
  {
    ctx.connect(remoteAddress, localAddress, promise);
  }
  
  public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise)
    throws Exception
  {
    ctx.disconnect(promise);
  }
  
  public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception
  {
    ctx.deregister(promise);
  }
  
  public void read(ChannelHandlerContext ctx) throws Exception
  {
    ctx.read();
  }
  
  public void flush(ChannelHandlerContext ctx) throws Exception
  {
    ctx.flush();
  }
  
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
  {
    ctx.fireExceptionCaught(cause);
    ctx.close();
  }
}
