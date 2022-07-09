package io.netty.channel.group;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelId;
import io.netty.channel.ServerChannel;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.StringUtil;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;


















public class DefaultChannelGroup
  extends AbstractSet<Channel>
  implements ChannelGroup
{
  private static final AtomicInteger nextId = new AtomicInteger();
  private final String name;
  private final EventExecutor executor;
  private final ConcurrentMap<ChannelId, Channel> serverChannels = PlatformDependent.newConcurrentHashMap();
  private final ConcurrentMap<ChannelId, Channel> nonServerChannels = PlatformDependent.newConcurrentHashMap();
  private final ChannelFutureListener remover = new ChannelFutureListener()
  {
    public void operationComplete(ChannelFuture future) throws Exception {
      remove(future.channel());
    }
  };
  private final VoidChannelGroupFuture voidFuture = new VoidChannelGroupFuture(this);
  
  private final boolean stayClosed;
  
  private volatile boolean closed;
  

  public DefaultChannelGroup(EventExecutor executor)
  {
    this(executor, false);
  }
  




  public DefaultChannelGroup(String name, EventExecutor executor)
  {
    this(name, executor, false);
  }
  





  public DefaultChannelGroup(EventExecutor executor, boolean stayClosed)
  {
    this("group-0x" + Integer.toHexString(nextId.incrementAndGet()), executor, stayClosed);
  }
  






  public DefaultChannelGroup(String name, EventExecutor executor, boolean stayClosed)
  {
    ObjectUtil.checkNotNull(name, "name");
    this.name = name;
    this.executor = executor;
    this.stayClosed = stayClosed;
  }
  
  public String name()
  {
    return name;
  }
  
  public Channel find(ChannelId id)
  {
    Channel c = (Channel)nonServerChannels.get(id);
    if (c != null) {
      return c;
    }
    return (Channel)serverChannels.get(id);
  }
  

  public boolean isEmpty()
  {
    return (nonServerChannels.isEmpty()) && (serverChannels.isEmpty());
  }
  
  public int size()
  {
    return nonServerChannels.size() + serverChannels.size();
  }
  
  public boolean contains(Object o)
  {
    if ((o instanceof ServerChannel))
      return serverChannels.containsValue(o);
    if ((o instanceof Channel)) {
      return nonServerChannels.containsValue(o);
    }
    return false;
  }
  
  public boolean add(Channel channel)
  {
    ConcurrentMap<ChannelId, Channel> map = (channel instanceof ServerChannel) ? serverChannels : nonServerChannels;
    

    boolean added = map.putIfAbsent(channel.id(), channel) == null;
    if (added) {
      channel.closeFuture().addListener(remover);
    }
    
    if ((stayClosed) && (closed))
    {











      channel.close();
    }
    
    return added;
  }
  
  public boolean remove(Object o)
  {
    Channel c = null;
    if ((o instanceof ChannelId)) {
      c = (Channel)nonServerChannels.remove(o);
      if (c == null) {
        c = (Channel)serverChannels.remove(o);
      }
    } else if ((o instanceof Channel)) {
      c = (Channel)o;
      if ((c instanceof ServerChannel)) {
        c = (Channel)serverChannels.remove(c.id());
      } else {
        c = (Channel)nonServerChannels.remove(c.id());
      }
    }
    
    if (c == null) {
      return false;
    }
    
    c.closeFuture().removeListener(remover);
    return true;
  }
  
  public void clear()
  {
    nonServerChannels.clear();
    serverChannels.clear();
  }
  
  public Iterator<Channel> iterator()
  {
    return new CombinedIterator(serverChannels
      .values().iterator(), nonServerChannels
      .values().iterator());
  }
  
  public Object[] toArray()
  {
    Collection<Channel> channels = new ArrayList(size());
    channels.addAll(serverChannels.values());
    channels.addAll(nonServerChannels.values());
    return channels.toArray();
  }
  
  public <T> T[] toArray(T[] a)
  {
    Collection<Channel> channels = new ArrayList(size());
    channels.addAll(serverChannels.values());
    channels.addAll(nonServerChannels.values());
    return channels.toArray(a);
  }
  
  public ChannelGroupFuture close()
  {
    return close(ChannelMatchers.all());
  }
  
  public ChannelGroupFuture disconnect()
  {
    return disconnect(ChannelMatchers.all());
  }
  
  public ChannelGroupFuture deregister()
  {
    return deregister(ChannelMatchers.all());
  }
  
  public ChannelGroupFuture write(Object message)
  {
    return write(message, ChannelMatchers.all());
  }
  

  private static Object safeDuplicate(Object message)
  {
    if ((message instanceof ByteBuf))
      return ((ByteBuf)message).retainedDuplicate();
    if ((message instanceof ByteBufHolder)) {
      return ((ByteBufHolder)message).retainedDuplicate();
    }
    return ReferenceCountUtil.retain(message);
  }
  

  public ChannelGroupFuture write(Object message, ChannelMatcher matcher)
  {
    return write(message, matcher, false);
  }
  
  public ChannelGroupFuture write(Object message, ChannelMatcher matcher, boolean voidPromise)
  {
    ObjectUtil.checkNotNull(message, "message");
    ObjectUtil.checkNotNull(matcher, "matcher");
    Channel c;
    ChannelGroupFuture future;
    ChannelGroupFuture future; if (voidPromise) {
      for (Iterator localIterator = nonServerChannels.values().iterator(); localIterator.hasNext();) { c = (Channel)localIterator.next();
        if (matcher.matches(c)) {
          c.write(safeDuplicate(message), c.voidPromise());
        }
      }
      future = voidFuture;
    } else {
      Object futures = new LinkedHashMap(nonServerChannels.size());
      for (Channel c : nonServerChannels.values()) {
        if (matcher.matches(c)) {
          ((Map)futures).put(c, c.write(safeDuplicate(message)));
        }
      }
      future = new DefaultChannelGroupFuture(this, (Map)futures, executor);
    }
    ReferenceCountUtil.release(message);
    return future;
  }
  
  public ChannelGroup flush()
  {
    return flush(ChannelMatchers.all());
  }
  
  public ChannelGroupFuture flushAndWrite(Object message)
  {
    return writeAndFlush(message);
  }
  
  public ChannelGroupFuture writeAndFlush(Object message)
  {
    return writeAndFlush(message, ChannelMatchers.all());
  }
  
  public ChannelGroupFuture disconnect(ChannelMatcher matcher)
  {
    ObjectUtil.checkNotNull(matcher, "matcher");
    

    Map<Channel, ChannelFuture> futures = new LinkedHashMap(size());
    
    for (Channel c : serverChannels.values()) {
      if (matcher.matches(c)) {
        futures.put(c, c.disconnect());
      }
    }
    for (Channel c : nonServerChannels.values()) {
      if (matcher.matches(c)) {
        futures.put(c, c.disconnect());
      }
    }
    
    return new DefaultChannelGroupFuture(this, futures, executor);
  }
  
  public ChannelGroupFuture close(ChannelMatcher matcher)
  {
    ObjectUtil.checkNotNull(matcher, "matcher");
    

    Map<Channel, ChannelFuture> futures = new LinkedHashMap(size());
    
    if (stayClosed)
    {





      closed = true;
    }
    
    for (Channel c : serverChannels.values()) {
      if (matcher.matches(c)) {
        futures.put(c, c.close());
      }
    }
    for (Channel c : nonServerChannels.values()) {
      if (matcher.matches(c)) {
        futures.put(c, c.close());
      }
    }
    
    return new DefaultChannelGroupFuture(this, futures, executor);
  }
  
  public ChannelGroupFuture deregister(ChannelMatcher matcher)
  {
    ObjectUtil.checkNotNull(matcher, "matcher");
    

    Map<Channel, ChannelFuture> futures = new LinkedHashMap(size());
    
    for (Channel c : serverChannels.values()) {
      if (matcher.matches(c)) {
        futures.put(c, c.deregister());
      }
    }
    for (Channel c : nonServerChannels.values()) {
      if (matcher.matches(c)) {
        futures.put(c, c.deregister());
      }
    }
    
    return new DefaultChannelGroupFuture(this, futures, executor);
  }
  
  public ChannelGroup flush(ChannelMatcher matcher)
  {
    for (Channel c : nonServerChannels.values()) {
      if (matcher.matches(c)) {
        c.flush();
      }
    }
    return this;
  }
  
  public ChannelGroupFuture flushAndWrite(Object message, ChannelMatcher matcher)
  {
    return writeAndFlush(message, matcher);
  }
  
  public ChannelGroupFuture writeAndFlush(Object message, ChannelMatcher matcher)
  {
    return writeAndFlush(message, matcher, false);
  }
  
  public ChannelGroupFuture writeAndFlush(Object message, ChannelMatcher matcher, boolean voidPromise)
  {
    ObjectUtil.checkNotNull(message, "message");
    Channel c;
    ChannelGroupFuture future;
    ChannelGroupFuture future; if (voidPromise) {
      for (Iterator localIterator = nonServerChannels.values().iterator(); localIterator.hasNext();) { c = (Channel)localIterator.next();
        if (matcher.matches(c)) {
          c.writeAndFlush(safeDuplicate(message), c.voidPromise());
        }
      }
      future = voidFuture;
    } else {
      Object futures = new LinkedHashMap(nonServerChannels.size());
      for (Channel c : nonServerChannels.values()) {
        if (matcher.matches(c)) {
          ((Map)futures).put(c, c.writeAndFlush(safeDuplicate(message)));
        }
      }
      future = new DefaultChannelGroupFuture(this, (Map)futures, executor);
    }
    ReferenceCountUtil.release(message);
    return future;
  }
  
  public ChannelGroupFuture newCloseFuture()
  {
    return newCloseFuture(ChannelMatchers.all());
  }
  

  public ChannelGroupFuture newCloseFuture(ChannelMatcher matcher)
  {
    Map<Channel, ChannelFuture> futures = new LinkedHashMap(size());
    
    for (Channel c : serverChannels.values()) {
      if (matcher.matches(c)) {
        futures.put(c, c.closeFuture());
      }
    }
    for (Channel c : nonServerChannels.values()) {
      if (matcher.matches(c)) {
        futures.put(c, c.closeFuture());
      }
    }
    
    return new DefaultChannelGroupFuture(this, futures, executor);
  }
  
  public int hashCode()
  {
    return System.identityHashCode(this);
  }
  
  public boolean equals(Object o)
  {
    return this == o;
  }
  
  public int compareTo(ChannelGroup o)
  {
    int v = name().compareTo(o.name());
    if (v != 0) {
      return v;
    }
    
    return System.identityHashCode(this) - System.identityHashCode(o);
  }
  
  public String toString()
  {
    return StringUtil.simpleClassName(this) + "(name: " + name() + ", size: " + size() + ')';
  }
}
