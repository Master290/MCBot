package io.netty.handler.codec.http.websocketx;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.net.URI;






















public class WebSocketClientHandshaker07
  extends WebSocketClientHandshaker
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(WebSocketClientHandshaker07.class);
  



  public static final String MAGIC_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
  


  private String expectedChallengeResponseString;
  


  private final boolean allowExtensions;
  


  private final boolean performMasking;
  


  private final boolean allowMaskMismatch;
  



  public WebSocketClientHandshaker07(URI webSocketURL, WebSocketVersion version, String subprotocol, boolean allowExtensions, HttpHeaders customHeaders, int maxFramePayloadLength)
  {
    this(webSocketURL, version, subprotocol, allowExtensions, customHeaders, maxFramePayloadLength, true, false);
  }
  

























  public WebSocketClientHandshaker07(URI webSocketURL, WebSocketVersion version, String subprotocol, boolean allowExtensions, HttpHeaders customHeaders, int maxFramePayloadLength, boolean performMasking, boolean allowMaskMismatch)
  {
    this(webSocketURL, version, subprotocol, allowExtensions, customHeaders, maxFramePayloadLength, performMasking, allowMaskMismatch, 10000L);
  }
  




























  public WebSocketClientHandshaker07(URI webSocketURL, WebSocketVersion version, String subprotocol, boolean allowExtensions, HttpHeaders customHeaders, int maxFramePayloadLength, boolean performMasking, boolean allowMaskMismatch, long forceCloseTimeoutMillis)
  {
    this(webSocketURL, version, subprotocol, allowExtensions, customHeaders, maxFramePayloadLength, performMasking, allowMaskMismatch, forceCloseTimeoutMillis, false);
  }
  
































  WebSocketClientHandshaker07(URI webSocketURL, WebSocketVersion version, String subprotocol, boolean allowExtensions, HttpHeaders customHeaders, int maxFramePayloadLength, boolean performMasking, boolean allowMaskMismatch, long forceCloseTimeoutMillis, boolean absoluteUpgradeUrl)
  {
    super(webSocketURL, version, subprotocol, customHeaders, maxFramePayloadLength, forceCloseTimeoutMillis, absoluteUpgradeUrl);
    
    this.allowExtensions = allowExtensions;
    this.performMasking = performMasking;
    this.allowMaskMismatch = allowMaskMismatch;
  }
  


















  protected FullHttpRequest newHandshakeRequest()
  {
    URI wsURL = uri();
    

    byte[] nonce = WebSocketUtil.randomBytes(16);
    String key = WebSocketUtil.base64(nonce);
    
    String acceptSeed = key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    byte[] sha1 = WebSocketUtil.sha1(acceptSeed.getBytes(CharsetUtil.US_ASCII));
    expectedChallengeResponseString = WebSocketUtil.base64(sha1);
    
    if (logger.isDebugEnabled()) {
      logger.debug("WebSocket version 07 client handshake key: {}, expected response: {}", key, expectedChallengeResponseString);
    }
    



    FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, upgradeUrl(wsURL), Unpooled.EMPTY_BUFFER);
    
    HttpHeaders headers = request.headers();
    
    if (customHeaders != null) {
      headers.add(customHeaders);
      if (!headers.contains(HttpHeaderNames.HOST))
      {


        headers.set(HttpHeaderNames.HOST, websocketHostValue(wsURL));
      }
    } else {
      headers.set(HttpHeaderNames.HOST, websocketHostValue(wsURL));
    }
    


    headers.set(HttpHeaderNames.UPGRADE, HttpHeaderValues.WEBSOCKET).set(HttpHeaderNames.CONNECTION, HttpHeaderValues.UPGRADE).set(HttpHeaderNames.SEC_WEBSOCKET_KEY, key);
    
    if (!headers.contains(HttpHeaderNames.SEC_WEBSOCKET_ORIGIN)) {
      headers.set(HttpHeaderNames.SEC_WEBSOCKET_ORIGIN, websocketOriginValue(wsURL));
    }
    
    String expectedSubprotocol = expectedSubprotocol();
    if ((expectedSubprotocol != null) && (!expectedSubprotocol.isEmpty())) {
      headers.set(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL, expectedSubprotocol);
    }
    
    headers.set(HttpHeaderNames.SEC_WEBSOCKET_VERSION, version().toAsciiString());
    return request;
  }
  

















  protected void verify(FullHttpResponse response)
  {
    HttpResponseStatus status = response.status();
    if (!HttpResponseStatus.SWITCHING_PROTOCOLS.equals(status)) {
      throw new WebSocketClientHandshakeException("Invalid handshake response getStatus: " + status, response);
    }
    
    HttpHeaders headers = response.headers();
    CharSequence upgrade = headers.get(HttpHeaderNames.UPGRADE);
    if (!HttpHeaderValues.WEBSOCKET.contentEqualsIgnoreCase(upgrade)) {
      throw new WebSocketClientHandshakeException("Invalid handshake response upgrade: " + upgrade, response);
    }
    
    if (!headers.containsValue(HttpHeaderNames.CONNECTION, HttpHeaderValues.UPGRADE, true))
    {
      throw new WebSocketClientHandshakeException("Invalid handshake response connection: " + headers.get(HttpHeaderNames.CONNECTION), response);
    }
    
    CharSequence accept = headers.get(HttpHeaderNames.SEC_WEBSOCKET_ACCEPT);
    if ((accept == null) || (!accept.equals(expectedChallengeResponseString))) {
      throw new WebSocketClientHandshakeException(String.format("Invalid challenge. Actual: %s. Expected: %s", new Object[] { accept, expectedChallengeResponseString }), response);
    }
  }
  

  protected WebSocketFrameDecoder newWebsocketDecoder()
  {
    return new WebSocket07FrameDecoder(false, allowExtensions, maxFramePayloadLength(), allowMaskMismatch);
  }
  
  protected WebSocketFrameEncoder newWebSocketEncoder()
  {
    return new WebSocket07FrameEncoder(performMasking);
  }
  
  public WebSocketClientHandshaker07 setForceCloseTimeoutMillis(long forceCloseTimeoutMillis)
  {
    super.setForceCloseTimeoutMillis(forceCloseTimeoutMillis);
    return this;
  }
}
