package io.netty.handler.codec.mqtt;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;


















final class MqttCodecUtil
{
  private static final char[] TOPIC_WILDCARDS = { '#', '+' };
  
  static final AttributeKey<MqttVersion> MQTT_VERSION_KEY = AttributeKey.valueOf("NETTY_CODEC_MQTT_VERSION");
  
  static MqttVersion getMqttVersion(ChannelHandlerContext ctx) {
    Attribute<MqttVersion> attr = ctx.channel().attr(MQTT_VERSION_KEY);
    MqttVersion version = (MqttVersion)attr.get();
    if (version == null) {
      return MqttVersion.MQTT_3_1_1;
    }
    return version;
  }
  
  static void setMqttVersion(ChannelHandlerContext ctx, MqttVersion version) {
    Attribute<MqttVersion> attr = ctx.channel().attr(MQTT_VERSION_KEY);
    attr.set(version);
  }
  
  static boolean isValidPublishTopicName(String topicName)
  {
    for (char c : TOPIC_WILDCARDS) {
      if (topicName.indexOf(c) >= 0) {
        return false;
      }
    }
    return true;
  }
  
  static boolean isValidMessageId(int messageId) {
    return messageId != 0;
  }
  
  static boolean isValidClientId(MqttVersion mqttVersion, int maxClientIdLength, String clientId) {
    if (mqttVersion == MqttVersion.MQTT_3_1) {
      return (clientId != null) && (clientId.length() >= 1) && 
        (clientId.length() <= maxClientIdLength);
    }
    if ((mqttVersion == MqttVersion.MQTT_3_1_1) || (mqttVersion == MqttVersion.MQTT_5))
    {

      return clientId != null;
    }
    throw new IllegalArgumentException(mqttVersion + " is unknown mqtt version");
  }
  
  static MqttFixedHeader validateFixedHeader(ChannelHandlerContext ctx, MqttFixedHeader mqttFixedHeader) {
    switch (1.$SwitchMap$io$netty$handler$codec$mqtt$MqttMessageType[mqttFixedHeader.messageType().ordinal()]) {
    case 1: 
    case 2: 
    case 3: 
      if (mqttFixedHeader.qosLevel() != MqttQoS.AT_LEAST_ONCE) {
        throw new DecoderException(mqttFixedHeader.messageType().name() + " message must have QoS 1");
      }
      return mqttFixedHeader;
    case 4: 
      if (getMqttVersion(ctx) != MqttVersion.MQTT_5) {
        throw new DecoderException("AUTH message requires at least MQTT 5");
      }
      return mqttFixedHeader;
    }
    return mqttFixedHeader;
  }
  
  static MqttFixedHeader resetUnusedFields(MqttFixedHeader mqttFixedHeader)
  {
    switch (1.$SwitchMap$io$netty$handler$codec$mqtt$MqttMessageType[mqttFixedHeader.messageType().ordinal()]) {
    case 5: 
    case 6: 
    case 7: 
    case 8: 
    case 9: 
    case 10: 
    case 11: 
    case 12: 
    case 13: 
    case 14: 
      if ((mqttFixedHeader.isDup()) || 
        (mqttFixedHeader.qosLevel() != MqttQoS.AT_MOST_ONCE) || 
        (mqttFixedHeader.isRetain())) {
        return new MqttFixedHeader(mqttFixedHeader
          .messageType(), false, MqttQoS.AT_MOST_ONCE, false, mqttFixedHeader
          


          .remainingLength());
      }
      return mqttFixedHeader;
    case 1: 
    case 2: 
    case 3: 
      if (mqttFixedHeader.isRetain()) {
        return new MqttFixedHeader(mqttFixedHeader
          .messageType(), mqttFixedHeader
          .isDup(), mqttFixedHeader
          .qosLevel(), false, mqttFixedHeader
          
          .remainingLength());
      }
      return mqttFixedHeader;
    }
    return mqttFixedHeader;
  }
  
  private MqttCodecUtil() {}
}
