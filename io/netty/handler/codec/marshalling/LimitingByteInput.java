package io.netty.handler.codec.marshalling;

import io.netty.util.internal.ObjectUtil;
import java.io.IOException;
import org.jboss.marshalling.ByteInput;






















class LimitingByteInput
  implements ByteInput
{
  private static final TooBigObjectException EXCEPTION = new TooBigObjectException();
  private final ByteInput input;
  private final long limit;
  private long read;
  
  LimitingByteInput(ByteInput input, long limit)
  {
    this.input = input;
    this.limit = ObjectUtil.checkPositive(limit, "limit");
  }
  
  public void close()
    throws IOException
  {}
  
  public int available()
    throws IOException
  {
    return readable(input.available());
  }
  
  public int read() throws IOException
  {
    int readable = readable(1);
    if (readable > 0) {
      int b = input.read();
      read += 1L;
      return b;
    }
    throw EXCEPTION;
  }
  
  public int read(byte[] array)
    throws IOException
  {
    return read(array, 0, array.length);
  }
  
  public int read(byte[] array, int offset, int length) throws IOException
  {
    int readable = readable(length);
    if (readable > 0) {
      int i = input.read(array, offset, readable);
      read += i;
      return i;
    }
    throw EXCEPTION;
  }
  
  public long skip(long bytes)
    throws IOException
  {
    int readable = readable((int)bytes);
    if (readable > 0) {
      long i = input.skip(readable);
      read += i;
      return i;
    }
    throw EXCEPTION;
  }
  
  private int readable(int length)
  {
    return (int)Math.min(length, limit - read);
  }
  
  static final class TooBigObjectException
    extends IOException
  {
    private static final long serialVersionUID = 1L;
    
    TooBigObjectException() {}
  }
}
