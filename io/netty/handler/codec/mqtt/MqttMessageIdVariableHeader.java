package io.netty.handler.codec.mqtt;

import io.netty.util.internal.StringUtil;




















public class MqttMessageIdVariableHeader
{
  private final int messageId;
  
  public static MqttMessageIdVariableHeader from(int messageId)
  {
    if ((messageId < 1) || (messageId > 65535)) {
      throw new IllegalArgumentException("messageId: " + messageId + " (expected: 1 ~ 65535)");
    }
    return new MqttMessageIdVariableHeader(messageId);
  }
  
  protected MqttMessageIdVariableHeader(int messageId) {
    this.messageId = messageId;
  }
  
  public int messageId() {
    return messageId;
  }
  
  public String toString()
  {
    return 
    

      StringUtil.simpleClassName(this) + '[' + "messageId=" + messageId + ']';
  }
  
  public MqttMessageIdAndPropertiesVariableHeader withEmptyProperties()
  {
    return new MqttMessageIdAndPropertiesVariableHeader(messageId, MqttProperties.NO_PROPERTIES);
  }
  
  MqttMessageIdAndPropertiesVariableHeader withDefaultEmptyProperties() {
    return withEmptyProperties();
  }
}
