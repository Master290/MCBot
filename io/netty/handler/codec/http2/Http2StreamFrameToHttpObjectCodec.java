package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpScheme;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.AsciiString;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import java.util.List;


























@ChannelHandler.Sharable
public class Http2StreamFrameToHttpObjectCodec
  extends MessageToMessageCodec<Http2StreamFrame, HttpObject>
{
  private static final AttributeKey<HttpScheme> SCHEME_ATTR_KEY = AttributeKey.valueOf(HttpScheme.class, "STREAMFRAMECODEC_SCHEME");
  
  private final boolean isServer;
  private final boolean validateHeaders;
  
  public Http2StreamFrameToHttpObjectCodec(boolean isServer, boolean validateHeaders)
  {
    this.isServer = isServer;
    this.validateHeaders = validateHeaders;
  }
  
  public Http2StreamFrameToHttpObjectCodec(boolean isServer) {
    this(isServer, true);
  }
  
  public boolean acceptInboundMessage(Object msg) throws Exception
  {
    return ((msg instanceof Http2HeadersFrame)) || ((msg instanceof Http2DataFrame));
  }
  
  protected void decode(ChannelHandlerContext ctx, Http2StreamFrame frame, List<Object> out) throws Exception
  {
    if ((frame instanceof Http2HeadersFrame)) {
      Http2HeadersFrame headersFrame = (Http2HeadersFrame)frame;
      Http2Headers headers = headersFrame.headers();
      Http2FrameStream stream = headersFrame.stream();
      int id = stream == null ? 0 : stream.id();
      
      CharSequence status = headers.status();
      


      if ((null != status) && (HttpResponseStatus.CONTINUE.codeAsText().contentEquals(status))) {
        FullHttpMessage fullMsg = newFullMessage(id, headers, ctx.alloc());
        out.add(fullMsg);
        return;
      }
      
      if (headersFrame.isEndStream()) {
        if ((headers.method() == null) && (status == null)) {
          LastHttpContent last = new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER, validateHeaders);
          HttpConversionUtil.addHttp2ToHttpHeaders(id, headers, last.trailingHeaders(), HttpVersion.HTTP_1_1, true, true);
          
          out.add(last);
        } else {
          FullHttpMessage full = newFullMessage(id, headers, ctx.alloc());
          out.add(full);
        }
      } else {
        HttpMessage req = newMessage(id, headers);
        if (!HttpUtil.isContentLengthSet(req)) {
          req.headers().add(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
        }
        out.add(req);
      }
    } else if ((frame instanceof Http2DataFrame)) {
      Http2DataFrame dataFrame = (Http2DataFrame)frame;
      if (dataFrame.isEndStream()) {
        out.add(new DefaultLastHttpContent(dataFrame.content().retain(), validateHeaders));
      } else {
        out.add(new DefaultHttpContent(dataFrame.content().retain()));
      }
    }
  }
  
  private void encodeLastContent(LastHttpContent last, List<Object> out) {
    boolean needFiller = (!(last instanceof FullHttpMessage)) && (last.trailingHeaders().isEmpty());
    if ((last.content().isReadable()) || (needFiller)) {
      out.add(new DefaultHttp2DataFrame(last.content().retain(), last.trailingHeaders().isEmpty()));
    }
    if (!last.trailingHeaders().isEmpty()) {
      Http2Headers headers = HttpConversionUtil.toHttp2Headers(last.trailingHeaders(), validateHeaders);
      out.add(new DefaultHttp2HeadersFrame(headers, true));
    }
  }
  













  protected void encode(ChannelHandlerContext ctx, HttpObject obj, List<Object> out)
    throws Exception
  {
    if ((obj instanceof HttpResponse)) {
      HttpResponse res = (HttpResponse)obj;
      if (res.status().equals(HttpResponseStatus.CONTINUE)) {
        if ((res instanceof FullHttpResponse)) {
          Http2Headers headers = toHttp2Headers(ctx, res);
          out.add(new DefaultHttp2HeadersFrame(headers, false));
          return;
        }
        throw new EncoderException(HttpResponseStatus.CONTINUE + " must be a FullHttpResponse");
      }
    }
    


    if ((obj instanceof HttpMessage)) {
      Http2Headers headers = toHttp2Headers(ctx, (HttpMessage)obj);
      boolean noMoreFrames = false;
      if ((obj instanceof FullHttpMessage)) {
        FullHttpMessage full = (FullHttpMessage)obj;
        noMoreFrames = (!full.content().isReadable()) && (full.trailingHeaders().isEmpty());
      }
      
      out.add(new DefaultHttp2HeadersFrame(headers, noMoreFrames));
    }
    
    if ((obj instanceof LastHttpContent)) {
      LastHttpContent last = (LastHttpContent)obj;
      encodeLastContent(last, out);
    } else if ((obj instanceof HttpContent)) {
      HttpContent cont = (HttpContent)obj;
      out.add(new DefaultHttp2DataFrame(cont.content().retain(), false));
    }
  }
  
  private Http2Headers toHttp2Headers(ChannelHandlerContext ctx, HttpMessage msg) {
    if ((msg instanceof HttpRequest)) {
      msg.headers().set(HttpConversionUtil.ExtensionHeaderNames.SCHEME
        .text(), 
        connectionScheme(ctx));
    }
    
    return HttpConversionUtil.toHttp2Headers(msg, validateHeaders);
  }
  
  private HttpMessage newMessage(int id, Http2Headers headers) throws Http2Exception
  {
    return isServer ? 
      HttpConversionUtil.toHttpRequest(id, headers, validateHeaders) : 
      HttpConversionUtil.toHttpResponse(id, headers, validateHeaders);
  }
  
  private FullHttpMessage newFullMessage(int id, Http2Headers headers, ByteBufAllocator alloc)
    throws Http2Exception
  {
    return isServer ? 
      HttpConversionUtil.toFullHttpRequest(id, headers, alloc, validateHeaders) : 
      HttpConversionUtil.toFullHttpResponse(id, headers, alloc, validateHeaders);
  }
  
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception
  {
    super.handlerAdded(ctx);
    





    Attribute<HttpScheme> schemeAttribute = connectionSchemeAttribute(ctx);
    if (schemeAttribute.get() == null) {
      HttpScheme scheme = isSsl(ctx) ? HttpScheme.HTTPS : HttpScheme.HTTP;
      schemeAttribute.set(scheme);
    }
  }
  
  protected boolean isSsl(ChannelHandlerContext ctx) {
    Channel connChannel = connectionChannel(ctx);
    return null != connChannel.pipeline().get(SslHandler.class);
  }
  
  private static HttpScheme connectionScheme(ChannelHandlerContext ctx) {
    HttpScheme scheme = (HttpScheme)connectionSchemeAttribute(ctx).get();
    return scheme == null ? HttpScheme.HTTP : scheme;
  }
  
  private static Attribute<HttpScheme> connectionSchemeAttribute(ChannelHandlerContext ctx) {
    Channel ch = connectionChannel(ctx);
    return ch.attr(SCHEME_ATTR_KEY);
  }
  
  private static Channel connectionChannel(ChannelHandlerContext ctx) {
    Channel ch = ctx.channel();
    return (ch instanceof Http2StreamChannel) ? ch.parent() : ch;
  }
}
