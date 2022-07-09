package io.netty.handler.codec.http.multipart;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.util.internal.ObjectUtil;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;






















public abstract class AbstractMemoryHttpData
  extends AbstractHttpData
{
  private ByteBuf byteBuf;
  private int chunkPosition;
  
  protected AbstractMemoryHttpData(String name, Charset charset, long size)
  {
    super(name, charset, size);
    byteBuf = Unpooled.EMPTY_BUFFER;
  }
  
  public void setContent(ByteBuf buffer) throws IOException
  {
    ObjectUtil.checkNotNull(buffer, "buffer");
    long localsize = buffer.readableBytes();
    checkSize(localsize);
    if ((definedSize > 0L) && (definedSize < localsize)) {
      throw new IOException("Out of size: " + localsize + " > " + definedSize);
    }
    
    if (byteBuf != null) {
      byteBuf.release();
    }
    byteBuf = buffer;
    size = localsize;
    setCompleted();
  }
  
  public void setContent(InputStream inputStream) throws IOException
  {
    ObjectUtil.checkNotNull(inputStream, "inputStream");
    
    byte[] bytes = new byte['䀀'];
    ByteBuf buffer = Unpooled.buffer();
    int written = 0;
    try {
      int read = inputStream.read(bytes);
      while (read > 0) {
        buffer.writeBytes(bytes, 0, read);
        written += read;
        checkSize(written);
        read = inputStream.read(bytes);
      }
    } catch (IOException e) {
      buffer.release();
      throw e;
    }
    size = written;
    if ((definedSize > 0L) && (definedSize < size)) {
      buffer.release();
      throw new IOException("Out of size: " + size + " > " + definedSize);
    }
    if (byteBuf != null) {
      byteBuf.release();
    }
    byteBuf = buffer;
    setCompleted();
  }
  
  public void addContent(ByteBuf buffer, boolean last)
    throws IOException
  {
    if (buffer != null) {
      long localsize = buffer.readableBytes();
      checkSize(size + localsize);
      if ((definedSize > 0L) && (definedSize < size + localsize)) {
        throw new IOException("Out of size: " + (size + localsize) + " > " + definedSize);
      }
      
      size += localsize;
      if (byteBuf == null) {
        byteBuf = buffer;
      } else if (localsize == 0L)
      {
        buffer.release();
      } else if (byteBuf.readableBytes() == 0)
      {
        byteBuf.release();
        byteBuf = buffer;
      } else if ((byteBuf instanceof CompositeByteBuf)) {
        CompositeByteBuf cbb = (CompositeByteBuf)byteBuf;
        cbb.addComponent(true, buffer);
      } else {
        CompositeByteBuf cbb = Unpooled.compositeBuffer(Integer.MAX_VALUE);
        cbb.addComponents(true, new ByteBuf[] { byteBuf, buffer });
        byteBuf = cbb;
      }
    }
    if (last) {
      setCompleted();
    } else {
      ObjectUtil.checkNotNull(buffer, "buffer");
    }
  }
  
  public void setContent(File file) throws IOException
  {
    ObjectUtil.checkNotNull(file, "file");
    
    long newsize = file.length();
    if (newsize > 2147483647L) {
      throw new IllegalArgumentException("File too big to be loaded in memory");
    }
    checkSize(newsize);
    RandomAccessFile accessFile = new RandomAccessFile(file, "r");
    try
    {
      FileChannel fileChannel = accessFile.getChannel();
      try {
        byte[] array = new byte[(int)newsize];
        ByteBuffer byteBuffer = ByteBuffer.wrap(array);
        int read = 0;
        while (read < newsize) {
          read += fileChannel.read(byteBuffer);
        }
      }
      finally {}
    } finally {
      ByteBuffer byteBuffer;
      accessFile.close(); }
    ByteBuffer byteBuffer;
    byteBuffer.flip();
    if (byteBuf != null) {
      byteBuf.release();
    }
    byteBuf = Unpooled.wrappedBuffer(Integer.MAX_VALUE, new ByteBuffer[] { byteBuffer });
    size = newsize;
    setCompleted();
  }
  
  public void delete()
  {
    if (byteBuf != null) {
      byteBuf.release();
      byteBuf = null;
    }
  }
  
  public byte[] get()
  {
    if (byteBuf == null) {
      return Unpooled.EMPTY_BUFFER.array();
    }
    byte[] array = new byte[byteBuf.readableBytes()];
    byteBuf.getBytes(byteBuf.readerIndex(), array);
    return array;
  }
  
  public String getString()
  {
    return getString(HttpConstants.DEFAULT_CHARSET);
  }
  
  public String getString(Charset encoding)
  {
    if (byteBuf == null) {
      return "";
    }
    if (encoding == null) {
      encoding = HttpConstants.DEFAULT_CHARSET;
    }
    return byteBuf.toString(encoding);
  }
  





  public ByteBuf getByteBuf()
  {
    return byteBuf;
  }
  
  public ByteBuf getChunk(int length) throws IOException
  {
    if ((byteBuf == null) || (length == 0) || (byteBuf.readableBytes() == 0)) {
      chunkPosition = 0;
      return Unpooled.EMPTY_BUFFER;
    }
    int sizeLeft = byteBuf.readableBytes() - chunkPosition;
    if (sizeLeft == 0) {
      chunkPosition = 0;
      return Unpooled.EMPTY_BUFFER;
    }
    int sliceLength = length;
    if (sizeLeft < length) {
      sliceLength = sizeLeft;
    }
    ByteBuf chunk = byteBuf.retainedSlice(chunkPosition, sliceLength);
    chunkPosition += sliceLength;
    return chunk;
  }
  
  public boolean isInMemory()
  {
    return true;
  }
  
  public boolean renameTo(File dest) throws IOException
  {
    ObjectUtil.checkNotNull(dest, "dest");
    if (byteBuf == null)
    {
      if (!dest.createNewFile()) {
        throw new IOException("file exists already: " + dest);
      }
      return true;
    }
    int length = byteBuf.readableBytes();
    long written = 0L;
    RandomAccessFile accessFile = new RandomAccessFile(dest, "rw");
    try {
      FileChannel fileChannel = accessFile.getChannel();
      try {
        if (byteBuf.nioBufferCount() == 1) {
          ByteBuffer byteBuffer = byteBuf.nioBuffer();
          while (written < length) {
            written += fileChannel.write(byteBuffer);
          }
        } else {
          ByteBuffer[] byteBuffers = byteBuf.nioBuffers();
          while (written < length) {
            written += fileChannel.write(byteBuffers);
          }
        }
        fileChannel.force(false);
      }
      finally {}
    }
    finally {
      accessFile.close();
    }
    return written == length;
  }
  
  public File getFile() throws IOException
  {
    throw new IOException("Not represented by a file");
  }
  
  public HttpData touch()
  {
    return touch(null);
  }
  
  public HttpData touch(Object hint)
  {
    if (byteBuf != null) {
      byteBuf.touch(hint);
    }
    return this;
  }
}
