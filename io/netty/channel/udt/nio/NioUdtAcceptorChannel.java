package io.netty.channel.udt.nio;

import com.barchart.udt.TypeUDT;
import com.barchart.udt.nio.NioServerSocketUDT;
import com.barchart.udt.nio.ServerSocketChannelUDT;
import com.barchart.udt.nio.SocketChannelUDT;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.nio.AbstractNioMessageChannel;
import io.netty.channel.udt.DefaultUdtServerChannelConfig;
import io.netty.channel.udt.UdtChannel;
import io.netty.channel.udt.UdtServerChannel;
import io.netty.channel.udt.UdtServerChannelConfig;
import io.netty.util.internal.SocketUtils;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;






















@Deprecated
public abstract class NioUdtAcceptorChannel
  extends AbstractNioMessageChannel
  implements UdtServerChannel
{
  protected static final InternalLogger logger = InternalLoggerFactory.getInstance(NioUdtAcceptorChannel.class);
  
  private static final ChannelMetadata METADATA = new ChannelMetadata(false, 16);
  private final UdtServerChannelConfig config;
  
  protected NioUdtAcceptorChannel(ServerSocketChannelUDT channelUDT)
  {
    super(null, channelUDT, 16);
    try {
      channelUDT.configureBlocking(false);
      config = new DefaultUdtServerChannelConfig(this, channelUDT, true);
    } catch (Exception e) {
      try {
        channelUDT.close();
      } catch (Exception e2) {
        if (logger.isWarnEnabled()) {
          logger.warn("Failed to close channel.", e2);
        }
      }
      throw new ChannelException("Failed to configure channel.", e);
    }
  }
  
  protected NioUdtAcceptorChannel(TypeUDT type) {
    this(NioUdtProvider.newAcceptorChannelUDT(type));
  }
  
  public UdtServerChannelConfig config()
  {
    return config;
  }
  
  protected void doBind(SocketAddress localAddress) throws Exception
  {
    javaChannel().socket().bind(localAddress, config.getBacklog());
  }
  
  protected void doClose() throws Exception
  {
    javaChannel().close();
  }
  
  protected boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress)
    throws Exception
  {
    throw new UnsupportedOperationException();
  }
  
  protected void doDisconnect() throws Exception
  {
    throw new UnsupportedOperationException();
  }
  
  protected void doFinishConnect() throws Exception
  {
    throw new UnsupportedOperationException();
  }
  
  protected boolean doWriteMessage(Object msg, ChannelOutboundBuffer in) throws Exception
  {
    throw new UnsupportedOperationException();
  }
  
  protected final Object filterOutboundMessage(Object msg) throws Exception
  {
    throw new UnsupportedOperationException();
  }
  
  public boolean isActive()
  {
    return javaChannel().socket().isBound();
  }
  
  protected ServerSocketChannelUDT javaChannel()
  {
    return (ServerSocketChannelUDT)super.javaChannel();
  }
  
  protected SocketAddress localAddress0()
  {
    return SocketUtils.localSocketAddress(javaChannel().socket());
  }
  
  public InetSocketAddress localAddress()
  {
    return (InetSocketAddress)super.localAddress();
  }
  
  public InetSocketAddress remoteAddress()
  {
    return null;
  }
  
  protected SocketAddress remoteAddress0()
  {
    return null;
  }
  
  public ChannelMetadata metadata()
  {
    return METADATA;
  }
  
  protected int doReadMessages(List<Object> buf) throws Exception
  {
    SocketChannelUDT channelUDT = (SocketChannelUDT)SocketUtils.accept(javaChannel());
    if (channelUDT == null) {
      return 0;
    }
    buf.add(newConnectorChannel(channelUDT));
    return 1;
  }
  
  protected abstract UdtChannel newConnectorChannel(SocketChannelUDT paramSocketChannelUDT);
}
