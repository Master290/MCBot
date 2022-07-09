package io.netty.handler.ssl;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.DecoderException;
import io.netty.util.AsyncMapping;
import io.netty.util.DomainNameMapping;
import io.netty.util.Mapping;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;



















public class SniHandler
  extends AbstractSniHandler<SslContext>
{
  private static final Selection EMPTY_SELECTION = new Selection(null, null);
  
  protected final AsyncMapping<String, SslContext> mapping;
  
  private volatile Selection selection = EMPTY_SELECTION;
  





  public SniHandler(Mapping<? super String, ? extends SslContext> mapping)
  {
    this(new AsyncMappingAdapter(mapping, null));
  }
  





  public SniHandler(DomainNameMapping<? extends SslContext> mapping)
  {
    this(mapping);
  }
  






  public SniHandler(AsyncMapping<? super String, ? extends SslContext> mapping)
  {
    this.mapping = ((AsyncMapping)ObjectUtil.checkNotNull(mapping, "mapping"));
  }
  


  public String hostname()
  {
    return selection.hostname;
  }
  


  public SslContext sslContext()
  {
    return selection.context;
  }
  





  protected Future<SslContext> lookup(ChannelHandlerContext ctx, String hostname)
    throws Exception
  {
    return mapping.map(hostname, ctx.executor().newPromise());
  }
  
  protected final void onLookupComplete(ChannelHandlerContext ctx, String hostname, Future<SslContext> future)
    throws Exception
  {
    if (!future.isSuccess()) {
      Throwable cause = future.cause();
      if ((cause instanceof Error)) {
        throw ((Error)cause);
      }
      throw new DecoderException("failed to get the SslContext for " + hostname, cause);
    }
    
    SslContext sslContext = (SslContext)future.getNow();
    selection = new Selection(sslContext, hostname);
    try {
      replaceHandler(ctx, hostname, sslContext);
    } catch (Throwable cause) {
      selection = EMPTY_SELECTION;
      PlatformDependent.throwException(cause);
    }
  }
  







  protected void replaceHandler(ChannelHandlerContext ctx, String hostname, SslContext sslContext)
    throws Exception
  {
    SslHandler sslHandler = null;
    try {
      sslHandler = newSslHandler(sslContext, ctx.alloc());
      ctx.pipeline().replace(this, SslHandler.class.getName(), sslHandler);
      sslHandler = null;

    }
    finally
    {
      if (sslHandler != null) {
        ReferenceCountUtil.safeRelease(sslHandler.engine());
      }
    }
  }
  



  protected SslHandler newSslHandler(SslContext context, ByteBufAllocator allocator)
  {
    return context.newHandler(allocator);
  }
  
  private static final class AsyncMappingAdapter implements AsyncMapping<String, SslContext> {
    private final Mapping<? super String, ? extends SslContext> mapping;
    
    private AsyncMappingAdapter(Mapping<? super String, ? extends SslContext> mapping) {
      this.mapping = ((Mapping)ObjectUtil.checkNotNull(mapping, "mapping"));
    }
    
    public Future<SslContext> map(String input, Promise<SslContext> promise)
    {
      try
      {
        context = (SslContext)mapping.map(input);
      } catch (Throwable cause) { SslContext context;
        return promise.setFailure(cause); }
      SslContext context;
      return promise.setSuccess(context);
    }
  }
  
  private static final class Selection {
    final SslContext context;
    final String hostname;
    
    Selection(SslContext context, String hostname) {
      this.context = context;
      this.hostname = hostname;
    }
  }
}
