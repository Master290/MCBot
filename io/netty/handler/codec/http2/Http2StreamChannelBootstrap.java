package io.netty.handler.codec.http2;

import io.netty.channel.Channel;
import io.netty.channel.Channel.Unsafe;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.nio.channels.ClosedChannelException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;












public final class Http2StreamChannelBootstrap
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(Http2StreamChannelBootstrap.class);
  
  private static final Map.Entry<ChannelOption<?>, Object>[] EMPTY_OPTION_ARRAY = new Map.Entry[0];
  
  private static final Map.Entry<AttributeKey<?>, Object>[] EMPTY_ATTRIBUTE_ARRAY = new Map.Entry[0];
  


  private final Map<ChannelOption<?>, Object> options = new LinkedHashMap();
  private final Map<AttributeKey<?>, Object> attrs = new ConcurrentHashMap();
  
  private final Channel channel;
  private volatile ChannelHandler handler;
  private volatile ChannelHandlerContext multiplexCtx;
  
  public Http2StreamChannelBootstrap(Channel channel)
  {
    this.channel = ((Channel)ObjectUtil.checkNotNull(channel, "channel"));
  }
  




  public <T> Http2StreamChannelBootstrap option(ChannelOption<T> option, T value)
  {
    ObjectUtil.checkNotNull(option, "option");
    
    synchronized (options) {
      if (value == null) {
        options.remove(option);
      } else {
        options.put(option, value);
      }
    }
    return this;
  }
  




  public <T> Http2StreamChannelBootstrap attr(AttributeKey<T> key, T value)
  {
    ObjectUtil.checkNotNull(key, "key");
    if (value == null) {
      attrs.remove(key);
    } else {
      attrs.put(key, value);
    }
    return this;
  }
  


  public Http2StreamChannelBootstrap handler(ChannelHandler handler)
  {
    this.handler = ((ChannelHandler)ObjectUtil.checkNotNull(handler, "handler"));
    return this;
  }
  



  public Future<Http2StreamChannel> open()
  {
    return open(channel.eventLoop().newPromise());
  }
  



  public Future<Http2StreamChannel> open(final Promise<Http2StreamChannel> promise)
  {
    try
    {
      ChannelHandlerContext ctx = findCtx();
      EventExecutor executor = ctx.executor();
      if (executor.inEventLoop()) {
        open0(ctx, promise);
      } else {
        final ChannelHandlerContext finalCtx = ctx;
        executor.execute(new Runnable()
        {
          public void run() {
            if (channel.isActive()) {
              open0(finalCtx, promise);
            } else {
              promise.setFailure(new ClosedChannelException());
            }
          }
        });
      }
    } catch (Throwable cause) {
      promise.setFailure(cause);
    }
    return promise;
  }
  
  private ChannelHandlerContext findCtx() throws ClosedChannelException
  {
    ChannelHandlerContext ctx = multiplexCtx;
    if ((ctx != null) && (!ctx.isRemoved())) {
      return ctx;
    }
    ChannelPipeline pipeline = channel.pipeline();
    ctx = pipeline.context(Http2MultiplexCodec.class);
    if (ctx == null) {
      ctx = pipeline.context(Http2MultiplexHandler.class);
    }
    if (ctx == null) {
      if (channel.isActive())
      {
        throw new IllegalStateException(StringUtil.simpleClassName(Http2MultiplexCodec.class) + " or " + StringUtil.simpleClassName(Http2MultiplexHandler.class) + " must be in the ChannelPipeline of Channel " + channel);
      }
      
      throw new ClosedChannelException();
    }
    
    multiplexCtx = ctx;
    return ctx;
  }
  


  @Deprecated
  public void open0(ChannelHandlerContext ctx, final Promise<Http2StreamChannel> promise)
  {
    assert (ctx.executor().inEventLoop());
    if (!promise.setUncancellable()) {
      return;
    }
    try {
      Http2StreamChannel streamChannel;
      if ((ctx.handler() instanceof Http2MultiplexCodec)) {
        streamChannel = ((Http2MultiplexCodec)ctx.handler()).newOutboundStream();
      } else
        streamChannel = ((Http2MultiplexHandler)ctx.handler()).newOutboundStream();
    } catch (Exception e) {
      Http2StreamChannel streamChannel;
      promise.setFailure(e); return;
    }
    final Http2StreamChannel streamChannel;
    try {
      init(streamChannel);
    } catch (Exception e) {
      streamChannel.unsafe().closeForcibly();
      promise.setFailure(e);
      return;
    }
    
    ChannelFuture future = ctx.channel().eventLoop().register(streamChannel);
    future.addListener(new ChannelFutureListener()
    {
      public void operationComplete(ChannelFuture future) {
        if (future.isSuccess()) {
          promise.setSuccess(streamChannel);
        } else if (future.isCancelled()) {
          promise.cancel(false);
        } else {
          if (streamChannel.isRegistered()) {
            streamChannel.close();
          } else {
            streamChannel.unsafe().closeForcibly();
          }
          
          promise.setFailure(future.cause());
        }
      }
    });
  }
  
  private void init(Channel channel) {
    ChannelPipeline p = channel.pipeline();
    ChannelHandler handler = this.handler;
    if (handler != null) {
      p.addLast(new ChannelHandler[] { handler });
    }
    Map.Entry<ChannelOption<?>, Object>[] optionArray;
    synchronized (options) {
      optionArray = (Map.Entry[])options.entrySet().toArray(EMPTY_OPTION_ARRAY);
    }
    Map.Entry<ChannelOption<?>, Object>[] optionArray;
    setChannelOptions(channel, optionArray);
    setAttributes(channel, (Map.Entry[])attrs.entrySet().toArray(EMPTY_ATTRIBUTE_ARRAY));
  }
  
  private static void setChannelOptions(Channel channel, Map.Entry<ChannelOption<?>, Object>[] options)
  {
    for (Map.Entry<ChannelOption<?>, Object> e : options) {
      setChannelOption(channel, (ChannelOption)e.getKey(), e.getValue());
    }
  }
  
  private static void setChannelOption(Channel channel, ChannelOption<?> option, Object value)
  {
    try
    {
      ChannelOption<Object> opt = option;
      if (!channel.config().setOption(opt, value)) {
        logger.warn("Unknown channel option '{}' for channel '{}'", option, channel);
      }
    } catch (Throwable t) {
      logger.warn("Failed to set channel option '{}' with value '{}' for channel '{}'", new Object[] { option, value, channel, t });
    }
  }
  

  private static void setAttributes(Channel channel, Map.Entry<AttributeKey<?>, Object>[] options)
  {
    for (Map.Entry<AttributeKey<?>, Object> e : options)
    {
      AttributeKey<Object> key = (AttributeKey)e.getKey();
      channel.attr(key).set(e.getValue());
    }
  }
}
