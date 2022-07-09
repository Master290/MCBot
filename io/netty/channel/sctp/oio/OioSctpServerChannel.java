package io.netty.channel.sctp.oio;

import com.sun.nio.sctp.SctpChannel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.oio.AbstractOioMessageChannel;
import io.netty.channel.sctp.DefaultSctpServerChannelConfig;
import io.netty.channel.sctp.SctpServerChannelConfig;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


























@Deprecated
public class OioSctpServerChannel
  extends AbstractOioMessageChannel
  implements io.netty.channel.sctp.SctpServerChannel
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(OioSctpServerChannel.class);
  
  private static final ChannelMetadata METADATA = new ChannelMetadata(false, 1);
  private final com.sun.nio.sctp.SctpServerChannel sch;
  
  private static com.sun.nio.sctp.SctpServerChannel newServerSocket() {
    try { return com.sun.nio.sctp.SctpServerChannel.open();
    } catch (IOException e) {
      throw new ChannelException("failed to create a sctp server channel", e);
    }
  }
  


  private final SctpServerChannelConfig config;
  
  private final Selector selector;
  
  public OioSctpServerChannel()
  {
    this(newServerSocket());
  }
  




  public OioSctpServerChannel(com.sun.nio.sctp.SctpServerChannel sch)
  {
    super(null);
    this.sch = ((com.sun.nio.sctp.SctpServerChannel)ObjectUtil.checkNotNull(sch, "sctp server channel"));
    boolean success = false;
    try {
      sch.configureBlocking(false);
      selector = Selector.open();
      sch.register(selector, 16);
      config = new OioSctpServerChannelConfig(this, sch, null);
      success = true; return;
    } catch (Exception e) {
      throw new ChannelException("failed to initialize a sctp server channel", e);
    } finally {
      if (!success) {
        try {
          sch.close();
        } catch (IOException e) {
          logger.warn("Failed to close a sctp server channel.", e);
        }
      }
    }
  }
  
  public ChannelMetadata metadata()
  {
    return METADATA;
  }
  
  public SctpServerChannelConfig config()
  {
    return config;
  }
  
  public InetSocketAddress remoteAddress()
  {
    return null;
  }
  
  public InetSocketAddress localAddress()
  {
    return (InetSocketAddress)super.localAddress();
  }
  
  public boolean isOpen()
  {
    return sch.isOpen();
  }
  
  protected SocketAddress localAddress0()
  {
    try {
      Iterator<SocketAddress> i = sch.getAllLocalAddresses().iterator();
      if (i.hasNext()) {
        return (SocketAddress)i.next();
      }
    }
    catch (IOException localIOException) {}
    
    return null;
  }
  
  public Set<InetSocketAddress> allLocalAddresses()
  {
    try {
      Set<SocketAddress> allLocalAddresses = sch.getAllLocalAddresses();
      Set<InetSocketAddress> addresses = new LinkedHashSet(allLocalAddresses.size());
      for (SocketAddress socketAddress : allLocalAddresses) {
        addresses.add((InetSocketAddress)socketAddress);
      }
      return addresses;
    } catch (Throwable ignored) {}
    return Collections.emptySet();
  }
  

  public boolean isActive()
  {
    return (isOpen()) && (localAddress0() != null);
  }
  
  protected void doBind(SocketAddress localAddress) throws Exception
  {
    sch.bind(localAddress, config.getBacklog());
  }
  
  protected void doClose() throws Exception
  {
    try {
      selector.close();
    } catch (IOException e) {
      logger.warn("Failed to close a selector.", e);
    }
    sch.close();
  }
  
  protected int doReadMessages(List<Object> buf) throws Exception
  {
    if (!isActive()) {
      return -1;
    }
    
    SctpChannel s = null;
    int acceptedChannels = 0;
    try {
      int selectedKeys = selector.select(1000L);
      if (selectedKeys > 0) {
        Iterator<SelectionKey> selectionKeys = selector.selectedKeys().iterator();
        for (;;) {
          SelectionKey key = (SelectionKey)selectionKeys.next();
          selectionKeys.remove();
          if (key.isAcceptable()) {
            s = sch.accept();
            if (s != null) {
              buf.add(new OioSctpChannel(this, s));
              acceptedChannels++;
            }
          }
          if (!selectionKeys.hasNext()) {
            return acceptedChannels;
          }
        }
      }
    } catch (Throwable t) {
      logger.warn("Failed to create a new channel from an accepted sctp channel.", t);
      if (s != null) {
        try {
          s.close();
        } catch (Throwable t2) {
          logger.warn("Failed to close a sctp channel.", t2);
        }
      }
    }
    
    return acceptedChannels;
  }
  
  public ChannelFuture bindAddress(InetAddress localAddress)
  {
    return bindAddress(localAddress, newPromise());
  }
  
  public ChannelFuture bindAddress(final InetAddress localAddress, final ChannelPromise promise)
  {
    if (eventLoop().inEventLoop()) {
      try {
        sch.bindAddress(localAddress);
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
        sch.unbindAddress(localAddress);
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
  
  protected void doConnect(SocketAddress remoteAddress, SocketAddress localAddress)
    throws Exception
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
  
  protected void doWrite(ChannelOutboundBuffer in) throws Exception
  {
    throw new UnsupportedOperationException();
  }
  
  protected Object filterOutboundMessage(Object msg) throws Exception
  {
    throw new UnsupportedOperationException();
  }
  
  private final class OioSctpServerChannelConfig extends DefaultSctpServerChannelConfig {
    private OioSctpServerChannelConfig(OioSctpServerChannel channel, com.sun.nio.sctp.SctpServerChannel javaChannel) {
      super(javaChannel);
    }
    
    protected void autoReadCleared()
    {
      clearReadPending();
    }
  }
}
