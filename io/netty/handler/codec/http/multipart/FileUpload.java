package io.netty.handler.codec.http.multipart;

import io.netty.buffer.ByteBuf;

public abstract interface FileUpload
  extends HttpData
{
  public abstract String getFilename();
  
  public abstract void setFilename(String paramString);
  
  public abstract void setContentType(String paramString);
  
  public abstract String getContentType();
  
  public abstract void setContentTransferEncoding(String paramString);
  
  public abstract String getContentTransferEncoding();
  
  public abstract FileUpload copy();
  
  public abstract FileUpload duplicate();
  
  public abstract FileUpload retainedDuplicate();
  
  public abstract FileUpload replace(ByteBuf paramByteBuf);
  
  public abstract FileUpload retain();
  
  public abstract FileUpload retain(int paramInt);
  
  public abstract FileUpload touch();
  
  public abstract FileUpload touch(Object paramObject);
}
