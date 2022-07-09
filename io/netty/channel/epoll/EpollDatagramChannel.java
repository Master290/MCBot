package io.netty.channel.epoll;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.AddressedEnvelope;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultAddressedEnvelope;
import io.netty.channel.EventLoop;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.InternetProtocolFamily;
import io.netty.channel.unix.Errors;
import io.netty.channel.unix.Errors.NativeIoException;
import io.netty.channel.unix.SegmentedDatagramPacket;
import io.netty.channel.unix.Socket;
import io.netty.channel.unix.UnixChannelUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.UncheckedBooleanSupplier;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.RecyclableArrayList;
import io.netty.util.internal.StringUtil;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.PortUnreachableException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;


















public final class EpollDatagramChannel
  extends AbstractEpollChannel
  implements DatagramChannel
{
  private static final ChannelMetadata METADATA = new ChannelMetadata(true);
  private static final String EXPECTED_TYPES = " (expected: " + 
    StringUtil.simpleClassName(DatagramPacket.class) + ", " + 
    StringUtil.simpleClassName(AddressedEnvelope.class) + '<' + 
    StringUtil.simpleClassName(ByteBuf.class) + ", " + 
    StringUtil.simpleClassName(InetSocketAddress.class) + ">, " + 
    StringUtil.simpleClassName(ByteBuf.class) + ')';
  

  private final EpollDatagramChannelConfig config;
  

  private volatile boolean connected;
  

  public static boolean isSegmentedDatagramPacketSupported()
  {
    return (Epoll.isAvailable()) && (Native.IS_SUPPORTING_SENDMMSG) && (Native.IS_SUPPORTING_UDP_SEGMENT);
  }
  





  public EpollDatagramChannel()
  {
    this(null);
  }
  



  public EpollDatagramChannel(InternetProtocolFamily family)
  {
    this(family == null ? 
      LinuxSocket.newSocketDgram(Socket.isIPv6Preferred()) : LinuxSocket.newSocketDgram(family == InternetProtocolFamily.IPv6), false);
  }
  




  public EpollDatagramChannel(int fd)
  {
    this(new LinuxSocket(fd), true);
  }
  
  private EpollDatagramChannel(LinuxSocket fd, boolean active) {
    super(null, fd, active);
    config = new EpollDatagramChannelConfig(this);
  }
  
  public InetSocketAddress remoteAddress()
  {
    return (InetSocketAddress)super.remoteAddress();
  }
  
  public InetSocketAddress localAddress()
  {
    return (InetSocketAddress)super.localAddress();
  }
  
  public ChannelMetadata metadata()
  {
    return METADATA;
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
    try
    {
      socket.joinGroup(multicastAddress, networkInterface, source);
      promise.setSuccess();
    } catch (IOException e) {
      promise.setFailure(e);
    }
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
    try
    {
      socket.leaveGroup(multicastAddress, networkInterface, source);
      promise.setSuccess();
    } catch (IOException e) {
      promise.setFailure(e);
    }
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
  
  protected AbstractEpollChannel.AbstractEpollUnsafe newUnsafe()
  {
    return new EpollDatagramChannelUnsafe();
  }
  
  protected void doBind(SocketAddress localAddress) throws Exception
  {
    if ((localAddress instanceof InetSocketAddress)) {
      InetSocketAddress socketAddress = (InetSocketAddress)localAddress;
      if ((socketAddress.getAddress().isAnyLocalAddress()) && 
        ((socketAddress.getAddress() instanceof Inet4Address)) && 
        (socket.family() == InternetProtocolFamily.IPv6)) {
        localAddress = new InetSocketAddress(LinuxSocket.INET6_ANY, socketAddress.getPort());
      }
    }
    
    super.doBind(localAddress);
    active = true;
  }
  
  protected void doWrite(ChannelOutboundBuffer in) throws Exception
  {
    int maxMessagesPerWrite = maxMessagesPerWrite();
    while (maxMessagesPerWrite > 0) {
      Object msg = in.current();
      if (msg == null) {
        break;
      }
      

      try
      {
        if (((Native.IS_SUPPORTING_SENDMMSG) && (in.size() > 1)) || 
        
          ((in.current() instanceof SegmentedDatagramPacket))) {
          NativeDatagramPacketArray array = cleanDatagramPacketArray();
          array.add(in, isConnected(), maxMessagesPerWrite);
          int cnt = array.count();
          
          if (cnt >= 1)
          {
            int offset = 0;
            NativeDatagramPacketArray.NativeDatagramPacket[] packets = array.packets();
            
            int send = socket.sendmmsg(packets, offset, cnt);
            if (send == 0) {
              break;
            }
            
            for (int i = 0; i < send; i++) {
              in.remove();
            }
            maxMessagesPerWrite -= send;
            continue;
          }
        }
        boolean done = false;
        for (int i = config().getWriteSpinCount(); i > 0; i--) {
          if (doWriteMessage(msg)) {
            done = true;
            break;
          }
        }
        
        if (done) {
          in.remove();
          maxMessagesPerWrite--;
        } else {
          break;
        }
      } catch (IOException e) {
        maxMessagesPerWrite--;
        


        in.remove(e);
      }
    }
    
    if (in.isEmpty())
    {
      clearFlag(Native.EPOLLOUT);
    }
    else
      setFlag(Native.EPOLLOUT);
  }
  
  private boolean doWriteMessage(Object msg) throws Exception {
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
    
    return doWriteOrSendBytes(data, remoteAddress, false) > 0L;
  }
  
  protected Object filterOutboundMessage(Object msg)
  {
    if ((msg instanceof SegmentedDatagramPacket)) {
      if (!Native.IS_SUPPORTING_UDP_SEGMENT)
      {
        throw new UnsupportedOperationException("unsupported message type: " + StringUtil.simpleClassName(msg) + EXPECTED_TYPES);
      }
      SegmentedDatagramPacket packet = (SegmentedDatagramPacket)msg;
      ByteBuf content = (ByteBuf)packet.content();
      return UnixChannelUtil.isBufferCopyNeededForWrite(content) ? packet
        .replace(newDirectBuffer(packet, content)) : msg;
    }
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
  
  public EpollDatagramChannelConfig config()
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
  
  final class EpollDatagramChannelUnsafe extends AbstractEpollChannel.AbstractEpollUnsafe { EpollDatagramChannelUnsafe() { super(); }
    
    void epollInReady()
    {
      assert (eventLoop().inEventLoop());
      EpollDatagramChannelConfig config = config();
      if (shouldBreakEpollInReady(config)) {
        clearEpollIn0();
        return;
      }
      EpollRecvByteAllocatorHandle allocHandle = recvBufAllocHandle();
      allocHandle.edgeTriggered(isFlagSet(Native.EPOLLET));
      
      ChannelPipeline pipeline = pipeline();
      ByteBufAllocator allocator = config.getAllocator();
      allocHandle.reset(config);
      epollInBefore();
      
      Throwable exception = null;
      try {
        try {
          boolean connected = isConnected();
          do
          {
            int datagramSize = config().getMaxDatagramPayloadSize();
            
            ByteBuf byteBuf = allocHandle.allocate(allocator);
            

            int numDatagram = Native.IS_SUPPORTING_RECVMMSG ? byteBuf.writableBytes() / datagramSize : datagramSize == 0 ? 1 : 0;
            try {
              boolean read;
              if (numDatagram <= 1) { boolean read;
                if ((!connected) || (config.isUdpGro())) {
                  read = EpollDatagramChannel.this.recvmsg(allocHandle, EpollDatagramChannel.access$000(EpollDatagramChannel.this), byteBuf);
                } else {
                  read = EpollDatagramChannel.this.connectedRead(allocHandle, byteBuf, datagramSize);
                }
              }
              else {
                read = EpollDatagramChannel.this.scatteringRead(allocHandle, EpollDatagramChannel.access$000(EpollDatagramChannel.this), byteBuf, datagramSize, numDatagram);
              }
            } catch (Errors.NativeIoException e) {
              boolean read;
              if (connected) {
                throw EpollDatagramChannel.this.translateForConnected(e);
              }
              throw e;
            }
            boolean read;
            if (!read) break;
            readPending = false;




          }
          while (allocHandle.continueReading(UncheckedBooleanSupplier.TRUE_SUPPLIER));
        } catch (Throwable t) {
          exception = t;
        }
        
        allocHandle.readComplete();
        pipeline.fireChannelReadComplete();
        
        if (exception != null) {
          pipeline.fireExceptionCaught(exception);
        }
      } finally {
        epollInFinally(config);
      }
    }
  }
  
  private boolean connectedRead(EpollRecvByteAllocatorHandle allocHandle, ByteBuf byteBuf, int maxDatagramPacketSize) throws Exception
  {
    try
    {
      int writable = maxDatagramPacketSize != 0 ? Math.min(byteBuf.writableBytes(), maxDatagramPacketSize) : byteBuf.writableBytes();
      allocHandle.attemptedBytesRead(writable);
      
      int writerIndex = byteBuf.writerIndex();
      int localReadAmount;
      ByteBuffer buf; int localReadAmount; if (byteBuf.hasMemoryAddress()) {
        localReadAmount = socket.readAddress(byteBuf.memoryAddress(), writerIndex, writerIndex + writable);
      } else {
        buf = byteBuf.internalNioBuffer(writerIndex, writable);
        localReadAmount = socket.read(buf, buf.position(), buf.limit());
      }
      
      if (localReadAmount <= 0) {
        allocHandle.lastBytesRead(localReadAmount);
        

        return 0;
      }
      byteBuf.writerIndex(writerIndex + localReadAmount);
      
      allocHandle.lastBytesRead(maxDatagramPacketSize <= 0 ? localReadAmount : writable);
      

      DatagramPacket packet = new DatagramPacket(byteBuf, localAddress(), remoteAddress());
      allocHandle.incMessagesRead(1);
      
      pipeline().fireChannelRead(packet);
      byteBuf = null;
      return true;
    } finally {
      if (byteBuf != null) {
        byteBuf.release();
      }
    }
  }
  
  private IOException translateForConnected(Errors.NativeIoException e)
  {
    if (e.expectedErr() == Errors.ERROR_ECONNREFUSED_NEGATIVE) {
      PortUnreachableException error = new PortUnreachableException(e.getMessage());
      error.initCause(e);
      return error;
    }
    return e;
  }
  
  private static void addDatagramPacketToOut(DatagramPacket packet, RecyclableArrayList out)
  {
    if ((packet instanceof SegmentedDatagramPacket)) {
      SegmentedDatagramPacket segmentedDatagramPacket = (SegmentedDatagramPacket)packet;
      
      ByteBuf content = (ByteBuf)segmentedDatagramPacket.content();
      InetSocketAddress recipient = (InetSocketAddress)segmentedDatagramPacket.recipient();
      InetSocketAddress sender = (InetSocketAddress)segmentedDatagramPacket.sender();
      int segmentSize = segmentedDatagramPacket.segmentSize();
      do {
        out.add(new DatagramPacket(content.readRetainedSlice(Math.min(content.readableBytes(), segmentSize)), recipient, sender));
      }
      while (content.isReadable());
      
      segmentedDatagramPacket.release();
    } else {
      out.add(packet);
    }
  }
  
  private static void releaseAndRecycle(ByteBuf byteBuf, RecyclableArrayList packetList) {
    if (byteBuf != null) {
      byteBuf.release();
    }
    if (packetList != null) {
      for (int i = 0; i < packetList.size(); i++) {
        ReferenceCountUtil.release(packetList.get(i));
      }
      packetList.recycle();
    }
  }
  
  private static void processPacket(ChannelPipeline pipeline, EpollRecvByteAllocatorHandle handle, int bytesRead, DatagramPacket packet)
  {
    handle.lastBytesRead(bytesRead);
    handle.incMessagesRead(1);
    pipeline.fireChannelRead(packet);
  }
  
  private static void processPacketList(ChannelPipeline pipeline, EpollRecvByteAllocatorHandle handle, int bytesRead, RecyclableArrayList packetList)
  {
    int messagesRead = packetList.size();
    handle.lastBytesRead(bytesRead);
    handle.incMessagesRead(messagesRead);
    for (int i = 0; i < messagesRead; i++) {
      pipeline.fireChannelRead(packetList.set(i, Unpooled.EMPTY_BUFFER));
    }
  }
  
  private boolean recvmsg(EpollRecvByteAllocatorHandle allocHandle, NativeDatagramPacketArray array, ByteBuf byteBuf) throws IOException
  {
    RecyclableArrayList datagramPackets = null;
    try {
      int writable = byteBuf.writableBytes();
      
      boolean added = array.addWritable(byteBuf, byteBuf.writerIndex(), writable);
      assert (added);
      
      allocHandle.attemptedBytesRead(writable);
      
      NativeDatagramPacketArray.NativeDatagramPacket msg = array.packets()[0];
      
      int bytesReceived = socket.recvmsg(msg);
      if (bytesReceived == 0) {
        allocHandle.lastBytesRead(-1);
        return false;
      }
      byteBuf.writerIndex(bytesReceived);
      InetSocketAddress local = localAddress();
      DatagramPacket packet = msg.newDatagramPacket(byteBuf, local);
      if (!(packet instanceof SegmentedDatagramPacket)) {
        processPacket(pipeline(), allocHandle, bytesReceived, packet);
        byteBuf = null;

      }
      else
      {
        datagramPackets = RecyclableArrayList.newInstance();
        addDatagramPacketToOut(packet, datagramPackets);
        

        byteBuf = null;
        
        processPacketList(pipeline(), allocHandle, bytesReceived, datagramPackets);
        datagramPackets.recycle();
        datagramPackets = null;
      }
      
      return true;
    } finally {
      releaseAndRecycle(byteBuf, datagramPackets);
    }
  }
  
  private boolean scatteringRead(EpollRecvByteAllocatorHandle allocHandle, NativeDatagramPacketArray array, ByteBuf byteBuf, int datagramSize, int numDatagram) throws IOException
  {
    RecyclableArrayList datagramPackets = null;
    try {
      int offset = byteBuf.writerIndex();
      for (int i = 0; i < numDatagram; offset += datagramSize) {
        if (!array.addWritable(byteBuf, offset, datagramSize)) {
          break;
        }
        i++;
      }
      



      allocHandle.attemptedBytesRead(offset - byteBuf.writerIndex());
      
      NativeDatagramPacketArray.NativeDatagramPacket[] packets = array.packets();
      
      int received = socket.recvmmsg(packets, 0, array.count());
      if (received == 0) {
        allocHandle.lastBytesRead(-1);
        return false;
      }
      int bytesReceived = received * datagramSize;
      byteBuf.writerIndex(bytesReceived);
      InetSocketAddress local = localAddress();
      if (received == 1)
      {
        DatagramPacket packet = packets[0].newDatagramPacket(byteBuf, local);
        if (!(packet instanceof SegmentedDatagramPacket)) {
          processPacket(pipeline(), allocHandle, datagramSize, packet);
          byteBuf = null;
          return true;
        }
      }
      


      datagramPackets = RecyclableArrayList.newInstance();
      for (int i = 0; i < received; i++) {
        DatagramPacket packet = packets[i].newDatagramPacket(byteBuf.readRetainedSlice(datagramSize), local);
        addDatagramPacketToOut(packet, datagramPackets);
      }
      
      byteBuf.release();
      byteBuf = null;
      
      processPacketList(pipeline(), allocHandle, bytesReceived, datagramPackets);
      datagramPackets.recycle();
      datagramPackets = null;
      return 1;
    } finally {
      releaseAndRecycle(byteBuf, datagramPackets);
    }
  }
  
  private NativeDatagramPacketArray cleanDatagramPacketArray() {
    return ((EpollEventLoop)eventLoop()).cleanDatagramPacketArray();
  }
}
