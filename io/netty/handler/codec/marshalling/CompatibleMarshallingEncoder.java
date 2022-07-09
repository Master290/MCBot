package io.netty.handler.codec.marshalling;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.jboss.marshalling.Marshaller;































@ChannelHandler.Sharable
public class CompatibleMarshallingEncoder
  extends MessageToByteEncoder<Object>
{
  private final MarshallerProvider provider;
  
  public CompatibleMarshallingEncoder(MarshallerProvider provider)
  {
    this.provider = provider;
  }
  
  protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception
  {
    Marshaller marshaller = provider.getMarshaller(ctx);
    marshaller.start(new ChannelBufferByteOutput(out));
    marshaller.writeObject(msg);
    marshaller.finish();
    marshaller.close();
  }
}
