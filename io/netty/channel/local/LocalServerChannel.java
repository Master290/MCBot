package io.netty.channel.local;

import io.netty.channel.AbstractServerChannel;
import io.netty.channel.Channel.Unsafe;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.EventLoop;
import io.netty.channel.PreferHeapByteBufAllocator;
import io.netty.channel.RecvByteBufAllocator.Handle;
import io.netty.channel.SingleThreadEventLoop;
import io.netty.util.concurrent.SingleThreadEventExecutor;
import java.net.SocketAddress;
import java.util.ArrayDeque;
import java.util.Queue;



















public class LocalServerChannel
  extends AbstractServerChannel
{
  private final ChannelConfig config = new DefaultChannelConfig(this);
  private final Queue<Object> inboundBuffer = new ArrayDeque();
  private final Runnable shutdownHook = new Runnable()
  {
    public void run() {
      unsafe().close(unsafe().voidPromise());
    }
  };
  private volatile int state;
  private volatile LocalAddress localAddress;
  private volatile boolean acceptInProgress;
  
  public LocalServerChannel()
  {
    config().setAllocator(new PreferHeapByteBufAllocator(config.getAllocator()));
  }
  
  public ChannelConfig config()
  {
    return config;
  }
  
  public LocalAddress localAddress()
  {
    return (LocalAddress)super.localAddress();
  }
  
  public LocalAddress remoteAddress()
  {
    return (LocalAddress)super.remoteAddress();
  }
  
  public boolean isOpen()
  {
    return state < 2;
  }
  
  public boolean isActive()
  {
    return state == 1;
  }
  
  protected boolean isCompatible(EventLoop loop)
  {
    return loop instanceof SingleThreadEventLoop;
  }
  
  protected SocketAddress localAddress0()
  {
    return localAddress;
  }
  
  protected void doRegister() throws Exception
  {
    ((SingleThreadEventExecutor)eventLoop()).addShutdownHook(shutdownHook);
  }
  
  protected void doBind(SocketAddress localAddress) throws Exception
  {
    this.localAddress = LocalChannelRegistry.register(this, this.localAddress, localAddress);
    state = 1;
  }
  
  protected void doClose() throws Exception
  {
    if (state <= 1)
    {
      if (localAddress != null) {
        LocalChannelRegistry.unregister(localAddress);
        localAddress = null;
      }
      state = 2;
    }
  }
  
  protected void doDeregister() throws Exception
  {
    ((SingleThreadEventExecutor)eventLoop()).removeShutdownHook(shutdownHook);
  }
  
  protected void doBeginRead() throws Exception
  {
    if (acceptInProgress) {
      return;
    }
    
    Queue<Object> inboundBuffer = this.inboundBuffer;
    if (inboundBuffer.isEmpty()) {
      acceptInProgress = true;
      return;
    }
    
    readInbound();
  }
  
  LocalChannel serve(LocalChannel peer) {
    final LocalChannel child = newLocalChannel(peer);
    if (eventLoop().inEventLoop()) {
      serve0(child);
    } else {
      eventLoop().execute(new Runnable()
      {
        public void run() {
          LocalServerChannel.this.serve0(child);
        }
      });
    }
    return child;
  }
  
  private void readInbound() {
    RecvByteBufAllocator.Handle handle = unsafe().recvBufAllocHandle();
    handle.reset(config());
    ChannelPipeline pipeline = pipeline();
    do {
      Object m = inboundBuffer.poll();
      if (m == null) {
        break;
      }
      pipeline.fireChannelRead(m);
    } while (handle.continueReading());
    
    pipeline.fireChannelReadComplete();
  }
  



  protected LocalChannel newLocalChannel(LocalChannel peer)
  {
    return new LocalChannel(this, peer);
  }
  
  private void serve0(LocalChannel child) {
    inboundBuffer.add(child);
    if (acceptInProgress) {
      acceptInProgress = false;
      
      readInbound();
    }
  }
}
