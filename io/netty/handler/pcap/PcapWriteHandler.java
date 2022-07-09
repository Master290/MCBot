package io.netty.handler.pcap;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.NetUtil;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;










































public final class PcapWriteHandler
  extends ChannelDuplexHandler
  implements Closeable
{
  private final InternalLogger logger = InternalLoggerFactory.getInstance(PcapWriteHandler.class);
  




  private PcapWriter pCapWriter;
  




  private final OutputStream outputStream;
  



  private final boolean captureZeroByte;
  



  private final boolean writePcapGlobalHeader;
  



  private int sendSegmentNumber = 1;
  




  private int receiveSegmentNumber = 1;
  





  private InetSocketAddress srcAddr;
  




  private InetSocketAddress dstAddr;
  




  private boolean isClosed;
  





  public PcapWriteHandler(OutputStream outputStream)
  {
    this(outputStream, false, true);
  }
  













  public PcapWriteHandler(OutputStream outputStream, boolean captureZeroByte, boolean writePcapGlobalHeader)
  {
    this.outputStream = ((OutputStream)ObjectUtil.checkNotNull(outputStream, "OutputStream"));
    this.captureZeroByte = captureZeroByte;
    this.writePcapGlobalHeader = writePcapGlobalHeader;
  }
  
  public void channelActive(ChannelHandlerContext ctx)
    throws Exception
  {
    ByteBufAllocator byteBufAllocator = ctx.alloc();
    



    if (writePcapGlobalHeader)
    {
      ByteBuf byteBuf = byteBufAllocator.buffer();
      try {
        pCapWriter = new PcapWriter(outputStream, byteBuf);
      } catch (IOException ex) {
        ctx.channel().close();
        ctx.fireExceptionCaught(ex);
        logger.error("Caught Exception While Initializing PcapWriter, Closing Channel.", ex);
      } finally {
        byteBuf.release();
      }
    } else {
      pCapWriter = new PcapWriter(outputStream);
    }
    

    if ((ctx.channel() instanceof SocketChannel))
    {

      if ((ctx.channel().parent() instanceof ServerSocketChannel)) {
        srcAddr = ((InetSocketAddress)ctx.channel().remoteAddress());
        dstAddr = ((InetSocketAddress)ctx.channel().localAddress());
      } else {
        srcAddr = ((InetSocketAddress)ctx.channel().localAddress());
        dstAddr = ((InetSocketAddress)ctx.channel().remoteAddress());
      }
      
      logger.debug("Initiating Fake TCP 3-Way Handshake");
      
      ByteBuf tcpBuf = byteBufAllocator.buffer();
      
      try
      {
        TCPPacket.writePacket(tcpBuf, null, 0, 0, srcAddr.getPort(), dstAddr.getPort(), new TCPPacket.TCPFlag[] { TCPPacket.TCPFlag.SYN });
        completeTCPWrite(srcAddr, dstAddr, tcpBuf, byteBufAllocator, ctx);
        

        TCPPacket.writePacket(tcpBuf, null, 0, 1, dstAddr.getPort(), srcAddr.getPort(), new TCPPacket.TCPFlag[] { TCPPacket.TCPFlag.SYN, TCPPacket.TCPFlag.ACK });
        
        completeTCPWrite(dstAddr, srcAddr, tcpBuf, byteBufAllocator, ctx);
        

        TCPPacket.writePacket(tcpBuf, null, 1, 1, srcAddr.getPort(), dstAddr.getPort(), new TCPPacket.TCPFlag[] { TCPPacket.TCPFlag.ACK });
        completeTCPWrite(srcAddr, dstAddr, tcpBuf, byteBufAllocator, ctx);
      } finally {
        tcpBuf.release();
      }
      
      logger.debug("Finished Fake TCP 3-Way Handshake");
    } else if ((ctx.channel() instanceof DatagramChannel)) {
      DatagramChannel datagramChannel = (DatagramChannel)ctx.channel();
      


      if (datagramChannel.isConnected()) {
        srcAddr = ((InetSocketAddress)ctx.channel().localAddress());
        dstAddr = ((InetSocketAddress)ctx.channel().remoteAddress());
      }
    }
    
    super.channelActive(ctx);
  }
  
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
  {
    if (!isClosed) {
      if ((ctx.channel() instanceof SocketChannel)) {
        handleTCP(ctx, msg, false);
      } else if ((ctx.channel() instanceof DatagramChannel)) {
        handleUDP(ctx, msg);
      } else {
        logger.debug("Discarding Pcap Write for Unknown Channel Type: {}", ctx.channel());
      }
    }
    super.channelRead(ctx, msg);
  }
  
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception
  {
    if (!isClosed) {
      if ((ctx.channel() instanceof SocketChannel)) {
        handleTCP(ctx, msg, true);
      } else if ((ctx.channel() instanceof DatagramChannel)) {
        handleUDP(ctx, msg);
      } else {
        logger.debug("Discarding Pcap Write for Unknown Channel Type: {}", ctx.channel());
      }
    }
    super.write(ctx, msg, promise);
  }
  








  private void handleTCP(ChannelHandlerContext ctx, Object msg, boolean isWriteOperation)
  {
    if ((msg instanceof ByteBuf))
    {

      if ((((ByteBuf)msg).readableBytes() == 0) && (!captureZeroByte)) {
        logger.debug("Discarding Zero Byte TCP Packet. isWriteOperation {}", Boolean.valueOf(isWriteOperation));
        return;
      }
      
      ByteBufAllocator byteBufAllocator = ctx.alloc();
      ByteBuf packet = ((ByteBuf)msg).duplicate();
      ByteBuf tcpBuf = byteBufAllocator.buffer();
      int bytes = packet.readableBytes();
      try
      {
        if (isWriteOperation) {
          TCPPacket.writePacket(tcpBuf, packet, sendSegmentNumber, receiveSegmentNumber, srcAddr.getPort(), dstAddr
            .getPort(), new TCPPacket.TCPFlag[] { TCPPacket.TCPFlag.ACK });
          completeTCPWrite(srcAddr, dstAddr, tcpBuf, byteBufAllocator, ctx);
          logTCP(true, bytes, sendSegmentNumber, receiveSegmentNumber, srcAddr, dstAddr, false);
          
          sendSegmentNumber += bytes;
          
          TCPPacket.writePacket(tcpBuf, null, receiveSegmentNumber, sendSegmentNumber, dstAddr.getPort(), srcAddr
            .getPort(), new TCPPacket.TCPFlag[] { TCPPacket.TCPFlag.ACK });
          completeTCPWrite(dstAddr, srcAddr, tcpBuf, byteBufAllocator, ctx);
          logTCP(true, bytes, sendSegmentNumber, receiveSegmentNumber, dstAddr, srcAddr, true);
        } else {
          TCPPacket.writePacket(tcpBuf, packet, receiveSegmentNumber, sendSegmentNumber, dstAddr.getPort(), srcAddr
            .getPort(), new TCPPacket.TCPFlag[] { TCPPacket.TCPFlag.ACK });
          completeTCPWrite(dstAddr, srcAddr, tcpBuf, byteBufAllocator, ctx);
          logTCP(false, bytes, receiveSegmentNumber, sendSegmentNumber, dstAddr, srcAddr, false);
          
          receiveSegmentNumber += bytes;
          
          TCPPacket.writePacket(tcpBuf, null, sendSegmentNumber, receiveSegmentNumber, srcAddr.getPort(), dstAddr
            .getPort(), new TCPPacket.TCPFlag[] { TCPPacket.TCPFlag.ACK });
          completeTCPWrite(srcAddr, dstAddr, tcpBuf, byteBufAllocator, ctx);
          logTCP(false, bytes, sendSegmentNumber, receiveSegmentNumber, srcAddr, dstAddr, true);
        }
      } finally {
        tcpBuf.release();
      }
    } else {
      logger.debug("Discarding Pcap Write for TCP Object: {}", msg);
    }
  }
  










  private void completeTCPWrite(InetSocketAddress srcAddr, InetSocketAddress dstAddr, ByteBuf tcpBuf, ByteBufAllocator byteBufAllocator, ChannelHandlerContext ctx)
  {
    ByteBuf ipBuf = byteBufAllocator.buffer();
    ByteBuf ethernetBuf = byteBufAllocator.buffer();
    ByteBuf pcap = byteBufAllocator.buffer();
    try
    {
      if (((srcAddr.getAddress() instanceof Inet4Address)) && ((dstAddr.getAddress() instanceof Inet4Address))) {
        IPPacket.writeTCPv4(ipBuf, tcpBuf, 
          NetUtil.ipv4AddressToInt((Inet4Address)srcAddr.getAddress()), 
          NetUtil.ipv4AddressToInt((Inet4Address)dstAddr.getAddress()));
        
        EthernetPacket.writeIPv4(ethernetBuf, ipBuf);
      } else if (((srcAddr.getAddress() instanceof Inet6Address)) && ((dstAddr.getAddress() instanceof Inet6Address))) {
        IPPacket.writeTCPv6(ipBuf, tcpBuf, srcAddr
          .getAddress().getAddress(), dstAddr
          .getAddress().getAddress());
        
        EthernetPacket.writeIPv6(ethernetBuf, ipBuf);
      } else {
        logger.error("Source and Destination IP Address versions are not same. Source Address: {}, Destination Address: {}", srcAddr
          .getAddress(), dstAddr.getAddress());
        return;
      }
      

      pCapWriter.writePacket(pcap, ethernetBuf);
    } catch (IOException ex) {
      logger.error("Caught Exception While Writing Packet into Pcap", ex);
      ctx.fireExceptionCaught(ex);
    } finally {
      ipBuf.release();
      ethernetBuf.release();
      pcap.release();
    }
  }
  





  private void logTCP(boolean isWriteOperation, int bytes, int sendSegmentNumber, int receiveSegmentNumber, InetSocketAddress srcAddr, InetSocketAddress dstAddr, boolean ackOnly)
  {
    if (logger.isDebugEnabled()) {
      if (ackOnly) {
        logger.debug("Writing TCP ACK, isWriteOperation {}, Segment Number {}, Ack Number {}, Src Addr {}, Dst Addr {}", new Object[] {
          Boolean.valueOf(isWriteOperation), Integer.valueOf(sendSegmentNumber), Integer.valueOf(receiveSegmentNumber), dstAddr, srcAddr });
      } else {
        logger.debug("Writing TCP Data of {} Bytes, isWriteOperation {}, Segment Number {}, Ack Number {}, Src Addr {}, Dst Addr {}", new Object[] {
          Integer.valueOf(bytes), Boolean.valueOf(isWriteOperation), Integer.valueOf(sendSegmentNumber), 
          Integer.valueOf(receiveSegmentNumber), srcAddr, dstAddr });
      }
    }
  }
  






  private void handleUDP(ChannelHandlerContext ctx, Object msg)
  {
    ByteBuf udpBuf = ctx.alloc().buffer();
    try
    {
      if ((msg instanceof DatagramPacket))
      {

        if ((((ByteBuf)((DatagramPacket)msg).content()).readableBytes() == 0) && (!captureZeroByte)) {
          logger.debug("Discarding Zero Byte UDP Packet");
          return;
        }
        
        DatagramPacket datagramPacket = ((DatagramPacket)msg).duplicate();
        InetSocketAddress srcAddr = (InetSocketAddress)datagramPacket.sender();
        InetSocketAddress dstAddr = (InetSocketAddress)datagramPacket.recipient();
        


        if (srcAddr == null) {
          srcAddr = (InetSocketAddress)ctx.channel().localAddress();
        }
        
        logger.debug("Writing UDP Data of {} Bytes, Src Addr {}, Dst Addr {}", new Object[] {
          Integer.valueOf(((ByteBuf)datagramPacket.content()).readableBytes()), srcAddr, dstAddr });
        
        UDPPacket.writePacket(udpBuf, (ByteBuf)datagramPacket.content(), srcAddr.getPort(), dstAddr.getPort());
        completeUDPWrite(srcAddr, dstAddr, udpBuf, ctx.alloc(), ctx);
      } else if (((msg instanceof ByteBuf)) && (((DatagramChannel)ctx.channel()).isConnected()))
      {

        if ((((ByteBuf)msg).readableBytes() == 0) && (!captureZeroByte)) {
          logger.debug("Discarding Zero Byte UDP Packet");
          return;
        }
        
        ByteBuf byteBuf = ((ByteBuf)msg).duplicate();
        
        logger.debug("Writing UDP Data of {} Bytes, Src Addr {}, Dst Addr {}", new Object[] {
          Integer.valueOf(byteBuf.readableBytes()), this.srcAddr, this.dstAddr });
        
        UDPPacket.writePacket(udpBuf, byteBuf, this.srcAddr.getPort(), this.dstAddr.getPort());
        completeUDPWrite(this.srcAddr, this.dstAddr, udpBuf, ctx.alloc(), ctx);
      } else {
        logger.debug("Discarding Pcap Write for UDP Object: {}", msg);
      }
    } finally {
      udpBuf.release();
    }
  }
  










  private void completeUDPWrite(InetSocketAddress srcAddr, InetSocketAddress dstAddr, ByteBuf udpBuf, ByteBufAllocator byteBufAllocator, ChannelHandlerContext ctx)
  {
    ByteBuf ipBuf = byteBufAllocator.buffer();
    ByteBuf ethernetBuf = byteBufAllocator.buffer();
    ByteBuf pcap = byteBufAllocator.buffer();
    try
    {
      if (((srcAddr.getAddress() instanceof Inet4Address)) && ((dstAddr.getAddress() instanceof Inet4Address))) {
        IPPacket.writeUDPv4(ipBuf, udpBuf, 
          NetUtil.ipv4AddressToInt((Inet4Address)srcAddr.getAddress()), 
          NetUtil.ipv4AddressToInt((Inet4Address)dstAddr.getAddress()));
        
        EthernetPacket.writeIPv4(ethernetBuf, ipBuf);
      } else if (((srcAddr.getAddress() instanceof Inet6Address)) && ((dstAddr.getAddress() instanceof Inet6Address))) {
        IPPacket.writeUDPv6(ipBuf, udpBuf, srcAddr
          .getAddress().getAddress(), dstAddr
          .getAddress().getAddress());
        
        EthernetPacket.writeIPv6(ethernetBuf, ipBuf);
      } else {
        logger.error("Source and Destination IP Address versions are not same. Source Address: {}, Destination Address: {}", srcAddr
          .getAddress(), dstAddr.getAddress());
        return;
      }
      

      pCapWriter.writePacket(pcap, ethernetBuf);
    } catch (IOException ex) {
      logger.error("Caught Exception While Writing Packet into Pcap", ex);
      ctx.fireExceptionCaught(ex);
    } finally {
      ipBuf.release();
      ethernetBuf.release();
      pcap.release();
    }
  }
  

  public void handlerRemoved(ChannelHandlerContext ctx)
    throws Exception
  {
    if ((ctx.channel() instanceof SocketChannel)) {
      logger.debug("Starting Fake TCP FIN+ACK Flow to close connection");
      
      ByteBufAllocator byteBufAllocator = ctx.alloc();
      ByteBuf tcpBuf = byteBufAllocator.buffer();
      
      try
      {
        TCPPacket.writePacket(tcpBuf, null, sendSegmentNumber, receiveSegmentNumber, srcAddr.getPort(), dstAddr
          .getPort(), new TCPPacket.TCPFlag[] { TCPPacket.TCPFlag.FIN, TCPPacket.TCPFlag.ACK });
        completeTCPWrite(srcAddr, dstAddr, tcpBuf, byteBufAllocator, ctx);
        

        TCPPacket.writePacket(tcpBuf, null, receiveSegmentNumber, sendSegmentNumber, dstAddr.getPort(), srcAddr
          .getPort(), new TCPPacket.TCPFlag[] { TCPPacket.TCPFlag.FIN, TCPPacket.TCPFlag.ACK });
        completeTCPWrite(dstAddr, srcAddr, tcpBuf, byteBufAllocator, ctx);
        

        TCPPacket.writePacket(tcpBuf, null, sendSegmentNumber + 1, receiveSegmentNumber + 1, srcAddr
          .getPort(), dstAddr.getPort(), new TCPPacket.TCPFlag[] { TCPPacket.TCPFlag.ACK });
        completeTCPWrite(srcAddr, dstAddr, tcpBuf, byteBufAllocator, ctx);
      } finally {
        tcpBuf.release();
      }
      
      logger.debug("Finished Fake TCP FIN+ACK Flow to close connection");
    }
    
    close();
    super.handlerRemoved(ctx);
  }
  
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    throws Exception
  {
    if ((ctx.channel() instanceof SocketChannel)) {
      ByteBuf tcpBuf = ctx.alloc().buffer();
      
      try
      {
        TCPPacket.writePacket(tcpBuf, null, sendSegmentNumber, receiveSegmentNumber, srcAddr.getPort(), dstAddr
          .getPort(), new TCPPacket.TCPFlag[] { TCPPacket.TCPFlag.RST, TCPPacket.TCPFlag.ACK });
        completeTCPWrite(srcAddr, dstAddr, tcpBuf, ctx.alloc(), ctx);
      } finally {
        tcpBuf.release();
      }
      
      logger.debug("Sent Fake TCP RST to close connection");
    }
    
    close();
    ctx.fireExceptionCaught(cause);
  }
  






  public void close()
    throws IOException
  {
    if (isClosed) {
      logger.debug("PcapWriterHandler is already closed");
    } else {
      isClosed = true;
      pCapWriter.close();
      logger.debug("PcapWriterHandler is now closed");
    }
  }
}
