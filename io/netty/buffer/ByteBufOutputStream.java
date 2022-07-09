package io.netty.buffer;

import io.netty.util.CharsetUtil;
import io.netty.util.internal.ObjectUtil;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;






























public class ByteBufOutputStream
  extends OutputStream
  implements DataOutput
{
  private final ByteBuf buffer;
  private final int startIndex;
  private DataOutputStream utf8out;
  private boolean closed;
  
  public ByteBufOutputStream(ByteBuf buffer)
  {
    this.buffer = ((ByteBuf)ObjectUtil.checkNotNull(buffer, "buffer"));
    startIndex = buffer.writerIndex();
  }
  


  public int writtenBytes()
  {
    return buffer.writerIndex() - startIndex;
  }
  
  public void write(byte[] b, int off, int len) throws IOException
  {
    if (len == 0) {
      return;
    }
    
    buffer.writeBytes(b, off, len);
  }
  
  public void write(byte[] b) throws IOException
  {
    buffer.writeBytes(b);
  }
  
  public void write(int b) throws IOException
  {
    buffer.writeByte(b);
  }
  
  public void writeBoolean(boolean v) throws IOException
  {
    buffer.writeBoolean(v);
  }
  
  public void writeByte(int v) throws IOException
  {
    buffer.writeByte(v);
  }
  
  public void writeBytes(String s) throws IOException
  {
    buffer.writeCharSequence(s, CharsetUtil.US_ASCII);
  }
  
  public void writeChar(int v) throws IOException
  {
    buffer.writeChar(v);
  }
  
  public void writeChars(String s) throws IOException
  {
    int len = s.length();
    for (int i = 0; i < len; i++) {
      buffer.writeChar(s.charAt(i));
    }
  }
  
  public void writeDouble(double v) throws IOException
  {
    buffer.writeDouble(v);
  }
  
  public void writeFloat(float v) throws IOException
  {
    buffer.writeFloat(v);
  }
  
  public void writeInt(int v) throws IOException
  {
    buffer.writeInt(v);
  }
  
  public void writeLong(long v) throws IOException
  {
    buffer.writeLong(v);
  }
  
  public void writeShort(int v) throws IOException
  {
    buffer.writeShort((short)v);
  }
  
  public void writeUTF(String s) throws IOException
  {
    DataOutputStream out = utf8out;
    if (out == null) {
      if (closed) {
        throw new IOException("The stream is closed");
      }
      
      utf8out = (out = new DataOutputStream(this));
    }
    out.writeUTF(s);
  }
  


  public ByteBuf buffer()
  {
    return buffer;
  }
  
  public void close() throws IOException
  {
    if (closed) {
      return;
    }
    closed = true;
    try
    {
      super.close();
      
      if (utf8out != null) {
        utf8out.close();
      }
    }
    finally
    {
      if (utf8out != null) {
        utf8out.close();
      }
    }
  }
}
