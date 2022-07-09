package io.netty.handler.codec.mqtt;





















public final class MqttUnsubAckMessage
  extends MqttMessage
{
  public MqttUnsubAckMessage(MqttFixedHeader mqttFixedHeader, MqttMessageIdAndPropertiesVariableHeader variableHeader, MqttUnsubAckPayload payload)
  {
    super(mqttFixedHeader, variableHeader, MqttUnsubAckPayload.withEmptyDefaults(payload));
  }
  

  public MqttUnsubAckMessage(MqttFixedHeader mqttFixedHeader, MqttMessageIdVariableHeader variableHeader, MqttUnsubAckPayload payload)
  {
    this(mqttFixedHeader, fallbackVariableHeader(variableHeader), payload);
  }
  
  public MqttUnsubAckMessage(MqttFixedHeader mqttFixedHeader, MqttMessageIdVariableHeader variableHeader) {
    this(mqttFixedHeader, variableHeader, null);
  }
  
  private static MqttMessageIdAndPropertiesVariableHeader fallbackVariableHeader(MqttMessageIdVariableHeader variableHeader)
  {
    if ((variableHeader instanceof MqttMessageIdAndPropertiesVariableHeader)) {
      return (MqttMessageIdAndPropertiesVariableHeader)variableHeader;
    }
    return new MqttMessageIdAndPropertiesVariableHeader(variableHeader.messageId(), MqttProperties.NO_PROPERTIES);
  }
  
  public MqttMessageIdVariableHeader variableHeader()
  {
    return (MqttMessageIdVariableHeader)super.variableHeader();
  }
  
  public MqttMessageIdAndPropertiesVariableHeader idAndPropertiesVariableHeader() {
    return (MqttMessageIdAndPropertiesVariableHeader)super.variableHeader();
  }
  
  public MqttUnsubAckPayload payload()
  {
    return (MqttUnsubAckPayload)super.payload();
  }
}
