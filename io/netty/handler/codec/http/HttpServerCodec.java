package io.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.CombinedChannelDuplexHandler;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;


























public final class HttpServerCodec
  extends CombinedChannelDuplexHandler<HttpRequestDecoder, HttpResponseEncoder>
  implements HttpServerUpgradeHandler.SourceCodec
{
  private final Queue<HttpMethod> queue = new ArrayDeque();
  




  public HttpServerCodec()
  {
    this(4096, 8192, 8192);
  }
  


  public HttpServerCodec(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize)
  {
    init(new HttpServerRequestDecoder(maxInitialLineLength, maxHeaderSize, maxChunkSize), new HttpServerResponseEncoder(null));
  }
  



  public HttpServerCodec(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize, boolean validateHeaders)
  {
    init(new HttpServerRequestDecoder(maxInitialLineLength, maxHeaderSize, maxChunkSize, validateHeaders), new HttpServerResponseEncoder(null));
  }
  




  public HttpServerCodec(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize, boolean validateHeaders, int initialBufferSize)
  {
    init(new HttpServerRequestDecoder(maxInitialLineLength, maxHeaderSize, maxChunkSize, validateHeaders, initialBufferSize), new HttpServerResponseEncoder(null));
  }
  






  public HttpServerCodec(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize, boolean validateHeaders, int initialBufferSize, boolean allowDuplicateContentLengths)
  {
    init(new HttpServerRequestDecoder(maxInitialLineLength, maxHeaderSize, maxChunkSize, validateHeaders, initialBufferSize, allowDuplicateContentLengths), new HttpServerResponseEncoder(null));
  }
  





  public HttpServerCodec(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize, boolean validateHeaders, int initialBufferSize, boolean allowDuplicateContentLengths, boolean allowPartialChunks)
  {
    init(new HttpServerRequestDecoder(maxInitialLineLength, maxHeaderSize, maxChunkSize, validateHeaders, initialBufferSize, allowDuplicateContentLengths, allowPartialChunks), new HttpServerResponseEncoder(null));
  }
  






  public void upgradeFrom(ChannelHandlerContext ctx)
  {
    ctx.pipeline().remove(this);
  }
  
  private final class HttpServerRequestDecoder extends HttpRequestDecoder
  {
    HttpServerRequestDecoder(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize) {
      super(maxHeaderSize, maxChunkSize);
    }
    
    HttpServerRequestDecoder(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize, boolean validateHeaders)
    {
      super(maxHeaderSize, maxChunkSize, validateHeaders);
    }
    

    HttpServerRequestDecoder(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize, boolean validateHeaders, int initialBufferSize)
    {
      super(maxHeaderSize, maxChunkSize, validateHeaders, initialBufferSize);
    }
    
    HttpServerRequestDecoder(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize, boolean validateHeaders, int initialBufferSize, boolean allowDuplicateContentLengths)
    {
      super(maxHeaderSize, maxChunkSize, validateHeaders, initialBufferSize, allowDuplicateContentLengths);
    }
    


    HttpServerRequestDecoder(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize, boolean validateHeaders, int initialBufferSize, boolean allowDuplicateContentLengths, boolean allowPartialChunks)
    {
      super(maxHeaderSize, maxChunkSize, validateHeaders, initialBufferSize, allowDuplicateContentLengths, allowPartialChunks);
    }
    
    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out)
      throws Exception
    {
      int oldSize = out.size();
      super.decode(ctx, buffer, out);
      int size = out.size();
      for (int i = oldSize; i < size; i++) {
        Object obj = out.get(i);
        if ((obj instanceof HttpRequest)) {
          queue.add(((HttpRequest)obj).method());
        }
      }
    }
  }
  
  private final class HttpServerResponseEncoder extends HttpResponseEncoder {
    private HttpMethod method;
    
    private HttpServerResponseEncoder() {}
    
    protected void sanitizeHeadersBeforeEncode(HttpResponse msg, boolean isAlwaysEmpty) {
      if ((!isAlwaysEmpty) && (HttpMethod.CONNECT.equals(method)) && 
        (msg.status().codeClass() == HttpStatusClass.SUCCESS))
      {

        msg.headers().remove(HttpHeaderNames.TRANSFER_ENCODING);
        return;
      }
      
      super.sanitizeHeadersBeforeEncode(msg, isAlwaysEmpty);
    }
    
    protected boolean isContentAlwaysEmpty(HttpResponse msg)
    {
      method = ((HttpMethod)queue.poll());
      return (HttpMethod.HEAD.equals(method)) || (super.isContentAlwaysEmpty(msg));
    }
  }
}
