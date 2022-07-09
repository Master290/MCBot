package io.netty.handler.codec.mqtt;






















public final class MqttSubscribeMessage
  extends MqttMessage
{
  public MqttSubscribeMessage(MqttFixedHeader mqttFixedHeader, MqttMessageIdAndPropertiesVariableHeader variableHeader, MqttSubscribePayload payload)
  {
    super(mqttFixedHeader, variableHeader, payload);
  }
  


  public MqttSubscribeMessage(MqttFixedHeader mqttFixedHeader, MqttMessageIdVariableHeader variableHeader, MqttSubscribePayload payload)
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
  
  public MqttSubscribePayload payload()
  {
    return (MqttSubscribePayload)super.payload();
  }
}
