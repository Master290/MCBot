package io.netty.handler.codec.marshalling;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.jboss.marshalling.Marshaller;




























@ChannelHandler.Sharable
public class MarshallingEncoder
  extends MessageToByteEncoder<Object>
{
  private static final byte[] LENGTH_PLACEHOLDER = new byte[4];
  

  private final MarshallerProvider provider;
  


  public MarshallingEncoder(MarshallerProvider provider)
  {
    this.provider = provider;
  }
  
  protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception
  {
    Marshaller marshaller = provider.getMarshaller(ctx);
    int lengthPos = out.writerIndex();
    out.writeBytes(LENGTH_PLACEHOLDER);
    ChannelBufferByteOutput output = new ChannelBufferByteOutput(out);
    marshaller.start(output);
    marshaller.writeObject(msg);
    marshaller.finish();
    marshaller.close();
    
    out.setInt(lengthPos, out.writerIndex() - lengthPos - 4);
  }
}
