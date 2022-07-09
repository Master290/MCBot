package io.netty.handler.codec.http.websocketx;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.logging.InternalLogger;


































public class WebSocketServerHandshaker08
  extends WebSocketServerHandshaker
{
  public static final String WEBSOCKET_08_ACCEPT_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
  
  public WebSocketServerHandshaker08(String webSocketURL, String subprotocols, boolean allowExtensions, int maxFramePayloadLength)
  {
    this(webSocketURL, subprotocols, allowExtensions, maxFramePayloadLength, false);
  }
  


















  public WebSocketServerHandshaker08(String webSocketURL, String subprotocols, boolean allowExtensions, int maxFramePayloadLength, boolean allowMaskMismatch)
  {
    this(webSocketURL, subprotocols, WebSocketDecoderConfig.newBuilder()
      .allowExtensions(allowExtensions)
      .maxFramePayloadLength(maxFramePayloadLength)
      .allowMaskMismatch(allowMaskMismatch)
      .build());
  }
  











  public WebSocketServerHandshaker08(String webSocketURL, String subprotocols, WebSocketDecoderConfig decoderConfig)
  {
    super(WebSocketVersion.V08, webSocketURL, subprotocols, decoderConfig);
  }
  


































  protected FullHttpResponse newHandshakeResponse(FullHttpRequest req, HttpHeaders headers)
  {
    CharSequence key = req.headers().get(HttpHeaderNames.SEC_WEBSOCKET_KEY);
    if (key == null) {
      throw new WebSocketServerHandshakeException("not a WebSocket request: missing key", req);
    }
    

    FullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.SWITCHING_PROTOCOLS, req.content().alloc().buffer(0));
    
    if (headers != null) {
      res.headers().add(headers);
    }
    
    String acceptSeed = key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    byte[] sha1 = WebSocketUtil.sha1(acceptSeed.getBytes(CharsetUtil.US_ASCII));
    String accept = WebSocketUtil.base64(sha1);
    
    if (logger.isDebugEnabled()) {
      logger.debug("WebSocket version 08 server handshake key: {}, response: {}", key, accept);
    }
    


    res.headers().set(HttpHeaderNames.UPGRADE, HttpHeaderValues.WEBSOCKET).set(HttpHeaderNames.CONNECTION, HttpHeaderValues.UPGRADE).set(HttpHeaderNames.SEC_WEBSOCKET_ACCEPT, accept);
    
    String subprotocols = req.headers().get(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL);
    if (subprotocols != null) {
      String selectedSubprotocol = selectSubprotocol(subprotocols);
      if (selectedSubprotocol == null) {
        if (logger.isDebugEnabled()) {
          logger.debug("Requested subprotocol(s) not supported: {}", subprotocols);
        }
      } else {
        res.headers().set(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL, selectedSubprotocol);
      }
    }
    return res;
  }
  
  protected WebSocketFrameDecoder newWebsocketDecoder()
  {
    return new WebSocket08FrameDecoder(decoderConfig());
  }
  
  protected WebSocketFrameEncoder newWebSocketEncoder()
  {
    return new WebSocket08FrameEncoder(false);
  }
}
