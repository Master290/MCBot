package io.netty.handler.codec.http.websocketx.extensions;

import io.netty.util.internal.ObjectUtil;
import java.util.Collections;
import java.util.Map;





















public final class WebSocketExtensionData
{
  private final String name;
  private final Map<String, String> parameters;
  
  public WebSocketExtensionData(String name, Map<String, String> parameters)
  {
    this.name = ((String)ObjectUtil.checkNotNull(name, "name"));
    this.parameters = Collections.unmodifiableMap(
      (Map)ObjectUtil.checkNotNull(parameters, "parameters"));
  }
  


  public String name()
  {
    return name;
  }
  


  public Map<String, String> parameters()
  {
    return parameters;
  }
}
