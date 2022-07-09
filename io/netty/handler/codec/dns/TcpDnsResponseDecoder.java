package io.netty.handler.codec.dns;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import java.net.SocketAddress;



















public final class TcpDnsResponseDecoder
  extends LengthFieldBasedFrameDecoder
{
  private final DnsResponseDecoder<SocketAddress> responseDecoder;
  
  public TcpDnsResponseDecoder()
  {
    this(DnsRecordDecoder.DEFAULT, 65536);
  }
  




  public TcpDnsResponseDecoder(DnsRecordDecoder recordDecoder, int maxFrameLength)
  {
    super(maxFrameLength, 0, 2, 0, 2);
    
    responseDecoder = new DnsResponseDecoder(recordDecoder)
    {
      protected DnsResponse newResponse(SocketAddress sender, SocketAddress recipient, int id, DnsOpCode opCode, DnsResponseCode responseCode)
      {
        return new DefaultDnsResponse(id, opCode, responseCode);
      }
    };
  }
  
  protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception
  {
    ByteBuf frame = (ByteBuf)super.decode(ctx, in);
    if (frame == null) {
      return null;
    }
    try
    {
      return responseDecoder.decode(ctx.channel().remoteAddress(), ctx.channel().localAddress(), frame.slice());
    } finally {
      frame.release();
    }
  }
  
  protected ByteBuf extractFrame(ChannelHandlerContext ctx, ByteBuf buffer, int index, int length)
  {
    return buffer.copy(index, length);
  }
}
