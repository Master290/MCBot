package io.netty.channel.udt.nio;

import com.barchart.udt.TypeUDT;
import com.barchart.udt.nio.NioSocketUDT;
import com.barchart.udt.nio.SocketChannelUDT;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.FileRegion;
import io.netty.channel.RecvByteBufAllocator.Handle;
import io.netty.channel.nio.AbstractNioByteChannel;
import io.netty.channel.nio.AbstractNioChannel.NioUnsafe;
import io.netty.channel.udt.DefaultUdtChannelConfig;
import io.netty.channel.udt.UdtChannel;
import io.netty.channel.udt.UdtChannelConfig;
import io.netty.util.internal.SocketUtils;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;




















@Deprecated
public class NioUdtByteConnectorChannel
  extends AbstractNioByteChannel
  implements UdtChannel
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(NioUdtByteConnectorChannel.class);
  private final UdtChannelConfig config;
  
  public NioUdtByteConnectorChannel()
  {
    this(TypeUDT.STREAM);
  }
  
  public NioUdtByteConnectorChannel(Channel parent, SocketChannelUDT channelUDT) {
    super(parent, channelUDT);
    try {
      channelUDT.configureBlocking(false);
      switch (2.$SwitchMap$com$barchart$udt$StatusUDT[channelUDT.socketUDT().status().ordinal()]) {
      case 1: 
      case 2: 
        config = new DefaultUdtChannelConfig(this, channelUDT, true);
        break;
      default: 
        config = new DefaultUdtChannelConfig(this, channelUDT, false);
      }
    }
    catch (Exception e) {
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
  
  public NioUdtByteConnectorChannel(SocketChannelUDT channelUDT) {
    this(null, channelUDT);
  }
  
  public NioUdtByteConnectorChannel(TypeUDT type) {
    this(NioUdtProvider.newConnectorChannelUDT(type));
  }
  
  public UdtChannelConfig config()
  {
    return config;
  }
  
  protected void doBind(SocketAddress localAddress) throws Exception
  {
    privilegedBind(javaChannel(), localAddress);
  }
  
  protected void doClose() throws Exception
  {
    javaChannel().close();
  }
  
  protected boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress)
    throws Exception
  {
    doBind(localAddress != null ? localAddress : new InetSocketAddress(0));
    boolean success = false;
    try {
      boolean connected = SocketUtils.connect(javaChannel(), remoteAddress);
      if (!connected) {
        selectionKey().interestOps(
          selectionKey().interestOps() | 0x8);
      }
      success = true;
      return connected;
    } finally {
      if (!success) {
        doClose();
      }
    }
  }
  
  protected void doDisconnect() throws Exception
  {
    doClose();
  }
  
  protected void doFinishConnect() throws Exception
  {
    if (javaChannel().finishConnect()) {
      selectionKey().interestOps(
        selectionKey().interestOps() & 0xFFFFFFF7);
    } else {
      throw new Error("Provider error: failed to finish connect. Provider library should be upgraded.");
    }
  }
  
  protected int doReadBytes(ByteBuf byteBuf)
    throws Exception
  {
    RecvByteBufAllocator.Handle allocHandle = unsafe().recvBufAllocHandle();
    allocHandle.attemptedBytesRead(byteBuf.writableBytes());
    return byteBuf.writeBytes(javaChannel(), allocHandle.attemptedBytesRead());
  }
  
  protected int doWriteBytes(ByteBuf byteBuf) throws Exception
  {
    int expectedWrittenBytes = byteBuf.readableBytes();
    return byteBuf.readBytes(javaChannel(), expectedWrittenBytes);
  }
  
  protected ChannelFuture shutdownInput()
  {
    return newFailedFuture(new UnsupportedOperationException("shutdownInput"));
  }
  
  protected long doWriteFileRegion(FileRegion region) throws Exception
  {
    throw new UnsupportedOperationException();
  }
  
  public boolean isActive()
  {
    SocketChannelUDT channelUDT = javaChannel();
    return (channelUDT.isOpen()) && (channelUDT.isConnectFinished());
  }
  
  protected SocketChannelUDT javaChannel()
  {
    return (SocketChannelUDT)super.javaChannel();
  }
  
  protected SocketAddress localAddress0()
  {
    return javaChannel().socket().getLocalSocketAddress();
  }
  
  protected SocketAddress remoteAddress0()
  {
    return javaChannel().socket().getRemoteSocketAddress();
  }
  
  public InetSocketAddress localAddress()
  {
    return (InetSocketAddress)super.localAddress();
  }
  
  public InetSocketAddress remoteAddress()
  {
    return (InetSocketAddress)super.remoteAddress();
  }
  
  private static void privilegedBind(SocketChannelUDT socketChannel, final SocketAddress localAddress) throws IOException
  {
    try {
      AccessController.doPrivileged(new PrivilegedExceptionAction()
      {
        public Void run() throws IOException {
          val$socketChannel.bind(localAddress);
          return null;
        }
      });
    } catch (PrivilegedActionException e) {
      throw ((IOException)e.getCause());
    }
  }
}
