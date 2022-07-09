package io.netty.handler.codec.dns;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;


















@ChannelHandler.Sharable
public final class TcpDnsQueryEncoder
  extends MessageToByteEncoder<DnsQuery>
{
  private final DnsQueryEncoder encoder;
  
  public TcpDnsQueryEncoder()
  {
    this(DnsRecordEncoder.DEFAULT);
  }
  


  public TcpDnsQueryEncoder(DnsRecordEncoder recordEncoder)
  {
    encoder = new DnsQueryEncoder(recordEncoder);
  }
  

  protected void encode(ChannelHandlerContext ctx, DnsQuery msg, ByteBuf out)
    throws Exception
  {
    out.writerIndex(out.writerIndex() + 2);
    encoder.encode(msg, out);
    

    out.setShort(0, out.readableBytes() - 2);
  }
  

  protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, DnsQuery msg, boolean preferDirect)
  {
    if (preferDirect) {
      return ctx.alloc().ioBuffer(1024);
    }
    return ctx.alloc().heapBuffer(1024);
  }
}
