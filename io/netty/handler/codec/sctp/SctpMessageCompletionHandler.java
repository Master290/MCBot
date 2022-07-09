package io.netty.handler.codec.sctp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.sctp.SctpMessage;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import java.util.List;






















public class SctpMessageCompletionHandler
  extends MessageToMessageDecoder<SctpMessage>
{
  private final IntObjectMap<ByteBuf> fragments = new IntObjectHashMap();
  
  public SctpMessageCompletionHandler() {}
  
  protected void decode(ChannelHandlerContext ctx, SctpMessage msg, List<Object> out) throws Exception { ByteBuf byteBuf = msg.content();
    int protocolIdentifier = msg.protocolIdentifier();
    int streamIdentifier = msg.streamIdentifier();
    boolean isComplete = msg.isComplete();
    boolean isUnordered = msg.isUnordered();
    
    ByteBuf frag = (ByteBuf)fragments.remove(streamIdentifier);
    if (frag == null) {
      frag = Unpooled.EMPTY_BUFFER;
    }
    
    if ((isComplete) && (!frag.isReadable()))
    {
      out.add(msg);
    } else if ((!isComplete) && (frag.isReadable()))
    {
      fragments.put(streamIdentifier, Unpooled.wrappedBuffer(new ByteBuf[] { frag, byteBuf }));
    } else if ((isComplete) && (frag.isReadable()))
    {




      SctpMessage assembledMsg = new SctpMessage(protocolIdentifier, streamIdentifier, isUnordered, Unpooled.wrappedBuffer(new ByteBuf[] { frag, byteBuf }));
      out.add(assembledMsg);
    }
    else {
      fragments.put(streamIdentifier, byteBuf);
    }
    byteBuf.retain();
  }
  
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception
  {
    for (ByteBuf buffer : fragments.values()) {
      buffer.release();
    }
    fragments.clear();
    super.handlerRemoved(ctx);
  }
}
