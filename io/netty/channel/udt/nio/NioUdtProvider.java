package io.netty.channel.udt.nio;

import com.barchart.udt.SocketUDT;
import com.barchart.udt.TypeUDT;
import com.barchart.udt.nio.ChannelUDT;
import com.barchart.udt.nio.KindUDT;
import com.barchart.udt.nio.RendezvousChannelUDT;
import com.barchart.udt.nio.SelectorProviderUDT;
import com.barchart.udt.nio.ServerSocketChannelUDT;
import com.barchart.udt.nio.SocketChannelUDT;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFactory;
import io.netty.channel.udt.UdtChannel;
import io.netty.channel.udt.UdtServerChannel;
import java.io.IOException;
import java.nio.channels.spi.SelectorProvider;





























@Deprecated
public final class NioUdtProvider<T extends UdtChannel>
  implements ChannelFactory<T>
{
  public static final ChannelFactory<UdtServerChannel> BYTE_ACCEPTOR = new NioUdtProvider(TypeUDT.STREAM, KindUDT.ACCEPTOR);
  





  public static final ChannelFactory<UdtChannel> BYTE_CONNECTOR = new NioUdtProvider(TypeUDT.STREAM, KindUDT.CONNECTOR);
  





  public static final SelectorProvider BYTE_PROVIDER = SelectorProviderUDT.STREAM;
  




  public static final ChannelFactory<UdtChannel> BYTE_RENDEZVOUS = new NioUdtProvider(TypeUDT.STREAM, KindUDT.RENDEZVOUS);
  





  public static final ChannelFactory<UdtServerChannel> MESSAGE_ACCEPTOR = new NioUdtProvider(TypeUDT.DATAGRAM, KindUDT.ACCEPTOR);
  





  public static final ChannelFactory<UdtChannel> MESSAGE_CONNECTOR = new NioUdtProvider(TypeUDT.DATAGRAM, KindUDT.CONNECTOR);
  





  public static final SelectorProvider MESSAGE_PROVIDER = SelectorProviderUDT.DATAGRAM;
  




  public static final ChannelFactory<UdtChannel> MESSAGE_RENDEZVOUS = new NioUdtProvider(TypeUDT.DATAGRAM, KindUDT.RENDEZVOUS);
  

  private final KindUDT kind;
  

  private final TypeUDT type;
  

  public static ChannelUDT channelUDT(Channel channel)
  {
    if ((channel instanceof NioUdtByteAcceptorChannel)) {
      return ((NioUdtByteAcceptorChannel)channel).javaChannel();
    }
    if ((channel instanceof NioUdtByteRendezvousChannel)) {
      return ((NioUdtByteRendezvousChannel)channel).javaChannel();
    }
    if ((channel instanceof NioUdtByteConnectorChannel)) {
      return ((NioUdtByteConnectorChannel)channel).javaChannel();
    }
    

    if ((channel instanceof NioUdtMessageAcceptorChannel)) {
      return ((NioUdtMessageAcceptorChannel)channel).javaChannel();
    }
    if ((channel instanceof NioUdtMessageRendezvousChannel)) {
      return ((NioUdtMessageRendezvousChannel)channel).javaChannel();
    }
    if ((channel instanceof NioUdtMessageConnectorChannel)) {
      return ((NioUdtMessageConnectorChannel)channel).javaChannel();
    }
    
    return null;
  }
  


  static ServerSocketChannelUDT newAcceptorChannelUDT(TypeUDT type)
  {
    try
    {
      return SelectorProviderUDT.from(type).openServerSocketChannel();
    } catch (IOException e) {
      throw new ChannelException("failed to open a server socket channel", e);
    }
  }
  

  static SocketChannelUDT newConnectorChannelUDT(TypeUDT type)
  {
    try
    {
      return SelectorProviderUDT.from(type).openSocketChannel();
    } catch (IOException e) {
      throw new ChannelException("failed to open a socket channel", e);
    }
  }
  


  static RendezvousChannelUDT newRendezvousChannelUDT(TypeUDT type)
  {
    try
    {
      return SelectorProviderUDT.from(type).openRendezvousChannel();
    } catch (IOException e) {
      throw new ChannelException("failed to open a rendezvous channel", e);
    }
  }
  





  public static SocketUDT socketUDT(Channel channel)
  {
    ChannelUDT channelUDT = channelUDT(channel);
    if (channelUDT == null) {
      return null;
    }
    return channelUDT.socketUDT();
  }
  






  private NioUdtProvider(TypeUDT type, KindUDT kind)
  {
    this.type = type;
    this.kind = kind;
  }
  


  public KindUDT kind()
  {
    return kind;
  }
  





  public T newChannel()
  {
    switch (1.$SwitchMap$com$barchart$udt$nio$KindUDT[kind.ordinal()]) {
    case 1: 
      switch (type) {
      case DATAGRAM: 
        return new NioUdtMessageAcceptorChannel();
      case STREAM: 
        return new NioUdtByteAcceptorChannel();
      }
      throw new IllegalStateException("wrong type=" + type);
    
    case 2: 
      switch (type) {
      case DATAGRAM: 
        return new NioUdtMessageConnectorChannel();
      case STREAM: 
        return new NioUdtByteConnectorChannel();
      }
      throw new IllegalStateException("wrong type=" + type);
    
    case 3: 
      switch (type) {
      case DATAGRAM: 
        return new NioUdtMessageRendezvousChannel();
      case STREAM: 
        return new NioUdtByteRendezvousChannel();
      }
      throw new IllegalStateException("wrong type=" + type);
    }
    
    throw new IllegalStateException("wrong kind=" + kind);
  }
  



  public TypeUDT type()
  {
    return type;
  }
}
