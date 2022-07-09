package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;

public abstract interface Http2UnknownFrame
  extends Http2StreamFrame, ByteBufHolder
{
  public abstract Http2FrameStream stream();
  
  public abstract Http2UnknownFrame stream(Http2FrameStream paramHttp2FrameStream);
  
  public abstract byte frameType();
  
  public abstract Http2Flags flags();
  
  public abstract Http2UnknownFrame copy();
  
  public abstract Http2UnknownFrame duplicate();
  
  public abstract Http2UnknownFrame retainedDuplicate();
  
  public abstract Http2UnknownFrame replace(ByteBuf paramByteBuf);
  
  public abstract Http2UnknownFrame retain();
  
  public abstract Http2UnknownFrame retain(int paramInt);
  
  public abstract Http2UnknownFrame touch();
  
  public abstract Http2UnknownFrame touch(Object paramObject);
}
