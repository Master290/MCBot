package io.netty.handler.codec.spdy;

import io.netty.util.internal.StringUtil;
import java.util.Map.Entry;























public class DefaultSpdyHeadersFrame
  extends DefaultSpdyStreamFrame
  implements SpdyHeadersFrame
{
  private boolean invalid;
  private boolean truncated;
  private final SpdyHeaders headers;
  
  public DefaultSpdyHeadersFrame(int streamId)
  {
    this(streamId, true);
  }
  





  public DefaultSpdyHeadersFrame(int streamId, boolean validate)
  {
    super(streamId);
    headers = new DefaultSpdyHeaders(validate);
  }
  
  public SpdyHeadersFrame setStreamId(int streamId)
  {
    super.setStreamId(streamId);
    return this;
  }
  
  public SpdyHeadersFrame setLast(boolean last)
  {
    super.setLast(last);
    return this;
  }
  
  public boolean isInvalid()
  {
    return invalid;
  }
  
  public SpdyHeadersFrame setInvalid()
  {
    invalid = true;
    return this;
  }
  
  public boolean isTruncated()
  {
    return truncated;
  }
  
  public SpdyHeadersFrame setTruncated()
  {
    truncated = true;
    return this;
  }
  
  public SpdyHeaders headers()
  {
    return headers;
  }
  










  public String toString()
  {
    StringBuilder buf = new StringBuilder().append(StringUtil.simpleClassName(this)).append("(last: ").append(isLast()).append(')').append(StringUtil.NEWLINE).append("--> Stream-ID = ").append(streamId()).append(StringUtil.NEWLINE).append("--> Headers:").append(StringUtil.NEWLINE);
    appendHeaders(buf);
    

    buf.setLength(buf.length() - StringUtil.NEWLINE.length());
    return buf.toString();
  }
  
  protected void appendHeaders(StringBuilder buf) {
    for (Map.Entry<CharSequence, CharSequence> e : headers()) {
      buf.append("    ");
      buf.append((CharSequence)e.getKey());
      buf.append(": ");
      buf.append((CharSequence)e.getValue());
      buf.append(StringUtil.NEWLINE);
    }
  }
}
