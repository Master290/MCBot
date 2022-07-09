package io.netty.handler.codec.spdy;

import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;

























public class DefaultSpdySynStreamFrame
  extends DefaultSpdyHeadersFrame
  implements SpdySynStreamFrame
{
  private int associatedStreamId;
  private byte priority;
  private boolean unidirectional;
  
  public DefaultSpdySynStreamFrame(int streamId, int associatedStreamId, byte priority)
  {
    this(streamId, associatedStreamId, priority, true);
  }
  







  public DefaultSpdySynStreamFrame(int streamId, int associatedStreamId, byte priority, boolean validateHeaders)
  {
    super(streamId, validateHeaders);
    setAssociatedStreamId(associatedStreamId);
    setPriority(priority);
  }
  
  public SpdySynStreamFrame setStreamId(int streamId)
  {
    super.setStreamId(streamId);
    return this;
  }
  
  public SpdySynStreamFrame setLast(boolean last)
  {
    super.setLast(last);
    return this;
  }
  
  public SpdySynStreamFrame setInvalid()
  {
    super.setInvalid();
    return this;
  }
  
  public int associatedStreamId()
  {
    return associatedStreamId;
  }
  
  public SpdySynStreamFrame setAssociatedStreamId(int associatedStreamId)
  {
    ObjectUtil.checkPositiveOrZero(associatedStreamId, "associatedStreamId");
    this.associatedStreamId = associatedStreamId;
    return this;
  }
  
  public byte priority()
  {
    return priority;
  }
  
  public SpdySynStreamFrame setPriority(byte priority)
  {
    if ((priority < 0) || (priority > 7)) {
      throw new IllegalArgumentException("Priority must be between 0 and 7 inclusive: " + priority);
    }
    
    this.priority = priority;
    return this;
  }
  
  public boolean isUnidirectional()
  {
    return unidirectional;
  }
  
  public SpdySynStreamFrame setUnidirectional(boolean unidirectional)
  {
    this.unidirectional = unidirectional;
    return this;
  }
  










  public String toString()
  {
    StringBuilder buf = new StringBuilder().append(StringUtil.simpleClassName(this)).append("(last: ").append(isLast()).append("; unidirectional: ").append(isUnidirectional()).append(')').append(StringUtil.NEWLINE).append("--> Stream-ID = ").append(streamId()).append(StringUtil.NEWLINE);
    if (associatedStreamId != 0)
    {

      buf.append("--> Associated-To-Stream-ID = ").append(associatedStreamId()).append(StringUtil.NEWLINE);
    }
    



    buf.append("--> Priority = ").append(priority()).append(StringUtil.NEWLINE).append("--> Headers:").append(StringUtil.NEWLINE);
    appendHeaders(buf);
    

    buf.setLength(buf.length() - StringUtil.NEWLINE.length());
    return buf.toString();
  }
}
