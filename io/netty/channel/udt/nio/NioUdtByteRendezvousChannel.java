package io.netty.channel.udt.nio;

import com.barchart.udt.TypeUDT;



















@Deprecated
public class NioUdtByteRendezvousChannel
  extends NioUdtByteConnectorChannel
{
  public NioUdtByteRendezvousChannel()
  {
    super(NioUdtProvider.newRendezvousChannelUDT(TypeUDT.STREAM));
  }
}
