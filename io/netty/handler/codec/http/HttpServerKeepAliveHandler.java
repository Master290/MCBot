package io.netty.handler.codec.http;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;







































public class HttpServerKeepAliveHandler
  extends ChannelDuplexHandler
{
  private static final String MULTIPART_PREFIX = "multipart";
  private boolean persistentConnection = true;
  private int pendingResponses;
  
  public HttpServerKeepAliveHandler() {}
  
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
  {
    if ((msg instanceof HttpRequest)) {
      HttpRequest request = (HttpRequest)msg;
      if (persistentConnection) {
        pendingResponses += 1;
        persistentConnection = HttpUtil.isKeepAlive(request);
      }
    }
    super.channelRead(ctx, msg);
  }
  
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
    throws Exception
  {
    if ((msg instanceof HttpResponse)) {
      HttpResponse response = (HttpResponse)msg;
      trackResponse(response);
      
      if ((!HttpUtil.isKeepAlive(response)) || (!isSelfDefinedMessageLength(response)))
      {
        pendingResponses = 0;
        persistentConnection = false;
      }
      
      if (!shouldKeepAlive()) {
        HttpUtil.setKeepAlive(response, false);
      }
    }
    if (((msg instanceof LastHttpContent)) && (!shouldKeepAlive())) {
      promise = promise.unvoid().addListener(ChannelFutureListener.CLOSE);
    }
    super.write(ctx, msg, promise);
  }
  
  private void trackResponse(HttpResponse response) {
    if (!isInformational(response)) {
      pendingResponses -= 1;
    }
  }
  
  private boolean shouldKeepAlive() {
    return (pendingResponses != 0) || (persistentConnection);
  }
  













  private static boolean isSelfDefinedMessageLength(HttpResponse response)
  {
    return (HttpUtil.isContentLengthSet(response)) || (HttpUtil.isTransferEncodingChunked(response)) || (isMultipart(response)) || 
      (isInformational(response)) || (response.status().code() == HttpResponseStatus.NO_CONTENT.code());
  }
  
  private static boolean isInformational(HttpResponse response) {
    return response.status().codeClass() == HttpStatusClass.INFORMATIONAL;
  }
  
  private static boolean isMultipart(HttpResponse response) {
    String contentType = response.headers().get(HttpHeaderNames.CONTENT_TYPE);
    return (contentType != null) && 
      (contentType.regionMatches(true, 0, "multipart", 0, "multipart".length()));
  }
}
