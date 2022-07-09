package io.netty.channel.sctp.nio;

import com.sun.nio.sctp.SctpChannel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.nio.AbstractNioMessageChannel;
import io.netty.channel.nio.NioEventLoop;
import io.netty.channel.sctp.DefaultSctpServerChannelConfig;
import io.netty.channel.sctp.SctpServerChannelConfig;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;























public class NioSctpServerChannel
  extends AbstractNioMessageChannel
  implements io.netty.channel.sctp.SctpServerChannel
{
  private static final ChannelMetadata METADATA = new ChannelMetadata(false, 16);
  private final SctpServerChannelConfig config;
  
  private static com.sun.nio.sctp.SctpServerChannel newSocket() {
    try { return com.sun.nio.sctp.SctpServerChannel.open();
    } catch (IOException e) {
      throw new ChannelException("Failed to open a server socket.", e);
    }
  }
  





  public NioSctpServerChannel()
  {
    super(null, newSocket(), 16);
    config = new NioSctpServerChannelConfig(this, javaChannel(), null);
  }
  
  public ChannelMetadata metadata()
  {
    return METADATA;
  }
  
  public Set<InetSocketAddress> allLocalAddresses()
  {
    try {
      Set<SocketAddress> allLocalAddresses = javaChannel().getAllLocalAddresses();
      Set<InetSocketAddress> addresses = new LinkedHashSet(allLocalAddresses.size());
      for (SocketAddress socketAddress : allLocalAddresses) {
        addresses.add((InetSocketAddress)socketAddress);
      }
      return addresses;
    } catch (Throwable ignored) {}
    return Collections.emptySet();
  }
  

  public SctpServerChannelConfig config()
  {
    return config;
  }
  
  public boolean isActive()
  {
    return (isOpen()) && (!allLocalAddresses().isEmpty());
  }
  
  public InetSocketAddress remoteAddress()
  {
    return null;
  }
  
  public InetSocketAddress localAddress()
  {
    return (InetSocketAddress)super.localAddress();
  }
  
  protected com.sun.nio.sctp.SctpServerChannel javaChannel()
  {
    return (com.sun.nio.sctp.SctpServerChannel)super.javaChannel();
  }
  
  protected SocketAddress localAddress0()
  {
    try {
      Iterator<SocketAddress> i = javaChannel().getAllLocalAddresses().iterator();
      if (i.hasNext()) {
        return (SocketAddress)i.next();
      }
    }
    catch (IOException localIOException) {}
    
    return null;
  }
  
  protected void doBind(SocketAddress localAddress) throws Exception
  {
    javaChannel().bind(localAddress, config.getBacklog());
  }
  
  protected void doClose() throws Exception
  {
    javaChannel().close();
  }
  
  protected int doReadMessages(List<Object> buf) throws Exception
  {
    SctpChannel ch = javaChannel().accept();
    if (ch == null) {
      return 0;
    }
    buf.add(new NioSctpChannel(this, ch));
    return 1;
  }
  
  public ChannelFuture bindAddress(InetAddress localAddress)
  {
    return bindAddress(localAddress, newPromise());
  }
  
  public ChannelFuture bindAddress(final InetAddress localAddress, final ChannelPromise promise)
  {
    if (eventLoop().inEventLoop()) {
      try {
        javaChannel().bindAddress(localAddress);
        promise.setSuccess();
      } catch (Throwable t) {
        promise.setFailure(t);
      }
    } else {
      eventLoop().execute(new Runnable()
      {
        public void run() {
          bindAddress(localAddress, promise);
        }
      });
    }
    return promise;
  }
  
  public ChannelFuture unbindAddress(InetAddress localAddress)
  {
    return unbindAddress(localAddress, newPromise());
  }
  
  public ChannelFuture unbindAddress(final InetAddress localAddress, final ChannelPromise promise)
  {
    if (eventLoop().inEventLoop()) {
      try {
        javaChannel().unbindAddress(localAddress);
        promise.setSuccess();
      } catch (Throwable t) {
        promise.setFailure(t);
      }
    } else {
      eventLoop().execute(new Runnable()
      {
        public void run() {
          unbindAddress(localAddress, promise);
        }
      });
    }
    return promise;
  }
  

  protected boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress)
    throws Exception
  {
    throw new UnsupportedOperationException();
  }
  
  protected void doFinishConnect() throws Exception
  {
    throw new UnsupportedOperationException();
  }
  
  protected SocketAddress remoteAddress0()
  {
    return null;
  }
  
  protected void doDisconnect() throws Exception
  {
    throw new UnsupportedOperationException();
  }
  
  protected boolean doWriteMessage(Object msg, ChannelOutboundBuffer in) throws Exception
  {
    throw new UnsupportedOperationException();
  }
  
  protected Object filterOutboundMessage(Object msg) throws Exception
  {
    throw new UnsupportedOperationException();
  }
  
  private final class NioSctpServerChannelConfig extends DefaultSctpServerChannelConfig {
    private NioSctpServerChannelConfig(NioSctpServerChannel channel, com.sun.nio.sctp.SctpServerChannel javaChannel) {
      super(javaChannel);
    }
    
    protected void autoReadCleared()
    {
      clearReadPending();
    }
  }
}
