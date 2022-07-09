package io.netty.handler.codec.http;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.util.AsciiString;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;
import io.netty.util.internal.ObjectUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;












































































public class HttpServerUpgradeHandler
  extends HttpObjectAggregator
{
  private final SourceCodec sourceCodec;
  private final UpgradeCodecFactory upgradeCodecFactory;
  private final boolean validateHeaders;
  private boolean handlingUpgrade;
  
  public static final class UpgradeEvent
    implements ReferenceCounted
  {
    private final CharSequence protocol;
    private final FullHttpRequest upgradeRequest;
    
    UpgradeEvent(CharSequence protocol, FullHttpRequest upgradeRequest)
    {
      this.protocol = protocol;
      this.upgradeRequest = upgradeRequest;
    }
    


    public CharSequence protocol()
    {
      return protocol;
    }
    


    public FullHttpRequest upgradeRequest()
    {
      return upgradeRequest;
    }
    
    public int refCnt()
    {
      return upgradeRequest.refCnt();
    }
    
    public UpgradeEvent retain()
    {
      upgradeRequest.retain();
      return this;
    }
    
    public UpgradeEvent retain(int increment)
    {
      upgradeRequest.retain(increment);
      return this;
    }
    
    public UpgradeEvent touch()
    {
      upgradeRequest.touch();
      return this;
    }
    
    public UpgradeEvent touch(Object hint)
    {
      upgradeRequest.touch(hint);
      return this;
    }
    
    public boolean release()
    {
      return upgradeRequest.release();
    }
    
    public boolean release(int decrement)
    {
      return upgradeRequest.release(decrement);
    }
    
    public String toString()
    {
      return "UpgradeEvent [protocol=" + protocol + ", upgradeRequest=" + upgradeRequest + ']';
    }
  }
  


















  public HttpServerUpgradeHandler(SourceCodec sourceCodec, UpgradeCodecFactory upgradeCodecFactory)
  {
    this(sourceCodec, upgradeCodecFactory, 0);
  }
  








  public HttpServerUpgradeHandler(SourceCodec sourceCodec, UpgradeCodecFactory upgradeCodecFactory, int maxContentLength)
  {
    this(sourceCodec, upgradeCodecFactory, maxContentLength, true);
  }
  









  public HttpServerUpgradeHandler(SourceCodec sourceCodec, UpgradeCodecFactory upgradeCodecFactory, int maxContentLength, boolean validateHeaders)
  {
    super(maxContentLength);
    
    this.sourceCodec = ((SourceCodec)ObjectUtil.checkNotNull(sourceCodec, "sourceCodec"));
    this.upgradeCodecFactory = ((UpgradeCodecFactory)ObjectUtil.checkNotNull(upgradeCodecFactory, "upgradeCodecFactory"));
    this.validateHeaders = validateHeaders;
  }
  

  protected void decode(ChannelHandlerContext ctx, HttpObject msg, List<Object> out)
    throws Exception
  {
    if (!handlingUpgrade)
    {
      if ((msg instanceof HttpRequest)) {
        HttpRequest req = (HttpRequest)msg;
        if ((req.headers().contains(HttpHeaderNames.UPGRADE)) && 
          (shouldHandleUpgradeRequest(req))) {
          handlingUpgrade = true;
        } else {
          ReferenceCountUtil.retain(msg);
          ctx.fireChannelRead(msg); return;
        }
      }
      else {
        ReferenceCountUtil.retain(msg);
        ctx.fireChannelRead(msg); return;
      }
    }
    
    FullHttpRequest fullRequest;
    
    if ((msg instanceof FullHttpRequest)) {
      FullHttpRequest fullRequest = (FullHttpRequest)msg;
      ReferenceCountUtil.retain(msg);
      out.add(msg);
    }
    else {
      super.decode(ctx, msg, out);
      if (out.isEmpty())
      {
        return;
      }
      

      assert (out.size() == 1);
      handlingUpgrade = false;
      fullRequest = (FullHttpRequest)out.get(0);
    }
    
    if (upgrade(ctx, fullRequest))
    {


      out.clear();
    }
  }
  















  protected boolean shouldHandleUpgradeRequest(HttpRequest req)
  {
    return true;
  }
  








  private boolean upgrade(ChannelHandlerContext ctx, FullHttpRequest request)
  {
    List<CharSequence> requestedProtocols = splitHeader(request.headers().get(HttpHeaderNames.UPGRADE));
    int numRequestedProtocols = requestedProtocols.size();
    UpgradeCodec upgradeCodec = null;
    CharSequence upgradeProtocol = null;
    UpgradeCodec c; for (int i = 0; i < numRequestedProtocols; i++) {
      CharSequence p = (CharSequence)requestedProtocols.get(i);
      c = upgradeCodecFactory.newUpgradeCodec(p);
      if (c != null) {
        upgradeProtocol = p;
        upgradeCodec = c;
        break;
      }
    }
    
    if (upgradeCodec == null)
    {
      return false;
    }
    

    List<String> connectionHeaderValues = request.headers().getAll(HttpHeaderNames.CONNECTION);
    
    if (connectionHeaderValues == null) {
      return false;
    }
    
    StringBuilder concatenatedConnectionValue = new StringBuilder(connectionHeaderValues.size() * 10);
    for (CharSequence connectionHeaderValue : connectionHeaderValues) {
      concatenatedConnectionValue.append(connectionHeaderValue).append(',');
    }
    concatenatedConnectionValue.setLength(concatenatedConnectionValue.length() - 1);
    

    Collection<CharSequence> requiredHeaders = upgradeCodec.requiredUpgradeHeaders();
    List<CharSequence> values = splitHeader(concatenatedConnectionValue);
    if ((!AsciiString.containsContentEqualsIgnoreCase(values, HttpHeaderNames.UPGRADE)) || 
      (!AsciiString.containsAllContentEqualsIgnoreCase(values, requiredHeaders))) {
      return false;
    }
    

    for (CharSequence requiredHeader : requiredHeaders) {
      if (!request.headers().contains(requiredHeader)) {
        return false;
      }
    }
    


    FullHttpResponse upgradeResponse = createUpgradeResponse(upgradeProtocol);
    if (!upgradeCodec.prepareUpgradeResponse(ctx, request, upgradeResponse.headers())) {
      return false;
    }
    

    UpgradeEvent event = new UpgradeEvent(upgradeProtocol, request);
    



    try
    {
      ChannelFuture writeComplete = ctx.writeAndFlush(upgradeResponse);
      
      sourceCodec.upgradeFrom(ctx);
      upgradeCodec.upgradeTo(ctx, request);
      

      ctx.pipeline().remove(this);
      


      ctx.fireUserEventTriggered(event.retain());
      



      writeComplete.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
    }
    finally {
      event.release();
    }
    return true;
  }
  


  private FullHttpResponse createUpgradeResponse(CharSequence upgradeProtocol)
  {
    DefaultFullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.SWITCHING_PROTOCOLS, Unpooled.EMPTY_BUFFER, validateHeaders);
    
    res.headers().add(HttpHeaderNames.CONNECTION, HttpHeaderValues.UPGRADE);
    res.headers().add(HttpHeaderNames.UPGRADE, upgradeProtocol);
    return res;
  }
  



  private static List<CharSequence> splitHeader(CharSequence header)
  {
    StringBuilder builder = new StringBuilder(header.length());
    List<CharSequence> protocols = new ArrayList(4);
    for (int i = 0; i < header.length(); i++) {
      char c = header.charAt(i);
      if (!Character.isWhitespace(c))
      {


        if (c == ',')
        {
          protocols.add(builder.toString());
          builder.setLength(0);
        } else {
          builder.append(c);
        }
      }
    }
    
    if (builder.length() > 0) {
      protocols.add(builder.toString());
    }
    
    return protocols;
  }
  
  public static abstract interface UpgradeCodecFactory
  {
    public abstract HttpServerUpgradeHandler.UpgradeCodec newUpgradeCodec(CharSequence paramCharSequence);
  }
  
  public static abstract interface UpgradeCodec
  {
    public abstract Collection<CharSequence> requiredUpgradeHeaders();
    
    public abstract boolean prepareUpgradeResponse(ChannelHandlerContext paramChannelHandlerContext, FullHttpRequest paramFullHttpRequest, HttpHeaders paramHttpHeaders);
    
    public abstract void upgradeTo(ChannelHandlerContext paramChannelHandlerContext, FullHttpRequest paramFullHttpRequest);
  }
  
  public static abstract interface SourceCodec
  {
    public abstract void upgradeFrom(ChannelHandlerContext paramChannelHandlerContext);
  }
}
