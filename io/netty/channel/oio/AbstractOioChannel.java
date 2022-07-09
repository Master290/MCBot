package io.netty.channel.oio;

import io.netty.channel.AbstractChannel;
import io.netty.channel.AbstractChannel.AbstractUnsafe;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.ThreadPerChannelEventLoop;
import java.net.SocketAddress;




















@Deprecated
public abstract class AbstractOioChannel
  extends AbstractChannel
{
  protected static final int SO_TIMEOUT = 1000;
  boolean readPending;
  boolean readWhenInactive;
  final Runnable readTask = new Runnable()
  {
    public void run() {
      doRead();
    }
  };
  private final Runnable clearReadPendingRunnable = new Runnable()
  {
    public void run() {
      readPending = false;
    }
  };
  


  protected AbstractOioChannel(Channel parent)
  {
    super(parent);
  }
  


  protected AbstractChannel.AbstractUnsafe newUnsafe() { return new DefaultOioUnsafe(null); }
  
  private final class DefaultOioUnsafe extends AbstractChannel.AbstractUnsafe {
    private DefaultOioUnsafe() { super(); }
    

    public void connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise)
    {
      if ((!promise.setUncancellable()) || (!ensureOpen(promise))) {
        return;
      }
      try
      {
        boolean wasActive = isActive();
        doConnect(remoteAddress, localAddress);
        


        boolean active = isActive();
        
        safeSetSuccess(promise);
        if ((!wasActive) && (active)) {
          pipeline().fireChannelActive();
        }
      } catch (Throwable t) {
        safeSetFailure(promise, annotateConnectException(t, remoteAddress));
        closeIfClosed();
      }
    }
  }
  
  protected boolean isCompatible(EventLoop loop)
  {
    return loop instanceof ThreadPerChannelEventLoop;
  }
  

  protected abstract void doConnect(SocketAddress paramSocketAddress1, SocketAddress paramSocketAddress2)
    throws Exception;
  

  protected void doBeginRead()
    throws Exception
  {
    if (readPending) {
      return;
    }
    if (!isActive()) {
      readWhenInactive = true;
      return;
    }
    
    readPending = true;
    eventLoop().execute(readTask);
  }
  


  protected abstract void doRead();
  

  @Deprecated
  protected boolean isReadPending()
  {
    return readPending;
  }
  



  @Deprecated
  protected void setReadPending(final boolean readPending)
  {
    if (isRegistered()) {
      EventLoop eventLoop = eventLoop();
      if (eventLoop.inEventLoop()) {
        this.readPending = readPending;
      } else {
        eventLoop.execute(new Runnable()
        {
          public void run() {
            readPending = readPending;
          }
        });
      }
    } else {
      this.readPending = readPending;
    }
  }
  


  protected final void clearReadPending()
  {
    if (isRegistered()) {
      EventLoop eventLoop = eventLoop();
      if (eventLoop.inEventLoop()) {
        readPending = false;
      } else {
        eventLoop.execute(clearReadPendingRunnable);
      }
    }
    else {
      readPending = false;
    }
  }
}
