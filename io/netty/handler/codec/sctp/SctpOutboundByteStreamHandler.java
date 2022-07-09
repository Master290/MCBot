package io.netty.handler.codec.sctp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.sctp.SctpMessage;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.util.List;























public class SctpOutboundByteStreamHandler
  extends MessageToMessageEncoder<ByteBuf>
{
  private final int streamIdentifier;
  private final int protocolIdentifier;
  private final boolean unordered;
  
  public SctpOutboundByteStreamHandler(int streamIdentifier, int protocolIdentifier)
  {
    this(streamIdentifier, protocolIdentifier, false);
  }
  




  public SctpOutboundByteStreamHandler(int streamIdentifier, int protocolIdentifier, boolean unordered)
  {
    this.streamIdentifier = streamIdentifier;
    this.protocolIdentifier = protocolIdentifier;
    this.unordered = unordered;
  }
  
  protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception
  {
    out.add(new SctpMessage(protocolIdentifier, streamIdentifier, unordered, msg.retain()));
  }
}
