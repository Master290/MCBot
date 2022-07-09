package io.netty.handler.codec.mqtt;

import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;

























public final class MqttFixedHeader
{
  private final MqttMessageType messageType;
  private final boolean isDup;
  private final MqttQoS qosLevel;
  private final boolean isRetain;
  private final int remainingLength;
  
  public MqttFixedHeader(MqttMessageType messageType, boolean isDup, MqttQoS qosLevel, boolean isRetain, int remainingLength)
  {
    this.messageType = ((MqttMessageType)ObjectUtil.checkNotNull(messageType, "messageType"));
    this.isDup = isDup;
    this.qosLevel = ((MqttQoS)ObjectUtil.checkNotNull(qosLevel, "qosLevel"));
    this.isRetain = isRetain;
    this.remainingLength = remainingLength;
  }
  
  public MqttMessageType messageType() {
    return messageType;
  }
  
  public boolean isDup() {
    return isDup;
  }
  
  public MqttQoS qosLevel() {
    return qosLevel;
  }
  
  public boolean isRetain() {
    return isRetain;
  }
  
  public int remainingLength() {
    return remainingLength;
  }
  
  public String toString()
  {
    return 
    





      StringUtil.simpleClassName(this) + '[' + "messageType=" + messageType + ", isDup=" + isDup + ", qosLevel=" + qosLevel + ", isRetain=" + isRetain + ", remainingLength=" + remainingLength + ']';
  }
}
