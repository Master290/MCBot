package io.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;

public abstract interface HttpContent
  extends HttpObject, ByteBufHolder
{
  public abstract HttpContent copy();
  
  public abstract HttpContent duplicate();
  
  public abstract HttpContent retainedDuplicate();
  
  public abstract HttpContent replace(ByteBuf paramByteBuf);
  
  public abstract HttpContent retain();
  
  public abstract HttpContent retain(int paramInt);
  
  public abstract HttpContent touch();
  
  public abstract HttpContent touch(Object paramObject);
}
