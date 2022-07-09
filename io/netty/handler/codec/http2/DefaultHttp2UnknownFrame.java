package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.DefaultByteBufHolder;
import io.netty.buffer.Unpooled;
import io.netty.util.internal.StringUtil;














public final class DefaultHttp2UnknownFrame
  extends DefaultByteBufHolder
  implements Http2UnknownFrame
{
  private final byte frameType;
  private final Http2Flags flags;
  private Http2FrameStream stream;
  
  public DefaultHttp2UnknownFrame(byte frameType, Http2Flags flags)
  {
    this(frameType, flags, Unpooled.EMPTY_BUFFER);
  }
  
  public DefaultHttp2UnknownFrame(byte frameType, Http2Flags flags, ByteBuf data) {
    super(data);
    this.frameType = frameType;
    this.flags = flags;
  }
  
  public Http2FrameStream stream()
  {
    return stream;
  }
  
  public DefaultHttp2UnknownFrame stream(Http2FrameStream stream)
  {
    this.stream = stream;
    return this;
  }
  
  public byte frameType()
  {
    return frameType;
  }
  
  public Http2Flags flags()
  {
    return flags;
  }
  
  public String name()
  {
    return "UNKNOWN";
  }
  
  public DefaultHttp2UnknownFrame copy()
  {
    return replace(content().copy());
  }
  
  public DefaultHttp2UnknownFrame duplicate()
  {
    return replace(content().duplicate());
  }
  
  public DefaultHttp2UnknownFrame retainedDuplicate()
  {
    return replace(content().retainedDuplicate());
  }
  
  public DefaultHttp2UnknownFrame replace(ByteBuf content)
  {
    return new DefaultHttp2UnknownFrame(frameType, flags, content).stream(stream);
  }
  
  public DefaultHttp2UnknownFrame retain()
  {
    super.retain();
    return this;
  }
  
  public DefaultHttp2UnknownFrame retain(int increment)
  {
    super.retain(increment);
    return this;
  }
  
  public String toString()
  {
    return 
      StringUtil.simpleClassName(this) + "(frameType=" + frameType + ", stream=" + stream + ", flags=" + flags + ", content=" + contentToString() + ')';
  }
  
  public DefaultHttp2UnknownFrame touch()
  {
    super.touch();
    return this;
  }
  
  public DefaultHttp2UnknownFrame touch(Object hint)
  {
    super.touch(hint);
    return this;
  }
  
  public boolean equals(Object o)
  {
    if (!(o instanceof DefaultHttp2UnknownFrame)) {
      return false;
    }
    DefaultHttp2UnknownFrame other = (DefaultHttp2UnknownFrame)o;
    Http2FrameStream otherStream = other.stream();
    return ((stream == otherStream) || ((otherStream != null) && (otherStream.equals(stream)))) && 
      (flags.equals(other.flags())) && 
      (frameType == other.frameType()) && 
      (super.equals(other));
  }
  
  public int hashCode()
  {
    int hash = super.hashCode();
    hash = hash * 31 + frameType;
    hash = hash * 31 + flags.hashCode();
    if (stream != null) {
      hash = hash * 31 + stream.hashCode();
    }
    
    return hash;
  }
}
