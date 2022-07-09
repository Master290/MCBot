package io.netty.handler.ssl;

import io.netty.buffer.ByteBufAllocator;
import io.netty.util.internal.ObjectUtil;
import java.util.List;
import java.util.concurrent.Executor;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSessionContext;


















public abstract class DelegatingSslContext
  extends SslContext
{
  private final SslContext ctx;
  
  protected DelegatingSslContext(SslContext ctx)
  {
    this.ctx = ((SslContext)ObjectUtil.checkNotNull(ctx, "ctx"));
  }
  
  public final boolean isClient()
  {
    return ctx.isClient();
  }
  
  public final List<String> cipherSuites()
  {
    return ctx.cipherSuites();
  }
  
  public final long sessionCacheSize()
  {
    return ctx.sessionCacheSize();
  }
  
  public final long sessionTimeout()
  {
    return ctx.sessionTimeout();
  }
  
  public final ApplicationProtocolNegotiator applicationProtocolNegotiator()
  {
    return ctx.applicationProtocolNegotiator();
  }
  
  public final SSLEngine newEngine(ByteBufAllocator alloc)
  {
    SSLEngine engine = ctx.newEngine(alloc);
    initEngine(engine);
    return engine;
  }
  
  public final SSLEngine newEngine(ByteBufAllocator alloc, String peerHost, int peerPort)
  {
    SSLEngine engine = ctx.newEngine(alloc, peerHost, peerPort);
    initEngine(engine);
    return engine;
  }
  
  protected final SslHandler newHandler(ByteBufAllocator alloc, boolean startTls)
  {
    SslHandler handler = ctx.newHandler(alloc, startTls);
    initHandler(handler);
    return handler;
  }
  
  protected final SslHandler newHandler(ByteBufAllocator alloc, String peerHost, int peerPort, boolean startTls)
  {
    SslHandler handler = ctx.newHandler(alloc, peerHost, peerPort, startTls);
    initHandler(handler);
    return handler;
  }
  
  protected SslHandler newHandler(ByteBufAllocator alloc, boolean startTls, Executor executor)
  {
    SslHandler handler = ctx.newHandler(alloc, startTls, executor);
    initHandler(handler);
    return handler;
  }
  

  protected SslHandler newHandler(ByteBufAllocator alloc, String peerHost, int peerPort, boolean startTls, Executor executor)
  {
    SslHandler handler = ctx.newHandler(alloc, peerHost, peerPort, startTls, executor);
    initHandler(handler);
    return handler;
  }
  
  public final SSLSessionContext sessionContext()
  {
    return ctx.sessionContext();
  }
  



  protected abstract void initEngine(SSLEngine paramSSLEngine);
  



  protected void initHandler(SslHandler handler)
  {
    initEngine(handler.engine());
  }
}
