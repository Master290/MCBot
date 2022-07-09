package io.netty.handler.codec.http.multipart;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelException;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.util.internal.ObjectUtil;
import java.io.IOException;
import java.nio.charset.Charset;






















public class MemoryFileUpload
  extends AbstractMemoryHttpData
  implements FileUpload
{
  private String filename;
  private String contentType;
  private String contentTransferEncoding;
  
  public MemoryFileUpload(String name, String filename, String contentType, String contentTransferEncoding, Charset charset, long size)
  {
    super(name, charset, size);
    setFilename(filename);
    setContentType(contentType);
    setContentTransferEncoding(contentTransferEncoding);
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
    return 
    





      HttpHeaderNames.CONTENT_DISPOSITION + ": " + HttpHeaderValues.FORM_DATA + "; " + HttpHeaderValues.NAME + "=\"" + getName() + "\"; " + HttpHeaderValues.FILENAME + "=\"" + filename + "\"\r\n" + HttpHeaderNames.CONTENT_TYPE + ": " + contentType + (getCharset() != null ? "; " + HttpHeaderValues.CHARSET + '=' + getCharset().name() + "\r\n" : "\r\n") + HttpHeaderNames.CONTENT_LENGTH + ": " + length() + "\r\nCompleted: " + isCompleted() + "\r\nIsInMemory: " + isInMemory();
  }
  
  public FileUpload copy()
  {
    ByteBuf content = content();
    return replace(content != null ? content.copy() : content);
  }
  
  public FileUpload duplicate()
  {
    ByteBuf content = content();
    return replace(content != null ? content.duplicate() : content);
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
    MemoryFileUpload upload = new MemoryFileUpload(getName(), getFilename(), getContentType(), getContentTransferEncoding(), getCharset(), size);
    if (content != null) {
      try {
        upload.setContent(content);
        return upload;
      } catch (IOException e) {
        throw new ChannelException(e);
      }
    }
    return upload;
  }
  
  public FileUpload retain()
  {
    super.retain();
    return this;
  }
  
  public FileUpload retain(int increment)
  {
    super.retain(increment);
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
