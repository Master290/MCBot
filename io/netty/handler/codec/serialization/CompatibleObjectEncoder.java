package io.netty.handler.codec.serialization;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.internal.ObjectUtil;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;


























public class CompatibleObjectEncoder
  extends MessageToByteEncoder<Serializable>
{
  private final int resetInterval;
  private int writtenObjects;
  
  public CompatibleObjectEncoder()
  {
    this(16);
  }
  








  public CompatibleObjectEncoder(int resetInterval)
  {
    this.resetInterval = ObjectUtil.checkPositiveOrZero(resetInterval, "resetInterval");
  }
  



  protected ObjectOutputStream newObjectOutputStream(OutputStream out)
    throws Exception
  {
    return new ObjectOutputStream(out);
  }
  
  protected void encode(ChannelHandlerContext ctx, Serializable msg, ByteBuf out)
    throws Exception
  {
    ObjectOutputStream oos = newObjectOutputStream(new ByteBufOutputStream(out));
    try
    {
      if (resetInterval != 0)
      {
        writtenObjects += 1;
        if (writtenObjects % resetInterval == 0) {
          oos.reset();
        }
      }
      
      oos.writeObject(msg);
      oos.flush();
    } finally {
      oos.close();
    }
  }
}
