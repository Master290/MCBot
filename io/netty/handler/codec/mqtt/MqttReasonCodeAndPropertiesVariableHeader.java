package io.netty.handler.codec.mqtt;

import io.netty.util.internal.StringUtil;





















public final class MqttReasonCodeAndPropertiesVariableHeader
{
  private final byte reasonCode;
  private final MqttProperties properties;
  public static final byte REASON_CODE_OK = 0;
  
  public MqttReasonCodeAndPropertiesVariableHeader(byte reasonCode, MqttProperties properties)
  {
    this.reasonCode = reasonCode;
    this.properties = MqttProperties.withEmptyDefaults(properties);
  }
  
  public byte reasonCode() {
    return reasonCode;
  }
  
  public MqttProperties properties() {
    return properties;
  }
  
  public String toString()
  {
    return 
    


      StringUtil.simpleClassName(this) + '[' + "reasonCode=" + reasonCode + ", properties=" + properties + ']';
  }
}
