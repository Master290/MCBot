package io.netty.handler.codec.serialization;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import java.io.ObjectOutputStream;
import java.io.Serializable;
























@ChannelHandler.Sharable
public class ObjectEncoder
  extends MessageToByteEncoder<Serializable>
{
  private static final byte[] LENGTH_PLACEHOLDER = new byte[4];
  
  public ObjectEncoder() {}
  
  protected void encode(ChannelHandlerContext ctx, Serializable msg, ByteBuf out) throws Exception { int startIdx = out.writerIndex();
    
    ByteBufOutputStream bout = new ByteBufOutputStream(out);
    ObjectOutputStream oout = null;
    try {
      bout.write(LENGTH_PLACEHOLDER);
      oout = new CompactObjectOutputStream(bout);
      oout.writeObject(msg);
      oout.flush();
    } finally {
      if (oout != null) {
        oout.close();
      } else {
        bout.close();
      }
    }
    
    int endIdx = out.writerIndex();
    
    out.setInt(startIdx, endIdx - startIdx - 4);
  }
}
