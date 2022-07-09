package io.netty.channel.socket.oio;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.AddressedEnvelope;
import io.netty.channel.Channel.Unsafe;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.RecvByteBufAllocator.Handle;
import io.netty.channel.oio.AbstractOioMessageChannel;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramChannelConfig;
import io.netty.util.internal.EmptyArrays;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.channels.NotYetConnectedException;
import java.util.List;
import java.util.Locale;
























@Deprecated
public class OioDatagramChannel
  extends AbstractOioMessageChannel
  implements DatagramChannel
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(OioDatagramChannel.class);
  
  private static final ChannelMetadata METADATA = new ChannelMetadata(true);
  private static final String EXPECTED_TYPES = " (expected: " + 
    StringUtil.simpleClassName(io.netty.channel.socket.DatagramPacket.class) + ", " + 
    StringUtil.simpleClassName(AddressedEnvelope.class) + '<' + 
    StringUtil.simpleClassName(ByteBuf.class) + ", " + 
    StringUtil.simpleClassName(SocketAddress.class) + ">, " + 
    StringUtil.simpleClassName(ByteBuf.class) + ')';
  
  private final MulticastSocket socket;
  private final OioDatagramChannelConfig config;
  private final java.net.DatagramPacket tmpPacket = new java.net.DatagramPacket(EmptyArrays.EMPTY_BYTES, 0);
  
  private static MulticastSocket newSocket() {
    try {
      return new MulticastSocket(null);
    } catch (Exception e) {
      throw new ChannelException("failed to create a new socket", e);
    }
  }
  


  public OioDatagramChannel()
  {
    this(newSocket());
  }
  




  public OioDatagramChannel(MulticastSocket socket)
  {
    super(null);
    
    boolean success = false;
    try {
      socket.setSoTimeout(1000);
      socket.setBroadcast(false);
      success = true;
    } catch (SocketException e) {
      throw new ChannelException("Failed to configure the datagram socket timeout.", e);
    }
    finally {
      if (!success) {
        socket.close();
      }
    }
    
    this.socket = socket;
    config = new DefaultOioDatagramChannelConfig(this, socket);
  }
  
  public ChannelMetadata metadata()
  {
    return METADATA;
  }
  






  public DatagramChannelConfig config()
  {
    return config;
  }
  
  public boolean isOpen()
  {
    return !socket.isClosed();
  }
  

  public boolean isActive()
  {
    return (isOpen()) && (
      ((((Boolean)config.getOption(ChannelOption.DATAGRAM_CHANNEL_ACTIVE_ON_REGISTRATION)).booleanValue()) && (isRegistered())) || 
      (socket.isBound()));
  }
  
  public boolean isConnected()
  {
    return socket.isConnected();
  }
  
  protected SocketAddress localAddress0()
  {
    return socket.getLocalSocketAddress();
  }
  
  protected SocketAddress remoteAddress0()
  {
    return socket.getRemoteSocketAddress();
  }
  
  protected void doBind(SocketAddress localAddress) throws Exception
  {
    socket.bind(localAddress);
  }
  
  public InetSocketAddress localAddress()
  {
    return (InetSocketAddress)super.localAddress();
  }
  
  public InetSocketAddress remoteAddress()
  {
    return (InetSocketAddress)super.remoteAddress();
  }
  
  protected void doConnect(SocketAddress remoteAddress, SocketAddress localAddress)
    throws Exception
  {
    if (localAddress != null) {
      socket.bind(localAddress);
    }
    
    boolean success = false;
    try {
      socket.connect(remoteAddress);
      success = true; return;
    } finally {
      if (!success) {
        try {
          socket.close();
        } catch (Throwable t) {
          logger.warn("Failed to close a socket.", t);
        }
      }
    }
  }
  
  protected void doDisconnect() throws Exception
  {
    socket.disconnect();
  }
  
  protected void doClose() throws Exception
  {
    socket.close();
  }
  
  protected int doReadMessages(List<Object> buf) throws Exception
  {
    DatagramChannelConfig config = config();
    RecvByteBufAllocator.Handle allocHandle = unsafe().recvBufAllocHandle();
    
    ByteBuf data = config.getAllocator().heapBuffer(allocHandle.guess());
    boolean free = true;
    try
    {
      tmpPacket.setAddress(null);
      tmpPacket.setData(data.array(), data.arrayOffset(), data.capacity());
      socket.receive(tmpPacket);
      
      InetSocketAddress remoteAddr = (InetSocketAddress)tmpPacket.getSocketAddress();
      
      allocHandle.lastBytesRead(tmpPacket.getLength());
      buf.add(new io.netty.channel.socket.DatagramPacket(data.writerIndex(allocHandle.lastBytesRead()), localAddress(), remoteAddr));
      free = false;
      return 1;
    }
    catch (SocketTimeoutException e) {
      return 0;
    } catch (SocketException e) {
      if (!e.getMessage().toLowerCase(Locale.US).contains("socket closed")) {
        throw e;
      }
      return -1;
    } catch (Throwable cause) { int i;
      PlatformDependent.throwException(cause);
      return -1;
    } finally {
      if (free) {
        data.release();
      }
    }
  }
  
  protected void doWrite(ChannelOutboundBuffer in) throws Exception
  {
    for (;;) {
      Object o = in.current();
      if (o == null) {
        break;
      }
      ByteBuf data;
      ByteBuf data;
      SocketAddress remoteAddress;
      if ((o instanceof AddressedEnvelope))
      {
        AddressedEnvelope<ByteBuf, SocketAddress> envelope = (AddressedEnvelope)o;
        SocketAddress remoteAddress = envelope.recipient();
        data = (ByteBuf)envelope.content();
      } else {
        data = (ByteBuf)o;
        remoteAddress = null;
      }
      
      int length = data.readableBytes();
      try {
        if (remoteAddress != null) {
          tmpPacket.setSocketAddress(remoteAddress);
        } else {
          if (!isConnected())
          {

            throw new NotYetConnectedException();
          }
          
          tmpPacket.setAddress(null);
        }
        if (data.hasArray()) {
          tmpPacket.setData(data.array(), data.arrayOffset() + data.readerIndex(), length);
        } else {
          tmpPacket.setData(ByteBufUtil.getBytes(data, data.readerIndex(), length));
        }
        socket.send(tmpPacket);
        in.remove();

      }
      catch (Exception e)
      {
        in.remove(e);
      }
    }
  }
  
  protected Object filterOutboundMessage(Object msg)
  {
    if (((msg instanceof io.netty.channel.socket.DatagramPacket)) || ((msg instanceof ByteBuf))) {
      return msg;
    }
    
    if ((msg instanceof AddressedEnvelope))
    {
      AddressedEnvelope<Object, SocketAddress> e = (AddressedEnvelope)msg;
      if ((e.content() instanceof ByteBuf)) {
        return msg;
      }
    }
    

    throw new UnsupportedOperationException("unsupported message type: " + StringUtil.simpleClassName(msg) + EXPECTED_TYPES);
  }
  
  public ChannelFuture joinGroup(InetAddress multicastAddress)
  {
    return joinGroup(multicastAddress, newPromise());
  }
  
  public ChannelFuture joinGroup(InetAddress multicastAddress, ChannelPromise promise)
  {
    ensureBound();
    try {
      socket.joinGroup(multicastAddress);
      promise.setSuccess();
    } catch (IOException e) {
      promise.setFailure(e);
    }
    return promise;
  }
  
  public ChannelFuture joinGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface)
  {
    return joinGroup(multicastAddress, networkInterface, newPromise());
  }
  


  public ChannelFuture joinGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface, ChannelPromise promise)
  {
    ensureBound();
    try {
      socket.joinGroup(multicastAddress, networkInterface);
      promise.setSuccess();
    } catch (IOException e) {
      promise.setFailure(e);
    }
    return promise;
  }
  

  public ChannelFuture joinGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source)
  {
    return newFailedFuture(new UnsupportedOperationException());
  }
  


  public ChannelFuture joinGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source, ChannelPromise promise)
  {
    promise.setFailure(new UnsupportedOperationException());
    return promise;
  }
  
  private void ensureBound() {
    if (!isActive())
    {
      throw new IllegalStateException(DatagramChannel.class.getName() + " must be bound to join a group.");
    }
  }
  

  public ChannelFuture leaveGroup(InetAddress multicastAddress)
  {
    return leaveGroup(multicastAddress, newPromise());
  }
  
  public ChannelFuture leaveGroup(InetAddress multicastAddress, ChannelPromise promise)
  {
    try {
      socket.leaveGroup(multicastAddress);
      promise.setSuccess();
    } catch (IOException e) {
      promise.setFailure(e);
    }
    return promise;
  }
  

  public ChannelFuture leaveGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface)
  {
    return leaveGroup(multicastAddress, networkInterface, newPromise());
  }
  

  public ChannelFuture leaveGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface, ChannelPromise promise)
  {
    try
    {
      socket.leaveGroup(multicastAddress, networkInterface);
      promise.setSuccess();
    } catch (IOException e) {
      promise.setFailure(e);
    }
    return promise;
  }
  

  public ChannelFuture leaveGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source)
  {
    return newFailedFuture(new UnsupportedOperationException());
  }
  


  public ChannelFuture leaveGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source, ChannelPromise promise)
  {
    promise.setFailure(new UnsupportedOperationException());
    return promise;
  }
  

  public ChannelFuture block(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress sourceToBlock)
  {
    return newFailedFuture(new UnsupportedOperationException());
  }
  


  public ChannelFuture block(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress sourceToBlock, ChannelPromise promise)
  {
    promise.setFailure(new UnsupportedOperationException());
    return promise;
  }
  

  public ChannelFuture block(InetAddress multicastAddress, InetAddress sourceToBlock)
  {
    return newFailedFuture(new UnsupportedOperationException());
  }
  

  public ChannelFuture block(InetAddress multicastAddress, InetAddress sourceToBlock, ChannelPromise promise)
  {
    promise.setFailure(new UnsupportedOperationException());
    return promise;
  }
}
