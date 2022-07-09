package io.netty.handler.codec.mqtt;

import io.netty.util.internal.StringUtil;





















public final class MqttConnAckVariableHeader
{
  private final MqttConnectReturnCode connectReturnCode;
  private final boolean sessionPresent;
  private final MqttProperties properties;
  
  public MqttConnAckVariableHeader(MqttConnectReturnCode connectReturnCode, boolean sessionPresent)
  {
    this(connectReturnCode, sessionPresent, MqttProperties.NO_PROPERTIES);
  }
  
  public MqttConnAckVariableHeader(MqttConnectReturnCode connectReturnCode, boolean sessionPresent, MqttProperties properties)
  {
    this.connectReturnCode = connectReturnCode;
    this.sessionPresent = sessionPresent;
    this.properties = MqttProperties.withEmptyDefaults(properties);
  }
  
  public MqttConnectReturnCode connectReturnCode() {
    return connectReturnCode;
  }
  
  public boolean isSessionPresent() {
    return sessionPresent;
  }
  
  public MqttProperties properties() {
    return properties;
  }
  
  public String toString()
  {
    return 
    


      StringUtil.simpleClassName(this) + '[' + "connectReturnCode=" + connectReturnCode + ", sessionPresent=" + sessionPresent + ']';
  }
}
