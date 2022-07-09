package io.netty.handler.codec.spdy;

import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;

























public class DefaultSpdyWindowUpdateFrame
  implements SpdyWindowUpdateFrame
{
  private int streamId;
  private int deltaWindowSize;
  
  public DefaultSpdyWindowUpdateFrame(int streamId, int deltaWindowSize)
  {
    setStreamId(streamId);
    setDeltaWindowSize(deltaWindowSize);
  }
  
  public int streamId()
  {
    return streamId;
  }
  
  public SpdyWindowUpdateFrame setStreamId(int streamId)
  {
    ObjectUtil.checkPositiveOrZero(streamId, "streamId");
    this.streamId = streamId;
    return this;
  }
  
  public int deltaWindowSize()
  {
    return deltaWindowSize;
  }
  
  public SpdyWindowUpdateFrame setDeltaWindowSize(int deltaWindowSize)
  {
    ObjectUtil.checkPositive(deltaWindowSize, "deltaWindowSize");
    this.deltaWindowSize = deltaWindowSize;
    return this;
  }
  
  public String toString()
  {
    return 
    





      StringUtil.simpleClassName(this) + StringUtil.NEWLINE + "--> Stream-ID = " + streamId() + StringUtil.NEWLINE + "--> Delta-Window-Size = " + deltaWindowSize();
  }
}
