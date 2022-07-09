package io.netty.handler.codec.http.websocketx.extensions;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.internal.ObjectUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

































public class WebSocketServerExtensionHandler
  extends ChannelDuplexHandler
{
  private final List<WebSocketServerExtensionHandshaker> extensionHandshakers;
  private List<WebSocketServerExtension> validExtensions;
  
  public WebSocketServerExtensionHandler(WebSocketServerExtensionHandshaker... extensionHandshakers)
  {
    this.extensionHandshakers = Arrays.asList(ObjectUtil.checkNonEmpty(extensionHandshakers, "extensionHandshakers"));
  }
  
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    int rsv;
    if ((msg instanceof HttpRequest)) {
      HttpRequest request = (HttpRequest)msg;
      
      if (WebSocketExtensionUtil.isWebsocketUpgrade(request.headers())) {
        String extensionsHeader = request.headers().getAsString(HttpHeaderNames.SEC_WEBSOCKET_EXTENSIONS);
        
        if (extensionsHeader != null)
        {
          List<WebSocketExtensionData> extensions = WebSocketExtensionUtil.extractExtensions(extensionsHeader);
          rsv = 0;
          
          for (WebSocketExtensionData extensionData : extensions)
          {
            Iterator<WebSocketServerExtensionHandshaker> extensionHandshakersIterator = extensionHandshakers.iterator();
            WebSocketServerExtension validExtension = null;
            
            while ((validExtension == null) && (extensionHandshakersIterator.hasNext()))
            {
              WebSocketServerExtensionHandshaker extensionHandshaker = (WebSocketServerExtensionHandshaker)extensionHandshakersIterator.next();
              validExtension = extensionHandshaker.handshakeExtension(extensionData);
            }
            
            if ((validExtension != null) && ((validExtension.rsv() & rsv) == 0)) {
              if (validExtensions == null) {
                validExtensions = new ArrayList(1);
              }
              rsv |= validExtension.rsv();
              validExtensions.add(validExtension);
            }
          }
        }
      }
    }
    
    super.channelRead(ctx, msg);
  }
  
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception
  {
    if ((msg instanceof HttpResponse)) {
      HttpResponse httpResponse = (HttpResponse)msg;
      

      if (HttpResponseStatus.SWITCHING_PROTOCOLS.equals(httpResponse.status())) {
        handlePotentialUpgrade(ctx, promise, httpResponse);
      }
    }
    
    super.write(ctx, msg, promise);
  }
  
  private void handlePotentialUpgrade(final ChannelHandlerContext ctx, ChannelPromise promise, HttpResponse httpResponse)
  {
    HttpHeaders headers = httpResponse.headers();
    
    if (WebSocketExtensionUtil.isWebsocketUpgrade(headers))
    {
      if (validExtensions != null) {
        String headerValue = headers.getAsString(HttpHeaderNames.SEC_WEBSOCKET_EXTENSIONS);
        
        List<WebSocketExtensionData> extraExtensions = new ArrayList(extensionHandshakers.size());
        for (WebSocketServerExtension extension : validExtensions) {
          extraExtensions.add(extension.newReponseData());
        }
        
        String newHeaderValue = WebSocketExtensionUtil.computeMergeExtensionsHeaderValue(headerValue, extraExtensions);
        promise.addListener(new ChannelFutureListener()
        {
          public void operationComplete(ChannelFuture future) {
            if (future.isSuccess()) {
              for (WebSocketServerExtension extension : validExtensions) {
                WebSocketExtensionDecoder decoder = extension.newExtensionDecoder();
                WebSocketExtensionEncoder encoder = extension.newExtensionEncoder();
                String name = ctx.name();
                ctx.pipeline()
                  .addAfter(name, decoder.getClass().getName(), decoder)
                  .addAfter(name, encoder.getClass().getName(), encoder);
              }
            }
          }
        });
        
        if (newHeaderValue != null) {
          headers.set(HttpHeaderNames.SEC_WEBSOCKET_EXTENSIONS, newHeaderValue);
        }
      }
      
      promise.addListener(new ChannelFutureListener()
      {
        public void operationComplete(ChannelFuture future) {
          if (future.isSuccess()) {
            ctx.pipeline().remove(WebSocketServerExtensionHandler.this);
          }
        }
      });
    }
  }
}
