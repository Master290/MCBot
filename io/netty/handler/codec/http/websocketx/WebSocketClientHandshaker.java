package io.netty.handler.codec.http.websocketx;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundInvoker;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.codec.http.HttpScheme;
import io.netty.util.AsciiString;
import io.netty.util.NetUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.ObjectUtil;
import java.net.URI;
import java.nio.channels.ClosedChannelException;
import java.util.Locale;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;



















public abstract class WebSocketClientHandshaker
{
  private static final String HTTP_SCHEME_PREFIX = HttpScheme.HTTP + "://";
  private static final String HTTPS_SCHEME_PREFIX = HttpScheme.HTTPS + "://";
  
  protected static final int DEFAULT_FORCE_CLOSE_TIMEOUT_MILLIS = 10000;
  
  private final URI uri;
  
  private final WebSocketVersion version;
  
  private volatile boolean handshakeComplete;
  private volatile long forceCloseTimeoutMillis = 10000L;
  

  private volatile int forceCloseInit;
  
  private static final AtomicIntegerFieldUpdater<WebSocketClientHandshaker> FORCE_CLOSE_INIT_UPDATER = AtomicIntegerFieldUpdater.newUpdater(WebSocketClientHandshaker.class, "forceCloseInit");
  


  private volatile boolean forceCloseComplete;
  


  private final String expectedSubprotocol;
  


  private volatile String actualSubprotocol;
  


  protected final HttpHeaders customHeaders;
  


  private final int maxFramePayloadLength;
  


  private final boolean absoluteUpgradeUrl;
  



  protected WebSocketClientHandshaker(URI uri, WebSocketVersion version, String subprotocol, HttpHeaders customHeaders, int maxFramePayloadLength)
  {
    this(uri, version, subprotocol, customHeaders, maxFramePayloadLength, 10000L);
  }
  


















  protected WebSocketClientHandshaker(URI uri, WebSocketVersion version, String subprotocol, HttpHeaders customHeaders, int maxFramePayloadLength, long forceCloseTimeoutMillis)
  {
    this(uri, version, subprotocol, customHeaders, maxFramePayloadLength, forceCloseTimeoutMillis, false);
  }
  





















  protected WebSocketClientHandshaker(URI uri, WebSocketVersion version, String subprotocol, HttpHeaders customHeaders, int maxFramePayloadLength, long forceCloseTimeoutMillis, boolean absoluteUpgradeUrl)
  {
    this.uri = uri;
    this.version = version;
    expectedSubprotocol = subprotocol;
    this.customHeaders = customHeaders;
    this.maxFramePayloadLength = maxFramePayloadLength;
    this.forceCloseTimeoutMillis = forceCloseTimeoutMillis;
    this.absoluteUpgradeUrl = absoluteUpgradeUrl;
  }
  


  public URI uri()
  {
    return uri;
  }
  


  public WebSocketVersion version()
  {
    return version;
  }
  


  public int maxFramePayloadLength()
  {
    return maxFramePayloadLength;
  }
  


  public boolean isHandshakeComplete()
  {
    return handshakeComplete;
  }
  
  private void setHandshakeComplete() {
    handshakeComplete = true;
  }
  


  public String expectedSubprotocol()
  {
    return expectedSubprotocol;
  }
  



  public String actualSubprotocol()
  {
    return actualSubprotocol;
  }
  
  private void setActualSubprotocol(String actualSubprotocol) {
    this.actualSubprotocol = actualSubprotocol;
  }
  
  public long forceCloseTimeoutMillis() {
    return forceCloseTimeoutMillis;
  }
  



  protected boolean isForceCloseComplete()
  {
    return forceCloseComplete;
  }
  





  public WebSocketClientHandshaker setForceCloseTimeoutMillis(long forceCloseTimeoutMillis)
  {
    this.forceCloseTimeoutMillis = forceCloseTimeoutMillis;
    return this;
  }
  





  public ChannelFuture handshake(Channel channel)
  {
    ObjectUtil.checkNotNull(channel, "channel");
    return handshake(channel, channel.newPromise());
  }
  







  public final ChannelFuture handshake(Channel channel, final ChannelPromise promise)
  {
    ChannelPipeline pipeline = channel.pipeline();
    HttpResponseDecoder decoder = (HttpResponseDecoder)pipeline.get(HttpResponseDecoder.class);
    if (decoder == null) {
      HttpClientCodec codec = (HttpClientCodec)pipeline.get(HttpClientCodec.class);
      if (codec == null) {
        promise.setFailure(new IllegalStateException("ChannelPipeline does not contain an HttpResponseDecoder or HttpClientCodec"));
        
        return promise;
      }
    }
    
    FullHttpRequest request = newHandshakeRequest();
    
    channel.writeAndFlush(request).addListener(new ChannelFutureListener()
    {
      public void operationComplete(ChannelFuture future) {
        if (future.isSuccess()) {
          ChannelPipeline p = future.channel().pipeline();
          ChannelHandlerContext ctx = p.context(HttpRequestEncoder.class);
          if (ctx == null) {
            ctx = p.context(HttpClientCodec.class);
          }
          if (ctx == null) {
            promise.setFailure(new IllegalStateException("ChannelPipeline does not contain an HttpRequestEncoder or HttpClientCodec"));
            
            return;
          }
          p.addAfter(ctx.name(), "ws-encoder", newWebSocketEncoder());
          
          promise.setSuccess();
        } else {
          promise.setFailure(future.cause());
        }
      }
    });
    return promise;
  }
  





  protected abstract FullHttpRequest newHandshakeRequest();
  





  public final void finishHandshake(Channel channel, FullHttpResponse response)
  {
    verify(response);
    


    String receivedProtocol = response.headers().get(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL);
    receivedProtocol = receivedProtocol != null ? receivedProtocol.trim() : null;
    String expectedProtocol = expectedSubprotocol != null ? expectedSubprotocol : "";
    boolean protocolValid = false;
    
    if ((expectedProtocol.isEmpty()) && (receivedProtocol == null))
    {
      protocolValid = true;
      setActualSubprotocol(expectedSubprotocol);
    } else if ((!expectedProtocol.isEmpty()) && (receivedProtocol != null) && (!receivedProtocol.isEmpty()))
    {
      for (String protocol : expectedProtocol.split(",")) {
        if (protocol.trim().equals(receivedProtocol)) {
          protocolValid = true;
          setActualSubprotocol(receivedProtocol);
          break;
        }
      }
    }
    
    if (!protocolValid) {
      throw new WebSocketClientHandshakeException(String.format("Invalid subprotocol. Actual: %s. Expected one of: %s", new Object[] { receivedProtocol, expectedSubprotocol }), response);
    }
    


    setHandshakeComplete();
    
    final ChannelPipeline p = channel.pipeline();
    
    HttpContentDecompressor decompressor = (HttpContentDecompressor)p.get(HttpContentDecompressor.class);
    if (decompressor != null) {
      p.remove(decompressor);
    }
    

    HttpObjectAggregator aggregator = (HttpObjectAggregator)p.get(HttpObjectAggregator.class);
    if (aggregator != null) {
      p.remove(aggregator);
    }
    
    ChannelHandlerContext ctx = p.context(HttpResponseDecoder.class);
    if (ctx == null) {
      ctx = p.context(HttpClientCodec.class);
      if (ctx == null) {
        throw new IllegalStateException("ChannelPipeline does not contain an HttpRequestEncoder or HttpClientCodec");
      }
      
      final HttpClientCodec codec = (HttpClientCodec)ctx.handler();
      
      codec.removeOutboundHandler();
      
      p.addAfter(ctx.name(), "ws-decoder", newWebsocketDecoder());
      



      channel.eventLoop().execute(new Runnable()
      {
        public void run() {
          p.remove(codec);
        }
      });
    } else {
      if (p.get(HttpRequestEncoder.class) != null)
      {
        p.remove(HttpRequestEncoder.class);
      }
      final ChannelHandlerContext context = ctx;
      p.addAfter(context.name(), "ws-decoder", newWebsocketDecoder());
      



      channel.eventLoop().execute(new Runnable()
      {
        public void run() {
          p.remove(context.handler());
        }
      });
    }
  }
  









  public final ChannelFuture processHandshake(Channel channel, HttpResponse response)
  {
    return processHandshake(channel, response, channel.newPromise());
  }
  












  public final ChannelFuture processHandshake(final Channel channel, HttpResponse response, final ChannelPromise promise)
  {
    if ((response instanceof FullHttpResponse)) {
      try {
        finishHandshake(channel, (FullHttpResponse)response);
        promise.setSuccess();
      } catch (Throwable cause) {
        promise.setFailure(cause);
      }
    } else {
      ChannelPipeline p = channel.pipeline();
      ChannelHandlerContext ctx = p.context(HttpResponseDecoder.class);
      if (ctx == null) {
        ctx = p.context(HttpClientCodec.class);
        if (ctx == null) {
          return promise.setFailure(new IllegalStateException("ChannelPipeline does not contain an HttpResponseDecoder or HttpClientCodec"));
        }
      }
      




      String aggregatorName = "httpAggregator";
      p.addAfter(ctx.name(), aggregatorName, new HttpObjectAggregator(8192));
      p.addAfter(aggregatorName, "handshaker", new SimpleChannelInboundHandler()
      {
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception
        {
          ctx.pipeline().remove(this);
          try {
            finishHandshake(channel, msg);
            promise.setSuccess();
          } catch (Throwable cause) {
            promise.setFailure(cause);
          }
        }
        
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
          throws Exception
        {
          ctx.pipeline().remove(this);
          promise.setFailure(cause);
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
        ctx.fireChannelRead(ReferenceCountUtil.retain(response));
      } catch (Throwable cause) {
        promise.setFailure(cause);
      }
    }
    return promise;
  }
  





  protected abstract void verify(FullHttpResponse paramFullHttpResponse);
  





  protected abstract WebSocketFrameDecoder newWebsocketDecoder();
  





  protected abstract WebSocketFrameEncoder newWebSocketEncoder();
  




  public ChannelFuture close(Channel channel, CloseWebSocketFrame frame)
  {
    ObjectUtil.checkNotNull(channel, "channel");
    return close(channel, frame, channel.newPromise());
  }
  












  public ChannelFuture close(Channel channel, CloseWebSocketFrame frame, ChannelPromise promise)
  {
    ObjectUtil.checkNotNull(channel, "channel");
    return close0(channel, channel, frame, promise);
  }
  







  public ChannelFuture close(ChannelHandlerContext ctx, CloseWebSocketFrame frame)
  {
    ObjectUtil.checkNotNull(ctx, "ctx");
    return close(ctx, frame, ctx.newPromise());
  }
  









  public ChannelFuture close(ChannelHandlerContext ctx, CloseWebSocketFrame frame, ChannelPromise promise)
  {
    ObjectUtil.checkNotNull(ctx, "ctx");
    return close0(ctx, ctx.channel(), frame, promise);
  }
  
  private ChannelFuture close0(final ChannelOutboundInvoker invoker, final Channel channel, CloseWebSocketFrame frame, ChannelPromise promise)
  {
    invoker.writeAndFlush(frame, promise);
    final long forceCloseTimeoutMillis = this.forceCloseTimeoutMillis;
    final WebSocketClientHandshaker handshaker = this;
    if ((forceCloseTimeoutMillis <= 0L) || (!channel.isActive()) || (forceCloseInit != 0)) {
      return promise;
    }
    
    promise.addListener(new ChannelFutureListener()
    {


      public void operationComplete(ChannelFuture future)
      {

        if ((future.isSuccess()) && (channel.isActive()) && 
          (WebSocketClientHandshaker.FORCE_CLOSE_INIT_UPDATER.compareAndSet(handshaker, 0, 1))) {
          final Future<?> forceCloseFuture = channel.eventLoop().schedule(new Runnable()
          {
            public void run() {
              if (val$channel.isActive()) {
                val$invoker.close();
                forceCloseComplete = true; } } }, forceCloseTimeoutMillis, TimeUnit.MILLISECONDS);
          



          channel.closeFuture().addListener(new ChannelFutureListener()
          {
            public void operationComplete(ChannelFuture future) throws Exception {
              forceCloseFuture.cancel(false);
            }
          });
        }
      }
    });
    return promise;
  }
  


  protected String upgradeUrl(URI wsURL)
  {
    if (absoluteUpgradeUrl) {
      return wsURL.toString();
    }
    
    String path = wsURL.getRawPath();
    path = (path == null) || (path.isEmpty()) ? "/" : path;
    String query = wsURL.getRawQuery();
    return (query != null) && (!query.isEmpty()) ? path + '?' + query : path;
  }
  
  static CharSequence websocketHostValue(URI wsURL) {
    int port = wsURL.getPort();
    if (port == -1) {
      return wsURL.getHost();
    }
    String host = wsURL.getHost();
    String scheme = wsURL.getScheme();
    if (port == HttpScheme.HTTP.port()) {
      return (HttpScheme.HTTP.name().contentEquals(scheme)) || 
        (WebSocketScheme.WS.name().contentEquals(scheme)) ? host : 
        NetUtil.toSocketAddressString(host, port);
    }
    if (port == HttpScheme.HTTPS.port()) {
      return (HttpScheme.HTTPS.name().contentEquals(scheme)) || 
        (WebSocketScheme.WSS.name().contentEquals(scheme)) ? host : 
        NetUtil.toSocketAddressString(host, port);
    }
    


    return NetUtil.toSocketAddressString(host, port);
  }
  
  static CharSequence websocketOriginValue(URI wsURL) {
    String scheme = wsURL.getScheme();
    
    int port = wsURL.getPort();
    int defaultPort;
    String schemePrefix; int defaultPort; if ((WebSocketScheme.WSS.name().contentEquals(scheme)) || 
      (HttpScheme.HTTPS.name().contentEquals(scheme)) || ((scheme == null) && 
      (port == WebSocketScheme.WSS.port())))
    {
      String schemePrefix = HTTPS_SCHEME_PREFIX;
      defaultPort = WebSocketScheme.WSS.port();
    } else {
      schemePrefix = HTTP_SCHEME_PREFIX;
      defaultPort = WebSocketScheme.WS.port();
    }
    

    String host = wsURL.getHost().toLowerCase(Locale.US);
    
    if ((port != defaultPort) && (port != -1))
    {

      return schemePrefix + NetUtil.toSocketAddressString(host, port);
    }
    return schemePrefix + host;
  }
}
