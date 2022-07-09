package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;

public abstract interface Http2GoAwayFrame
  extends Http2Frame, ByteBufHolder
{
  public abstract long errorCode();
  
  public abstract int extraStreamIds();
  
  public abstract Http2GoAwayFrame setExtraStreamIds(int paramInt);
  
  public abstract int lastStreamId();
  
  public abstract ByteBuf content();
  
  public abstract Http2GoAwayFrame copy();
  
  public abstract Http2GoAwayFrame duplicate();
  
  public abstract Http2GoAwayFrame retainedDuplicate();
  
  public abstract Http2GoAwayFrame replace(ByteBuf paramByteBuf);
  
  public abstract Http2GoAwayFrame retain();
  
  public abstract Http2GoAwayFrame retain(int paramInt);
  
  public abstract Http2GoAwayFrame touch();
  
  public abstract Http2GoAwayFrame touch(Object paramObject);
}
