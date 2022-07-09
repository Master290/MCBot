package io.netty.handler.codec.dns;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.net.InetSocketAddress;
import java.util.List;






















@ChannelHandler.Sharable
public class DatagramDnsResponseDecoder
  extends MessageToMessageDecoder<DatagramPacket>
{
  private final DnsResponseDecoder<InetSocketAddress> responseDecoder;
  
  public DatagramDnsResponseDecoder()
  {
    this(DnsRecordDecoder.DEFAULT);
  }
  


  public DatagramDnsResponseDecoder(DnsRecordDecoder recordDecoder)
  {
    responseDecoder = new DnsResponseDecoder(recordDecoder)
    {
      protected DnsResponse newResponse(InetSocketAddress sender, InetSocketAddress recipient, int id, DnsOpCode opCode, DnsResponseCode responseCode)
      {
        return new DatagramDnsResponse(sender, recipient, id, opCode, responseCode);
      }
    };
  }
  
  protected void decode(ChannelHandlerContext ctx, DatagramPacket packet, List<Object> out) throws Exception
  {
    try {
      out.add(decodeResponse(ctx, packet));
    } catch (IndexOutOfBoundsException e) {
      throw new CorruptedFrameException("Unable to decode response", e);
    }
  }
  
  protected DnsResponse decodeResponse(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
    return responseDecoder.decode(packet.sender(), packet.recipient(), (ByteBuf)packet.content());
  }
}
