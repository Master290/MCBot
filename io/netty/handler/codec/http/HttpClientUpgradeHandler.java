package io.netty.handler.codec.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.util.AsciiString;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.ObjectUtil;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;






public class HttpClientUpgradeHandler
  extends HttpObjectAggregator
  implements ChannelOutboundHandler
{
  private final SourceCodec sourceCodec;
  private final UpgradeCodec upgradeCodec;
  private boolean upgradeRequested;
  
  public static abstract interface UpgradeCodec
  {
    public abstract CharSequence protocol();
    
    public abstract Collection<CharSequence> setUpgradeHeaders(ChannelHandlerContext paramChannelHandlerContext, HttpRequest paramHttpRequest);
    
    public abstract void upgradeTo(ChannelHandlerContext paramChannelHandlerContext, FullHttpResponse paramFullHttpResponse)
      throws Exception;
  }
  
  public static abstract interface SourceCodec
  {
    public abstract void prepareUpgradeFrom(ChannelHandlerContext paramChannelHandlerContext);
    
    public abstract void upgradeFrom(ChannelHandlerContext paramChannelHandlerContext);
  }
  
  public static enum UpgradeEvent
  {
    UPGRADE_ISSUED, 
    



    UPGRADE_SUCCESSFUL, 
    




    UPGRADE_REJECTED;
    


























    private UpgradeEvent() {}
  }
  


























  public HttpClientUpgradeHandler(SourceCodec sourceCodec, UpgradeCodec upgradeCodec, int maxContentLength)
  {
    super(maxContentLength);
    this.sourceCodec = ((SourceCodec)ObjectUtil.checkNotNull(sourceCodec, "sourceCodec"));
    this.upgradeCodec = ((UpgradeCodec)ObjectUtil.checkNotNull(upgradeCodec, "upgradeCodec"));
  }
  
  public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception
  {
    ctx.bind(localAddress, promise);
  }
  
  public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise)
    throws Exception
  {
    ctx.connect(remoteAddress, localAddress, promise);
  }
  
  public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception
  {
    ctx.disconnect(promise);
  }
  
  public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception
  {
    ctx.close(promise);
  }
  
  public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception
  {
    ctx.deregister(promise);
  }
  
  public void read(ChannelHandlerContext ctx) throws Exception
  {
    ctx.read();
  }
  
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
    throws Exception
  {
    if (!(msg instanceof HttpRequest)) {
      ctx.write(msg, promise);
      return;
    }
    
    if (upgradeRequested) {
      promise.setFailure(new IllegalStateException("Attempting to write HTTP request with upgrade in progress"));
      
      return;
    }
    
    upgradeRequested = true;
    setUpgradeRequestHeaders(ctx, (HttpRequest)msg);
    

    ctx.write(msg, promise);
    

    ctx.fireUserEventTriggered(UpgradeEvent.UPGRADE_ISSUED);
  }
  
  public void flush(ChannelHandlerContext ctx)
    throws Exception
  {
    ctx.flush();
  }
  
  protected void decode(ChannelHandlerContext ctx, HttpObject msg, List<Object> out)
    throws Exception
  {
    FullHttpResponse response = null;
    try {
      if (!upgradeRequested) {
        throw new IllegalStateException("Read HTTP response without requesting protocol switch");
      }
      
      if ((msg instanceof HttpResponse)) {
        HttpResponse rep = (HttpResponse)msg;
        if (!HttpResponseStatus.SWITCHING_PROTOCOLS.equals(rep.status()))
        {



          ctx.fireUserEventTriggered(UpgradeEvent.UPGRADE_REJECTED);
          removeThisHandler(ctx);
          ctx.fireChannelRead(msg);
          return;
        }
      }
      
      if ((msg instanceof FullHttpResponse)) {
        response = (FullHttpResponse)msg;
        
        response.retain();
        out.add(response);
      }
      else {
        super.decode(ctx, msg, out);
        if (out.isEmpty())
        {
          return;
        }
        
        assert (out.size() == 1);
        response = (FullHttpResponse)out.get(0);
      }
      
      CharSequence upgradeHeader = response.headers().get(HttpHeaderNames.UPGRADE);
      if ((upgradeHeader != null) && (!AsciiString.contentEqualsIgnoreCase(upgradeCodec.protocol(), upgradeHeader))) {
        throw new IllegalStateException("Switching Protocols response with unexpected UPGRADE protocol: " + upgradeHeader);
      }
      


      sourceCodec.prepareUpgradeFrom(ctx);
      upgradeCodec.upgradeTo(ctx, response);
      

      ctx.fireUserEventTriggered(UpgradeEvent.UPGRADE_SUCCESSFUL);
      


      sourceCodec.upgradeFrom(ctx);
      


      response.release();
      out.clear();
      removeThisHandler(ctx);
    } catch (Throwable t) {
      ReferenceCountUtil.release(response);
      ctx.fireExceptionCaught(t);
      removeThisHandler(ctx);
    }
  }
  
  private static void removeThisHandler(ChannelHandlerContext ctx) {
    ctx.pipeline().remove(ctx.name());
  }
  



  private void setUpgradeRequestHeaders(ChannelHandlerContext ctx, HttpRequest request)
  {
    request.headers().set(HttpHeaderNames.UPGRADE, upgradeCodec.protocol());
    

    Set<CharSequence> connectionParts = new LinkedHashSet(2);
    connectionParts.addAll(upgradeCodec.setUpgradeHeaders(ctx, request));
    

    StringBuilder builder = new StringBuilder();
    for (CharSequence part : connectionParts) {
      builder.append(part);
      builder.append(',');
    }
    builder.append(HttpHeaderValues.UPGRADE);
    request.headers().add(HttpHeaderNames.CONNECTION, builder.toString());
  }
}
