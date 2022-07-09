package io.netty.handler.codec.http.multipart;

import io.netty.buffer.ByteBuf;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;























public class MixedFileUpload
  implements FileUpload
{
  private final String baseDir;
  private final boolean deleteOnExit;
  private FileUpload fileUpload;
  private final long limitSize;
  private final long definedSize;
  private long maxSize = -1L;
  

  public MixedFileUpload(String name, String filename, String contentType, String contentTransferEncoding, Charset charset, long size, long limitSize)
  {
    this(name, filename, contentType, contentTransferEncoding, charset, size, limitSize, DiskFileUpload.baseDirectory, DiskFileUpload.deleteOnExitTemporaryFile);
  }
  


  public MixedFileUpload(String name, String filename, String contentType, String contentTransferEncoding, Charset charset, long size, long limitSize, String baseDir, boolean deleteOnExit)
  {
    this.limitSize = limitSize;
    if (size > this.limitSize) {
      fileUpload = new DiskFileUpload(name, filename, contentType, contentTransferEncoding, charset, size);
    }
    else {
      fileUpload = new MemoryFileUpload(name, filename, contentType, contentTransferEncoding, charset, size);
    }
    
    definedSize = size;
    this.baseDir = baseDir;
    this.deleteOnExit = deleteOnExit;
  }
  
  public long getMaxSize()
  {
    return maxSize;
  }
  
  public void setMaxSize(long maxSize)
  {
    this.maxSize = maxSize;
    fileUpload.setMaxSize(maxSize);
  }
  
  public void checkSize(long newSize) throws IOException
  {
    if ((maxSize >= 0L) && (newSize > maxSize)) {
      throw new IOException("Size exceed allowed maximum capacity");
    }
  }
  
  public void addContent(ByteBuf buffer, boolean last)
    throws IOException
  {
    if ((fileUpload instanceof MemoryFileUpload)) {
      checkSize(fileUpload.length() + buffer.readableBytes());
      if (fileUpload.length() + buffer.readableBytes() > limitSize)
      {


        DiskFileUpload diskFileUpload = new DiskFileUpload(fileUpload.getName(), fileUpload.getFilename(), fileUpload.getContentType(), fileUpload.getContentTransferEncoding(), fileUpload.getCharset(), definedSize, baseDir, deleteOnExit);
        
        diskFileUpload.setMaxSize(maxSize);
        ByteBuf data = fileUpload.getByteBuf();
        if ((data != null) && (data.isReadable())) {
          diskFileUpload.addContent(data.retain(), false);
        }
        
        fileUpload.release();
        
        fileUpload = diskFileUpload;
      }
    }
    fileUpload.addContent(buffer, last);
  }
  
  public void delete()
  {
    fileUpload.delete();
  }
  
  public byte[] get() throws IOException
  {
    return fileUpload.get();
  }
  
  public ByteBuf getByteBuf() throws IOException
  {
    return fileUpload.getByteBuf();
  }
  
  public Charset getCharset()
  {
    return fileUpload.getCharset();
  }
  
  public String getContentType()
  {
    return fileUpload.getContentType();
  }
  
  public String getContentTransferEncoding()
  {
    return fileUpload.getContentTransferEncoding();
  }
  
  public String getFilename()
  {
    return fileUpload.getFilename();
  }
  
  public String getString() throws IOException
  {
    return fileUpload.getString();
  }
  
  public String getString(Charset encoding) throws IOException
  {
    return fileUpload.getString(encoding);
  }
  
  public boolean isCompleted()
  {
    return fileUpload.isCompleted();
  }
  
  public boolean isInMemory()
  {
    return fileUpload.isInMemory();
  }
  
  public long length()
  {
    return fileUpload.length();
  }
  
  public long definedLength()
  {
    return fileUpload.definedLength();
  }
  
  public boolean renameTo(File dest) throws IOException
  {
    return fileUpload.renameTo(dest);
  }
  
  public void setCharset(Charset charset)
  {
    fileUpload.setCharset(charset);
  }
  
  public void setContent(ByteBuf buffer) throws IOException
  {
    checkSize(buffer.readableBytes());
    if ((buffer.readableBytes() > limitSize) && 
      ((fileUpload instanceof MemoryFileUpload))) {
      FileUpload memoryUpload = fileUpload;
      



      fileUpload = new DiskFileUpload(memoryUpload.getName(), memoryUpload.getFilename(), memoryUpload.getContentType(), memoryUpload.getContentTransferEncoding(), memoryUpload.getCharset(), definedSize, baseDir, deleteOnExit);
      
      fileUpload.setMaxSize(maxSize);
      

      memoryUpload.release();
    }
    
    fileUpload.setContent(buffer);
  }
  
  public void setContent(File file) throws IOException
  {
    checkSize(file.length());
    if ((file.length() > limitSize) && 
      ((fileUpload instanceof MemoryFileUpload))) {
      FileUpload memoryUpload = fileUpload;
      




      fileUpload = new DiskFileUpload(memoryUpload.getName(), memoryUpload.getFilename(), memoryUpload.getContentType(), memoryUpload.getContentTransferEncoding(), memoryUpload.getCharset(), definedSize, baseDir, deleteOnExit);
      
      fileUpload.setMaxSize(maxSize);
      

      memoryUpload.release();
    }
    
    fileUpload.setContent(file);
  }
  
  public void setContent(InputStream inputStream) throws IOException
  {
    if ((fileUpload instanceof MemoryFileUpload)) {
      FileUpload memoryUpload = fileUpload;
      




      fileUpload = new DiskFileUpload(fileUpload.getName(), fileUpload.getFilename(), fileUpload.getContentType(), fileUpload.getContentTransferEncoding(), fileUpload.getCharset(), definedSize, baseDir, deleteOnExit);
      
      fileUpload.setMaxSize(maxSize);
      

      memoryUpload.release();
    }
    fileUpload.setContent(inputStream);
  }
  
  public void setContentType(String contentType)
  {
    fileUpload.setContentType(contentType);
  }
  
  public void setContentTransferEncoding(String contentTransferEncoding)
  {
    fileUpload.setContentTransferEncoding(contentTransferEncoding);
  }
  
  public void setFilename(String filename)
  {
    fileUpload.setFilename(filename);
  }
  
  public InterfaceHttpData.HttpDataType getHttpDataType()
  {
    return fileUpload.getHttpDataType();
  }
  
  public String getName()
  {
    return fileUpload.getName();
  }
  
  public int hashCode()
  {
    return fileUpload.hashCode();
  }
  
  public boolean equals(Object obj)
  {
    return fileUpload.equals(obj);
  }
  
  public int compareTo(InterfaceHttpData o)
  {
    return fileUpload.compareTo(o);
  }
  
  public String toString()
  {
    return "Mixed: " + fileUpload;
  }
  
  public ByteBuf getChunk(int length) throws IOException
  {
    return fileUpload.getChunk(length);
  }
  
  public File getFile() throws IOException
  {
    return fileUpload.getFile();
  }
  
  public FileUpload copy()
  {
    return fileUpload.copy();
  }
  
  public FileUpload duplicate()
  {
    return fileUpload.duplicate();
  }
  
  public FileUpload retainedDuplicate()
  {
    return fileUpload.retainedDuplicate();
  }
  
  public FileUpload replace(ByteBuf content)
  {
    return fileUpload.replace(content);
  }
  
  public ByteBuf content()
  {
    return fileUpload.content();
  }
  
  public int refCnt()
  {
    return fileUpload.refCnt();
  }
  
  public FileUpload retain()
  {
    fileUpload.retain();
    return this;
  }
  
  public FileUpload retain(int increment)
  {
    fileUpload.retain(increment);
    return this;
  }
  
  public FileUpload touch()
  {
    fileUpload.touch();
    return this;
  }
  
  public FileUpload touch(Object hint)
  {
    fileUpload.touch(hint);
    return this;
  }
  
  public boolean release()
  {
    return fileUpload.release();
  }
  
  public boolean release(int decrement)
  {
    return fileUpload.release(decrement);
  }
}
