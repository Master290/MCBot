package io.netty.handler.codec.marshalling;

import io.netty.buffer.ByteBuf;
import java.io.IOException;
import org.jboss.marshalling.ByteOutput;























class ChannelBufferByteOutput
  implements ByteOutput
{
  private final ByteBuf buffer;
  
  ChannelBufferByteOutput(ByteBuf buffer)
  {
    this.buffer = buffer;
  }
  

  public void close()
    throws IOException
  {}
  
  public void flush()
    throws IOException
  {}
  
  public void write(int b)
    throws IOException
  {
    buffer.writeByte(b);
  }
  
  public void write(byte[] bytes) throws IOException
  {
    buffer.writeBytes(bytes);
  }
  
  public void write(byte[] bytes, int srcIndex, int length) throws IOException
  {
    buffer.writeBytes(bytes, srcIndex, length);
  }
  



  ByteBuf getBuffer()
  {
    return buffer;
  }
}
