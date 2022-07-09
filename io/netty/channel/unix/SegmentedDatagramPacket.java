package io.netty.channel.unix;

import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.internal.ObjectUtil;
import java.net.InetSocketAddress;


























public class SegmentedDatagramPacket
  extends DatagramPacket
{
  private final int segmentSize;
  
  public SegmentedDatagramPacket(ByteBuf data, int segmentSize, InetSocketAddress recipient)
  {
    super(data, recipient);
    this.segmentSize = ObjectUtil.checkPositive(segmentSize, "segmentSize");
  }
  







  public SegmentedDatagramPacket(ByteBuf data, int segmentSize, InetSocketAddress recipient, InetSocketAddress sender)
  {
    super(data, recipient, sender);
    this.segmentSize = ObjectUtil.checkPositive(segmentSize, "segmentSize");
  }
  




  public int segmentSize()
  {
    return segmentSize;
  }
  
  public SegmentedDatagramPacket copy()
  {
    return new SegmentedDatagramPacket(((ByteBuf)content()).copy(), segmentSize, (InetSocketAddress)recipient(), (InetSocketAddress)sender());
  }
  
  public SegmentedDatagramPacket duplicate()
  {
    return new SegmentedDatagramPacket(((ByteBuf)content()).duplicate(), segmentSize, (InetSocketAddress)recipient(), (InetSocketAddress)sender());
  }
  
  public SegmentedDatagramPacket retainedDuplicate()
  {
    return new SegmentedDatagramPacket(((ByteBuf)content()).retainedDuplicate(), segmentSize, (InetSocketAddress)recipient(), (InetSocketAddress)sender());
  }
  
  public SegmentedDatagramPacket replace(ByteBuf content)
  {
    return new SegmentedDatagramPacket(content, segmentSize, (InetSocketAddress)recipient(), (InetSocketAddress)sender());
  }
  
  public SegmentedDatagramPacket retain()
  {
    super.retain();
    return this;
  }
  
  public SegmentedDatagramPacket retain(int increment)
  {
    super.retain(increment);
    return this;
  }
  
  public SegmentedDatagramPacket touch()
  {
    super.touch();
    return this;
  }
  
  public SegmentedDatagramPacket touch(Object hint)
  {
    super.touch(hint);
    return this;
  }
}
