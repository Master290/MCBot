package io.netty.handler.codec.spdy;

import io.netty.util.internal.StringUtil;






















public class DefaultSpdyPingFrame
  implements SpdyPingFrame
{
  private int id;
  
  public DefaultSpdyPingFrame(int id)
  {
    setId(id);
  }
  
  public int id()
  {
    return id;
  }
  
  public SpdyPingFrame setId(int id)
  {
    this.id = id;
    return this;
  }
  
  public String toString()
  {
    return 
    


      StringUtil.simpleClassName(this) + StringUtil.NEWLINE + "--> ID = " + id();
  }
}
