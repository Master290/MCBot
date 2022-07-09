package io.netty.handler.codec.mqtt;





















public final class MqttSubAckMessage
  extends MqttMessage
{
  public MqttSubAckMessage(MqttFixedHeader mqttFixedHeader, MqttMessageIdAndPropertiesVariableHeader variableHeader, MqttSubAckPayload payload)
  {
    super(mqttFixedHeader, variableHeader, payload);
  }
  


  public MqttSubAckMessage(MqttFixedHeader mqttFixedHeader, MqttMessageIdVariableHeader variableHeader, MqttSubAckPayload payload)
  {
    this(mqttFixedHeader, variableHeader.withDefaultEmptyProperties(), payload);
  }
  
  public MqttMessageIdVariableHeader variableHeader()
  {
    return (MqttMessageIdVariableHeader)super.variableHeader();
  }
  
  public MqttMessageIdAndPropertiesVariableHeader idAndPropertiesVariableHeader() {
    return (MqttMessageIdAndPropertiesVariableHeader)super.variableHeader();
  }
  
  public MqttSubAckPayload payload()
  {
    return (MqttSubAckPayload)super.payload();
  }
}
