package io.netty.handler.codec.http2;

import io.netty.channel.ChannelId;

















final class Http2StreamChannelId
  implements ChannelId
{
  private static final long serialVersionUID = -6642338822166867585L;
  private final int id;
  private final ChannelId parentId;
  
  Http2StreamChannelId(ChannelId parentId, int id)
  {
    this.parentId = parentId;
    this.id = id;
  }
  
  public String asShortText()
  {
    return parentId.asShortText() + '/' + id;
  }
  
  public String asLongText()
  {
    return parentId.asLongText() + '/' + id;
  }
  
  public int compareTo(ChannelId o)
  {
    if ((o instanceof Http2StreamChannelId)) {
      Http2StreamChannelId otherId = (Http2StreamChannelId)o;
      int res = parentId.compareTo(parentId);
      if (res == 0) {
        return id - id;
      }
      return res;
    }
    
    return parentId.compareTo(o);
  }
  
  public int hashCode()
  {
    return id * 31 + parentId.hashCode();
  }
  
  public boolean equals(Object obj)
  {
    if (!(obj instanceof Http2StreamChannelId)) {
      return false;
    }
    Http2StreamChannelId otherId = (Http2StreamChannelId)obj;
    return (id == id) && (parentId.equals(parentId));
  }
  
  public String toString()
  {
    return asShortText();
  }
}
