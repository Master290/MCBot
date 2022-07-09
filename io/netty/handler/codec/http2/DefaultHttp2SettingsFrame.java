package io.netty.handler.codec.http2;

import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;




















public class DefaultHttp2SettingsFrame
  implements Http2SettingsFrame
{
  private final Http2Settings settings;
  
  public DefaultHttp2SettingsFrame(Http2Settings settings)
  {
    this.settings = ((Http2Settings)ObjectUtil.checkNotNull(settings, "settings"));
  }
  
  public Http2Settings settings()
  {
    return settings;
  }
  
  public String name()
  {
    return "SETTINGS";
  }
  
  public boolean equals(Object o)
  {
    if (!(o instanceof Http2SettingsFrame)) {
      return false;
    }
    Http2SettingsFrame other = (Http2SettingsFrame)o;
    return settings.equals(other.settings());
  }
  
  public int hashCode()
  {
    return settings.hashCode();
  }
  
  public String toString()
  {
    return StringUtil.simpleClassName(this) + "(settings=" + settings + ')';
  }
}
