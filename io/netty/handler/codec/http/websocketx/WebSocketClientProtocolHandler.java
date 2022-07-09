package io.netty.handler.codec.http.websocketx;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.internal.ObjectUtil;
import java.net.URI;
import java.util.List;






































public class WebSocketClientProtocolHandler
  extends WebSocketProtocolHandler
{
  private final WebSocketClientHandshaker handshaker;
  private final WebSocketClientProtocolConfig clientConfig;
  
  public WebSocketClientHandshaker handshaker()
  {
    return handshaker;
  }
  





  public static enum ClientHandshakeStateEvent
  {
    HANDSHAKE_TIMEOUT, 
    



    HANDSHAKE_ISSUED, 
    



    HANDSHAKE_COMPLETE;
    


    private ClientHandshakeStateEvent() {}
  }
  

  public WebSocketClientProtocolHandler(WebSocketClientProtocolConfig clientConfig)
  {
    super(((WebSocketClientProtocolConfig)ObjectUtil.checkNotNull(clientConfig, "clientConfig")).dropPongFrames(), clientConfig
      .sendCloseFrame(), clientConfig.forceCloseTimeoutMillis());
    handshaker = WebSocketClientHandshakerFactory.newHandshaker(clientConfig
      .webSocketUri(), clientConfig
      .version(), clientConfig
      .subprotocol(), clientConfig
      .allowExtensions(), clientConfig
      .customHeaders(), clientConfig
      .maxFramePayloadLength(), clientConfig
      .performMasking(), clientConfig
      .allowMaskMismatch(), clientConfig
      .forceCloseTimeoutMillis(), clientConfig
      .absoluteUpgradeUrl());
    
    this.clientConfig = clientConfig;
  }
  


























  public WebSocketClientProtocolHandler(URI webSocketURL, WebSocketVersion version, String subprotocol, boolean allowExtensions, HttpHeaders customHeaders, int maxFramePayloadLength, boolean handleCloseFrames, boolean performMasking, boolean allowMaskMismatch)
  {
    this(webSocketURL, version, subprotocol, allowExtensions, customHeaders, maxFramePayloadLength, handleCloseFrames, performMasking, allowMaskMismatch, 10000L);
  }
  






























  public WebSocketClientProtocolHandler(URI webSocketURL, WebSocketVersion version, String subprotocol, boolean allowExtensions, HttpHeaders customHeaders, int maxFramePayloadLength, boolean handleCloseFrames, boolean performMasking, boolean allowMaskMismatch, long handshakeTimeoutMillis)
  {
    this(WebSocketClientHandshakerFactory.newHandshaker(webSocketURL, version, subprotocol, allowExtensions, customHeaders, maxFramePayloadLength, performMasking, allowMaskMismatch), handleCloseFrames, handshakeTimeoutMillis);
  }
  





















  public WebSocketClientProtocolHandler(URI webSocketURL, WebSocketVersion version, String subprotocol, boolean allowExtensions, HttpHeaders customHeaders, int maxFramePayloadLength, boolean handleCloseFrames)
  {
    this(webSocketURL, version, subprotocol, allowExtensions, customHeaders, maxFramePayloadLength, handleCloseFrames, 10000L);
  }
  






















  public WebSocketClientProtocolHandler(URI webSocketURL, WebSocketVersion version, String subprotocol, boolean allowExtensions, HttpHeaders customHeaders, int maxFramePayloadLength, boolean handleCloseFrames, long handshakeTimeoutMillis)
  {
    this(webSocketURL, version, subprotocol, allowExtensions, customHeaders, maxFramePayloadLength, handleCloseFrames, true, false, handshakeTimeoutMillis);
  }
  

















  public WebSocketClientProtocolHandler(URI webSocketURL, WebSocketVersion version, String subprotocol, boolean allowExtensions, HttpHeaders customHeaders, int maxFramePayloadLength)
  {
    this(webSocketURL, version, subprotocol, allowExtensions, customHeaders, maxFramePayloadLength, 10000L);
  }
  




















  public WebSocketClientProtocolHandler(URI webSocketURL, WebSocketVersion version, String subprotocol, boolean allowExtensions, HttpHeaders customHeaders, int maxFramePayloadLength, long handshakeTimeoutMillis)
  {
    this(webSocketURL, version, subprotocol, allowExtensions, customHeaders, maxFramePayloadLength, true, handshakeTimeoutMillis);
  }
  









  public WebSocketClientProtocolHandler(WebSocketClientHandshaker handshaker, boolean handleCloseFrames)
  {
    this(handshaker, handleCloseFrames, 10000L);
  }
  












  public WebSocketClientProtocolHandler(WebSocketClientHandshaker handshaker, boolean handleCloseFrames, long handshakeTimeoutMillis)
  {
    this(handshaker, handleCloseFrames, true, handshakeTimeoutMillis);
  }
  











  public WebSocketClientProtocolHandler(WebSocketClientHandshaker handshaker, boolean handleCloseFrames, boolean dropPongFrames)
  {
    this(handshaker, handleCloseFrames, dropPongFrames, 10000L);
  }
  














  public WebSocketClientProtocolHandler(WebSocketClientHandshaker handshaker, boolean handleCloseFrames, boolean dropPongFrames, long handshakeTimeoutMillis)
  {
    super(dropPongFrames);
    this.handshaker = handshaker;
    


    clientConfig = WebSocketClientProtocolConfig.newBuilder().handleCloseFrames(handleCloseFrames).handshakeTimeoutMillis(handshakeTimeoutMillis).build();
  }
  






  public WebSocketClientProtocolHandler(WebSocketClientHandshaker handshaker)
  {
    this(handshaker, 10000L);
  }
  









  public WebSocketClientProtocolHandler(WebSocketClientHandshaker handshaker, long handshakeTimeoutMillis)
  {
    this(handshaker, true, handshakeTimeoutMillis);
  }
  
  protected void decode(ChannelHandlerContext ctx, WebSocketFrame frame, List<Object> out) throws Exception
  {
    if ((clientConfig.handleCloseFrames()) && ((frame instanceof CloseWebSocketFrame))) {
      ctx.close();
      return;
    }
    super.decode(ctx, frame, out);
  }
  
  protected WebSocketClientHandshakeException buildHandshakeException(String message)
  {
    return new WebSocketClientHandshakeException(message);
  }
  
  public void handlerAdded(ChannelHandlerContext ctx)
  {
    ChannelPipeline cp = ctx.pipeline();
    if (cp.get(WebSocketClientProtocolHandshakeHandler.class) == null)
    {
      ctx.pipeline().addBefore(ctx.name(), WebSocketClientProtocolHandshakeHandler.class.getName(), new WebSocketClientProtocolHandshakeHandler(handshaker, clientConfig
        .handshakeTimeoutMillis()));
    }
    if (cp.get(Utf8FrameValidator.class) == null)
    {
      ctx.pipeline().addBefore(ctx.name(), Utf8FrameValidator.class.getName(), new Utf8FrameValidator());
    }
  }
}
