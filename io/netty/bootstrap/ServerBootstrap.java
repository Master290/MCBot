package io.netty.bootstrap;

import io.netty.channel.Channel;
import io.netty.channel.Channel.Unsafe;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.util.AttributeKey;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


















public class ServerBootstrap
  extends AbstractBootstrap<ServerBootstrap, ServerChannel>
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(ServerBootstrap.class);
  


  private final Map<ChannelOption<?>, Object> childOptions = new LinkedHashMap();
  private final Map<AttributeKey<?>, Object> childAttrs = new ConcurrentHashMap();
  private final ServerBootstrapConfig config = new ServerBootstrapConfig(this);
  private volatile EventLoopGroup childGroup;
  private volatile ChannelHandler childHandler;
  
  public ServerBootstrap() {}
  
  private ServerBootstrap(ServerBootstrap bootstrap) {
    super(bootstrap);
    childGroup = childGroup;
    childHandler = childHandler;
    synchronized (childOptions) {
      childOptions.putAll(childOptions);
    }
    childAttrs.putAll(childAttrs);
  }
  



  public ServerBootstrap group(EventLoopGroup group)
  {
    return group(group, group);
  }
  




  public ServerBootstrap group(EventLoopGroup parentGroup, EventLoopGroup childGroup)
  {
    super.group(parentGroup);
    if (this.childGroup != null) {
      throw new IllegalStateException("childGroup set already");
    }
    this.childGroup = ((EventLoopGroup)ObjectUtil.checkNotNull(childGroup, "childGroup"));
    return this;
  }
  




  public <T> ServerBootstrap childOption(ChannelOption<T> childOption, T value)
  {
    ObjectUtil.checkNotNull(childOption, "childOption");
    synchronized (childOptions) {
      if (value == null) {
        childOptions.remove(childOption);
      } else {
        childOptions.put(childOption, value);
      }
    }
    return this;
  }
  



  public <T> ServerBootstrap childAttr(AttributeKey<T> childKey, T value)
  {
    ObjectUtil.checkNotNull(childKey, "childKey");
    if (value == null) {
      childAttrs.remove(childKey);
    } else {
      childAttrs.put(childKey, value);
    }
    return this;
  }
  


  public ServerBootstrap childHandler(ChannelHandler childHandler)
  {
    this.childHandler = ((ChannelHandler)ObjectUtil.checkNotNull(childHandler, "childHandler"));
    return this;
  }
  
  void init(Channel channel)
  {
    setChannelOptions(channel, newOptionsArray(), logger);
    setAttributes(channel, newAttributesArray());
    
    ChannelPipeline p = channel.pipeline();
    
    final EventLoopGroup currentChildGroup = childGroup;
    final ChannelHandler currentChildHandler = childHandler;
    final Map.Entry<ChannelOption<?>, Object>[] currentChildOptions = newOptionsArray(childOptions);
    final Map.Entry<AttributeKey<?>, Object>[] currentChildAttrs = newAttributesArray(childAttrs);
    
    p.addLast(new ChannelHandler[] { new ChannelInitializer()
    {
      public void initChannel(final Channel ch) {
        final ChannelPipeline pipeline = ch.pipeline();
        ChannelHandler handler = config.handler();
        if (handler != null) {
          pipeline.addLast(new ChannelHandler[] { handler });
        }
        
        ch.eventLoop().execute(new Runnable()
        {
          public void run() {
            pipeline.addLast(new ChannelHandler[] { new ServerBootstrap.ServerBootstrapAcceptor(ch, val$currentChildGroup, val$currentChildHandler, val$currentChildOptions, val$currentChildAttrs) });
          }
        });
      }
    } });
  }
  

  public ServerBootstrap validate()
  {
    super.validate();
    if (childHandler == null) {
      throw new IllegalStateException("childHandler not set");
    }
    if (childGroup == null) {
      logger.warn("childGroup is not set. Using parentGroup instead.");
      childGroup = config.group();
    }
    return this;
  }
  
  private static class ServerBootstrapAcceptor
    extends ChannelInboundHandlerAdapter
  {
    private final EventLoopGroup childGroup;
    private final ChannelHandler childHandler;
    private final Map.Entry<ChannelOption<?>, Object>[] childOptions;
    private final Map.Entry<AttributeKey<?>, Object>[] childAttrs;
    private final Runnable enableAutoReadTask;
    
    ServerBootstrapAcceptor(final Channel channel, EventLoopGroup childGroup, ChannelHandler childHandler, Map.Entry<ChannelOption<?>, Object>[] childOptions, Map.Entry<AttributeKey<?>, Object>[] childAttrs)
    {
      this.childGroup = childGroup;
      this.childHandler = childHandler;
      this.childOptions = childOptions;
      this.childAttrs = childAttrs;
      





      enableAutoReadTask = new Runnable()
      {
        public void run() {
          channel.config().setAutoRead(true);
        }
      };
    }
    

    public void channelRead(ChannelHandlerContext ctx, Object msg)
    {
      final Channel child = (Channel)msg;
      
      child.pipeline().addLast(new ChannelHandler[] { childHandler });
      
      AbstractBootstrap.setChannelOptions(child, childOptions, ServerBootstrap.logger);
      AbstractBootstrap.setAttributes(child, childAttrs);
      try
      {
        childGroup.register(child).addListener(new ChannelFutureListener()
        {
          public void operationComplete(ChannelFuture future) throws Exception {
            if (!future.isSuccess()) {
              ServerBootstrap.ServerBootstrapAcceptor.forceClose(child, future.cause());
            }
          }
        });
      } catch (Throwable t) {
        forceClose(child, t);
      }
    }
    
    private static void forceClose(Channel child, Throwable t) {
      child.unsafe().closeForcibly();
      ServerBootstrap.logger.warn("Failed to register an accepted channel: {}", child, t);
    }
    
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
      ChannelConfig config = ctx.channel().config();
      if (config.isAutoRead())
      {

        config.setAutoRead(false);
        ctx.channel().eventLoop().schedule(enableAutoReadTask, 1L, TimeUnit.SECONDS);
      }
      

      ctx.fireExceptionCaught(cause);
    }
  }
  

  public ServerBootstrap clone()
  {
    return new ServerBootstrap(this);
  }
  





  @Deprecated
  public EventLoopGroup childGroup()
  {
    return childGroup;
  }
  
  final ChannelHandler childHandler() {
    return childHandler;
  }
  
  final Map<ChannelOption<?>, Object> childOptions() {
    synchronized (childOptions) {
      return copiedMap(childOptions);
    }
  }
  
  final Map<AttributeKey<?>, Object> childAttrs() {
    return copiedMap(childAttrs);
  }
  
  public final ServerBootstrapConfig config()
  {
    return config;
  }
}
