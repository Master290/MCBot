package io.netty.channel;

import io.netty.util.ReferenceCountUtil;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;




















public class DefaultChannelPipeline
  implements ChannelPipeline
{
  static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultChannelPipeline.class);
  
  private static final String HEAD_NAME = generateName0(HeadContext.class);
  private static final String TAIL_NAME = generateName0(TailContext.class);
  
  private static final FastThreadLocal<Map<Class<?>, String>> nameCaches = new FastThreadLocal()
  {
    protected Map<Class<?>, String> initialValue()
    {
      return new WeakHashMap();
    }
  };
  

  private static final AtomicReferenceFieldUpdater<DefaultChannelPipeline, MessageSizeEstimator.Handle> ESTIMATOR = AtomicReferenceFieldUpdater.newUpdater(DefaultChannelPipeline.class, MessageSizeEstimator.Handle.class, "estimatorHandle");
  
  final AbstractChannelHandlerContext head;
  
  final AbstractChannelHandlerContext tail;
  private final Channel channel;
  private final ChannelFuture succeededFuture;
  private final VoidChannelPromise voidPromise;
  private final boolean touch = ResourceLeakDetector.isEnabled();
  
  private Map<EventExecutorGroup, EventExecutor> childExecutors;
  private volatile MessageSizeEstimator.Handle estimatorHandle;
  private boolean firstRegistration = true;
  




  private PendingHandlerCallback pendingHandlerCallbackHead;
  



  private boolean registered;
  




  protected DefaultChannelPipeline(Channel channel)
  {
    this.channel = ((Channel)ObjectUtil.checkNotNull(channel, "channel"));
    succeededFuture = new SucceededChannelFuture(channel, null);
    voidPromise = new VoidChannelPromise(channel, true);
    
    tail = new TailContext(this);
    head = new HeadContext(this);
    
    head.next = tail;
    tail.prev = head;
  }
  
  final MessageSizeEstimator.Handle estimatorHandle() {
    MessageSizeEstimator.Handle handle = estimatorHandle;
    if (handle == null) {
      handle = channel.config().getMessageSizeEstimator().newHandle();
      if (!ESTIMATOR.compareAndSet(this, null, handle)) {
        handle = estimatorHandle;
      }
    }
    return handle;
  }
  
  final Object touch(Object msg, AbstractChannelHandlerContext next) {
    return touch ? ReferenceCountUtil.touch(msg, next) : msg;
  }
  
  private AbstractChannelHandlerContext newContext(EventExecutorGroup group, String name, ChannelHandler handler) {
    return new DefaultChannelHandlerContext(this, childExecutor(group), name, handler);
  }
  
  private EventExecutor childExecutor(EventExecutorGroup group) {
    if (group == null) {
      return null;
    }
    Boolean pinEventExecutor = (Boolean)channel.config().getOption(ChannelOption.SINGLE_EVENTEXECUTOR_PER_GROUP);
    if ((pinEventExecutor != null) && (!pinEventExecutor.booleanValue())) {
      return group.next();
    }
    Map<EventExecutorGroup, EventExecutor> childExecutors = this.childExecutors;
    if (childExecutors == null)
    {
      childExecutors = this.childExecutors = new IdentityHashMap(4);
    }
    

    EventExecutor childExecutor = (EventExecutor)childExecutors.get(group);
    if (childExecutor == null) {
      childExecutor = group.next();
      childExecutors.put(group, childExecutor);
    }
    return childExecutor;
  }
  
  public final Channel channel() {
    return channel;
  }
  
  public final ChannelPipeline addFirst(String name, ChannelHandler handler)
  {
    return addFirst(null, name, handler);
  }
  

  public final ChannelPipeline addFirst(EventExecutorGroup group, String name, ChannelHandler handler)
  {
    synchronized (this) {
      checkMultiplicity(handler);
      name = filterName(name, handler);
      
      AbstractChannelHandlerContext newCtx = newContext(group, name, handler);
      
      addFirst0(newCtx);
      



      if (!registered) {
        newCtx.setAddPending();
        callHandlerCallbackLater(newCtx, true);
        return this;
      }
      
      EventExecutor executor = newCtx.executor();
      if (!executor.inEventLoop()) {
        callHandlerAddedInEventLoop(newCtx, executor);
        return this;
      } }
    AbstractChannelHandlerContext newCtx;
    callHandlerAdded0(newCtx);
    return this;
  }
  
  private void addFirst0(AbstractChannelHandlerContext newCtx) {
    AbstractChannelHandlerContext nextCtx = head.next;
    prev = head;
    next = nextCtx;
    head.next = newCtx;
    prev = newCtx;
  }
  
  public final ChannelPipeline addLast(String name, ChannelHandler handler)
  {
    return addLast(null, name, handler);
  }
  

  public final ChannelPipeline addLast(EventExecutorGroup group, String name, ChannelHandler handler)
  {
    synchronized (this) {
      checkMultiplicity(handler);
      
      AbstractChannelHandlerContext newCtx = newContext(group, filterName(name, handler), handler);
      
      addLast0(newCtx);
      



      if (!registered) {
        newCtx.setAddPending();
        callHandlerCallbackLater(newCtx, true);
        return this;
      }
      
      EventExecutor executor = newCtx.executor();
      if (!executor.inEventLoop()) {
        callHandlerAddedInEventLoop(newCtx, executor);
        return this;
      } }
    AbstractChannelHandlerContext newCtx;
    callHandlerAdded0(newCtx);
    return this;
  }
  
  private void addLast0(AbstractChannelHandlerContext newCtx) {
    AbstractChannelHandlerContext prev = tail.prev;
    prev = prev;
    next = tail;
    next = newCtx;
    tail.prev = newCtx;
  }
  
  public final ChannelPipeline addBefore(String baseName, String name, ChannelHandler handler)
  {
    return addBefore(null, baseName, name, handler);
  }
  



  public final ChannelPipeline addBefore(EventExecutorGroup group, String baseName, String name, ChannelHandler handler)
  {
    synchronized (this) {
      checkMultiplicity(handler);
      name = filterName(name, handler);
      AbstractChannelHandlerContext ctx = getContextOrDie(baseName);
      
      AbstractChannelHandlerContext newCtx = newContext(group, name, handler);
      
      addBefore0(ctx, newCtx);
      



      if (!registered) {
        newCtx.setAddPending();
        callHandlerCallbackLater(newCtx, true);
        return this;
      }
      
      EventExecutor executor = newCtx.executor();
      if (!executor.inEventLoop()) {
        callHandlerAddedInEventLoop(newCtx, executor);
        return this; } }
    AbstractChannelHandlerContext ctx;
    AbstractChannelHandlerContext newCtx;
    callHandlerAdded0(newCtx);
    return this;
  }
  
  private static void addBefore0(AbstractChannelHandlerContext ctx, AbstractChannelHandlerContext newCtx) {
    prev = prev;
    next = ctx;
    prev.next = newCtx;
    prev = newCtx;
  }
  
  private String filterName(String name, ChannelHandler handler) {
    if (name == null) {
      return generateName(handler);
    }
    checkDuplicateName(name);
    return name;
  }
  
  public final ChannelPipeline addAfter(String baseName, String name, ChannelHandler handler)
  {
    return addAfter(null, baseName, name, handler);
  }
  




  public final ChannelPipeline addAfter(EventExecutorGroup group, String baseName, String name, ChannelHandler handler)
  {
    synchronized (this) {
      checkMultiplicity(handler);
      name = filterName(name, handler);
      AbstractChannelHandlerContext ctx = getContextOrDie(baseName);
      
      AbstractChannelHandlerContext newCtx = newContext(group, name, handler);
      
      addAfter0(ctx, newCtx);
      



      if (!registered) {
        newCtx.setAddPending();
        callHandlerCallbackLater(newCtx, true);
        return this;
      }
      EventExecutor executor = newCtx.executor();
      if (!executor.inEventLoop()) {
        callHandlerAddedInEventLoop(newCtx, executor);
        return this; } }
    AbstractChannelHandlerContext ctx;
    AbstractChannelHandlerContext newCtx;
    callHandlerAdded0(newCtx);
    return this;
  }
  
  private static void addAfter0(AbstractChannelHandlerContext ctx, AbstractChannelHandlerContext newCtx) {
    prev = ctx;
    next = next;
    next.prev = newCtx;
    next = newCtx;
  }
  
  public final ChannelPipeline addFirst(ChannelHandler handler) {
    return addFirst(null, handler);
  }
  
  public final ChannelPipeline addFirst(ChannelHandler... handlers)
  {
    return addFirst(null, handlers);
  }
  
  public final ChannelPipeline addFirst(EventExecutorGroup executor, ChannelHandler... handlers)
  {
    ObjectUtil.checkNotNull(handlers, "handlers");
    if ((handlers.length == 0) || (handlers[0] == null)) {
      return this;
    }
    

    for (int size = 1; size < handlers.length; size++) {
      if (handlers[size] == null) {
        break;
      }
    }
    
    for (int i = size - 1; i >= 0; i--) {
      ChannelHandler h = handlers[i];
      addFirst(executor, null, h);
    }
    
    return this;
  }
  
  public final ChannelPipeline addLast(ChannelHandler handler) {
    return addLast(null, handler);
  }
  
  public final ChannelPipeline addLast(ChannelHandler... handlers)
  {
    return addLast(null, handlers);
  }
  
  public final ChannelPipeline addLast(EventExecutorGroup executor, ChannelHandler... handlers)
  {
    ObjectUtil.checkNotNull(handlers, "handlers");
    
    for (ChannelHandler h : handlers) {
      if (h == null) {
        break;
      }
      addLast(executor, null, h);
    }
    
    return this;
  }
  
  private String generateName(ChannelHandler handler) {
    Map<Class<?>, String> cache = (Map)nameCaches.get();
    Class<?> handlerType = handler.getClass();
    String name = (String)cache.get(handlerType);
    if (name == null) {
      name = generateName0(handlerType);
      cache.put(handlerType, name);
    }
    


    if (context0(name) != null) {
      String baseName = name.substring(0, name.length() - 1);
      for (int i = 1;; i++) {
        String newName = baseName + i;
        if (context0(newName) == null) {
          name = newName;
          break;
        }
      }
    }
    return name;
  }
  
  private static String generateName0(Class<?> handlerType) {
    return StringUtil.simpleClassName(handlerType) + "#0";
  }
  
  public final ChannelPipeline remove(ChannelHandler handler)
  {
    remove(getContextOrDie(handler));
    return this;
  }
  
  public final ChannelHandler remove(String name)
  {
    return remove(getContextOrDie(name)).handler();
  }
  

  public final <T extends ChannelHandler> T remove(Class<T> handlerType)
  {
    return remove(getContextOrDie(handlerType)).handler();
  }
  
  public final <T extends ChannelHandler> T removeIfExists(String name) {
    return removeIfExists(context(name));
  }
  
  public final <T extends ChannelHandler> T removeIfExists(Class<T> handlerType) {
    return removeIfExists(context(handlerType));
  }
  
  public final <T extends ChannelHandler> T removeIfExists(ChannelHandler handler) {
    return removeIfExists(context(handler));
  }
  
  private <T extends ChannelHandler> T removeIfExists(ChannelHandlerContext ctx)
  {
    if (ctx == null) {
      return null;
    }
    return remove((AbstractChannelHandlerContext)ctx).handler();
  }
  
  private AbstractChannelHandlerContext remove(final AbstractChannelHandlerContext ctx) {
    assert ((ctx != head) && (ctx != tail));
    
    synchronized (this) {
      atomicRemoveFromHandlerList(ctx);
      



      if (!registered) {
        callHandlerCallbackLater(ctx, false);
        return ctx;
      }
      
      EventExecutor executor = ctx.executor();
      if (!executor.inEventLoop()) {
        executor.execute(new Runnable()
        {
          public void run() {
            DefaultChannelPipeline.this.callHandlerRemoved0(ctx);
          }
        });
        return ctx;
      }
    }
    callHandlerRemoved0(ctx);
    return ctx;
  }
  


  private synchronized void atomicRemoveFromHandlerList(AbstractChannelHandlerContext ctx)
  {
    AbstractChannelHandlerContext prev = prev;
    AbstractChannelHandlerContext next = next;
    next = next;
    prev = prev;
  }
  
  public final ChannelHandler removeFirst()
  {
    if (head.next == tail) {
      throw new NoSuchElementException();
    }
    return remove(head.next).handler();
  }
  
  public final ChannelHandler removeLast()
  {
    if (head.next == tail) {
      throw new NoSuchElementException();
    }
    return remove(tail.prev).handler();
  }
  
  public final ChannelPipeline replace(ChannelHandler oldHandler, String newName, ChannelHandler newHandler)
  {
    replace(getContextOrDie(oldHandler), newName, newHandler);
    return this;
  }
  
  public final ChannelHandler replace(String oldName, String newName, ChannelHandler newHandler)
  {
    return replace(getContextOrDie(oldName), newName, newHandler);
  }
  


  public final <T extends ChannelHandler> T replace(Class<T> oldHandlerType, String newName, ChannelHandler newHandler)
  {
    return replace(getContextOrDie(oldHandlerType), newName, newHandler);
  }
  
  private ChannelHandler replace(final AbstractChannelHandlerContext ctx, String newName, ChannelHandler newHandler)
  {
    assert ((ctx != head) && (ctx != tail));
    

    synchronized (this) {
      checkMultiplicity(newHandler);
      if (newName == null) {
        newName = generateName(newHandler);
      } else {
        boolean sameName = ctx.name().equals(newName);
        if (!sameName) {
          checkDuplicateName(newName);
        }
      }
      
      final AbstractChannelHandlerContext newCtx = newContext(executor, newName, newHandler);
      
      replace0(ctx, newCtx);
      




      if (!registered) {
        callHandlerCallbackLater(newCtx, true);
        callHandlerCallbackLater(ctx, false);
        return ctx.handler();
      }
      EventExecutor executor = ctx.executor();
      if (!executor.inEventLoop()) {
        executor.execute(new Runnable()
        {

          public void run()
          {

            DefaultChannelPipeline.this.callHandlerAdded0(newCtx);
            DefaultChannelPipeline.this.callHandlerRemoved0(ctx);
          }
        });
        return ctx.handler();
      }
    }
    
    AbstractChannelHandlerContext newCtx;
    
    callHandlerAdded0(newCtx);
    callHandlerRemoved0(ctx);
    return ctx.handler();
  }
  
  private static void replace0(AbstractChannelHandlerContext oldCtx, AbstractChannelHandlerContext newCtx) {
    AbstractChannelHandlerContext prev = oldCtx.prev;
    AbstractChannelHandlerContext next = oldCtx.next;
    prev = prev;
    next = next;
    




    next = newCtx;
    prev = newCtx;
    

    oldCtx.prev = newCtx;
    oldCtx.next = newCtx;
  }
  
  private static void checkMultiplicity(ChannelHandler handler) {
    if ((handler instanceof ChannelHandlerAdapter)) {
      ChannelHandlerAdapter h = (ChannelHandlerAdapter)handler;
      if ((!h.isSharable()) && (added))
      {
        throw new ChannelPipelineException(h.getClass().getName() + " is not a @Sharable handler, so can't be added or removed multiple times.");
      }
      
      added = true;
    }
  }
  
  private void callHandlerAdded0(AbstractChannelHandlerContext ctx) {
    try {
      ctx.callHandlerAdded();
    } catch (Throwable t) {
      boolean removed = false;
      try {
        atomicRemoveFromHandlerList(ctx);
        ctx.callHandlerRemoved();
        removed = true;
      } catch (Throwable t2) {
        if (logger.isWarnEnabled()) {
          logger.warn("Failed to remove a handler: " + ctx.name(), t2);
        }
      }
      
      if (removed) {
        fireExceptionCaught(new ChannelPipelineException(ctx
          .handler().getClass().getName() + ".handlerAdded() has thrown an exception; removed.", t));
      }
      else {
        fireExceptionCaught(new ChannelPipelineException(ctx
          .handler().getClass().getName() + ".handlerAdded() has thrown an exception; also failed to remove.", t));
      }
    }
  }
  
  private void callHandlerRemoved0(AbstractChannelHandlerContext ctx)
  {
    try
    {
      ctx.callHandlerRemoved();
    } catch (Throwable t) {
      fireExceptionCaught(new ChannelPipelineException(ctx
        .handler().getClass().getName() + ".handlerRemoved() has thrown an exception.", t));
    }
  }
  
  final void invokeHandlerAddedIfNeeded() {
    assert (channel.eventLoop().inEventLoop());
    if (firstRegistration) {
      firstRegistration = false;
      

      callHandlerAddedForAllHandlers();
    }
  }
  
  public final ChannelHandler first()
  {
    ChannelHandlerContext first = firstContext();
    if (first == null) {
      return null;
    }
    return first.handler();
  }
  
  public final ChannelHandlerContext firstContext()
  {
    AbstractChannelHandlerContext first = head.next;
    if (first == tail) {
      return null;
    }
    return head.next;
  }
  
  public final ChannelHandler last()
  {
    AbstractChannelHandlerContext last = tail.prev;
    if (last == head) {
      return null;
    }
    return last.handler();
  }
  
  public final ChannelHandlerContext lastContext()
  {
    AbstractChannelHandlerContext last = tail.prev;
    if (last == head) {
      return null;
    }
    return last;
  }
  
  public final ChannelHandler get(String name)
  {
    ChannelHandlerContext ctx = context(name);
    if (ctx == null) {
      return null;
    }
    return ctx.handler();
  }
  


  public final <T extends ChannelHandler> T get(Class<T> handlerType)
  {
    ChannelHandlerContext ctx = context(handlerType);
    if (ctx == null) {
      return null;
    }
    return ctx.handler();
  }
  

  public final ChannelHandlerContext context(String name)
  {
    return context0((String)ObjectUtil.checkNotNull(name, "name"));
  }
  
  public final ChannelHandlerContext context(ChannelHandler handler)
  {
    ObjectUtil.checkNotNull(handler, "handler");
    
    AbstractChannelHandlerContext ctx = head.next;
    for (;;)
    {
      if (ctx == null) {
        return null;
      }
      
      if (ctx.handler() == handler) {
        return ctx;
      }
      
      ctx = next;
    }
  }
  
  public final ChannelHandlerContext context(Class<? extends ChannelHandler> handlerType)
  {
    ObjectUtil.checkNotNull(handlerType, "handlerType");
    
    AbstractChannelHandlerContext ctx = head.next;
    for (;;) {
      if (ctx == null) {
        return null;
      }
      if (handlerType.isAssignableFrom(ctx.handler().getClass())) {
        return ctx;
      }
      ctx = next;
    }
  }
  
  public final List<String> names()
  {
    List<String> list = new ArrayList();
    AbstractChannelHandlerContext ctx = head.next;
    for (;;) {
      if (ctx == null) {
        return list;
      }
      list.add(ctx.name());
      ctx = next;
    }
  }
  
  public final Map<String, ChannelHandler> toMap()
  {
    Map<String, ChannelHandler> map = new LinkedHashMap();
    AbstractChannelHandlerContext ctx = head.next;
    for (;;) {
      if (ctx == tail) {
        return map;
      }
      map.put(ctx.name(), ctx.handler());
      ctx = next;
    }
  }
  
  public final Iterator<Map.Entry<String, ChannelHandler>> iterator()
  {
    return toMap().entrySet().iterator();
  }
  





  public final String toString()
  {
    StringBuilder buf = new StringBuilder().append(StringUtil.simpleClassName(this)).append('{');
    AbstractChannelHandlerContext ctx = head.next;
    
    while (ctx != tail)
    {






      buf.append('(').append(ctx.name()).append(" = ").append(ctx.handler().getClass().getName()).append(')');
      
      ctx = next;
      if (ctx == tail) {
        break;
      }
      
      buf.append(", ");
    }
    buf.append('}');
    return buf.toString();
  }
  
  public final ChannelPipeline fireChannelRegistered()
  {
    AbstractChannelHandlerContext.invokeChannelRegistered(head);
    return this;
  }
  
  public final ChannelPipeline fireChannelUnregistered()
  {
    AbstractChannelHandlerContext.invokeChannelUnregistered(head);
    return this;
  }
  









  private synchronized void destroy()
  {
    destroyUp(head.next, false);
  }
  
  private void destroyUp(AbstractChannelHandlerContext ctx, boolean inEventLoop) {
    Thread currentThread = Thread.currentThread();
    AbstractChannelHandlerContext tail = this.tail;
    for (;;) {
      if (ctx == tail) {
        destroyDown(currentThread, prev, inEventLoop);
        break;
      }
      
      EventExecutor executor = ctx.executor();
      if ((!inEventLoop) && (!executor.inEventLoop(currentThread))) {
        final AbstractChannelHandlerContext finalCtx = ctx;
        executor.execute(new Runnable()
        {
          public void run() {
            DefaultChannelPipeline.this.destroyUp(finalCtx, true);
          }
        });
        break;
      }
      
      ctx = next;
      inEventLoop = false;
    }
  }
  
  private void destroyDown(Thread currentThread, AbstractChannelHandlerContext ctx, boolean inEventLoop)
  {
    AbstractChannelHandlerContext head = this.head;
    
    while (ctx != head)
    {


      EventExecutor executor = ctx.executor();
      if ((inEventLoop) || (executor.inEventLoop(currentThread))) {
        atomicRemoveFromHandlerList(ctx);
        callHandlerRemoved0(ctx);
      } else {
        final AbstractChannelHandlerContext finalCtx = ctx;
        executor.execute(new Runnable()
        {
          public void run() {
            DefaultChannelPipeline.this.destroyDown(Thread.currentThread(), finalCtx, true);
          }
        });
        break;
      }
      
      ctx = prev;
      inEventLoop = false;
    }
  }
  
  public final ChannelPipeline fireChannelActive()
  {
    AbstractChannelHandlerContext.invokeChannelActive(head);
    return this;
  }
  
  public final ChannelPipeline fireChannelInactive()
  {
    AbstractChannelHandlerContext.invokeChannelInactive(head);
    return this;
  }
  
  public final ChannelPipeline fireExceptionCaught(Throwable cause)
  {
    AbstractChannelHandlerContext.invokeExceptionCaught(head, cause);
    return this;
  }
  
  public final ChannelPipeline fireUserEventTriggered(Object event)
  {
    AbstractChannelHandlerContext.invokeUserEventTriggered(head, event);
    return this;
  }
  
  public final ChannelPipeline fireChannelRead(Object msg)
  {
    AbstractChannelHandlerContext.invokeChannelRead(head, msg);
    return this;
  }
  
  public final ChannelPipeline fireChannelReadComplete()
  {
    AbstractChannelHandlerContext.invokeChannelReadComplete(head);
    return this;
  }
  
  public final ChannelPipeline fireChannelWritabilityChanged()
  {
    AbstractChannelHandlerContext.invokeChannelWritabilityChanged(head);
    return this;
  }
  
  public final ChannelFuture bind(SocketAddress localAddress)
  {
    return tail.bind(localAddress);
  }
  
  public final ChannelFuture connect(SocketAddress remoteAddress)
  {
    return tail.connect(remoteAddress);
  }
  
  public final ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress)
  {
    return tail.connect(remoteAddress, localAddress);
  }
  
  public final ChannelFuture disconnect()
  {
    return tail.disconnect();
  }
  
  public final ChannelFuture close()
  {
    return tail.close();
  }
  
  public final ChannelFuture deregister()
  {
    return tail.deregister();
  }
  
  public final ChannelPipeline flush()
  {
    tail.flush();
    return this;
  }
  
  public final ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise)
  {
    return tail.bind(localAddress, promise);
  }
  
  public final ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise)
  {
    return tail.connect(remoteAddress, promise);
  }
  

  public final ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise)
  {
    return tail.connect(remoteAddress, localAddress, promise);
  }
  
  public final ChannelFuture disconnect(ChannelPromise promise)
  {
    return tail.disconnect(promise);
  }
  
  public final ChannelFuture close(ChannelPromise promise)
  {
    return tail.close(promise);
  }
  
  public final ChannelFuture deregister(ChannelPromise promise)
  {
    return tail.deregister(promise);
  }
  
  public final ChannelPipeline read()
  {
    tail.read();
    return this;
  }
  
  public final ChannelFuture write(Object msg)
  {
    return tail.write(msg);
  }
  
  public final ChannelFuture write(Object msg, ChannelPromise promise)
  {
    return tail.write(msg, promise);
  }
  
  public final ChannelFuture writeAndFlush(Object msg, ChannelPromise promise)
  {
    return tail.writeAndFlush(msg, promise);
  }
  
  public final ChannelFuture writeAndFlush(Object msg)
  {
    return tail.writeAndFlush(msg);
  }
  
  public final ChannelPromise newPromise()
  {
    return new DefaultChannelPromise(channel);
  }
  
  public final ChannelProgressivePromise newProgressivePromise()
  {
    return new DefaultChannelProgressivePromise(channel);
  }
  
  public final ChannelFuture newSucceededFuture()
  {
    return succeededFuture;
  }
  
  public final ChannelFuture newFailedFuture(Throwable cause)
  {
    return new FailedChannelFuture(channel, null, cause);
  }
  
  public final ChannelPromise voidPromise()
  {
    return voidPromise;
  }
  
  private void checkDuplicateName(String name) {
    if (context0(name) != null) {
      throw new IllegalArgumentException("Duplicate handler name: " + name);
    }
  }
  
  private AbstractChannelHandlerContext context0(String name) {
    AbstractChannelHandlerContext context = head.next;
    while (context != tail) {
      if (context.name().equals(name)) {
        return context;
      }
      context = next;
    }
    return null;
  }
  
  private AbstractChannelHandlerContext getContextOrDie(String name) {
    AbstractChannelHandlerContext ctx = (AbstractChannelHandlerContext)context(name);
    if (ctx == null) {
      throw new NoSuchElementException(name);
    }
    return ctx;
  }
  
  private AbstractChannelHandlerContext getContextOrDie(ChannelHandler handler)
  {
    AbstractChannelHandlerContext ctx = (AbstractChannelHandlerContext)context(handler);
    if (ctx == null) {
      throw new NoSuchElementException(handler.getClass().getName());
    }
    return ctx;
  }
  
  private AbstractChannelHandlerContext getContextOrDie(Class<? extends ChannelHandler> handlerType)
  {
    AbstractChannelHandlerContext ctx = (AbstractChannelHandlerContext)context(handlerType);
    if (ctx == null) {
      throw new NoSuchElementException(handlerType.getName());
    }
    return ctx;
  }
  

  private void callHandlerAddedForAllHandlers()
  {
    synchronized (this) {
      assert (!registered);
      

      registered = true;
      
      PendingHandlerCallback pendingHandlerCallbackHead = this.pendingHandlerCallbackHead;
      
      this.pendingHandlerCallbackHead = null;
    }
    

    PendingHandlerCallback pendingHandlerCallbackHead;
    
    PendingHandlerCallback task = pendingHandlerCallbackHead;
    while (task != null) {
      task.execute();
      task = next;
    }
  }
  
  private void callHandlerCallbackLater(AbstractChannelHandlerContext ctx, boolean added) {
    assert (!registered);
    
    PendingHandlerCallback task = added ? new PendingHandlerAddedTask(ctx) : new PendingHandlerRemovedTask(ctx);
    PendingHandlerCallback pending = pendingHandlerCallbackHead;
    if (pending == null) {
      pendingHandlerCallbackHead = task;
    }
    else {
      while (next != null) {
        pending = next;
      }
      next = task;
    }
  }
  
  private void callHandlerAddedInEventLoop(final AbstractChannelHandlerContext newCtx, EventExecutor executor) {
    newCtx.setAddPending();
    executor.execute(new Runnable()
    {
      public void run() {
        DefaultChannelPipeline.this.callHandlerAdded0(newCtx);
      }
    });
  }
  


  protected void onUnhandledInboundException(Throwable cause)
  {
    try
    {
      logger.warn("An exceptionCaught() event was fired, and it reached at the tail of the pipeline. It usually means the last handler in the pipeline did not handle the exception.", cause);
      



      ReferenceCountUtil.release(cause); } finally { ReferenceCountUtil.release(cause);
    }
  }
  





  protected void onUnhandledInboundChannelActive() {}
  




  protected void onUnhandledInboundChannelInactive() {}
  




  protected void onUnhandledInboundMessage(Object msg)
  {
    try
    {
      logger.debug("Discarded inbound message {} that reached at the tail of the pipeline. Please check your pipeline configuration.", msg);
      


      ReferenceCountUtil.release(msg); } finally { ReferenceCountUtil.release(msg);
    }
  }
  




  protected void onUnhandledInboundMessage(ChannelHandlerContext ctx, Object msg)
  {
    onUnhandledInboundMessage(msg);
    if (logger.isDebugEnabled()) {
      logger.debug("Discarded message pipeline : {}. Channel : {}.", ctx
        .pipeline().names(), ctx.channel());
    }
  }
  






  protected void onUnhandledInboundChannelReadComplete() {}
  





  protected void onUnhandledInboundUserEventTriggered(Object evt)
  {
    ReferenceCountUtil.release(evt);
  }
  



  protected void onUnhandledChannelWritabilityChanged() {}
  


  protected void incrementPendingOutboundBytes(long size)
  {
    ChannelOutboundBuffer buffer = channel.unsafe().outboundBuffer();
    if (buffer != null) {
      buffer.incrementPendingOutboundBytes(size);
    }
  }
  
  protected void decrementPendingOutboundBytes(long size)
  {
    ChannelOutboundBuffer buffer = channel.unsafe().outboundBuffer();
    if (buffer != null) {
      buffer.decrementPendingOutboundBytes(size);
    }
  }
  
  final class TailContext extends AbstractChannelHandlerContext implements ChannelInboundHandler
  {
    TailContext(DefaultChannelPipeline pipeline)
    {
      super(null, DefaultChannelPipeline.TAIL_NAME, TailContext.class);
      setAddComplete();
    }
    
    public ChannelHandler handler()
    {
      return this;
    }
    

    public void channelRegistered(ChannelHandlerContext ctx) {}
    

    public void channelUnregistered(ChannelHandlerContext ctx) {}
    
    public void channelActive(ChannelHandlerContext ctx)
    {
      onUnhandledInboundChannelActive();
    }
    
    public void channelInactive(ChannelHandlerContext ctx)
    {
      onUnhandledInboundChannelInactive();
    }
    
    public void channelWritabilityChanged(ChannelHandlerContext ctx)
    {
      onUnhandledChannelWritabilityChanged();
    }
    

    public void handlerAdded(ChannelHandlerContext ctx) {}
    

    public void handlerRemoved(ChannelHandlerContext ctx) {}
    
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
    {
      onUnhandledInboundUserEventTriggered(evt);
    }
    
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    {
      onUnhandledInboundException(cause);
    }
    
    public void channelRead(ChannelHandlerContext ctx, Object msg)
    {
      onUnhandledInboundMessage(ctx, msg);
    }
    
    public void channelReadComplete(ChannelHandlerContext ctx)
    {
      onUnhandledInboundChannelReadComplete();
    }
  }
  
  final class HeadContext extends AbstractChannelHandlerContext implements ChannelOutboundHandler, ChannelInboundHandler
  {
    private final Channel.Unsafe unsafe;
    
    HeadContext(DefaultChannelPipeline pipeline)
    {
      super(null, DefaultChannelPipeline.HEAD_NAME, HeadContext.class);
      unsafe = pipeline.channel().unsafe();
      setAddComplete();
    }
    
    public ChannelHandler handler()
    {
      return this;
    }
    



    public void handlerAdded(ChannelHandlerContext ctx) {}
    


    public void handlerRemoved(ChannelHandlerContext ctx) {}
    


    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise)
    {
      unsafe.bind(localAddress, promise);
    }
    



    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise)
    {
      unsafe.connect(remoteAddress, localAddress, promise);
    }
    
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise)
    {
      unsafe.disconnect(promise);
    }
    
    public void close(ChannelHandlerContext ctx, ChannelPromise promise)
    {
      unsafe.close(promise);
    }
    
    public void deregister(ChannelHandlerContext ctx, ChannelPromise promise)
    {
      unsafe.deregister(promise);
    }
    
    public void read(ChannelHandlerContext ctx)
    {
      unsafe.beginRead();
    }
    
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
    {
      unsafe.write(msg, promise);
    }
    
    public void flush(ChannelHandlerContext ctx)
    {
      unsafe.flush();
    }
    
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    {
      ctx.fireExceptionCaught(cause);
    }
    
    public void channelRegistered(ChannelHandlerContext ctx)
    {
      invokeHandlerAddedIfNeeded();
      ctx.fireChannelRegistered();
    }
    
    public void channelUnregistered(ChannelHandlerContext ctx)
    {
      ctx.fireChannelUnregistered();
      

      if (!channel.isOpen()) {
        DefaultChannelPipeline.this.destroy();
      }
    }
    
    public void channelActive(ChannelHandlerContext ctx)
    {
      ctx.fireChannelActive();
      
      readIfIsAutoRead();
    }
    
    public void channelInactive(ChannelHandlerContext ctx)
    {
      ctx.fireChannelInactive();
    }
    
    public void channelRead(ChannelHandlerContext ctx, Object msg)
    {
      ctx.fireChannelRead(msg);
    }
    
    public void channelReadComplete(ChannelHandlerContext ctx)
    {
      ctx.fireChannelReadComplete();
      
      readIfIsAutoRead();
    }
    
    private void readIfIsAutoRead() {
      if (channel.config().isAutoRead()) {
        channel.read();
      }
    }
    
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
    {
      ctx.fireUserEventTriggered(evt);
    }
    
    public void channelWritabilityChanged(ChannelHandlerContext ctx)
    {
      ctx.fireChannelWritabilityChanged();
    }
  }
  
  private static abstract class PendingHandlerCallback implements Runnable {
    final AbstractChannelHandlerContext ctx;
    PendingHandlerCallback next;
    
    PendingHandlerCallback(AbstractChannelHandlerContext ctx) {
      this.ctx = ctx;
    }
    
    abstract void execute();
  }
  
  private final class PendingHandlerAddedTask extends DefaultChannelPipeline.PendingHandlerCallback
  {
    PendingHandlerAddedTask(AbstractChannelHandlerContext ctx) {
      super();
    }
    
    public void run()
    {
      DefaultChannelPipeline.this.callHandlerAdded0(ctx);
    }
    
    void execute()
    {
      EventExecutor executor = ctx.executor();
      if (executor.inEventLoop()) {
        DefaultChannelPipeline.this.callHandlerAdded0(ctx);
      } else {
        try {
          executor.execute(this);
        } catch (RejectedExecutionException e) {
          if (DefaultChannelPipeline.logger.isWarnEnabled()) {
            DefaultChannelPipeline.logger.warn("Can't invoke handlerAdded() as the EventExecutor {} rejected it, removing handler {}.", new Object[] { executor, ctx
            
              .name(), e });
          }
          DefaultChannelPipeline.this.atomicRemoveFromHandlerList(ctx);
          ctx.setRemoved();
        }
      }
    }
  }
  
  private final class PendingHandlerRemovedTask extends DefaultChannelPipeline.PendingHandlerCallback
  {
    PendingHandlerRemovedTask(AbstractChannelHandlerContext ctx) {
      super();
    }
    
    public void run()
    {
      DefaultChannelPipeline.this.callHandlerRemoved0(ctx);
    }
    
    void execute()
    {
      EventExecutor executor = ctx.executor();
      if (executor.inEventLoop()) {
        DefaultChannelPipeline.this.callHandlerRemoved0(ctx);
      } else {
        try {
          executor.execute(this);
        } catch (RejectedExecutionException e) {
          if (DefaultChannelPipeline.logger.isWarnEnabled()) {
            DefaultChannelPipeline.logger.warn("Can't invoke handlerRemoved() as the EventExecutor {} rejected it, removing handler {}.", new Object[] { executor, ctx
            
              .name(), e });
          }
          
          ctx.setRemoved();
        }
      }
    }
  }
}
