package io.netty.handler.ssl.ocsp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.ReferenceCountedOpenSslEngine;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import io.netty.util.internal.ObjectUtil;
import javax.net.ssl.SSLHandshakeException;























public abstract class OcspClientHandler
  extends ChannelInboundHandlerAdapter
{
  private final ReferenceCountedOpenSslEngine engine;
  
  protected OcspClientHandler(ReferenceCountedOpenSslEngine engine)
  {
    this.engine = ((ReferenceCountedOpenSslEngine)ObjectUtil.checkNotNull(engine, "engine"));
  }
  

  protected abstract boolean verify(ChannelHandlerContext paramChannelHandlerContext, ReferenceCountedOpenSslEngine paramReferenceCountedOpenSslEngine)
    throws Exception;
  
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
    throws Exception
  {
    if ((evt instanceof SslHandshakeCompletionEvent)) {
      ctx.pipeline().remove(this);
      
      SslHandshakeCompletionEvent event = (SslHandshakeCompletionEvent)evt;
      if ((event.isSuccess()) && (!verify(ctx, engine))) {
        throw new SSLHandshakeException("Bad OCSP response");
      }
    }
    
    ctx.fireUserEventTriggered(evt);
  }
}
