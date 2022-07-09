package io.netty.bootstrap;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.util.AttributeKey;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;
import java.net.SocketAddress;
import java.util.Map;



















public abstract class AbstractBootstrapConfig<B extends AbstractBootstrap<B, C>, C extends Channel>
{
  protected final B bootstrap;
  
  protected AbstractBootstrapConfig(B bootstrap)
  {
    this.bootstrap = ((AbstractBootstrap)ObjectUtil.checkNotNull(bootstrap, "bootstrap"));
  }
  


  public final SocketAddress localAddress()
  {
    return bootstrap.localAddress();
  }
  



  public final ChannelFactory<? extends C> channelFactory()
  {
    return bootstrap.channelFactory();
  }
  


  public final ChannelHandler handler()
  {
    return bootstrap.handler();
  }
  


  public final Map<ChannelOption<?>, Object> options()
  {
    return bootstrap.options();
  }
  


  public final Map<AttributeKey<?>, Object> attrs()
  {
    return bootstrap.attrs();
  }
  



  public final EventLoopGroup group()
  {
    return bootstrap.group();
  }
  


  public String toString()
  {
    StringBuilder buf = new StringBuilder().append(StringUtil.simpleClassName(this)).append('(');
    EventLoopGroup group = group();
    if (group != null)
    {

      buf.append("group: ").append(StringUtil.simpleClassName(group)).append(", ");
    }
    
    ChannelFactory<? extends C> factory = channelFactory();
    if (factory != null)
    {

      buf.append("channelFactory: ").append(factory).append(", ");
    }
    SocketAddress localAddress = localAddress();
    if (localAddress != null)
    {

      buf.append("localAddress: ").append(localAddress).append(", ");
    }
    
    Map<ChannelOption<?>, Object> options = options();
    if (!options.isEmpty())
    {

      buf.append("options: ").append(options).append(", ");
    }
    Map<AttributeKey<?>, Object> attrs = attrs();
    if (!attrs.isEmpty())
    {

      buf.append("attrs: ").append(attrs).append(", ");
    }
    ChannelHandler handler = handler();
    if (handler != null)
    {

      buf.append("handler: ").append(handler).append(", ");
    }
    if (buf.charAt(buf.length() - 1) == '(') {
      buf.append(')');
    } else {
      buf.setCharAt(buf.length() - 2, ')');
      buf.setLength(buf.length() - 1);
    }
    return buf.toString();
  }
}
