package io.netty.bootstrap;

import io.netty.channel.Channel;
import io.netty.channel.Channel.Unsafe;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ReflectiveChannelFactory;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.SocketUtils;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.logging.InternalLogger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;



















public abstract class AbstractBootstrap<B extends AbstractBootstrap<B, C>, C extends Channel>
  implements Cloneable
{
  private static final Map.Entry<ChannelOption<?>, Object>[] EMPTY_OPTION_ARRAY = new Map.Entry[0];
  
  private static final Map.Entry<AttributeKey<?>, Object>[] EMPTY_ATTRIBUTE_ARRAY = new Map.Entry[0];
  

  volatile EventLoopGroup group;
  
  private volatile ChannelFactory<? extends C> channelFactory;
  
  private volatile SocketAddress localAddress;
  
  private final Map<ChannelOption<?>, Object> options = new LinkedHashMap();
  private final Map<AttributeKey<?>, Object> attrs = new ConcurrentHashMap();
  
  private volatile ChannelHandler handler;
  
  AbstractBootstrap() {}
  
  AbstractBootstrap(AbstractBootstrap<B, C> bootstrap)
  {
    group = group;
    channelFactory = channelFactory;
    handler = handler;
    localAddress = localAddress;
    synchronized (options) {
      options.putAll(options);
    }
    attrs.putAll(attrs);
  }
  



  public B group(EventLoopGroup group)
  {
    ObjectUtil.checkNotNull(group, "group");
    if (this.group != null) {
      throw new IllegalStateException("group set already");
    }
    this.group = group;
    return self();
  }
  
  private B self()
  {
    return this;
  }
  




  public B channel(Class<? extends C> channelClass)
  {
    return channelFactory(new ReflectiveChannelFactory(
      (Class)ObjectUtil.checkNotNull(channelClass, "channelClass")));
  }
  



  @Deprecated
  public B channelFactory(ChannelFactory<? extends C> channelFactory)
  {
    ObjectUtil.checkNotNull(channelFactory, "channelFactory");
    if (this.channelFactory != null) {
      throw new IllegalStateException("channelFactory set already");
    }
    
    this.channelFactory = channelFactory;
    return self();
  }
  







  public B channelFactory(io.netty.channel.ChannelFactory<? extends C> channelFactory)
  {
    return channelFactory(channelFactory);
  }
  


  public B localAddress(SocketAddress localAddress)
  {
    this.localAddress = localAddress;
    return self();
  }
  


  public B localAddress(int inetPort)
  {
    return localAddress(new InetSocketAddress(inetPort));
  }
  


  public B localAddress(String inetHost, int inetPort)
  {
    return localAddress(SocketUtils.socketAddress(inetHost, inetPort));
  }
  


  public B localAddress(InetAddress inetHost, int inetPort)
  {
    return localAddress(new InetSocketAddress(inetHost, inetPort));
  }
  



  public <T> B option(ChannelOption<T> option, T value)
  {
    ObjectUtil.checkNotNull(option, "option");
    synchronized (options) {
      if (value == null) {
        options.remove(option);
      } else {
        options.put(option, value);
      }
    }
    return self();
  }
  



  public <T> B attr(AttributeKey<T> key, T value)
  {
    ObjectUtil.checkNotNull(key, "key");
    if (value == null) {
      attrs.remove(key);
    } else {
      attrs.put(key, value);
    }
    return self();
  }
  



  public B validate()
  {
    if (group == null) {
      throw new IllegalStateException("group not set");
    }
    if (channelFactory == null) {
      throw new IllegalStateException("channel or channelFactory not set");
    }
    return self();
  }
  





  public abstract B clone();
  




  public ChannelFuture register()
  {
    validate();
    return initAndRegister();
  }
  


  public ChannelFuture bind()
  {
    validate();
    SocketAddress localAddress = this.localAddress;
    if (localAddress == null) {
      throw new IllegalStateException("localAddress not set");
    }
    return doBind(localAddress);
  }
  


  public ChannelFuture bind(int inetPort)
  {
    return bind(new InetSocketAddress(inetPort));
  }
  


  public ChannelFuture bind(String inetHost, int inetPort)
  {
    return bind(SocketUtils.socketAddress(inetHost, inetPort));
  }
  


  public ChannelFuture bind(InetAddress inetHost, int inetPort)
  {
    return bind(new InetSocketAddress(inetHost, inetPort));
  }
  


  public ChannelFuture bind(SocketAddress localAddress)
  {
    validate();
    return doBind((SocketAddress)ObjectUtil.checkNotNull(localAddress, "localAddress"));
  }
  
  private ChannelFuture doBind(final SocketAddress localAddress) {
    final ChannelFuture regFuture = initAndRegister();
    final Channel channel = regFuture.channel();
    if (regFuture.cause() != null) {
      return regFuture;
    }
    
    if (regFuture.isDone())
    {
      ChannelPromise promise = channel.newPromise();
      doBind0(regFuture, channel, localAddress, promise);
      return promise;
    }
    
    final PendingRegistrationPromise promise = new PendingRegistrationPromise(channel);
    regFuture.addListener(new ChannelFutureListener()
    {
      public void operationComplete(ChannelFuture future) throws Exception {
        Throwable cause = future.cause();
        if (cause != null)
        {

          promise.setFailure(cause);
        }
        else
        {
          promise.registered();
          
          AbstractBootstrap.doBind0(regFuture, channel, localAddress, promise);
        }
      }
    });
    return promise;
  }
  
  final ChannelFuture initAndRegister()
  {
    Channel channel = null;
    try {
      channel = channelFactory.newChannel();
      init(channel);
    } catch (Throwable t) {
      if (channel != null)
      {
        channel.unsafe().closeForcibly();
        
        return new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE).setFailure(t);
      }
      
      return new DefaultChannelPromise(new FailedChannel(), GlobalEventExecutor.INSTANCE).setFailure(t);
    }
    
    ChannelFuture regFuture = config().group().register(channel);
    if (regFuture.cause() != null) {
      if (channel.isRegistered()) {
        channel.close();
      } else {
        channel.unsafe().closeForcibly();
      }
    }
    









    return regFuture;
  }
  


  abstract void init(Channel paramChannel)
    throws Exception;
  

  private static void doBind0(ChannelFuture regFuture, final Channel channel, final SocketAddress localAddress, final ChannelPromise promise)
  {
    channel.eventLoop().execute(new Runnable()
    {
      public void run() {
        if (val$regFuture.isSuccess()) {
          channel.bind(localAddress, promise).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
        } else {
          promise.setFailure(val$regFuture.cause());
        }
      }
    });
  }
  


  public B handler(ChannelHandler handler)
  {
    this.handler = ((ChannelHandler)ObjectUtil.checkNotNull(handler, "handler"));
    return self();
  }
  




  @Deprecated
  public final EventLoopGroup group()
  {
    return group;
  }
  


  public abstract AbstractBootstrapConfig<B, C> config();
  

  final Map.Entry<ChannelOption<?>, Object>[] newOptionsArray()
  {
    return newOptionsArray(options);
  }
  
  static Map.Entry<ChannelOption<?>, Object>[] newOptionsArray(Map<ChannelOption<?>, Object> options) {
    synchronized (options) {
      return (Map.Entry[])new LinkedHashMap(options).entrySet().toArray(EMPTY_OPTION_ARRAY);
    }
  }
  
  final Map.Entry<AttributeKey<?>, Object>[] newAttributesArray() {
    return newAttributesArray(attrs0());
  }
  
  static Map.Entry<AttributeKey<?>, Object>[] newAttributesArray(Map<AttributeKey<?>, Object> attributes) {
    return (Map.Entry[])attributes.entrySet().toArray(EMPTY_ATTRIBUTE_ARRAY);
  }
  
  final Map<ChannelOption<?>, Object> options0() {
    return options;
  }
  
  final Map<AttributeKey<?>, Object> attrs0() {
    return attrs;
  }
  
  final SocketAddress localAddress() {
    return localAddress;
  }
  
  final ChannelFactory<? extends C> channelFactory()
  {
    return channelFactory;
  }
  
  final ChannelHandler handler() {
    return handler;
  }
  
  final Map<ChannelOption<?>, Object> options() {
    synchronized (options) {
      return copiedMap(options);
    }
  }
  
  final Map<AttributeKey<?>, Object> attrs() {
    return copiedMap(attrs);
  }
  
  static <K, V> Map<K, V> copiedMap(Map<K, V> map) {
    if (map.isEmpty()) {
      return Collections.emptyMap();
    }
    return Collections.unmodifiableMap(new HashMap(map));
  }
  
  static void setAttributes(Channel channel, Map.Entry<AttributeKey<?>, Object>[] attrs) {
    for (Map.Entry<AttributeKey<?>, Object> e : attrs)
    {
      AttributeKey<Object> key = (AttributeKey)e.getKey();
      channel.attr(key).set(e.getValue());
    }
  }
  
  static void setChannelOptions(Channel channel, Map.Entry<ChannelOption<?>, Object>[] options, InternalLogger logger)
  {
    for (Map.Entry<ChannelOption<?>, Object> e : options) {
      setChannelOption(channel, (ChannelOption)e.getKey(), e.getValue(), logger);
    }
  }
  
  private static void setChannelOption(Channel channel, ChannelOption<?> option, Object value, InternalLogger logger)
  {
    try
    {
      if (!channel.config().setOption(option, value)) {
        logger.warn("Unknown channel option '{}' for channel '{}'", option, channel);
      }
    } catch (Throwable t) {
      logger.warn("Failed to set channel option '{}' with value '{}' for channel '{}'", new Object[] { option, value, channel, t });
    }
  }
  



  public String toString()
  {
    StringBuilder buf = new StringBuilder().append(StringUtil.simpleClassName(this)).append('(').append(config()).append(')');
    return buf.toString();
  }
  
  static final class PendingRegistrationPromise
    extends DefaultChannelPromise
  {
    private volatile boolean registered;
    
    PendingRegistrationPromise(Channel channel)
    {
      super();
    }
    
    void registered() {
      registered = true;
    }
    
    protected EventExecutor executor()
    {
      if (registered)
      {


        return super.executor();
      }
      
      return GlobalEventExecutor.INSTANCE;
    }
  }
}
