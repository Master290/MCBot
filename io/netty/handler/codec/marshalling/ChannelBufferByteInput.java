package io.netty.handler.codec.marshalling;

import io.netty.buffer.ByteBuf;
import java.io.IOException;
import org.jboss.marshalling.ByteInput;


















class ChannelBufferByteInput
  implements ByteInput
{
  private final ByteBuf buffer;
  
  ChannelBufferByteInput(ByteBuf buffer)
  {
    this.buffer = buffer;
  }
  
  public void close()
    throws IOException
  {}
  
  public int available()
    throws IOException
  {
    return buffer.readableBytes();
  }
  
  public int read() throws IOException
  {
    if (buffer.isReadable()) {
      return buffer.readByte() & 0xFF;
    }
    return -1;
  }
  
  public int read(byte[] array) throws IOException
  {
    return read(array, 0, array.length);
  }
  
  public int read(byte[] dst, int dstIndex, int length) throws IOException
  {
    int available = available();
    if (available == 0) {
      return -1;
    }
    
    length = Math.min(available, length);
    buffer.readBytes(dst, dstIndex, length);
    return length;
  }
  
  public long skip(long bytes) throws IOException
  {
    int readable = buffer.readableBytes();
    if (readable < bytes) {
      bytes = readable;
    }
    buffer.readerIndex((int)(buffer.readerIndex() + bytes));
    return bytes;
  }
}
