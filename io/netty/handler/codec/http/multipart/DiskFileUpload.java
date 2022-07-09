package io.netty.handler.codec.http.multipart;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelException;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.util.internal.ObjectUtil;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;


















public class DiskFileUpload
  extends AbstractDiskHttpData
  implements FileUpload
{
  public static String baseDirectory;
  public static boolean deleteOnExitTemporaryFile = true;
  
  public static final String prefix = "FUp_";
  
  public static final String postfix = ".tmp";
  
  private final String baseDir;
  
  private final boolean deleteOnExit;
  
  private String filename;
  
  private String contentType;
  
  private String contentTransferEncoding;
  
  public DiskFileUpload(String name, String filename, String contentType, String contentTransferEncoding, Charset charset, long size, String baseDir, boolean deleteOnExit)
  {
    super(name, charset, size);
    setFilename(filename);
    setContentType(contentType);
    setContentTransferEncoding(contentTransferEncoding);
    this.baseDir = (baseDir == null ? baseDirectory : baseDir);
    this.deleteOnExit = deleteOnExit;
  }
  
  public DiskFileUpload(String name, String filename, String contentType, String contentTransferEncoding, Charset charset, long size)
  {
    this(name, filename, contentType, contentTransferEncoding, charset, size, baseDirectory, deleteOnExitTemporaryFile);
  }
  

  public InterfaceHttpData.HttpDataType getHttpDataType()
  {
    return InterfaceHttpData.HttpDataType.FileUpload;
  }
  
  public String getFilename()
  {
    return filename;
  }
  
  public void setFilename(String filename)
  {
    this.filename = ((String)ObjectUtil.checkNotNull(filename, "filename"));
  }
  
  public int hashCode()
  {
    return FileUploadUtil.hashCode(this);
  }
  
  public boolean equals(Object o)
  {
    return ((o instanceof FileUpload)) && (FileUploadUtil.equals(this, (FileUpload)o));
  }
  
  public int compareTo(InterfaceHttpData o)
  {
    if (!(o instanceof FileUpload))
    {
      throw new ClassCastException("Cannot compare " + getHttpDataType() + " with " + o.getHttpDataType());
    }
    return compareTo((FileUpload)o);
  }
  
  public int compareTo(FileUpload o) {
    return FileUploadUtil.compareTo(this, o);
  }
  
  public void setContentType(String contentType)
  {
    this.contentType = ((String)ObjectUtil.checkNotNull(contentType, "contentType"));
  }
  
  public String getContentType()
  {
    return contentType;
  }
  
  public String getContentTransferEncoding()
  {
    return contentTransferEncoding;
  }
  
  public void setContentTransferEncoding(String contentTransferEncoding)
  {
    this.contentTransferEncoding = contentTransferEncoding;
  }
  
  public String toString()
  {
    File file = null;
    try {
      file = getFile();
    }
    catch (IOException localIOException) {}
    

    return 
    






      HttpHeaderNames.CONTENT_DISPOSITION + ": " + HttpHeaderValues.FORM_DATA + "; " + HttpHeaderValues.NAME + "=\"" + getName() + "\"; " + HttpHeaderValues.FILENAME + "=\"" + filename + "\"\r\n" + HttpHeaderNames.CONTENT_TYPE + ": " + contentType + (getCharset() != null ? "; " + HttpHeaderValues.CHARSET + '=' + getCharset().name() + "\r\n" : "\r\n") + HttpHeaderNames.CONTENT_LENGTH + ": " + length() + "\r\nCompleted: " + isCompleted() + "\r\nIsInMemory: " + isInMemory() + "\r\nRealFile: " + (file != null ? file.getAbsolutePath() : "null") + " DefaultDeleteAfter: " + deleteOnExitTemporaryFile;
  }
  

  protected boolean deleteOnExit()
  {
    return deleteOnExit;
  }
  
  protected String getBaseDirectory()
  {
    return baseDir;
  }
  
  protected String getDiskFilename()
  {
    return "upload";
  }
  
  protected String getPostfix()
  {
    return ".tmp";
  }
  
  protected String getPrefix()
  {
    return "FUp_";
  }
  
  public FileUpload copy()
  {
    ByteBuf content = content();
    return replace(content != null ? content.copy() : null);
  }
  
  public FileUpload duplicate()
  {
    ByteBuf content = content();
    return replace(content != null ? content.duplicate() : null);
  }
  
  public FileUpload retainedDuplicate()
  {
    ByteBuf content = content();
    if (content != null) {
      content = content.retainedDuplicate();
      boolean success = false;
      try {
        FileUpload duplicate = replace(content);
        success = true;
        return duplicate;
      } finally {
        if (!success) {
          content.release();
        }
      }
    }
    return replace(null);
  }
  


  public FileUpload replace(ByteBuf content)
  {
    DiskFileUpload upload = new DiskFileUpload(getName(), getFilename(), getContentType(), getContentTransferEncoding(), getCharset(), size, baseDir, deleteOnExit);
    
    if (content != null) {
      try {
        upload.setContent(content);
      } catch (IOException e) {
        throw new ChannelException(e);
      }
    }
    return upload;
  }
  
  public FileUpload retain(int increment)
  {
    super.retain(increment);
    return this;
  }
  
  public FileUpload retain()
  {
    super.retain();
    return this;
  }
  
  public FileUpload touch()
  {
    super.touch();
    return this;
  }
  
  public FileUpload touch(Object hint)
  {
    super.touch(hint);
    return this;
  }
}
