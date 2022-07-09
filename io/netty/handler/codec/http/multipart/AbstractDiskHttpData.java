package io.netty.handler.codec.http.multipart;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.util.internal.EmptyArrays;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;




















public abstract class AbstractDiskHttpData
  extends AbstractHttpData
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractDiskHttpData.class);
  private File file;
  private boolean isRenamed;
  private FileChannel fileChannel;
  
  protected AbstractDiskHttpData(String name, Charset charset, long size)
  {
    super(name, charset, size);
  }
  



  protected abstract String getDiskFilename();
  



  protected abstract String getPrefix();
  



  protected abstract String getBaseDirectory();
  



  protected abstract String getPostfix();
  



  protected abstract boolean deleteOnExit();
  



  private File tempFile()
    throws IOException
  {
    String diskFilename = getDiskFilename();
    String newpostfix; String newpostfix; if (diskFilename != null) {
      newpostfix = '_' + diskFilename;
    } else
      newpostfix = getPostfix();
    File tmpFile;
    File tmpFile;
    if (getBaseDirectory() == null)
    {
      tmpFile = PlatformDependent.createTempFile(getPrefix(), newpostfix, null);
    } else {
      tmpFile = PlatformDependent.createTempFile(getPrefix(), newpostfix, new File(
        getBaseDirectory()));
    }
    if (deleteOnExit())
    {
      DeleteFileOnExitHook.add(tmpFile.getPath());
    }
    return tmpFile;
  }
  
  public void setContent(ByteBuf buffer) throws IOException
  {
    ObjectUtil.checkNotNull(buffer, "buffer");
    try {
      size = buffer.readableBytes();
      checkSize(size);
      if ((definedSize > 0L) && (definedSize < size)) {
        throw new IOException("Out of size: " + size + " > " + definedSize);
      }
      if (file == null) {
        file = tempFile();
      }
      if (buffer.readableBytes() == 0)
      {
        if (!file.createNewFile()) {
          if (file.length() == 0L) {
            return;
          }
          if ((!file.delete()) || (!file.createNewFile())) {
            throw new IOException("file exists already: " + file);
          }
        }
        
        return;
      }
      RandomAccessFile accessFile = new RandomAccessFile(file, "rw");
      try {
        accessFile.setLength(0L);
        FileChannel localfileChannel = accessFile.getChannel();
        ByteBuffer byteBuffer = buffer.nioBuffer();
        int written = 0;
        while (written < size) {
          written += localfileChannel.write(byteBuffer);
        }
        buffer.readerIndex(buffer.readerIndex() + written);
        localfileChannel.force(false);
      } finally {
        accessFile.close();
      }
      setCompleted();
    }
    finally
    {
      buffer.release();
    }
  }
  
  public void addContent(ByteBuf buffer, boolean last)
    throws IOException
  {
    if (buffer != null) {
      try {
        int localsize = buffer.readableBytes();
        checkSize(size + localsize);
        if ((definedSize > 0L) && (definedSize < size + localsize)) {
          throw new IOException("Out of size: " + (size + localsize) + " > " + definedSize);
        }
        
        if (file == null) {
          file = tempFile();
        }
        if (fileChannel == null) {
          RandomAccessFile accessFile = new RandomAccessFile(file, "rw");
          fileChannel = accessFile.getChannel();
        }
        int remaining = localsize;
        long position = fileChannel.position();
        int index = buffer.readerIndex();
        while (remaining > 0) {
          int written = buffer.getBytes(index, fileChannel, position, remaining);
          if (written < 0) {
            break;
          }
          remaining -= written;
          position += written;
          index += written;
        }
        fileChannel.position(position);
        buffer.readerIndex(index);
        size += localsize - remaining;
      }
      finally
      {
        buffer.release();
      }
    }
    if (last) {
      if (file == null) {
        file = tempFile();
      }
      if (fileChannel == null) {
        RandomAccessFile accessFile = new RandomAccessFile(file, "rw");
        fileChannel = accessFile.getChannel();
      }
      try {
        fileChannel.force(false);
      } finally {
        fileChannel.close();
      }
      fileChannel = null;
      setCompleted();
    } else {
      ObjectUtil.checkNotNull(buffer, "buffer");
    }
  }
  
  public void setContent(File file) throws IOException
  {
    long size = file.length();
    checkSize(size);
    this.size = size;
    if (this.file != null) {
      delete();
    }
    this.file = file;
    isRenamed = true;
    setCompleted();
  }
  
  public void setContent(InputStream inputStream) throws IOException
  {
    ObjectUtil.checkNotNull(inputStream, "inputStream");
    if (file != null) {
      delete();
    }
    file = tempFile();
    RandomAccessFile accessFile = new RandomAccessFile(file, "rw");
    int written = 0;
    try {
      accessFile.setLength(0L);
      FileChannel localfileChannel = accessFile.getChannel();
      byte[] bytes = new byte['䀀'];
      ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
      int read = inputStream.read(bytes);
      while (read > 0) {
        byteBuffer.position(read).flip();
        written += localfileChannel.write(byteBuffer);
        checkSize(written);
        read = inputStream.read(bytes);
      }
      localfileChannel.force(false);
    } finally {
      accessFile.close();
    }
    size = written;
    if ((definedSize > 0L) && (definedSize < size)) {
      if (!file.delete()) {
        logger.warn("Failed to delete: {}", file);
      }
      file = null;
      throw new IOException("Out of size: " + size + " > " + definedSize);
    }
    isRenamed = true;
    setCompleted();
  }
  
  public void delete()
  {
    if (fileChannel != null) {
      try {
        fileChannel.force(false);
        

        try
        {
          fileChannel.close();
        } catch (IOException e) {
          logger.warn("Failed to close a file.", e);
        }
        
        fileChannel = null;
      }
      catch (IOException e)
      {
        logger.warn("Failed to force.", e);
      } finally {
        try {
          fileChannel.close();
        } catch (IOException e) {
          logger.warn("Failed to close a file.", e);
        }
      }
    }
    
    if (!isRenamed) {
      String filePath = null;
      
      if ((file != null) && (file.exists())) {
        filePath = file.getPath();
        if (!file.delete()) {
          filePath = null;
          logger.warn("Failed to delete: {}", file);
        }
      }
      

      if ((deleteOnExit()) && (filePath != null)) {
        DeleteFileOnExitHook.remove(filePath);
      }
      file = null;
    }
  }
  
  public byte[] get() throws IOException
  {
    if (file == null) {
      return EmptyArrays.EMPTY_BYTES;
    }
    return readFrom(file);
  }
  
  public ByteBuf getByteBuf() throws IOException
  {
    if (file == null) {
      return Unpooled.EMPTY_BUFFER;
    }
    byte[] array = readFrom(file);
    return Unpooled.wrappedBuffer(array);
  }
  
  public ByteBuf getChunk(int length) throws IOException
  {
    if ((file == null) || (length == 0)) {
      return Unpooled.EMPTY_BUFFER;
    }
    if (fileChannel == null) {
      RandomAccessFile accessFile = new RandomAccessFile(file, "r");
      fileChannel = accessFile.getChannel();
    }
    int read = 0;
    ByteBuffer byteBuffer = ByteBuffer.allocate(length);
    try {
      while (read < length) {
        int readnow = fileChannel.read(byteBuffer);
        if (readnow == -1) {
          fileChannel.close();
          fileChannel = null;
          break;
        }
        read += readnow;
      }
    } catch (IOException e) {
      fileChannel.close();
      fileChannel = null;
      throw e;
    }
    if (read == 0) {
      return Unpooled.EMPTY_BUFFER;
    }
    byteBuffer.flip();
    ByteBuf buffer = Unpooled.wrappedBuffer(byteBuffer);
    buffer.readerIndex(0);
    buffer.writerIndex(read);
    return buffer;
  }
  
  public String getString() throws IOException
  {
    return getString(HttpConstants.DEFAULT_CHARSET);
  }
  
  public String getString(Charset encoding) throws IOException
  {
    if (file == null) {
      return "";
    }
    if (encoding == null) {
      byte[] array = readFrom(file);
      return new String(array, HttpConstants.DEFAULT_CHARSET.name());
    }
    byte[] array = readFrom(file);
    return new String(array, encoding.name());
  }
  
  public boolean isInMemory()
  {
    return false;
  }
  
  public boolean renameTo(File dest) throws IOException
  {
    ObjectUtil.checkNotNull(dest, "dest");
    if (file == null) {
      throw new IOException("No file defined so cannot be renamed");
    }
    if (!file.renameTo(dest))
    {
      IOException exception = null;
      RandomAccessFile inputAccessFile = null;
      RandomAccessFile outputAccessFile = null;
      long chunkSize = 8196L;
      long position = 0L;
      try {
        inputAccessFile = new RandomAccessFile(file, "r");
        outputAccessFile = new RandomAccessFile(dest, "rw");
        FileChannel in = inputAccessFile.getChannel();
        FileChannel out = outputAccessFile.getChannel();
        while (position < size) {
          if (chunkSize < size - position) {
            chunkSize = size - position;
          }
          position += in.transferTo(position, chunkSize, out);
        }
        


        if (inputAccessFile != null) {
          try {
            inputAccessFile.close();
          } catch (IOException e) {
            if (exception == null) {
              exception = e;
            } else {
              logger.warn("Multiple exceptions detected, the following will be suppressed {}", e);
            }
          }
        }
        if (outputAccessFile != null) {
          try {
            outputAccessFile.close();
          } catch (IOException e) {
            if (exception == null) {
              exception = e;
            } else {
              logger.warn("Multiple exceptions detected, the following will be suppressed {}", e);
            }
          }
        }
        
        if (exception == null) {
          break label378;
        }
      }
      catch (IOException e)
      {
        exception = e;
      } finally {
        if (inputAccessFile != null) {
          try {
            inputAccessFile.close();
          } catch (IOException e) {
            if (exception == null) {
              exception = e;
            } else {
              logger.warn("Multiple exceptions detected, the following will be suppressed {}", e);
            }
          }
        }
        if (outputAccessFile != null) {
          try {
            outputAccessFile.close();
          } catch (IOException e) {
            if (exception == null) {
              exception = e;
            } else {
              logger.warn("Multiple exceptions detected, the following will be suppressed {}", e);
            }
          }
        }
      }
      
      throw exception;
      label378:
      if (position == size) {
        if (!file.delete()) {
          logger.warn("Failed to delete: {}", file);
        }
        file = dest;
        isRenamed = true;
        return true;
      }
      if (!dest.delete()) {
        logger.warn("Failed to delete: {}", dest);
      }
      return false;
    }
    
    file = dest;
    isRenamed = true;
    return true;
  }
  



  private static byte[] readFrom(File src)
    throws IOException
  {
    long srcsize = src.length();
    if (srcsize > 2147483647L) {
      throw new IllegalArgumentException("File too big to be loaded in memory");
    }
    
    RandomAccessFile accessFile = new RandomAccessFile(src, "r");
    byte[] array = new byte[(int)srcsize];
    try {
      FileChannel fileChannel = accessFile.getChannel();
      ByteBuffer byteBuffer = ByteBuffer.wrap(array);
      int read = 0;
      while (read < srcsize) {
        read += fileChannel.read(byteBuffer);
      }
    } finally {
      accessFile.close();
    }
    return array;
  }
  
  public File getFile() throws IOException
  {
    return file;
  }
  
  public HttpData touch()
  {
    return this;
  }
  
  public HttpData touch(Object hint)
  {
    return this;
  }
}
