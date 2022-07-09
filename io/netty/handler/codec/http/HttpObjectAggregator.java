package io.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.MessageAggregator;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;































































public class HttpObjectAggregator
  extends MessageAggregator<HttpObject, HttpMessage, HttpContent, FullHttpMessage>
{
  private static final InternalLogger logger;
  private static final FullHttpResponse CONTINUE;
  private static final FullHttpResponse EXPECTATION_FAILED;
  private static final FullHttpResponse TOO_LARGE_CLOSE;
  private static final FullHttpResponse TOO_LARGE;
  private final boolean closeOnExpectationFailed;
  
  static
  {
    logger = InternalLoggerFactory.getInstance(HttpObjectAggregator.class);
    CONTINUE = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE, Unpooled.EMPTY_BUFFER);
    
    EXPECTATION_FAILED = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.EXPECTATION_FAILED, Unpooled.EMPTY_BUFFER);
    
    TOO_LARGE_CLOSE = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE, Unpooled.EMPTY_BUFFER);
    
    TOO_LARGE = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE, Unpooled.EMPTY_BUFFER);
    


    EXPECTATION_FAILED.headers().set(HttpHeaderNames.CONTENT_LENGTH, Integer.valueOf(0));
    TOO_LARGE.headers().set(HttpHeaderNames.CONTENT_LENGTH, Integer.valueOf(0));
    
    TOO_LARGE_CLOSE.headers().set(HttpHeaderNames.CONTENT_LENGTH, Integer.valueOf(0));
    TOO_LARGE_CLOSE.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
  }
  







  public HttpObjectAggregator(int maxContentLength)
  {
    this(maxContentLength, false);
  }
  








  public HttpObjectAggregator(int maxContentLength, boolean closeOnExpectationFailed)
  {
    super(maxContentLength);
    this.closeOnExpectationFailed = closeOnExpectationFailed;
  }
  
  protected boolean isStartMessage(HttpObject msg) throws Exception
  {
    return msg instanceof HttpMessage;
  }
  
  protected boolean isContentMessage(HttpObject msg) throws Exception
  {
    return msg instanceof HttpContent;
  }
  
  protected boolean isLastContentMessage(HttpContent msg) throws Exception
  {
    return msg instanceof LastHttpContent;
  }
  
  protected boolean isAggregated(HttpObject msg) throws Exception
  {
    return msg instanceof FullHttpMessage;
  }
  
  protected boolean isContentLengthInvalid(HttpMessage start, int maxContentLength)
  {
    try {
      return HttpUtil.getContentLength(start, -1L) > maxContentLength;
    } catch (NumberFormatException e) {}
    return false;
  }
  
  private static Object continueResponse(HttpMessage start, int maxContentLength, ChannelPipeline pipeline)
  {
    if (HttpUtil.isUnsupportedExpectation(start))
    {
      pipeline.fireUserEventTriggered(HttpExpectationFailedEvent.INSTANCE);
      return EXPECTATION_FAILED.retainedDuplicate(); }
    if (HttpUtil.is100ContinueExpected(start))
    {
      if (HttpUtil.getContentLength(start, -1L) <= maxContentLength) {
        return CONTINUE.retainedDuplicate();
      }
      pipeline.fireUserEventTriggered(HttpExpectationFailedEvent.INSTANCE);
      return TOO_LARGE.retainedDuplicate();
    }
    
    return null;
  }
  
  protected Object newContinueResponse(HttpMessage start, int maxContentLength, ChannelPipeline pipeline)
  {
    Object response = continueResponse(start, maxContentLength, pipeline);
    

    if (response != null) {
      start.headers().remove(HttpHeaderNames.EXPECT);
    }
    return response;
  }
  
  protected boolean closeAfterContinueResponse(Object msg)
  {
    return (closeOnExpectationFailed) && (ignoreContentAfterContinueResponse(msg));
  }
  
  protected boolean ignoreContentAfterContinueResponse(Object msg)
  {
    if ((msg instanceof HttpResponse)) {
      HttpResponse httpResponse = (HttpResponse)msg;
      return httpResponse.status().codeClass().equals(HttpStatusClass.CLIENT_ERROR);
    }
    return false;
  }
  
  protected FullHttpMessage beginAggregation(HttpMessage start, ByteBuf content) throws Exception
  {
    assert (!(start instanceof FullHttpMessage));
    
    HttpUtil.setTransferEncodingChunked(start, false);
    
    AggregatedFullHttpMessage ret;
    if ((start instanceof HttpRequest)) {
      ret = new AggregatedFullHttpRequest((HttpRequest)start, content, null); } else { AggregatedFullHttpMessage ret;
      if ((start instanceof HttpResponse)) {
        ret = new AggregatedFullHttpResponse((HttpResponse)start, content, null);
      } else
        throw new Error(); }
    AggregatedFullHttpMessage ret;
    return ret;
  }
  
  protected void aggregate(FullHttpMessage aggregated, HttpContent content) throws Exception
  {
    if ((content instanceof LastHttpContent))
    {
      ((AggregatedFullHttpMessage)aggregated).setTrailingHeaders(((LastHttpContent)content).trailingHeaders());
    }
  }
  





  protected void finishAggregation(FullHttpMessage aggregated)
    throws Exception
  {
    if (!HttpUtil.isContentLengthSet(aggregated)) {
      aggregated.headers().set(HttpHeaderNames.CONTENT_LENGTH, 
      
        String.valueOf(aggregated.content().readableBytes()));
    }
  }
  
  protected void handleOversizedMessage(final ChannelHandlerContext ctx, HttpMessage oversized) throws Exception
  {
    if ((oversized instanceof HttpRequest))
    {



      if (((oversized instanceof FullHttpMessage)) || (
        (!HttpUtil.is100ContinueExpected(oversized)) && (!HttpUtil.isKeepAlive(oversized)))) {
        ChannelFuture future = ctx.writeAndFlush(TOO_LARGE_CLOSE.retainedDuplicate());
        future.addListener(new ChannelFutureListener()
        {
          public void operationComplete(ChannelFuture future) throws Exception {
            if (!future.isSuccess()) {
              HttpObjectAggregator.logger.debug("Failed to send a 413 Request Entity Too Large.", future.cause());
            }
            ctx.close();
          }
        });
      } else {
        ctx.writeAndFlush(TOO_LARGE.retainedDuplicate()).addListener(new ChannelFutureListener()
        {
          public void operationComplete(ChannelFuture future) throws Exception {
            if (!future.isSuccess()) {
              HttpObjectAggregator.logger.debug("Failed to send a 413 Request Entity Too Large.", future.cause());
              ctx.close();
            }
          }
        });
      }
    } else { if ((oversized instanceof HttpResponse)) {
        ctx.close();
        throw new TooLongFrameException("Response entity too large: " + oversized);
      }
      throw new IllegalStateException();
    }
  }
  
  private static abstract class AggregatedFullHttpMessage implements FullHttpMessage {
    protected final HttpMessage message;
    private final ByteBuf content;
    private HttpHeaders trailingHeaders;
    
    AggregatedFullHttpMessage(HttpMessage message, ByteBuf content, HttpHeaders trailingHeaders) {
      this.message = message;
      this.content = content;
      this.trailingHeaders = trailingHeaders;
    }
    
    public HttpHeaders trailingHeaders()
    {
      HttpHeaders trailingHeaders = this.trailingHeaders;
      if (trailingHeaders == null) {
        return EmptyHttpHeaders.INSTANCE;
      }
      return trailingHeaders;
    }
    
    void setTrailingHeaders(HttpHeaders trailingHeaders)
    {
      this.trailingHeaders = trailingHeaders;
    }
    
    public HttpVersion getProtocolVersion()
    {
      return message.protocolVersion();
    }
    
    public HttpVersion protocolVersion()
    {
      return message.protocolVersion();
    }
    
    public FullHttpMessage setProtocolVersion(HttpVersion version)
    {
      message.setProtocolVersion(version);
      return this;
    }
    
    public HttpHeaders headers()
    {
      return message.headers();
    }
    
    public DecoderResult decoderResult()
    {
      return message.decoderResult();
    }
    
    public DecoderResult getDecoderResult()
    {
      return message.decoderResult();
    }
    
    public void setDecoderResult(DecoderResult result)
    {
      message.setDecoderResult(result);
    }
    
    public ByteBuf content()
    {
      return content;
    }
    
    public int refCnt()
    {
      return content.refCnt();
    }
    
    public FullHttpMessage retain()
    {
      content.retain();
      return this;
    }
    
    public FullHttpMessage retain(int increment)
    {
      content.retain(increment);
      return this;
    }
    
    public FullHttpMessage touch(Object hint)
    {
      content.touch(hint);
      return this;
    }
    
    public FullHttpMessage touch()
    {
      content.touch();
      return this;
    }
    
    public boolean release()
    {
      return content.release();
    }
    
    public boolean release(int decrement)
    {
      return content.release(decrement);
    }
    
    public abstract FullHttpMessage copy();
    
    public abstract FullHttpMessage duplicate();
    
    public abstract FullHttpMessage retainedDuplicate();
  }
  
  private static final class AggregatedFullHttpRequest
    extends HttpObjectAggregator.AggregatedFullHttpMessage
    implements FullHttpRequest
  {
    AggregatedFullHttpRequest(HttpRequest request, ByteBuf content, HttpHeaders trailingHeaders)
    {
      super(content, trailingHeaders);
    }
    
    public FullHttpRequest copy()
    {
      return replace(content().copy());
    }
    
    public FullHttpRequest duplicate()
    {
      return replace(content().duplicate());
    }
    
    public FullHttpRequest retainedDuplicate()
    {
      return replace(content().retainedDuplicate());
    }
    

    public FullHttpRequest replace(ByteBuf content)
    {
      DefaultFullHttpRequest dup = new DefaultFullHttpRequest(protocolVersion(), method(), uri(), content, headers().copy(), trailingHeaders().copy());
      dup.setDecoderResult(decoderResult());
      return dup;
    }
    
    public FullHttpRequest retain(int increment)
    {
      super.retain(increment);
      return this;
    }
    
    public FullHttpRequest retain()
    {
      super.retain();
      return this;
    }
    
    public FullHttpRequest touch()
    {
      super.touch();
      return this;
    }
    
    public FullHttpRequest touch(Object hint)
    {
      super.touch(hint);
      return this;
    }
    
    public FullHttpRequest setMethod(HttpMethod method)
    {
      ((HttpRequest)message).setMethod(method);
      return this;
    }
    
    public FullHttpRequest setUri(String uri)
    {
      ((HttpRequest)message).setUri(uri);
      return this;
    }
    
    public HttpMethod getMethod()
    {
      return ((HttpRequest)message).method();
    }
    
    public String getUri()
    {
      return ((HttpRequest)message).uri();
    }
    
    public HttpMethod method()
    {
      return getMethod();
    }
    
    public String uri()
    {
      return getUri();
    }
    
    public FullHttpRequest setProtocolVersion(HttpVersion version)
    {
      super.setProtocolVersion(version);
      return this;
    }
    
    public String toString()
    {
      return HttpMessageUtil.appendFullRequest(new StringBuilder(256), this).toString();
    }
  }
  
  private static final class AggregatedFullHttpResponse extends HttpObjectAggregator.AggregatedFullHttpMessage implements FullHttpResponse
  {
    AggregatedFullHttpResponse(HttpResponse message, ByteBuf content, HttpHeaders trailingHeaders)
    {
      super(content, trailingHeaders);
    }
    
    public FullHttpResponse copy()
    {
      return replace(content().copy());
    }
    
    public FullHttpResponse duplicate()
    {
      return replace(content().duplicate());
    }
    
    public FullHttpResponse retainedDuplicate()
    {
      return replace(content().retainedDuplicate());
    }
    

    public FullHttpResponse replace(ByteBuf content)
    {
      DefaultFullHttpResponse dup = new DefaultFullHttpResponse(getProtocolVersion(), getStatus(), content, headers().copy(), trailingHeaders().copy());
      dup.setDecoderResult(decoderResult());
      return dup;
    }
    
    public FullHttpResponse setStatus(HttpResponseStatus status)
    {
      ((HttpResponse)message).setStatus(status);
      return this;
    }
    
    public HttpResponseStatus getStatus()
    {
      return ((HttpResponse)message).status();
    }
    
    public HttpResponseStatus status()
    {
      return getStatus();
    }
    
    public FullHttpResponse setProtocolVersion(HttpVersion version)
    {
      super.setProtocolVersion(version);
      return this;
    }
    
    public FullHttpResponse retain(int increment)
    {
      super.retain(increment);
      return this;
    }
    
    public FullHttpResponse retain()
    {
      super.retain();
      return this;
    }
    
    public FullHttpResponse touch(Object hint)
    {
      super.touch(hint);
      return this;
    }
    
    public FullHttpResponse touch()
    {
      super.touch();
      return this;
    }
    
    public String toString()
    {
      return HttpMessageUtil.appendFullResponse(new StringBuilder(256), this).toString();
    }
  }
}
