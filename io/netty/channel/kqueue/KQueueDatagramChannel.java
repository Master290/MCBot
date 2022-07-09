package io.netty.channel.kqueue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.AddressedEnvelope;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultAddressedEnvelope;
import io.netty.channel.EventLoop;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramChannelConfig;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.unix.DatagramSocketAddress;
import io.netty.channel.unix.Errors;
import io.netty.channel.unix.Errors.NativeIoException;
import io.netty.channel.unix.IovArray;
import io.netty.channel.unix.UnixChannelUtil;
import io.netty.util.UncheckedBooleanSupplier;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.PortUnreachableException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
















public final class KQueueDatagramChannel
  extends AbstractKQueueDatagramChannel
  implements DatagramChannel
{
  private static final String EXPECTED_TYPES = " (expected: " + 
    StringUtil.simpleClassName(DatagramPacket.class) + ", " + 
    StringUtil.simpleClassName(AddressedEnvelope.class) + '<' + 
    StringUtil.simpleClassName(ByteBuf.class) + ", " + 
    StringUtil.simpleClassName(InetSocketAddress.class) + ">, " + 
    StringUtil.simpleClassName(ByteBuf.class) + ')';
  private volatile boolean connected;
  private final KQueueDatagramChannelConfig config;
  
  public KQueueDatagramChannel()
  {
    super(null, BsdSocket.newSocketDgram(), false);
    config = new KQueueDatagramChannelConfig(this);
  }
  
  public KQueueDatagramChannel(int fd) {
    this(new BsdSocket(fd), true);
  }
  
  KQueueDatagramChannel(BsdSocket socket, boolean active) {
    super(null, socket, active);
    config = new KQueueDatagramChannelConfig(this);
  }
  
  public InetSocketAddress remoteAddress()
  {
    return (InetSocketAddress)super.remoteAddress();
  }
  
  public InetSocketAddress localAddress()
  {
    return (InetSocketAddress)super.localAddress();
  }
  

  public boolean isActive()
  {
    return (socket.isOpen()) && (((config.getActiveOnOpen()) && (isRegistered())) || (active));
  }
  
  public boolean isConnected()
  {
    return connected;
  }
  
  public ChannelFuture joinGroup(InetAddress multicastAddress)
  {
    return joinGroup(multicastAddress, newPromise());
  }
  
  public ChannelFuture joinGroup(InetAddress multicastAddress, ChannelPromise promise)
  {
    try {
      return joinGroup(multicastAddress, 
      
        NetworkInterface.getByInetAddress(localAddress().getAddress()), null, promise);
    }
    catch (SocketException e) {
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
    return joinGroup(multicastAddress.getAddress(), networkInterface, null, promise);
  }
  

  public ChannelFuture joinGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source)
  {
    return joinGroup(multicastAddress, networkInterface, source, newPromise());
  }
  



  public ChannelFuture joinGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source, ChannelPromise promise)
  {
    ObjectUtil.checkNotNull(multicastAddress, "multicastAddress");
    ObjectUtil.checkNotNull(networkInterface, "networkInterface");
    
    promise.setFailure(new UnsupportedOperationException("Multicast not supported"));
    return promise;
  }
  
  public ChannelFuture leaveGroup(InetAddress multicastAddress)
  {
    return leaveGroup(multicastAddress, newPromise());
  }
  
  public ChannelFuture leaveGroup(InetAddress multicastAddress, ChannelPromise promise)
  {
    try {
      return leaveGroup(multicastAddress, 
        NetworkInterface.getByInetAddress(localAddress().getAddress()), null, promise);
    } catch (SocketException e) {
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
    return leaveGroup(multicastAddress.getAddress(), networkInterface, null, promise);
  }
  

  public ChannelFuture leaveGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source)
  {
    return leaveGroup(multicastAddress, networkInterface, source, newPromise());
  }
  


  public ChannelFuture leaveGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source, ChannelPromise promise)
  {
    ObjectUtil.checkNotNull(multicastAddress, "multicastAddress");
    ObjectUtil.checkNotNull(networkInterface, "networkInterface");
    
    promise.setFailure(new UnsupportedOperationException("Multicast not supported"));
    
    return promise;
  }
  


  public ChannelFuture block(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress sourceToBlock)
  {
    return block(multicastAddress, networkInterface, sourceToBlock, newPromise());
  }
  


  public ChannelFuture block(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress sourceToBlock, ChannelPromise promise)
  {
    ObjectUtil.checkNotNull(multicastAddress, "multicastAddress");
    ObjectUtil.checkNotNull(sourceToBlock, "sourceToBlock");
    ObjectUtil.checkNotNull(networkInterface, "networkInterface");
    promise.setFailure(new UnsupportedOperationException("Multicast not supported"));
    return promise;
  }
  
  public ChannelFuture block(InetAddress multicastAddress, InetAddress sourceToBlock)
  {
    return block(multicastAddress, sourceToBlock, newPromise());
  }
  
  public ChannelFuture block(InetAddress multicastAddress, InetAddress sourceToBlock, ChannelPromise promise)
  {
    try
    {
      return block(multicastAddress, 
      
        NetworkInterface.getByInetAddress(localAddress().getAddress()), sourceToBlock, promise);
    }
    catch (Throwable e) {
      promise.setFailure(e);
    }
    return promise;
  }
  
  protected AbstractKQueueChannel.AbstractKQueueUnsafe newUnsafe()
  {
    return new KQueueDatagramChannelUnsafe();
  }
  
  protected void doBind(SocketAddress localAddress) throws Exception
  {
    super.doBind(localAddress);
    active = true;
  }
  
  protected boolean doWriteMessage(Object msg) throws Exception {
    InetSocketAddress remoteAddress;
    ByteBuf data;
    InetSocketAddress remoteAddress;
    if ((msg instanceof AddressedEnvelope))
    {
      AddressedEnvelope<ByteBuf, InetSocketAddress> envelope = (AddressedEnvelope)msg;
      
      ByteBuf data = (ByteBuf)envelope.content();
      remoteAddress = (InetSocketAddress)envelope.recipient();
    } else {
      data = (ByteBuf)msg;
      remoteAddress = null;
    }
    
    int dataLen = data.readableBytes();
    if (dataLen == 0) {
      return true;
    }
    long writtenBytes;
    long writtenBytes;
    if (data.hasMemoryAddress()) {
      long memoryAddress = data.memoryAddress();
      long writtenBytes; if (remoteAddress == null) {
        writtenBytes = socket.writeAddress(memoryAddress, data.readerIndex(), data.writerIndex());
      } else
        writtenBytes = socket.sendToAddress(memoryAddress, data.readerIndex(), data.writerIndex(), remoteAddress
          .getAddress(), remoteAddress.getPort());
    } else { long writtenBytes;
      if (data.nioBufferCount() > 1) {
        IovArray array = ((KQueueEventLoop)eventLoop()).cleanArray();
        array.add(data, data.readerIndex(), data.readableBytes());
        int cnt = array.count();
        assert (cnt != 0);
        long writtenBytes;
        if (remoteAddress == null) {
          writtenBytes = socket.writevAddresses(array.memoryAddress(0), cnt);
        } else {
          writtenBytes = socket.sendToAddresses(array.memoryAddress(0), cnt, remoteAddress
            .getAddress(), remoteAddress.getPort());
        }
      } else {
        ByteBuffer nioData = data.internalNioBuffer(data.readerIndex(), data.readableBytes());
        long writtenBytes; if (remoteAddress == null) {
          writtenBytes = socket.write(nioData, nioData.position(), nioData.limit());
        } else {
          writtenBytes = socket.sendTo(nioData, nioData.position(), nioData.limit(), remoteAddress
            .getAddress(), remoteAddress.getPort());
        }
      }
    }
    return writtenBytes > 0L;
  }
  
  protected Object filterOutboundMessage(Object msg)
  {
    if ((msg instanceof DatagramPacket)) {
      DatagramPacket packet = (DatagramPacket)msg;
      ByteBuf content = (ByteBuf)packet.content();
      return UnixChannelUtil.isBufferCopyNeededForWrite(content) ? new DatagramPacket(
        newDirectBuffer(packet, content), (InetSocketAddress)packet.recipient()) : msg;
    }
    
    if ((msg instanceof ByteBuf)) {
      ByteBuf buf = (ByteBuf)msg;
      return UnixChannelUtil.isBufferCopyNeededForWrite(buf) ? newDirectBuffer(buf) : buf;
    }
    
    if ((msg instanceof AddressedEnvelope))
    {
      AddressedEnvelope<Object, SocketAddress> e = (AddressedEnvelope)msg;
      if (((e.content() instanceof ByteBuf)) && (
        (e.recipient() == null) || ((e.recipient() instanceof InetSocketAddress))))
      {
        ByteBuf content = (ByteBuf)e.content();
        return UnixChannelUtil.isBufferCopyNeededForWrite(content) ? new DefaultAddressedEnvelope(
        
          newDirectBuffer(e, content), (InetSocketAddress)e.recipient()) : e;
      }
    }
    

    throw new UnsupportedOperationException("unsupported message type: " + StringUtil.simpleClassName(msg) + EXPECTED_TYPES);
  }
  
  public KQueueDatagramChannelConfig config()
  {
    return config;
  }
  
  protected void doDisconnect() throws Exception
  {
    socket.disconnect();
    connected = (this.active = 0);
    resetCachedAddresses();
  }
  
  protected boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception
  {
    if (super.doConnect(remoteAddress, localAddress)) {
      connected = true;
      return true;
    }
    return false;
  }
  
  protected void doClose() throws Exception
  {
    super.doClose();
    connected = false;
  }
  
  final class KQueueDatagramChannelUnsafe extends AbstractKQueueChannel.AbstractKQueueUnsafe { KQueueDatagramChannelUnsafe() { super(); }
    
    void readReady(KQueueRecvByteAllocatorHandle allocHandle)
    {
      assert (eventLoop().inEventLoop());
      DatagramChannelConfig config = config();
      if (shouldBreakReadReady(config)) {
        clearReadFilter0();
        return;
      }
      ChannelPipeline pipeline = pipeline();
      ByteBufAllocator allocator = config.getAllocator();
      allocHandle.reset(config);
      readReadyBefore();
      
      Throwable exception = null;
      try {
        ByteBuf byteBuf = null;
        try {
          boolean connected = isConnected();
          do {
            byteBuf = allocHandle.allocate(allocator);
            allocHandle.attemptedBytesRead(byteBuf.writableBytes());
            DatagramPacket packet;
            DatagramPacket packet;
            if (connected) {
              try {
                allocHandle.lastBytesRead(doReadBytes(byteBuf));
              }
              catch (Errors.NativeIoException e) {
                if (e.expectedErr() == Errors.ERROR_ECONNREFUSED_NEGATIVE) {
                  PortUnreachableException error = new PortUnreachableException(e.getMessage());
                  error.initCause(e);
                  throw error;
                }
                throw e;
              }
              if (allocHandle.lastBytesRead() <= 0)
              {
                byteBuf.release();
                byteBuf = null;
                break;
              }
              
              packet = new DatagramPacket(byteBuf, (InetSocketAddress)localAddress(), (InetSocketAddress)remoteAddress());
            } else { DatagramSocketAddress remoteAddress;
              DatagramSocketAddress remoteAddress;
              if (byteBuf.hasMemoryAddress())
              {
                remoteAddress = socket.recvFromAddress(byteBuf.memoryAddress(), byteBuf.writerIndex(), byteBuf
                  .capacity());
              } else {
                ByteBuffer nioData = byteBuf.internalNioBuffer(byteBuf
                  .writerIndex(), byteBuf.writableBytes());
                remoteAddress = socket.recvFrom(nioData, nioData.position(), nioData.limit());
              }
              
              if (remoteAddress == null) {
                allocHandle.lastBytesRead(-1);
                byteBuf.release();
                byteBuf = null;
                break;
              }
              InetSocketAddress localAddress = remoteAddress.localAddress();
              if (localAddress == null) {
                localAddress = (InetSocketAddress)localAddress();
              }
              allocHandle.lastBytesRead(remoteAddress.receivedAmount());
              byteBuf.writerIndex(byteBuf.writerIndex() + allocHandle.lastBytesRead());
              
              packet = new DatagramPacket(byteBuf, localAddress, remoteAddress);
            }
            
            allocHandle.incMessagesRead(1);
            
            readPending = false;
            pipeline.fireChannelRead(packet);
            
            byteBuf = null;


          }
          while (allocHandle.continueReading(UncheckedBooleanSupplier.TRUE_SUPPLIER));
        } catch (Throwable t) {
          if (byteBuf != null) {
            byteBuf.release();
          }
          exception = t;
        }
        
        allocHandle.readComplete();
        pipeline.fireChannelReadComplete();
        
        if (exception != null) {
          pipeline.fireExceptionCaught(exception);
        }
      } finally {
        readReadyFinally(config);
      }
    }
  }
}
