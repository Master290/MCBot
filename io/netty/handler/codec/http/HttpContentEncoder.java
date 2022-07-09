package io.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;





































public abstract class HttpContentEncoder
  extends MessageToMessageCodec<HttpRequest, HttpObject>
{
  public HttpContentEncoder() {}
  
  private static enum State
  {
    PASS_THROUGH, 
    AWAIT_HEADERS, 
    AWAIT_CONTENT;
    
    private State() {} }
  private static final CharSequence ZERO_LENGTH_HEAD = "HEAD";
  private static final CharSequence ZERO_LENGTH_CONNECT = "CONNECT";
  private static final int CONTINUE_CODE = HttpResponseStatus.CONTINUE.code();
  
  private final Queue<CharSequence> acceptEncodingQueue = new ArrayDeque();
  private EmbeddedChannel encoder;
  private State state = State.AWAIT_HEADERS;
  
  public boolean acceptOutboundMessage(Object msg) throws Exception
  {
    return ((msg instanceof HttpContent)) || ((msg instanceof HttpResponse));
  }
  
  protected void decode(ChannelHandlerContext ctx, HttpRequest msg, List<Object> out)
    throws Exception
  {
    List<String> acceptEncodingHeaders = msg.headers().getAll(HttpHeaderNames.ACCEPT_ENCODING);
    CharSequence acceptEncoding; CharSequence acceptEncoding; CharSequence acceptEncoding; switch (acceptEncodingHeaders.size()) {
    case 0: 
      acceptEncoding = HttpContentDecoder.IDENTITY;
      break;
    case 1: 
      acceptEncoding = (CharSequence)acceptEncodingHeaders.get(0);
      break;
    
    default: 
      acceptEncoding = StringUtil.join(",", acceptEncodingHeaders);
    }
    
    
    HttpMethod method = msg.method();
    if (HttpMethod.HEAD.equals(method)) {
      acceptEncoding = ZERO_LENGTH_HEAD;
    } else if (HttpMethod.CONNECT.equals(method)) {
      acceptEncoding = ZERO_LENGTH_CONNECT;
    }
    
    acceptEncodingQueue.add(acceptEncoding);
    out.add(ReferenceCountUtil.retain(msg));
  }
  
  protected void encode(ChannelHandlerContext ctx, HttpObject msg, List<Object> out) throws Exception
  {
    boolean isFull = ((msg instanceof HttpResponse)) && ((msg instanceof LastHttpContent));
    switch (1.$SwitchMap$io$netty$handler$codec$http$HttpContentEncoder$State[state.ordinal()]) {
    case 1: 
      ensureHeaders(msg);
      assert (encoder == null);
      
      HttpResponse res = (HttpResponse)msg;
      int code = res.status().code();
      CharSequence acceptEncoding;
      CharSequence acceptEncoding; if (code == CONTINUE_CODE)
      {

        acceptEncoding = null;
      }
      else {
        acceptEncoding = (CharSequence)acceptEncodingQueue.poll();
        if (acceptEncoding == null) {
          throw new IllegalStateException("cannot send more responses than requests");
        }
      }
      













      if (isPassthru(res.protocolVersion(), code, acceptEncoding)) {
        if (isFull) {
          out.add(ReferenceCountUtil.retain(res));
        } else {
          out.add(ReferenceCountUtil.retain(res));
          
          state = State.PASS_THROUGH;
        }
      }
      else
      {
        if (isFull)
        {
          if (!((ByteBufHolder)res).content().isReadable()) {
            out.add(ReferenceCountUtil.retain(res));
            break;
          }
        }
        

        Result result = beginEncode(res, acceptEncoding.toString());
        

        if (result == null) {
          if (isFull) {
            out.add(ReferenceCountUtil.retain(res));
          } else {
            out.add(ReferenceCountUtil.retain(res));
            
            state = State.PASS_THROUGH;
          }
        }
        else
        {
          encoder = result.contentEncoder();
          


          res.headers().set(HttpHeaderNames.CONTENT_ENCODING, result.targetContentEncoding());
          

          if (isFull)
          {
            HttpResponse newRes = new DefaultHttpResponse(res.protocolVersion(), res.status());
            newRes.headers().set(res.headers());
            out.add(newRes);
            
            ensureContent(res);
            encodeFullResponse(newRes, (HttpContent)res, out);
          }
          else
          {
            res.headers().remove(HttpHeaderNames.CONTENT_LENGTH);
            res.headers().set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
            
            out.add(ReferenceCountUtil.retain(res));
            state = State.AWAIT_CONTENT;
            if (!(msg instanceof HttpContent)) {
              break;
            }
          }
        }
      }
      
      break;
    case 2: 
      ensureContent(msg);
      if (encodeContent((HttpContent)msg, out)) {
        state = State.AWAIT_HEADERS;
      }
      
      break;
    case 3: 
      ensureContent(msg);
      out.add(ReferenceCountUtil.retain(msg));
      
      if ((msg instanceof LastHttpContent)) {
        state = State.AWAIT_HEADERS;
      }
      break;
    }
  }
  
  private void encodeFullResponse(HttpResponse newRes, HttpContent content, List<Object> out)
  {
    int existingMessages = out.size();
    encodeContent(content, out);
    
    if (HttpUtil.isContentLengthSet(newRes))
    {
      int messageSize = 0;
      for (int i = existingMessages; i < out.size(); i++) {
        Object item = out.get(i);
        if ((item instanceof HttpContent)) {
          messageSize += ((HttpContent)item).content().readableBytes();
        }
      }
      HttpUtil.setContentLength(newRes, messageSize);
    } else {
      newRes.headers().set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
    }
  }
  
  private static boolean isPassthru(HttpVersion version, int code, CharSequence httpMethod) {
    return (code < 200) || (code == 204) || (code == 304) || (httpMethod == ZERO_LENGTH_HEAD) || ((httpMethod == ZERO_LENGTH_CONNECT) && (code == 200)) || (version == HttpVersion.HTTP_1_0);
  }
  

  private static void ensureHeaders(HttpObject msg)
  {
    if (!(msg instanceof HttpResponse))
    {

      throw new IllegalStateException("unexpected message type: " + msg.getClass().getName() + " (expected: " + HttpResponse.class.getSimpleName() + ')');
    }
  }
  
  private static void ensureContent(HttpObject msg) {
    if (!(msg instanceof HttpContent))
    {

      throw new IllegalStateException("unexpected message type: " + msg.getClass().getName() + " (expected: " + HttpContent.class.getSimpleName() + ')');
    }
  }
  
  private boolean encodeContent(HttpContent c, List<Object> out) {
    ByteBuf content = c.content();
    
    encode(content, out);
    
    if ((c instanceof LastHttpContent)) {
      finishEncode(out);
      LastHttpContent last = (LastHttpContent)c;
      


      HttpHeaders headers = last.trailingHeaders();
      if (headers.isEmpty()) {
        out.add(LastHttpContent.EMPTY_LAST_CONTENT);
      } else {
        out.add(new ComposedLastHttpContent(headers, DecoderResult.SUCCESS));
      }
      return true;
    }
    return false;
  }
  






  protected abstract Result beginEncode(HttpResponse paramHttpResponse, String paramString)
    throws Exception;
  






  public void handlerRemoved(ChannelHandlerContext ctx)
    throws Exception
  {
    cleanupSafely(ctx);
    super.handlerRemoved(ctx);
  }
  
  public void channelInactive(ChannelHandlerContext ctx) throws Exception
  {
    cleanupSafely(ctx);
    super.channelInactive(ctx);
  }
  
  private void cleanup() {
    if (encoder != null)
    {
      encoder.finishAndReleaseAll();
      encoder = null;
    }
  }
  
  private void cleanupSafely(ChannelHandlerContext ctx) {
    try {
      cleanup();
    }
    catch (Throwable cause)
    {
      ctx.fireExceptionCaught(cause);
    }
  }
  
  private void encode(ByteBuf in, List<Object> out)
  {
    encoder.writeOutbound(new Object[] { in.retain() });
    fetchEncoderOutput(out);
  }
  
  private void finishEncode(List<Object> out) {
    if (encoder.finish()) {
      fetchEncoderOutput(out);
    }
    encoder = null;
  }
  
  private void fetchEncoderOutput(List<Object> out) {
    for (;;) {
      ByteBuf buf = (ByteBuf)encoder.readOutbound();
      if (buf == null) {
        break;
      }
      if (!buf.isReadable()) {
        buf.release();
      }
      else
        out.add(new DefaultHttpContent(buf));
    }
  }
  
  public static final class Result {
    private final String targetContentEncoding;
    private final EmbeddedChannel contentEncoder;
    
    public Result(String targetContentEncoding, EmbeddedChannel contentEncoder) {
      this.targetContentEncoding = ((String)ObjectUtil.checkNotNull(targetContentEncoding, "targetContentEncoding"));
      this.contentEncoder = ((EmbeddedChannel)ObjectUtil.checkNotNull(contentEncoder, "contentEncoder"));
    }
    
    public String targetContentEncoding() {
      return targetContentEncoding;
    }
    
    public EmbeddedChannel contentEncoder() {
      return contentEncoder;
    }
  }
}
