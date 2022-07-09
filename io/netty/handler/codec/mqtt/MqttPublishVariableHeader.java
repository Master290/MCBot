package io.netty.handler.codec.mqtt;

import io.netty.util.internal.StringUtil;



















public final class MqttPublishVariableHeader
{
  private final String topicName;
  private final int packetId;
  private final MqttProperties properties;
  
  public MqttPublishVariableHeader(String topicName, int packetId)
  {
    this(topicName, packetId, MqttProperties.NO_PROPERTIES);
  }
  
  public MqttPublishVariableHeader(String topicName, int packetId, MqttProperties properties) {
    this.topicName = topicName;
    this.packetId = packetId;
    this.properties = MqttProperties.withEmptyDefaults(properties);
  }
  
  public String topicName() {
    return topicName;
  }
  


  @Deprecated
  public int messageId()
  {
    return packetId;
  }
  
  public int packetId() {
    return packetId;
  }
  
  public MqttProperties properties() {
    return properties;
  }
  
  public String toString()
  {
    return 
    


      StringUtil.simpleClassName(this) + '[' + "topicName=" + topicName + ", packetId=" + packetId + ']';
  }
}
