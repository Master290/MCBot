package io.netty.channel.epoll;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelOutboundBuffer.MessageProcessor;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.unix.IovArray;
import io.netty.channel.unix.Limits;
import io.netty.channel.unix.NativeInetAddress;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;























final class NativeDatagramPacketArray
{
  private final NativeDatagramPacket[] packets = new NativeDatagramPacket[Limits.UIO_MAX_IOV];
  


  private final IovArray iovArray = new IovArray();
  

  private final byte[] ipv4Bytes = new byte[4];
  private final MyMessageProcessor processor = new MyMessageProcessor(null);
  private int count;
  
  NativeDatagramPacketArray()
  {
    for (int i = 0; i < packets.length; i++) {
      packets[i] = new NativeDatagramPacket();
    }
  }
  
  boolean addWritable(ByteBuf buf, int index, int len) {
    return add0(buf, index, len, 0, null);
  }
  
  private boolean add0(ByteBuf buf, int index, int len, int segmentLen, InetSocketAddress recipient) {
    if (count == packets.length)
    {

      return false;
    }
    if (len == 0) {
      return true;
    }
    int offset = iovArray.count();
    if ((offset == Limits.IOV_MAX) || (!iovArray.add(buf, index, len)))
    {
      return false;
    }
    NativeDatagramPacket p = packets[count];
    p.init(iovArray.memoryAddress(offset), iovArray.count() - offset, segmentLen, recipient);
    
    count += 1;
    return true;
  }
  
  void add(ChannelOutboundBuffer buffer, boolean connected, int maxMessagesPerWrite) throws Exception {
    processor.connected = connected;
    processor.maxMessagesPerWrite = maxMessagesPerWrite;
    buffer.forEachFlushedMessage(processor);
  }
  


  int count()
  {
    return count;
  }
  


  NativeDatagramPacket[] packets()
  {
    return packets;
  }
  
  void clear() {
    count = 0;
    iovArray.clear();
  }
  

  void release() { iovArray.release(); }
  
  private final class MyMessageProcessor implements ChannelOutboundBuffer.MessageProcessor {
    private boolean connected;
    private int maxMessagesPerWrite;
    
    private MyMessageProcessor() {}
    
    public boolean processMessage(Object msg) { boolean added;
      boolean added;
      if ((msg instanceof DatagramPacket)) {
        DatagramPacket packet = (DatagramPacket)msg;
        ByteBuf buf = (ByteBuf)packet.content();
        int segmentSize = 0;
        if ((packet instanceof io.netty.channel.unix.SegmentedDatagramPacket)) {
          int seg = ((io.netty.channel.unix.SegmentedDatagramPacket)packet).segmentSize();
          

          if (buf.readableBytes() > seg) {
            segmentSize = seg;
          }
        }
        added = NativeDatagramPacketArray.this.add0(buf, buf.readerIndex(), buf.readableBytes(), segmentSize, (InetSocketAddress)packet.recipient()); } else { boolean added;
        if (((msg instanceof ByteBuf)) && (connected)) {
          ByteBuf buf = (ByteBuf)msg;
          added = NativeDatagramPacketArray.this.add0(buf, buf.readerIndex(), buf.readableBytes(), 0, null);
        } else {
          added = false;
        } }
      if (added) {
        maxMessagesPerWrite -= 1;
        return maxMessagesPerWrite > 0;
      }
      return false;
    }
  }
  
  private static InetSocketAddress newAddress(byte[] addr, int addrLen, int port, int scopeId, byte[] ipv4Bytes) throws UnknownHostException {
    InetAddress address;
    InetAddress address;
    if (addrLen == ipv4Bytes.length) {
      System.arraycopy(addr, 0, ipv4Bytes, 0, addrLen);
      address = InetAddress.getByAddress(ipv4Bytes);
    } else {
      address = Inet6Address.getByAddress(null, addr, scopeId);
    }
    return new InetSocketAddress(address, port);
  }
  


  final class NativeDatagramPacket
  {
    private long memoryAddress;
    

    private int count;
    

    private final byte[] senderAddr = new byte[16];
    
    private int senderAddrLen;
    private int senderScopeId;
    private int senderPort;
    private final byte[] recipientAddr = new byte[16];
    private int recipientAddrLen;
    private int recipientScopeId;
    private int recipientPort;
    private int segmentSize;
    
    NativeDatagramPacket() {}
    
    private void init(long memoryAddress, int count, int segmentSize, InetSocketAddress recipient) { this.memoryAddress = memoryAddress;
      this.count = count;
      this.segmentSize = segmentSize;
      
      senderScopeId = 0;
      senderPort = 0;
      senderAddrLen = 0;
      
      if (recipient == null) {
        recipientScopeId = 0;
        recipientPort = 0;
        recipientAddrLen = 0;
      } else {
        InetAddress address = recipient.getAddress();
        if ((address instanceof Inet6Address)) {
          System.arraycopy(address.getAddress(), 0, recipientAddr, 0, recipientAddr.length);
          recipientScopeId = ((Inet6Address)address).getScopeId();
        } else {
          NativeInetAddress.copyIpv4MappedIpv6Address(address.getAddress(), recipientAddr);
          recipientScopeId = 0;
        }
        recipientAddrLen = recipientAddr.length;
        recipientPort = recipient.getPort();
      }
    }
    
    DatagramPacket newDatagramPacket(ByteBuf buffer, InetSocketAddress recipient) throws UnknownHostException {
      InetSocketAddress sender = NativeDatagramPacketArray.newAddress(senderAddr, senderAddrLen, senderPort, senderScopeId, ipv4Bytes);
      if (recipientAddrLen != 0) {
        recipient = NativeDatagramPacketArray.newAddress(recipientAddr, recipientAddrLen, recipientPort, recipientScopeId, ipv4Bytes);
      }
      buffer.writerIndex(count);
      

      if (segmentSize > 0) {
        return new SegmentedDatagramPacket(buffer, segmentSize, recipient, sender);
      }
      return new DatagramPacket(buffer, recipient, sender);
    }
  }
}
