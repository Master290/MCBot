package io.netty.handler.codec.dns;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.AddressedEnvelope;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.internal.ObjectUtil;
import java.net.InetSocketAddress;
import java.util.List;

























@ChannelHandler.Sharable
public class DatagramDnsResponseEncoder
  extends MessageToMessageEncoder<AddressedEnvelope<DnsResponse, InetSocketAddress>>
{
  private final DnsRecordEncoder recordEncoder;
  
  public DatagramDnsResponseEncoder()
  {
    this(DnsRecordEncoder.DEFAULT);
  }
  


  public DatagramDnsResponseEncoder(DnsRecordEncoder recordEncoder)
  {
    this.recordEncoder = ((DnsRecordEncoder)ObjectUtil.checkNotNull(recordEncoder, "recordEncoder"));
  }
  

  protected void encode(ChannelHandlerContext ctx, AddressedEnvelope<DnsResponse, InetSocketAddress> in, List<Object> out)
    throws Exception
  {
    InetSocketAddress recipient = (InetSocketAddress)in.recipient();
    DnsResponse response = (DnsResponse)in.content();
    ByteBuf buf = allocateBuffer(ctx, in);
    
    DnsMessageUtil.encodeDnsResponse(recordEncoder, response, buf);
    
    out.add(new DatagramPacket(buf, recipient, null));
  }
  




  protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, AddressedEnvelope<DnsResponse, InetSocketAddress> msg)
    throws Exception
  {
    return ctx.alloc().ioBuffer(1024);
  }
}
