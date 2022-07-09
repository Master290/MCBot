package io.netty.handler.codec.http.multipart;

import io.netty.buffer.ByteBuf;
import java.io.IOException;

public abstract interface Attribute
  extends HttpData
{
  public abstract String getValue()
    throws IOException;
  
  public abstract void setValue(String paramString)
    throws IOException;
  
  public abstract Attribute copy();
  
  public abstract Attribute duplicate();
  
  public abstract Attribute retainedDuplicate();
  
  public abstract Attribute replace(ByteBuf paramByteBuf);
  
  public abstract Attribute retain();
  
  public abstract Attribute retain(int paramInt);
  
  public abstract Attribute touch();
  
  public abstract Attribute touch(Object paramObject);
}
