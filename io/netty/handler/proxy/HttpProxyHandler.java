package io.netty.handler.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.base64.Base64;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.ObjectUtil;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
























public final class HttpProxyHandler
  extends ProxyHandler
{
  private static final String PROTOCOL = "http";
  private static final String AUTH_BASIC = "basic";
  private final HttpClientCodecWrapper codecWrapper = new HttpClientCodecWrapper(null);
  private final String username;
  private final String password;
  private final CharSequence authorization;
  private final HttpHeaders outboundHeaders;
  private final boolean ignoreDefaultPortsInConnectHostHeader;
  private HttpResponseStatus status;
  private HttpHeaders inboundHeaders;
  
  public HttpProxyHandler(SocketAddress proxyAddress) {
    this(proxyAddress, null);
  }
  
  public HttpProxyHandler(SocketAddress proxyAddress, HttpHeaders headers) {
    this(proxyAddress, headers, false);
  }
  

  public HttpProxyHandler(SocketAddress proxyAddress, HttpHeaders headers, boolean ignoreDefaultPortsInConnectHostHeader)
  {
    super(proxyAddress);
    username = null;
    password = null;
    authorization = null;
    outboundHeaders = headers;
    this.ignoreDefaultPortsInConnectHostHeader = ignoreDefaultPortsInConnectHostHeader;
  }
  
  public HttpProxyHandler(SocketAddress proxyAddress, String username, String password) {
    this(proxyAddress, username, password, null);
  }
  
  public HttpProxyHandler(SocketAddress proxyAddress, String username, String password, HttpHeaders headers)
  {
    this(proxyAddress, username, password, headers, false);
  }
  



  public HttpProxyHandler(SocketAddress proxyAddress, String username, String password, HttpHeaders headers, boolean ignoreDefaultPortsInConnectHostHeader)
  {
    super(proxyAddress);
    this.username = ((String)ObjectUtil.checkNotNull(username, "username"));
    this.password = ((String)ObjectUtil.checkNotNull(password, "password"));
    
    ByteBuf authz = Unpooled.copiedBuffer(username + ':' + password, CharsetUtil.UTF_8);
    try
    {
      authzBase64 = Base64.encode(authz, false);
    } finally { ByteBuf authzBase64;
      authz.release();
    }
    try {
      authorization = new AsciiString("Basic " + authzBase64.toString(CharsetUtil.US_ASCII));
    } finally { ByteBuf authzBase64;
      authzBase64.release();
    }
    
    outboundHeaders = headers;
    this.ignoreDefaultPortsInConnectHostHeader = ignoreDefaultPortsInConnectHostHeader;
  }
  
  public String protocol()
  {
    return "http";
  }
  
  public String authScheme()
  {
    return authorization != null ? "basic" : "none";
  }
  
  public String username() {
    return username;
  }
  
  public String password() {
    return password;
  }
  
  protected void addCodec(ChannelHandlerContext ctx) throws Exception
  {
    ChannelPipeline p = ctx.pipeline();
    String name = ctx.name();
    p.addBefore(name, null, codecWrapper);
  }
  
  protected void removeEncoder(ChannelHandlerContext ctx) throws Exception
  {
    codecWrapper.codec.removeOutboundHandler();
  }
  
  protected void removeDecoder(ChannelHandlerContext ctx) throws Exception
  {
    codecWrapper.codec.removeInboundHandler();
  }
  
  protected Object newInitialMessage(ChannelHandlerContext ctx) throws Exception
  {
    InetSocketAddress raddr = (InetSocketAddress)destinationAddress();
    
    String hostString = HttpUtil.formatHostnameForHttp(raddr);
    int port = raddr.getPort();
    String url = hostString + ":" + port;
    String hostHeader = (ignoreDefaultPortsInConnectHostHeader) && ((port == 80) || (port == 443)) ? hostString : url;
    


    FullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.CONNECT, url, Unpooled.EMPTY_BUFFER, false);
    



    req.headers().set(HttpHeaderNames.HOST, hostHeader);
    
    if (authorization != null) {
      req.headers().set(HttpHeaderNames.PROXY_AUTHORIZATION, authorization);
    }
    
    if (outboundHeaders != null) {
      req.headers().add(outboundHeaders);
    }
    
    return req;
  }
  
  protected boolean handleResponse(ChannelHandlerContext ctx, Object response) throws Exception
  {
    if ((response instanceof HttpResponse)) {
      if (status != null) {
        throw new HttpProxyConnectException(exceptionMessage("too many responses"), null);
      }
      HttpResponse res = (HttpResponse)response;
      status = res.status();
      inboundHeaders = res.headers();
    }
    
    boolean finished = response instanceof LastHttpContent;
    if (finished) {
      if (status == null) {
        throw new HttpProxyConnectException(exceptionMessage("missing response"), inboundHeaders);
      }
      if (status.code() != 200) {
        throw new HttpProxyConnectException(exceptionMessage("status: " + status), inboundHeaders);
      }
    }
    
    return finished;
  }
  


  public static final class HttpProxyConnectException
    extends ProxyConnectException
  {
    private static final long serialVersionUID = -8824334609292146066L;
    

    private final HttpHeaders headers;
    

    public HttpProxyConnectException(String message, HttpHeaders headers)
    {
      super();
      this.headers = headers;
    }
    


    public HttpHeaders headers()
    {
      return headers;
    }
  }
  
  private static final class HttpClientCodecWrapper implements ChannelInboundHandler, ChannelOutboundHandler {
    final HttpClientCodec codec = new HttpClientCodec();
    
    private HttpClientCodecWrapper() {}
    
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception { codec.handlerAdded(ctx); }
    
    public void handlerRemoved(ChannelHandlerContext ctx)
      throws Exception
    {
      codec.handlerRemoved(ctx);
    }
    
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
      codec.exceptionCaught(ctx, cause);
    }
    
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception
    {
      codec.channelRegistered(ctx);
    }
    
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception
    {
      codec.channelUnregistered(ctx);
    }
    
    public void channelActive(ChannelHandlerContext ctx) throws Exception
    {
      codec.channelActive(ctx);
    }
    
    public void channelInactive(ChannelHandlerContext ctx) throws Exception
    {
      codec.channelInactive(ctx);
    }
    
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
    {
      codec.channelRead(ctx, msg);
    }
    
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception
    {
      codec.channelReadComplete(ctx);
    }
    
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception
    {
      codec.userEventTriggered(ctx, evt);
    }
    
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception
    {
      codec.channelWritabilityChanged(ctx);
    }
    
    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise)
      throws Exception
    {
      codec.bind(ctx, localAddress, promise);
    }
    
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise)
      throws Exception
    {
      codec.connect(ctx, remoteAddress, localAddress, promise);
    }
    
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception
    {
      codec.disconnect(ctx, promise);
    }
    
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception
    {
      codec.close(ctx, promise);
    }
    
    public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception
    {
      codec.deregister(ctx, promise);
    }
    
    public void read(ChannelHandlerContext ctx) throws Exception
    {
      codec.read(ctx);
    }
    
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception
    {
      codec.write(ctx, msg, promise);
    }
    
    public void flush(ChannelHandlerContext ctx) throws Exception
    {
      codec.flush(ctx);
    }
  }
}
