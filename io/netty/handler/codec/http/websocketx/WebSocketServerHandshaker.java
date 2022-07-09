package io.netty.handler.codec.http.websocketx;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundInvoker;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.EmptyArrays;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.nio.channels.ClosedChannelException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;




















public abstract class WebSocketServerHandshaker
{
  protected static final InternalLogger logger = InternalLoggerFactory.getInstance(WebSocketServerHandshaker.class);
  



  private final String uri;
  



  private final String[] subprotocols;
  


  private final WebSocketVersion version;
  


  private final WebSocketDecoderConfig decoderConfig;
  


  private String selectedSubprotocol;
  


  public static final String SUB_PROTOCOL_WILDCARD = "*";
  



  protected WebSocketServerHandshaker(WebSocketVersion version, String uri, String subprotocols, int maxFramePayloadLength)
  {
    this(version, uri, subprotocols, WebSocketDecoderConfig.newBuilder()
      .maxFramePayloadLength(maxFramePayloadLength)
      .build());
  }
  













  protected WebSocketServerHandshaker(WebSocketVersion version, String uri, String subprotocols, WebSocketDecoderConfig decoderConfig)
  {
    this.version = version;
    this.uri = uri;
    if (subprotocols != null) {
      String[] subprotocolArray = subprotocols.split(",");
      for (int i = 0; i < subprotocolArray.length; i++) {
        subprotocolArray[i] = subprotocolArray[i].trim();
      }
      this.subprotocols = subprotocolArray;
    } else {
      this.subprotocols = EmptyArrays.EMPTY_STRINGS;
    }
    this.decoderConfig = ((WebSocketDecoderConfig)ObjectUtil.checkNotNull(decoderConfig, "decoderConfig"));
  }
  


  public String uri()
  {
    return uri;
  }
  


  public Set<String> subprotocols()
  {
    Set<String> ret = new LinkedHashSet();
    Collections.addAll(ret, subprotocols);
    return ret;
  }
  


  public WebSocketVersion version()
  {
    return version;
  }
  




  public int maxFramePayloadLength()
  {
    return decoderConfig.maxFramePayloadLength();
  }
  




  public WebSocketDecoderConfig decoderConfig()
  {
    return decoderConfig;
  }
  










  public ChannelFuture handshake(Channel channel, FullHttpRequest req)
  {
    return handshake(channel, req, null, channel.newPromise());
  }
  

















  public final ChannelFuture handshake(Channel channel, FullHttpRequest req, HttpHeaders responseHeaders, final ChannelPromise promise)
  {
    if (logger.isDebugEnabled()) {
      logger.debug("{} WebSocket version {} server handshake", channel, version());
    }
    FullHttpResponse response = newHandshakeResponse(req, responseHeaders);
    ChannelPipeline p = channel.pipeline();
    if (p.get(HttpObjectAggregator.class) != null) {
      p.remove(HttpObjectAggregator.class);
    }
    if (p.get(HttpContentCompressor.class) != null) {
      p.remove(HttpContentCompressor.class);
    }
    ChannelHandlerContext ctx = p.context(HttpRequestDecoder.class);
    String encoderName;
    final String encoderName; if (ctx == null)
    {
      ctx = p.context(HttpServerCodec.class);
      if (ctx == null) {
        promise.setFailure(new IllegalStateException("No HttpDecoder and no HttpServerCodec in the pipeline"));
        
        return promise;
      }
      p.addBefore(ctx.name(), "wsencoder", newWebSocketEncoder());
      p.addBefore(ctx.name(), "wsdecoder", newWebsocketDecoder());
      encoderName = ctx.name();
    } else {
      p.replace(ctx.name(), "wsdecoder", newWebsocketDecoder());
      
      encoderName = p.context(HttpResponseEncoder.class).name();
      p.addBefore(encoderName, "wsencoder", newWebSocketEncoder());
    }
    channel.writeAndFlush(response).addListener(new ChannelFutureListener()
    {
      public void operationComplete(ChannelFuture future) throws Exception {
        if (future.isSuccess()) {
          ChannelPipeline p = future.channel().pipeline();
          p.remove(encoderName);
          promise.setSuccess();
        } else {
          promise.setFailure(future.cause());
        }
      }
    });
    return promise;
  }
  










  public ChannelFuture handshake(Channel channel, HttpRequest req)
  {
    return handshake(channel, req, null, channel.newPromise());
  }
  

















  public final ChannelFuture handshake(final Channel channel, HttpRequest req, final HttpHeaders responseHeaders, final ChannelPromise promise)
  {
    if ((req instanceof FullHttpRequest)) {
      return handshake(channel, (FullHttpRequest)req, responseHeaders, promise);
    }
    if (logger.isDebugEnabled()) {
      logger.debug("{} WebSocket version {} server handshake", channel, version());
    }
    ChannelPipeline p = channel.pipeline();
    ChannelHandlerContext ctx = p.context(HttpRequestDecoder.class);
    if (ctx == null)
    {
      ctx = p.context(HttpServerCodec.class);
      if (ctx == null) {
        promise.setFailure(new IllegalStateException("No HttpDecoder and no HttpServerCodec in the pipeline"));
        
        return promise;
      }
    }
    



    String aggregatorName = "httpAggregator";
    p.addAfter(ctx.name(), aggregatorName, new HttpObjectAggregator(8192));
    p.addAfter(aggregatorName, "handshaker", new SimpleChannelInboundHandler()
    {
      protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception
      {
        ctx.pipeline().remove(this);
        handshake(channel, msg, responseHeaders, promise);
      }
      
      public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
        throws Exception
      {
        ctx.pipeline().remove(this);
        promise.tryFailure(cause);
        ctx.fireExceptionCaught(cause);
      }
      
      public void channelInactive(ChannelHandlerContext ctx)
        throws Exception
      {
        if (!promise.isDone()) {
          promise.tryFailure(new ClosedChannelException());
        }
        ctx.fireChannelInactive();
      }
    });
    try {
      ctx.fireChannelRead(ReferenceCountUtil.retain(req));
    } catch (Throwable cause) {
      promise.setFailure(cause);
    }
    return promise;
  }
  







  protected abstract FullHttpResponse newHandshakeResponse(FullHttpRequest paramFullHttpRequest, HttpHeaders paramHttpHeaders);
  






  public ChannelFuture close(Channel channel, CloseWebSocketFrame frame)
  {
    ObjectUtil.checkNotNull(channel, "channel");
    return close(channel, frame, channel.newPromise());
  }
  












  public ChannelFuture close(Channel channel, CloseWebSocketFrame frame, ChannelPromise promise)
  {
    return close0(channel, frame, promise);
  }
  







  public ChannelFuture close(ChannelHandlerContext ctx, CloseWebSocketFrame frame)
  {
    ObjectUtil.checkNotNull(ctx, "ctx");
    return close(ctx, frame, ctx.newPromise());
  }
  









  public ChannelFuture close(ChannelHandlerContext ctx, CloseWebSocketFrame frame, ChannelPromise promise)
  {
    ObjectUtil.checkNotNull(ctx, "ctx");
    return close0(ctx, frame, promise).addListener(ChannelFutureListener.CLOSE);
  }
  
  private ChannelFuture close0(ChannelOutboundInvoker invoker, CloseWebSocketFrame frame, ChannelPromise promise) {
    return invoker.writeAndFlush(frame, promise).addListener(ChannelFutureListener.CLOSE);
  }
  






  protected String selectSubprotocol(String requestedSubprotocols)
  {
    if ((requestedSubprotocols == null) || (subprotocols.length == 0)) {
      return null;
    }
    
    String[] requestedSubprotocolArray = requestedSubprotocols.split(",");
    for (String p : requestedSubprotocolArray) {
      String requestedSubprotocol = p.trim();
      
      for (String supportedSubprotocol : subprotocols) {
        if (("*".equals(supportedSubprotocol)) || 
          (requestedSubprotocol.equals(supportedSubprotocol))) {
          selectedSubprotocol = requestedSubprotocol;
          return requestedSubprotocol;
        }
      }
    }
    

    return null;
  }
  





  public String selectedSubprotocol()
  {
    return selectedSubprotocol;
  }
  
  protected abstract WebSocketFrameDecoder newWebsocketDecoder();
  
  protected abstract WebSocketFrameEncoder newWebSocketEncoder();
}
