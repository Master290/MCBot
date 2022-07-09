package io.netty.handler.codec.dns;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.util.internal.ObjectUtil;


















public final class TcpDnsQueryDecoder
  extends LengthFieldBasedFrameDecoder
{
  private final DnsRecordDecoder decoder;
  
  public TcpDnsQueryDecoder()
  {
    this(DnsRecordDecoder.DEFAULT, 65535);
  }
  


  public TcpDnsQueryDecoder(DnsRecordDecoder decoder, int maxFrameLength)
  {
    super(maxFrameLength, 0, 2, 0, 2);
    this.decoder = ((DnsRecordDecoder)ObjectUtil.checkNotNull(decoder, "decoder"));
  }
  
  protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception
  {
    ByteBuf frame = (ByteBuf)super.decode(ctx, in);
    if (frame == null) {
      return null;
    }
    
    DnsMessageUtil.decodeDnsQuery(decoder, frame.slice(), new DnsMessageUtil.DnsQueryFactory()
    {
      public DnsQuery newQuery(int id, DnsOpCode dnsOpCode) {
        return new DefaultDnsQuery(id, dnsOpCode);
      }
    });
  }
}
