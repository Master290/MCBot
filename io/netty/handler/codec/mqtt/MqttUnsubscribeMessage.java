package io.netty.handler.codec.mqtt;






















public final class MqttUnsubscribeMessage
  extends MqttMessage
{
  public MqttUnsubscribeMessage(MqttFixedHeader mqttFixedHeader, MqttMessageIdAndPropertiesVariableHeader variableHeader, MqttUnsubscribePayload payload)
  {
    super(mqttFixedHeader, variableHeader, payload);
  }
  


  public MqttUnsubscribeMessage(MqttFixedHeader mqttFixedHeader, MqttMessageIdVariableHeader variableHeader, MqttUnsubscribePayload payload)
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
  
  public MqttUnsubscribePayload payload()
  {
    return (MqttUnsubscribePayload)super.payload();
  }
}
