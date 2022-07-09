package io.netty.handler.codec.dns;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.AddressedEnvelope;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.net.InetSocketAddress;
import java.util.List;























@ChannelHandler.Sharable
public class DatagramDnsQueryEncoder
  extends MessageToMessageEncoder<AddressedEnvelope<DnsQuery, InetSocketAddress>>
{
  private final DnsQueryEncoder encoder;
  
  public DatagramDnsQueryEncoder()
  {
    this(DnsRecordEncoder.DEFAULT);
  }
  


  public DatagramDnsQueryEncoder(DnsRecordEncoder recordEncoder)
  {
    encoder = new DnsQueryEncoder(recordEncoder);
  }
  


  protected void encode(ChannelHandlerContext ctx, AddressedEnvelope<DnsQuery, InetSocketAddress> in, List<Object> out)
    throws Exception
  {
    InetSocketAddress recipient = (InetSocketAddress)in.recipient();
    DnsQuery query = (DnsQuery)in.content();
    ByteBuf buf = allocateBuffer(ctx, in);
    
    boolean success = false;
    try {
      encoder.encode(query, buf);
      success = true;
    } finally {
      if (!success) {
        buf.release();
      }
    }
    
    out.add(new DatagramPacket(buf, recipient, null));
  }
  




  protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, AddressedEnvelope<DnsQuery, InetSocketAddress> msg)
    throws Exception
  {
    return ctx.alloc().ioBuffer(1024);
  }
}
