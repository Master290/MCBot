package io.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;

public abstract interface FullHttpMessage
  extends HttpMessage, LastHttpContent
{
  public abstract FullHttpMessage copy();
  
  public abstract FullHttpMessage duplicate();
  
  public abstract FullHttpMessage retainedDuplicate();
  
  public abstract FullHttpMessage replace(ByteBuf paramByteBuf);
  
  public abstract FullHttpMessage retain(int paramInt);
  
  public abstract FullHttpMessage retain();
  
  public abstract FullHttpMessage touch();
  
  public abstract FullHttpMessage touch(Object paramObject);
}
