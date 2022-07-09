package io.netty.handler.codec.serialization;

import io.netty.util.internal.ObjectUtil;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.StreamCorruptedException;



























public class ObjectDecoderInputStream
  extends InputStream
  implements ObjectInput
{
  private final DataInputStream in;
  private final int maxObjectSize;
  private final ClassResolver classResolver;
  
  public ObjectDecoderInputStream(InputStream in)
  {
    this(in, null);
  }
  









  public ObjectDecoderInputStream(InputStream in, ClassLoader classLoader)
  {
    this(in, classLoader, 1048576);
  }
  










  public ObjectDecoderInputStream(InputStream in, int maxObjectSize)
  {
    this(in, null, maxObjectSize);
  }
  













  public ObjectDecoderInputStream(InputStream in, ClassLoader classLoader, int maxObjectSize)
  {
    ObjectUtil.checkNotNull(in, "in");
    ObjectUtil.checkPositive(maxObjectSize, "maxObjectSize");
    
    if ((in instanceof DataInputStream)) {
      this.in = ((DataInputStream)in);
    } else {
      this.in = new DataInputStream(in);
    }
    classResolver = ClassResolvers.weakCachingResolver(classLoader);
    this.maxObjectSize = maxObjectSize;
  }
  
  public Object readObject() throws ClassNotFoundException, IOException
  {
    int dataLen = readInt();
    if (dataLen <= 0) {
      throw new StreamCorruptedException("invalid data length: " + dataLen);
    }
    if (dataLen > maxObjectSize) {
      throw new StreamCorruptedException("data length too big: " + dataLen + " (max: " + maxObjectSize + ')');
    }
    

    return new CompactObjectInputStream(in, classResolver).readObject();
  }
  
  public int available() throws IOException
  {
    return in.available();
  }
  
  public void close() throws IOException
  {
    in.close();
  }
  

  public void mark(int readlimit)
  {
    in.mark(readlimit);
  }
  
  public boolean markSupported()
  {
    return in.markSupported();
  }
  
  public int read()
    throws IOException
  {
    return in.read();
  }
  
  public final int read(byte[] b, int off, int len) throws IOException
  {
    return in.read(b, off, len);
  }
  
  public final int read(byte[] b) throws IOException
  {
    return in.read(b);
  }
  
  public final boolean readBoolean() throws IOException
  {
    return in.readBoolean();
  }
  
  public final byte readByte() throws IOException
  {
    return in.readByte();
  }
  
  public final char readChar() throws IOException
  {
    return in.readChar();
  }
  
  public final double readDouble() throws IOException
  {
    return in.readDouble();
  }
  
  public final float readFloat() throws IOException
  {
    return in.readFloat();
  }
  
  public final void readFully(byte[] b, int off, int len) throws IOException
  {
    in.readFully(b, off, len);
  }
  
  public final void readFully(byte[] b) throws IOException
  {
    in.readFully(b);
  }
  
  public final int readInt() throws IOException
  {
    return in.readInt();
  }
  


  @Deprecated
  public final String readLine()
    throws IOException
  {
    return in.readLine();
  }
  
  public final long readLong() throws IOException
  {
    return in.readLong();
  }
  
  public final short readShort() throws IOException
  {
    return in.readShort();
  }
  
  public final int readUnsignedByte() throws IOException
  {
    return in.readUnsignedByte();
  }
  
  public final int readUnsignedShort() throws IOException
  {
    return in.readUnsignedShort();
  }
  
  public final String readUTF() throws IOException
  {
    return in.readUTF();
  }
  
  public void reset() throws IOException
  {
    in.reset();
  }
  
  public long skip(long n) throws IOException
  {
    return in.skip(n);
  }
  
  public final int skipBytes(int n) throws IOException
  {
    return in.skipBytes(n);
  }
}
