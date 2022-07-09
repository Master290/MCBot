package io.netty.handler.codec.mqtt;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderResult;



















public final class MqttMessageFactory
{
  public static MqttMessage newMessage(MqttFixedHeader mqttFixedHeader, Object variableHeader, Object payload)
  {
    switch (1.$SwitchMap$io$netty$handler$codec$mqtt$MqttMessageType[mqttFixedHeader.messageType().ordinal()]) {
    case 1: 
      return new MqttConnectMessage(mqttFixedHeader, (MqttConnectVariableHeader)variableHeader, (MqttConnectPayload)payload);
    



    case 2: 
      return new MqttConnAckMessage(mqttFixedHeader, (MqttConnAckVariableHeader)variableHeader);
    
    case 3: 
      return new MqttSubscribeMessage(mqttFixedHeader, (MqttMessageIdVariableHeader)variableHeader, (MqttSubscribePayload)payload);
    



    case 4: 
      return new MqttSubAckMessage(mqttFixedHeader, (MqttMessageIdVariableHeader)variableHeader, (MqttSubAckPayload)payload);
    



    case 5: 
      return new MqttUnsubAckMessage(mqttFixedHeader, (MqttMessageIdVariableHeader)variableHeader, (MqttUnsubAckPayload)payload);
    



    case 6: 
      return new MqttUnsubscribeMessage(mqttFixedHeader, (MqttMessageIdVariableHeader)variableHeader, (MqttUnsubscribePayload)payload);
    



    case 7: 
      return new MqttPublishMessage(mqttFixedHeader, (MqttPublishVariableHeader)variableHeader, (ByteBuf)payload);
    




    case 8: 
      return new MqttPubAckMessage(mqttFixedHeader, (MqttMessageIdVariableHeader)variableHeader);
    
    case 9: 
    case 10: 
    case 11: 
      return new MqttMessage(mqttFixedHeader, variableHeader);
    
    case 12: 
    case 13: 
      return new MqttMessage(mqttFixedHeader);
    

    case 14: 
    case 15: 
      return new MqttMessage(mqttFixedHeader, (MqttReasonCodeAndPropertiesVariableHeader)variableHeader);
    }
    
    
    throw new IllegalArgumentException("unknown message type: " + mqttFixedHeader.messageType());
  }
  
  public static MqttMessage newInvalidMessage(Throwable cause)
  {
    return new MqttMessage(null, null, null, DecoderResult.failure(cause));
  }
  
  public static MqttMessage newInvalidMessage(MqttFixedHeader mqttFixedHeader, Object variableHeader, Throwable cause)
  {
    return new MqttMessage(mqttFixedHeader, variableHeader, null, DecoderResult.failure(cause));
  }
  
  private MqttMessageFactory() {}
}
