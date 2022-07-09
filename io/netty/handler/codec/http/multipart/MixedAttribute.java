package io.netty.handler.codec.http.multipart;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpConstants;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;



















public class MixedAttribute
  implements Attribute
{
  private String baseDir;
  private boolean deleteOnExit;
  private Attribute attribute;
  private final long limitSize;
  private long maxSize = -1L;
  
  public MixedAttribute(String name, long limitSize) {
    this(name, limitSize, HttpConstants.DEFAULT_CHARSET);
  }
  
  public MixedAttribute(String name, long definedSize, long limitSize) {
    this(name, definedSize, limitSize, HttpConstants.DEFAULT_CHARSET);
  }
  
  public MixedAttribute(String name, long limitSize, Charset charset) {
    this(name, limitSize, charset, DiskAttribute.baseDirectory, DiskAttribute.deleteOnExitTemporaryFile);
  }
  
  public MixedAttribute(String name, long limitSize, Charset charset, String baseDir, boolean deleteOnExit) {
    this.limitSize = limitSize;
    attribute = new MemoryAttribute(name, charset);
    this.baseDir = baseDir;
    this.deleteOnExit = deleteOnExit;
  }
  
  public MixedAttribute(String name, long definedSize, long limitSize, Charset charset) {
    this(name, definedSize, limitSize, charset, DiskAttribute.baseDirectory, DiskAttribute.deleteOnExitTemporaryFile);
  }
  

  public MixedAttribute(String name, long definedSize, long limitSize, Charset charset, String baseDir, boolean deleteOnExit)
  {
    this.limitSize = limitSize;
    attribute = new MemoryAttribute(name, definedSize, charset);
    this.baseDir = baseDir;
    this.deleteOnExit = deleteOnExit;
  }
  
  public MixedAttribute(String name, String value, long limitSize) {
    this(name, value, limitSize, HttpConstants.DEFAULT_CHARSET, DiskAttribute.baseDirectory, DiskFileUpload.deleteOnExitTemporaryFile);
  }
  
  public MixedAttribute(String name, String value, long limitSize, Charset charset)
  {
    this(name, value, limitSize, charset, DiskAttribute.baseDirectory, DiskFileUpload.deleteOnExitTemporaryFile);
  }
  

  public MixedAttribute(String name, String value, long limitSize, Charset charset, String baseDir, boolean deleteOnExit)
  {
    this.limitSize = limitSize;
    if (value.length() > this.limitSize) {
      try {
        attribute = new DiskAttribute(name, value, charset, baseDir, deleteOnExit);
      }
      catch (IOException e) {
        try {
          attribute = new MemoryAttribute(name, value, charset);
        } catch (IOException ignore) {
          throw new IllegalArgumentException(e);
        }
      }
    } else {
      try {
        attribute = new MemoryAttribute(name, value, charset);
      } catch (IOException e) {
        throw new IllegalArgumentException(e);
      }
    }
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
    attribute.setMaxSize(maxSize);
  }
  
  public void checkSize(long newSize) throws IOException
  {
    if ((maxSize >= 0L) && (newSize > maxSize)) {
      throw new IOException("Size exceed allowed maximum capacity");
    }
  }
  
  public void addContent(ByteBuf buffer, boolean last) throws IOException
  {
    if ((attribute instanceof MemoryAttribute)) {
      checkSize(attribute.length() + buffer.readableBytes());
      if (attribute.length() + buffer.readableBytes() > limitSize)
      {
        DiskAttribute diskAttribute = new DiskAttribute(attribute.getName(), attribute.definedLength(), baseDir, deleteOnExit);
        diskAttribute.setMaxSize(maxSize);
        if (((MemoryAttribute)attribute).getByteBuf() != null) {
          diskAttribute.addContent(((MemoryAttribute)attribute)
            .getByteBuf(), false);
        }
        attribute = diskAttribute;
      }
    }
    attribute.addContent(buffer, last);
  }
  
  public void delete()
  {
    attribute.delete();
  }
  
  public byte[] get() throws IOException
  {
    return attribute.get();
  }
  
  public ByteBuf getByteBuf() throws IOException
  {
    return attribute.getByteBuf();
  }
  
  public Charset getCharset()
  {
    return attribute.getCharset();
  }
  
  public String getString() throws IOException
  {
    return attribute.getString();
  }
  
  public String getString(Charset encoding) throws IOException
  {
    return attribute.getString(encoding);
  }
  
  public boolean isCompleted()
  {
    return attribute.isCompleted();
  }
  
  public boolean isInMemory()
  {
    return attribute.isInMemory();
  }
  
  public long length()
  {
    return attribute.length();
  }
  
  public long definedLength()
  {
    return attribute.definedLength();
  }
  
  public boolean renameTo(File dest) throws IOException
  {
    return attribute.renameTo(dest);
  }
  
  public void setCharset(Charset charset)
  {
    attribute.setCharset(charset);
  }
  
  public void setContent(ByteBuf buffer) throws IOException
  {
    checkSize(buffer.readableBytes());
    if ((buffer.readableBytes() > limitSize) && 
      ((attribute instanceof MemoryAttribute)))
    {
      attribute = new DiskAttribute(attribute.getName(), attribute.definedLength(), baseDir, deleteOnExit);
      attribute.setMaxSize(maxSize);
    }
    
    attribute.setContent(buffer);
  }
  
  public void setContent(File file) throws IOException
  {
    checkSize(file.length());
    if ((file.length() > limitSize) && 
      ((attribute instanceof MemoryAttribute)))
    {
      attribute = new DiskAttribute(attribute.getName(), attribute.definedLength(), baseDir, deleteOnExit);
      attribute.setMaxSize(maxSize);
    }
    
    attribute.setContent(file);
  }
  
  public void setContent(InputStream inputStream) throws IOException
  {
    if ((attribute instanceof MemoryAttribute))
    {
      attribute = new DiskAttribute(attribute.getName(), attribute.definedLength(), baseDir, deleteOnExit);
      attribute.setMaxSize(maxSize);
    }
    attribute.setContent(inputStream);
  }
  
  public InterfaceHttpData.HttpDataType getHttpDataType()
  {
    return attribute.getHttpDataType();
  }
  
  public String getName()
  {
    return attribute.getName();
  }
  
  public int hashCode()
  {
    return attribute.hashCode();
  }
  
  public boolean equals(Object obj)
  {
    return attribute.equals(obj);
  }
  
  public int compareTo(InterfaceHttpData o)
  {
    return attribute.compareTo(o);
  }
  
  public String toString()
  {
    return "Mixed: " + attribute;
  }
  
  public String getValue() throws IOException
  {
    return attribute.getValue();
  }
  
  public void setValue(String value) throws IOException
  {
    attribute.setValue(value);
  }
  
  public ByteBuf getChunk(int length) throws IOException
  {
    return attribute.getChunk(length);
  }
  
  public File getFile() throws IOException
  {
    return attribute.getFile();
  }
  
  public Attribute copy()
  {
    return attribute.copy();
  }
  
  public Attribute duplicate()
  {
    return attribute.duplicate();
  }
  
  public Attribute retainedDuplicate()
  {
    return attribute.retainedDuplicate();
  }
  
  public Attribute replace(ByteBuf content)
  {
    return attribute.replace(content);
  }
  
  public ByteBuf content()
  {
    return attribute.content();
  }
  
  public int refCnt()
  {
    return attribute.refCnt();
  }
  
  public Attribute retain()
  {
    attribute.retain();
    return this;
  }
  
  public Attribute retain(int increment)
  {
    attribute.retain(increment);
    return this;
  }
  
  public Attribute touch()
  {
    attribute.touch();
    return this;
  }
  
  public Attribute touch(Object hint)
  {
    attribute.touch(hint);
    return this;
  }
  
  public boolean release()
  {
    return attribute.release();
  }
  
  public boolean release(int decrement)
  {
    return attribute.release(decrement);
  }
}
