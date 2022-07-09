package io.netty.channel.pool;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.BootstrapConfig;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import java.util.Deque;
import java.util.concurrent.Callable;





















public class SimpleChannelPool
  implements ChannelPool
{
  private static final AttributeKey<SimpleChannelPool> POOL_KEY = AttributeKey.newInstance("io.netty.channel.pool.SimpleChannelPool");
  private final Deque<Channel> deque = PlatformDependent.newConcurrentDeque();
  
  private final ChannelPoolHandler handler;
  
  private final ChannelHealthChecker healthCheck;
  
  private final Bootstrap bootstrap;
  
  private final boolean releaseHealthCheck;
  
  private final boolean lastRecentUsed;
  
  public SimpleChannelPool(Bootstrap bootstrap, ChannelPoolHandler handler)
  {
    this(bootstrap, handler, ChannelHealthChecker.ACTIVE);
  }
  







  public SimpleChannelPool(Bootstrap bootstrap, ChannelPoolHandler handler, ChannelHealthChecker healthCheck)
  {
    this(bootstrap, handler, healthCheck, true);
  }
  










  public SimpleChannelPool(Bootstrap bootstrap, ChannelPoolHandler handler, ChannelHealthChecker healthCheck, boolean releaseHealthCheck)
  {
    this(bootstrap, handler, healthCheck, releaseHealthCheck, true);
  }
  











  public SimpleChannelPool(Bootstrap bootstrap, final ChannelPoolHandler handler, ChannelHealthChecker healthCheck, boolean releaseHealthCheck, boolean lastRecentUsed)
  {
    this.handler = ((ChannelPoolHandler)ObjectUtil.checkNotNull(handler, "handler"));
    this.healthCheck = ((ChannelHealthChecker)ObjectUtil.checkNotNull(healthCheck, "healthCheck"));
    this.releaseHealthCheck = releaseHealthCheck;
    
    this.bootstrap = ((Bootstrap)ObjectUtil.checkNotNull(bootstrap, "bootstrap")).clone();
    this.bootstrap.handler(new ChannelInitializer()
    {
      protected void initChannel(Channel ch) throws Exception {
        assert (ch.eventLoop().inEventLoop());
        handler.channelCreated(ch);
      }
    });
    this.lastRecentUsed = lastRecentUsed;
  }
  




  protected Bootstrap bootstrap()
  {
    return bootstrap;
  }
  




  protected ChannelPoolHandler handler()
  {
    return handler;
  }
  




  protected ChannelHealthChecker healthChecker()
  {
    return healthCheck;
  }
  





  protected boolean releaseHealthCheck()
  {
    return releaseHealthCheck;
  }
  
  public final Future<Channel> acquire()
  {
    return acquire(bootstrap.config().group().next().newPromise());
  }
  
  public Future<Channel> acquire(Promise<Channel> promise)
  {
    return acquireHealthyFromPoolOrNew((Promise)ObjectUtil.checkNotNull(promise, "promise"));
  }
  



  private Future<Channel> acquireHealthyFromPoolOrNew(final Promise<Channel> promise)
  {
    try
    {
      final Channel ch = pollChannel();
      if (ch == null)
      {
        Bootstrap bs = bootstrap.clone();
        bs.attr(POOL_KEY, this);
        ChannelFuture f = connectChannel(bs);
        if (f.isDone()) {
          notifyConnect(f, promise);
        } else {
          f.addListener(new ChannelFutureListener()
          {
            public void operationComplete(ChannelFuture future) throws Exception {
              SimpleChannelPool.this.notifyConnect(future, promise);
            }
          });
        }
        return promise;
      }
      EventLoop loop = ch.eventLoop();
      if (loop.inEventLoop()) {
        doHealthCheck(ch, promise);
      } else {
        loop.execute(new Runnable()
        {
          public void run() {
            SimpleChannelPool.this.doHealthCheck(ch, promise);
          }
        });
      }
    } catch (Throwable cause) {
      promise.tryFailure(cause);
    }
    return promise;
  }
  
  private void notifyConnect(ChannelFuture future, Promise<Channel> promise) throws Exception {
    if (future.isSuccess()) {
      Channel channel = future.channel();
      handler.channelAcquired(channel);
      if (!promise.trySuccess(channel))
      {
        release(channel);
      }
    } else {
      promise.tryFailure(future.cause());
    }
  }
  
  private void doHealthCheck(final Channel ch, final Promise<Channel> promise) {
    assert (ch.eventLoop().inEventLoop());
    
    Future<Boolean> f = healthCheck.isHealthy(ch);
    if (f.isDone()) {
      notifyHealthCheck(f, ch, promise);
    } else {
      f.addListener(new FutureListener()
      {
        public void operationComplete(Future<Boolean> future) throws Exception {
          SimpleChannelPool.this.notifyHealthCheck(future, ch, promise);
        }
      });
    }
  }
  
  private void notifyHealthCheck(Future<Boolean> future, Channel ch, Promise<Channel> promise) {
    assert (ch.eventLoop().inEventLoop());
    
    if (future.isSuccess()) {
      if (((Boolean)future.getNow()).booleanValue()) {
        try {
          ch.attr(POOL_KEY).set(this);
          handler.channelAcquired(ch);
          promise.setSuccess(ch);
        } catch (Throwable cause) {
          closeAndFail(ch, cause, promise);
        }
      } else {
        closeChannel(ch);
        acquireHealthyFromPoolOrNew(promise);
      }
    } else {
      closeChannel(ch);
      acquireHealthyFromPoolOrNew(promise);
    }
  }
  





  protected ChannelFuture connectChannel(Bootstrap bs)
  {
    return bs.connect();
  }
  
  public final Future<Void> release(Channel channel)
  {
    return release(channel, channel.eventLoop().newPromise());
  }
  
  public Future<Void> release(final Channel channel, final Promise<Void> promise)
  {
    ObjectUtil.checkNotNull(channel, "channel");
    ObjectUtil.checkNotNull(promise, "promise");
    try {
      EventLoop loop = channel.eventLoop();
      if (loop.inEventLoop()) {
        doReleaseChannel(channel, promise);
      } else {
        loop.execute(new Runnable()
        {
          public void run() {
            SimpleChannelPool.this.doReleaseChannel(channel, promise);
          }
        });
      }
    } catch (Throwable cause) {
      closeAndFail(channel, cause, promise);
    }
    return promise;
  }
  
  private void doReleaseChannel(Channel channel, Promise<Void> promise) {
    assert (channel.eventLoop().inEventLoop());
    
    if (channel.attr(POOL_KEY).getAndSet(null) != this) {
      closeAndFail(channel, new IllegalArgumentException("Channel " + channel + " was not acquired from this ChannelPool"), promise);
    }
    else
    {
      try
      {

        if (releaseHealthCheck) {
          doHealthCheckOnRelease(channel, promise);
        } else {
          releaseAndOffer(channel, promise);
        }
      } catch (Throwable cause) {
        closeAndFail(channel, cause, promise);
      }
    }
  }
  
  private void doHealthCheckOnRelease(final Channel channel, final Promise<Void> promise) throws Exception {
    final Future<Boolean> f = healthCheck.isHealthy(channel);
    if (f.isDone()) {
      releaseAndOfferIfHealthy(channel, promise, f);
    } else {
      f.addListener(new FutureListener()
      {
        public void operationComplete(Future<Boolean> future) throws Exception {
          SimpleChannelPool.this.releaseAndOfferIfHealthy(channel, promise, f);
        }
      });
    }
  }
  






  private void releaseAndOfferIfHealthy(Channel channel, Promise<Void> promise, Future<Boolean> future)
    throws Exception
  {
    if (((Boolean)future.getNow()).booleanValue()) {
      releaseAndOffer(channel, promise);
    } else {
      handler.channelReleased(channel);
      promise.setSuccess(null);
    }
  }
  
  private void releaseAndOffer(Channel channel, Promise<Void> promise) throws Exception {
    if (offerChannel(channel)) {
      handler.channelReleased(channel);
      promise.setSuccess(null);
    } else {
      closeAndFail(channel, new ChannelPoolFullException(null), promise);
    }
  }
  
  private void closeChannel(Channel channel) {
    channel.attr(POOL_KEY).getAndSet(null);
    channel.close();
  }
  
  private void closeAndFail(Channel channel, Throwable cause, Promise<?> promise) {
    closeChannel(channel);
    promise.tryFailure(cause);
  }
  






  protected Channel pollChannel()
  {
    return lastRecentUsed ? (Channel)deque.pollLast() : (Channel)deque.pollFirst();
  }
  






  protected boolean offerChannel(Channel channel)
  {
    return deque.offer(channel);
  }
  
  public void close()
  {
    for (;;) {
      Channel channel = pollChannel();
      if (channel == null) {
        break;
      }
      
      channel.close().awaitUninterruptibly();
    }
  }
  





  public Future<Void> closeAsync()
  {
    GlobalEventExecutor.INSTANCE.submit(new Callable()
    {
      public Void call() throws Exception {
        close();
        return null;
      }
    });
  }
  
  private static final class ChannelPoolFullException extends IllegalStateException
  {
    private ChannelPoolFullException() {
      super();
    }
    

    public Throwable fillInStackTrace()
    {
      return this;
    }
  }
}
